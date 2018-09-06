package com.mobiledgex.sdkdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mobiledgex.matchingengine.FindCloudletResponse;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.MatchingEngineRequest;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import io.grpc.StatusRuntimeException;

import static com.mobiledgex.sdkdemo.MainActivity.HOSTNAME;

public class MatchingEngineHelper {
    private static final String TAG = "MatchingEngineHelper";
    private final Context mContext;
    private final View mView;

    private MatchingEngineResultsListener mMatchingEngineResultsListener;
    private Location mSpoofLocation = null;

    private FindCloudletResponse mClosestCloudlet;
    private MatchingEngine mMatchingEngine;
    private String someText = null;

    /**
     * Possible actions to perform with the matching engine.
     */
    enum RequestType {
        REQ_REGISTER_CLIENT,
        REQ_VERIFY_LOCATION,
        REQ_FIND_CLOUDLET,
        REQ_GET_CLOUDLETS
    }

    public MatchingEngineHelper(Context context, View view) {
        mContext = context;
        mView = view;

        mMatchingEngine = new MatchingEngine(mContext);

        //For testing on a phone without a SIM card
        mMatchingEngine.setNetworkSwitchingEnabled(false);
//        mMatchingEngine.setSSLEnabled(false);
    }

    /**
     * This method performs several actions with the matching engine in the background,
     * one after the other:
     * <ol>
     *     <li>registerClient</li>
     *     <li>verifyLocation</li>
     *     <li>findCloudlet</li>
     * </ol>
     *
     * @param location  The location to pass to the matching engine.
     */
    public void doEnhancedLocationUpdateInBackground (final Location location) {
        final Activity ctx = (Activity) mContext;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "doEnhancedLocationUpdateInBackground AsyncTask run(). location="+location);
                if(location == null) {
                    Log.w(TAG, "location is null. Aborting.");
                    return;
                }

