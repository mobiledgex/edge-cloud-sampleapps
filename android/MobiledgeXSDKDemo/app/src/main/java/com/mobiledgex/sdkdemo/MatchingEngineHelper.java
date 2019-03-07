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

import com.mobiledgex.matchingengine.MatchingEngine;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import io.grpc.StatusRuntimeException;

public class MatchingEngineHelper {
    private static final String TAG = "MatchingEngineHelper";
    private final Context mContext;
    private final View mView;

    private MatchingEngineResultsInterface mMatchingEngineResultsInterface;
    private Location mSpoofLocation = null;

    private AppClient.FindCloudletReply mClosestCloudlet;
    private MatchingEngine mMatchingEngine;
    private String someText = null;
    private String mHostname;

    /**
     * Possible actions to perform with the matching engine.
     */
    enum RequestType {
        REQ_REGISTER_CLIENT,
        REQ_VERIFY_LOCATION,
        REQ_FIND_CLOUDLET,
        REQ_GET_CLOUDLETS,
        REQ_DO_ALL
    }

    public MatchingEngineHelper(Context context, String hostname, View view) {
        Log.i(TAG, "MatchingEngineHelper mHostname="+hostname);
        mContext = context;
        mView = view;
        mHostname = hostname;
        mMatchingEngine = new MatchingEngine(mContext);
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
        new BackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, RequestType.REQ_DO_ALL, location);
    }

    /**
     * This method does a single matching engine action in the background, determined by the
     * reqType parameter. {@link RequestType}
     *
     * @param reqType  The request type.
     * @param location  The location to pass to the matching engine.
     */
    public void doRequestInBackground (final RequestType reqType, final Location location) {
        new BackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, reqType, location);
    }

    public class BackgroundRequest extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            RequestType reqType = (RequestType) params[0];
            Location location = (Location) params[1];
            Log.i(TAG, "BackgroundRequest reqType="+reqType+" location="+location);
            if(location == null) {
                Log.w(TAG, "location is null. Aborting.");
                return null;
            }
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
                return null;
            }

            // Create a request:
            try {
                String host = mHostname; // Override host.
                int port = mMatchingEngine.getPort(); // Keep same port.
                String carrierName = "TDG";
                String devName = "MobiledgeX SDK Demo"; //TODO: In the current demo config, this matches the appName.
                String appVersion = ""; //SDK will populate this automatically if we pass in "".
                boolean reportCookie = false;

                if(reqType == RequestType.REQ_REGISTER_CLIENT) {
                    reportCookie = true;
                }
                Log.i(TAG, "host:" + host);

                if (!registerClient(ctx, host, port, devName, appVersion, carrierName, reportCookie)) {
                    return null;
                }

                switch (reqType) {
                    case REQ_VERIFY_LOCATION:
                        verifyLocation(location, ctx, host, port, carrierName);
                        break;

                    case REQ_FIND_CLOUDLET:
                        findCloudlet(location, ctx, host, port, carrierName);
                        break;

                    case REQ_GET_CLOUDLETS:
                        getAppInstList(location, ctx, host, port, carrierName);
                        break;

                    case REQ_DO_ALL:
                        //In this case, we do all actions in order as long as each one is successful.
                        if(!getAppInstList(location, ctx, host, port, carrierName)) {
                            Log.e(TAG, "getAppInstList failed. aborting REQ_DO_ALL");
                            return null;
                        }
                        if(!verifyLocation(location, ctx, host, port, carrierName)) {
                            Log.e(TAG, "verifyLocation failed. aborting REQ_DO_ALL");
                            return null;
                        }
                        if(!findCloudlet(location, ctx, host, port, carrierName)) {
                            Log.e(TAG, "findCloudlet failed. aborting REQ_DO_ALL");
                            return null;
                        }
                        break;

                    default:
                        Log.e(TAG, "Unknown reqType: "+reqType);
                }

                Log.i(TAG, "someText=" + someText);
            } catch (IOException | StatusRuntimeException | IllegalArgumentException
                    | ExecutionException | InterruptedException ioe) {
                ioe.printStackTrace();
                toastOnUiThread(ioe.getMessage(), Toast.LENGTH_LONG);
            }
            return null;
        }
    }

    private boolean registerClient(Activity ctx, String host, int port, String devName, String appVersion,
                                   String carrierName, boolean reportCookie) throws InterruptedException, ExecutionException {
        AppClient.RegisterClientRequest registerClientRequest =
                mMatchingEngine.createRegisterClientRequest(ctx,
                        devName, "", appVersion, carrierName, null);
        Log.i(TAG, "registerClientRequest.getAppVers()=["+registerClientRequest.getAppVers()+"] registerClientRequest.getAppName()="+registerClientRequest.getAppName());
        AppClient.RegisterClientReply registerStatus =
                mMatchingEngine.registerClient(registerClientRequest,
                        host, port, 10000);
        if (registerStatus.getStatus() != AppClient.ReplyStatus.RS_SUCCESS) {
            someText = "Registration Failed. Error: " + registerStatus.getStatus();
            Log.e(TAG, someText);
            Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            return false;
        }
        Log.i(TAG, "SessionCookie:" + registerStatus.getSessionCookie());
        if(reportCookie) {
            Log.i(TAG, "REQ_REGISTER_CLIENT only.");
            mMatchingEngineResultsInterface.onRegister(registerStatus.getSessionCookie());
        }
        return true;
    }

    private boolean getAppInstList(Location location, Activity ctx, String host, int port, String carrierName) throws InterruptedException, ExecutionException {
        // Location Verification (Blocking, or use verifyLocationFuture):
        Log.i(TAG, "mMatchingEngine getHost()="+mMatchingEngine.getHost());
        AppClient.AppInstListRequest appInstListRequest =
                mMatchingEngine.createAppInstListRequest(ctx, carrierName, location);
        if(appInstListRequest != null) {
            AppClient.AppInstListReply cloudletList = mMatchingEngine.getAppInstList(appInstListRequest,
                    host, port, 10000);
            Log.i(TAG, "cloudletList.getStatus()="+cloudletList.getStatus());
            if (cloudletList.getStatus() != AppClient.AppInstListReply.AI_Status.AI_SUCCESS) {
                someText = "Cannot create AppInstListRequest object. Status="+cloudletList.getStatus()+"\n";
                Log.e(TAG, someText);
                Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                return false;
            }
            Log.i(TAG, "REQ_GET_CLOUDLETS cloudletList.getCloudletsCount()=" + cloudletList.getCloudletsCount());
            if (mMatchingEngineResultsInterface != null) {
                mMatchingEngineResultsInterface.onGetCloudletList(cloudletList);
            }
        } else {
            someText = "Cannot create AppInstListRequest object.\n";
            Log.e(TAG, someText);
            Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private boolean findCloudlet(Location location, Activity ctx, String host, int port, String carrierName) throws InterruptedException, ExecutionException {
        // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
        AppClient.FindCloudletRequest findCloudletRequest =
                mMatchingEngine.createFindCloudletRequest(ctx, carrierName, location);
        if(findCloudletRequest != null) {
            mClosestCloudlet = mMatchingEngine.findCloudlet(findCloudletRequest,
                    host, port, 10000);
            if(mClosestCloudlet.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
                someText = "findCloudlet Failed. Error: " + mClosestCloudlet.getStatus();
                Log.e(TAG, someText);
                Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                return false;
            }
            Log.i(TAG, "REQ_FIND_CLOUDLET mClosestCloudlet.uri=" + mClosestCloudlet.getFQDN());
            if (mMatchingEngineResultsInterface != null) {
                mMatchingEngineResultsInterface.onFindCloudlet(mClosestCloudlet);
            }
        } else {
            someText = "Cannot create FindCloudletRequest object.\n";
            Log.e(TAG, someText);
            Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private boolean verifyLocation(Location location, Activity ctx, String host, int port, String carrierName) throws InterruptedException, IOException, ExecutionException {
        // Location Verification (Blocking, or use verifyLocationFuture):
        AppClient.VerifyLocationRequest verifyRequest =
                mMatchingEngine.createVerifyLocationRequest(ctx, carrierName, location);
        if (verifyRequest != null) {
            AppClient.VerifyLocationReply verifiedLocation =
                    mMatchingEngine.verifyLocation(verifyRequest, host, port, 10000);
            someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                    ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                    ", Location Accuracy: " + verifiedLocation.getGPSLocationAccuracyKM() + " ]\n";

            if (mMatchingEngineResultsInterface != null) {
                mMatchingEngineResultsInterface.onVerifyLocation(verifiedLocation.getGpsLocationStatus(),
                        verifiedLocation.getGPSLocationAccuracyKM());
            }
        } else {
            someText = "Cannot create VerifyLocationRequest object.\n";
            Log.e(TAG, someText);
            Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
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

    public MatchingEngineResultsInterface getmMatchingEngineResultsInterface() {
        return mMatchingEngineResultsInterface;
    }

    public void setMatchingEngineResultsListener(MatchingEngineResultsInterface mMatchingEngineResultsInterface) {
        this.mMatchingEngineResultsInterface = mMatchingEngineResultsInterface;
    }

    public Location getSpoofedLocation() {
        return mSpoofLocation;
    }

    public void setSpoofedLocation(Location mSpoofLocation) {
        Log.i(TAG, "setSpoofedLocation("+mSpoofLocation+")");
        this.mSpoofLocation = mSpoofLocation;
    }

    public String getHostname() {
        return mHostname;
    }

    public void setHostname(String mHostname) {
        this.mHostname = mHostname;
    }
}
