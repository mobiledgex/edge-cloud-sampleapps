package com.mobiledgex.sdkdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mobiledgex.matchingengine.FindCloudletResponse;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.util.RequestPermissions;

import java.io.IOException;

import distributed_match_engine.AppClient;
import io.grpc.StatusRuntimeException;

public class MatchingEngineHelper {
    private static final String TAG = "MatchingEngineHelper";
    private final Context mContext;
    private final View mView;

    private MatchingEngineResultsListener mMatchingEngineResultsListener;
    private Location mSpoofLocation = null;

    private FindCloudletResponse mClosestCloudlet;
    private MatchingEngine mMatchingEngine;
    private String someText = null;

    public MatchingEngineHelper(Context context, View view) {
        mContext = context;
        mView = view;

        mMatchingEngine = new MatchingEngine();
//        mMatchingEngine.setHost("75.35.136.40");
//        mMatchingEngine.setHost("tdg.dme.mobiledgex.net");
//        mMatchingEngine.setHost("35.199.188.102");
        mMatchingEngine.setHost("acrotopia.com");
        mMatchingEngine.setPort(50051);
    }

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

                // Create a request:

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

                    AppClient.Match_Engine_Request req = mMatchingEngine.createRequest(ctx, location);
                    AppClient.Match_Engine_Status registerStatus = mMatchingEngine.registerClient(req, 10000);
                    if (registerStatus.getStatus() != AppClient.Match_Engine_Status.ME_Status.ME_SUCCESS) {
                        someText = "Registration Failed. Error: " + registerStatus.getStatus();
                        Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    req = mMatchingEngine.createRequest(ctx, location);
                    if (req != null) {
                        // Location Verification (Blocking, or use verifyLocationFuture):
                        AppClient.Match_Engine_Loc_Verify verifiedLocation = mMatchingEngine.verifyLocation(req, 10000);
                        someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                                ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                                ", Location Accuracy: " + verifiedLocation.getGPSLocationAccuracyKM() + " ]\n";

                        ctx.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mMatchingEngineResultsListener != null) {
                                    mMatchingEngineResultsListener.onVerifyLocation(someText.contains("LOC_VERIFIED"));
                                }
                            }
                        });

                        // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
                        mClosestCloudlet = mMatchingEngine.findCloudlet(req, 10000);
                        // FIXME: It's not possible to get a complete http(s) URI on just a service IP + port!
                        String serverip = null;
                        if (mClosestCloudlet.service_ip != null && mClosestCloudlet.service_ip.length > 0) {
                            serverip = mClosestCloudlet.service_ip[0] + ", ";
                            for (int i = 1; i < mClosestCloudlet.service_ip.length - 1; i++) {
                                serverip += mClosestCloudlet.service_ip[i] + ", ";
                            }
                            serverip += mClosestCloudlet.service_ip[mClosestCloudlet.service_ip.length - 1];
                        }
                        ctx.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mMatchingEngineResultsListener != null) {
                                    mMatchingEngineResultsListener.onFindCloudlet(mClosestCloudlet);
                                }
                            }
                        });

                        someText += "[Cloudlet Server: URI: [" + mClosestCloudlet.uri + "], Serverip: [" + serverip + "], Port: " + mClosestCloudlet.port + "]";
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
                } catch (StatusRuntimeException sre) {
                    sre.printStackTrace();
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }
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