                try {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                    boolean mexAllowed = prefs.getBoolean(mContext.getResources().getString(R.string.preference_mex_location_verification), false);

                    if(!mexAllowed) {
                        Snackbar snackbar = Snackbar.make(mView, "Enhanced Location not enabled", Snackbar.LENGTH_LONG);
                        snackbar.setAction("Settings", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(mContext, SettingsActivity.class);
                                mContext.startActivity(intent);
                            }
                        });
                        snackbar.show();
                        return;
                    }

                    // Create a request:
                    //MatchingEngineRequest req = mMatchingEngine.createRequest(ctx, location); // Regular use case.
                    String host = HOSTNAME; // Override host.
                    int port = mMatchingEngine.getPort(); // Keep same port.
                    String carrierName = "TDG";
                    String devName = "MobiledgeX SDK Demo"; //TODO: In the current demo config, this matches the appName.

                    MatchingEngineRequest req = mMatchingEngine.createRequest(ctx, host, port, carrierName, devName, location);
                    AppClient.Match_Engine_Status registerStatus = mMatchingEngine.registerClient(req, 10000);
                    if (registerStatus.getStatus() != AppClient.Match_Engine_Status.ME_Status.ME_SUCCESS) {
                        someText = "Registration Failed. Error: " + registerStatus.getStatus();
                        Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    req = mMatchingEngine.createRequest(ctx, host, port, carrierName, devName, location);;
                    if (req != null) {
                        // Location Verification (Blocking, or use verifyLocationFuture):
                        AppClient.Match_Engine_Loc_Verify verifiedLocation = mMatchingEngine.verifyLocation(req, 10000);
                        someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                                ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                                ", Location Accuracy: " + verifiedLocation.getGPSLocationAccuracyKM() + " ]\n";

                        if(mMatchingEngineResultsListener != null) {
                            mMatchingEngineResultsListener.onVerifyLocation(verifiedLocation.getGpsLocationStatus(),
                                    verifiedLocation.getGPSLocationAccuracyKM());
                        }

                        // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
                        mClosestCloudlet = mMatchingEngine.findCloudlet(req, 10000);
                        Log.i(TAG, "mClosestCloudlet.uri="+mClosestCloudlet.uri);
                        ctx.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mMatchingEngineResultsListener != null) {
                                    mMatchingEngineResultsListener.onFindCloudlet(mClosestCloudlet);
                                }
                            }
                        });

                        someText += "[Cloudlet Server: URI: [" + mClosestCloudlet.uri + "], Port: " + mClosestCloudlet.port + "]";
                    } else {
                        someText = "Cannot create request object.";
                        if (!mexAllowed) {
                            someText += " Reason: Enhanced location is disabled.";
                        }
                        Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                    }
                    Log.i(TAG, "0. someText=" + someText);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    toastOnUiThread(ioe.getMessage(), Toast.LENGTH_LONG);
                } catch (StatusRuntimeException sre) {
                    sre.printStackTrace();
                    toastOnUiThread(sre.getMessage(), Toast.LENGTH_LONG);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                    toastOnUiThread(iae.getMessage(), Toast.LENGTH_LONG);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    toastOnUiThread(ie.getMessage(), Toast.LENGTH_LONG);
                } catch (ExecutionException ee) {
                    ee.printStackTrace();
                    toastOnUiThread(ee.getMessage(), Toast.LENGTH_LONG);
                }
            }
        });

    }

    /**
     * This method does a single matching engine action in the background, determined by the
     * reqType parameter. {@link RequestType}
     *
     * @param reqType  The request type.
     * @param location  The location to pass to the matching engine.
     */
    public void doRequestInBackground (final RequestType reqType, final Location location) {
        Log.i(TAG, "doRequestInBackground() reqType="+reqType);
        final Activity ctx = (Activity) mContext;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean mexAllowed = prefs.getBoolean(mContext.getResources().getString(R.string.preference_mex_location_verification), false);

        if(!mexAllowed) {
            Snackbar snackbar = Snackbar.make(mView, "Enhanced Location not enabled", Snackbar.LENGTH_LONG);
            snackbar.setAction("Settings", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, SettingsActivity.class);
                    mContext.startActivity(intent);
                }
            });
            snackbar.show();
            return;
        }

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "doRequestInBackground AsyncTask run(). location="+location);
                if(location == null) {
                    Log.w(TAG, "location is null. Aborting.");
                    return;
                }

                // Create a request:
                try {
                    String host = HOSTNAME; // Override host.
                    int port = mMatchingEngine.getPort(); // Keep same port.
                    String carrierName = "TDG";
                    String devName = "MobiledgeX SDK Demo";

                    MatchingEngineRequest req = mMatchingEngine.createRequest(ctx, host, port, carrierName, devName, location);
                    AppClient.Match_Engine_Status registerStatus = mMatchingEngine.registerClient(req, 10000);
                    if (registerStatus.getStatus() != AppClient.Match_Engine_Status.ME_Status.ME_SUCCESS) {
                        someText = "Registration Failed. Error: " + registerStatus.getStatus();
                        Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Log.i(TAG, "SessionCookie:" + registerStatus.getSessionCookie());
                    if(reqType == RequestType.REQ_REGISTER_CLIENT) {
                        Log.i(TAG, "REQ_REGISTER_CLIENT only.");
                        mMatchingEngineResultsListener.onRegister(registerStatus.getSessionCookie());
                        return;
                    }

                    req = mMatchingEngine.createRequest(ctx, host, port, carrierName, devName, location);
                    if (req == null) {
                        someText = "Cannot create request object.";
                        Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                    }

                    switch (reqType) {
                        case REQ_VERIFY_LOCATION:
                            // Location Verification (Blocking, or use verifyLocationFuture):
                            AppClient.Match_Engine_Loc_Verify verifiedLocation = mMatchingEngine.verifyLocation(req, 10000);
                            someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                                    ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                                    ", Location Accuracy: " + verifiedLocation.getGPSLocationAccuracyKM() + " ]\n";

                            if(mMatchingEngineResultsListener != null) {
                                mMatchingEngineResultsListener.onVerifyLocation(verifiedLocation.getGpsLocationStatus(),
                                        verifiedLocation.getGPSLocationAccuracyKM());
                            }
                            break;

                        case REQ_FIND_CLOUDLET:
                            // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
                            mClosestCloudlet = mMatchingEngine.findCloudlet(req, 10000);
                            Log.i(TAG, "mClosestCloudlet.uri="+mClosestCloudlet.uri);
                            if(mMatchingEngineResultsListener != null) {
                                mMatchingEngineResultsListener.onFindCloudlet(mClosestCloudlet);
                            }
                            break;

                        case REQ_GET_CLOUDLETS:
                            // Location Verification (Blocking, or use verifyLocationFuture):
                            AppClient.Match_Engine_Cloudlet_List cloudletList = mMatchingEngine.getCloudletList(req, 10000);
                            if(mMatchingEngineResultsListener != null) {
                                mMatchingEngineResultsListener.onGetCloudletList(cloudletList);
                            }
                            break;

                        default:
                            Log.e(TAG, "Unknown reqType: "+reqType);

                    }

                    Log.i(TAG, "someText=" + someText);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    toastOnUiThread(ioe.getMessage(), Toast.LENGTH_LONG);
                } catch (StatusRuntimeException sre) {
                    sre.printStackTrace();
                    toastOnUiThread(sre.getMessage(), Toast.LENGTH_LONG);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                    toastOnUiThread(iae.getMessage(), Toast.LENGTH_LONG);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    toastOnUiThread(ie.getMessage(), Toast.LENGTH_LONG);
                } catch (ExecutionException ee) {
                    ee.printStackTrace();
                    toastOnUiThread(ee.getMessage(), Toast.LENGTH_LONG);
                }
            }
        });

    }

    /**
     * Utility method to create a toast on the UI thread.
     *
     * @param message
     * @param length
     */
    public void toastOnUiThread(final String message, final int length) {
        ((Activity)mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, length).show();
            }
        });
    }

    public MatchingEngine getMatchingEngine() {
        return mMatchingEngine;
    }

    public void setMatchingEngine(MatchingEngine mMatchingEngine) {
        this.mMatchingEngine = mMatchingEngine;
    }

    public MatchingEngineResultsListener getmMatchingEngineResultsListener() {
        return mMatchingEngineResultsListener;
    }

    public void setMatchingEngineResultsListener(MatchingEngineResultsListener mMatchingEngineResultsListener) {
        this.mMatchingEngineResultsListener = mMatchingEngineResultsListener;
    }

    public Location getSpoofedLocation() {
        return mSpoofLocation;
    }

    public void setSpoofedLocation(Location mSpoofLocation) {
        Log.i(TAG, "setSpoofedLocation("+mSpoofLocation+")");
        this.mSpoofLocation = mSpoofLocation;
    }
}
