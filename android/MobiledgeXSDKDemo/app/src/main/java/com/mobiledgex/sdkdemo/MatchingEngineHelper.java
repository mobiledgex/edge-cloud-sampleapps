/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiledgex.sdkdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mobiledgex.matchingengine.ChannelIterator;
import com.mobiledgex.matchingengine.MatchingEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import io.grpc.StatusRuntimeException;

import static com.mobiledgex.sdkdemo.MainActivity.mAppName;
import static com.mobiledgex.sdkdemo.MainActivity.mAppVersion;
import static com.mobiledgex.sdkdemo.MainActivity.mCarrierName;
import static com.mobiledgex.sdkdemo.MainActivity.mHostname;
import static com.mobiledgex.sdkdemo.MainActivity.mOrgName;
import static com.mobiledgex.sdkdemo.MainActivity.mAppInstancesLimit;
import static com.mobiledgex.sdkdemo.MainActivity.mFindCloudletMode;

public class MatchingEngineHelper {
    private static final String TAG = "MatchingEngineHelper";
    private final Context mContext;
    private final View mView;

    private MatchingEngineResultsInterface mMatchingEngineResultsInterface;
    private Location mSpoofLocation = null;

    private AppClient.FindCloudletReply mClosestCloudlet;
    private MatchingEngine mMatchingEngine;
    private String someText = null;

    /**
     * Possible actions to perform with the matching engine.
     */
    enum RequestType {
        REQ_REGISTER_CLIENT,
        REQ_VERIFY_LOCATION,
        REQ_FIND_CLOUDLET,
        REQ_GET_CLOUDLETS,
        REQ_DO_ALL,
        REQ_GET_QOS
    }

    public MatchingEngineHelper(Context context, View view) {
        Log.i(TAG, "MatchingEngineHelper()");
        mContext = context;
        mView = view;
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

    public void registerClientInBackground() {
        new BackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, RequestType.REQ_REGISTER_CLIENT, null);
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

    public void doQosRequestInBackground (ArrayList<AppClient.QosPosition> kpiPositions) {
        Location dummyLocation = new Location("Dummy");
        new BackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, RequestType.REQ_GET_QOS, dummyLocation, kpiPositions);
    }

    public class BackgroundRequest extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {
            RequestType reqType = (RequestType) params[0];
            Location location = (Location) params[1];
            Log.i(TAG, "BackgroundRequest reqType="+reqType);
            final Activity ctx = (Activity) mContext;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
            boolean locationVerificationAllowed = prefs.getBoolean(mContext.getResources().getString(R.string.preference_matching_engine_location_verification), false);

            if(!locationVerificationAllowed) {
                Snackbar snackbar = Snackbar.make(mView, "Enhanced Location not enabled", Snackbar.LENGTH_LONG);
                snackbar.setAction("Settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, SettingsActivity.class);
                        intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.MatchingEngineSettingsFragment.class.getName() );
                        intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
                        mContext.startActivity(intent);
                    }
                });
                snackbar.show();
                return null;
            }

            // In a real Edge-enabled app, if no carrierName, or active Subscription networks,
            // the app should use the public cloud instead.
            // For this demo app, we may want to test over wifi, which we can allow by disabling
            // the "Network Switching Enabled" preference.
            boolean netSwitchingAllowed = prefs.getBoolean(mContext.getResources().getString(R.string.preference_net_switching_allowed), false);
            if(netSwitchingAllowed) {
                boolean carrierNameFound = false;
                List<SubscriptionInfo> subList = mMatchingEngine.getActiveSubscriptionInfoList();
                if (subList != null && subList.size() > 0) {
                    for (SubscriptionInfo info : subList) {
                        if (info.getCarrierName().length() > 0) {
                            carrierNameFound = true;
                        }
                    }
                }
                if(!carrierNameFound) {
                    Snackbar snackbar = Snackbar.make(mView, "No valid SIM card. Disable \"Network Switching Enabled\" to continue.", Snackbar.LENGTH_LONG);
                    snackbar.setAction("Settings", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(mContext, SettingsActivity.class);
                            intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.MatchingEngineSettingsFragment.class.getName() );
                            intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
                            mContext.startActivity(intent);
                        }
                    });
                    snackbar.show();
                    return null;
                }
            }

            // Create a request:
            try {
                String host = mHostname; // Override host.
                int port = mMatchingEngine.getPort(); // Keep same port.
                //Note that mCarrierName came from preferences in MainActivity.

                boolean reportCookie = false;

                if(reqType == RequestType.REQ_REGISTER_CLIENT) {
                    reportCookie = true;
                } else {
                    // For all other cases, we must have a location.
                    if(location == null) {
                        Log.w(TAG, "location is null. Aborting.");
                        return null;
                    }
                }
                Log.i(TAG, "mHost:" + host);

                if (!registerClient(host, port, reportCookie)) {
                    return null;
                }

                switch (reqType) {
                    case REQ_REGISTER_CLIENT:
                        //Register already occurred above. Do nothing.
                        break;

                    case REQ_VERIFY_LOCATION:
                        verifyLocation(location, host, port);
                        break;

                    case REQ_FIND_CLOUDLET:
                        findCloudlet(location, host, port);
                        break;

                    case REQ_GET_CLOUDLETS:
                        getAppInstList(location, host, port);
                        break;

                    case REQ_DO_ALL:
                        //In this case, we do all actions in order as long as each one is successful.
                        if(!getAppInstList(location, host, port)) {
                            Log.e(TAG, "getAppInstList failed. aborting REQ_DO_ALL");
                            return null;
                        }
                        if(!verifyLocation(location, host, port)) {
                            Log.e(TAG, "verifyLocation failed. aborting REQ_DO_ALL");
                            return null;
                        }
                        if(!findCloudlet(location, host, port)) {
                            Log.e(TAG, "findCloudlet failed. aborting REQ_DO_ALL");
                            return null;
                        }
                        break;

                    case REQ_GET_QOS:
                        ArrayList<AppClient.QosPosition> positions = (ArrayList<AppClient.QosPosition>) params[2];
                        getQosPositionKpi(positions, host, port);
                        break;

                    default:
                        Log.e(TAG, "Unknown reqType: "+reqType);
                }

                Log.i(TAG, "someText=" + someText);
            } catch (IOException | StatusRuntimeException | IllegalArgumentException
                    | ExecutionException | InterruptedException ex) {
                ex.printStackTrace();
                toastOnUiThread(ex.getMessage(), Toast.LENGTH_LONG);
            }
            return null;
        }
    }

    private boolean registerClient(String host, int port, boolean reportCookie)
            throws InterruptedException, ExecutionException {
        AppClient.RegisterClientRequest registerClientRequest;
        try {
            registerClientRequest =
                    mMatchingEngine.createDefaultRegisterClientRequest(mContext, mOrgName)
                            .setCarrierName(mCarrierName)
                            .setAppName(mAppName)
                            .setAppVers(mAppVersion).build();
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, "NameNotFoundException in create default register client request. " + nnfe.getMessage());
            return false;
        }
        Log.i(TAG, "registerClientRequest: host="+host+" port="+port
                +" getAppName()="+registerClientRequest.getAppName()
                +" getAppVers()="+registerClientRequest.getAppVers()
                +" getOrgName()="+registerClientRequest.getOrgName()
                +" getCarrierName()="+registerClientRequest.getCarrierName());
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
            if(mMatchingEngineResultsInterface != null) {
                mMatchingEngineResultsInterface.onRegister(registerStatus.getSessionCookie());
            }
        }
        return true;
    }

    private boolean getAppInstList(Location location, String host, int port) throws InterruptedException, ExecutionException {
        // getAppInstList (Blocking, or use getAppInstListFuture):
        AppClient.AppInstListRequest appInstListRequest
                = mMatchingEngine.createDefaultAppInstListRequest(mContext, location)
                .setCarrierName(mCarrierName).setLimit(mAppInstancesLimit).build();
        if(appInstListRequest != null) {
            AppClient.AppInstListReply cloudletList = mMatchingEngine.getAppInstList(appInstListRequest,
                    host, port, 10000);
            Log.i(TAG, "cloudletList.getStatus()="+cloudletList.getStatus());
            if (cloudletList.getStatus() != AppClient.AppInstListReply.AIStatus.AI_SUCCESS) {
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

    private boolean findCloudlet(Location location, String host, int port) throws InterruptedException, ExecutionException {
        // Find the closest cloudlet for your application to use. (Blocking call, or use findCloudletFuture):
        AppClient.FindCloudletRequest findCloudletRequest = null;
        findCloudletRequest
                = mMatchingEngine.createDefaultFindCloudletRequest(mContext, location).setCarrierName(mCarrierName).build();
        if(findCloudletRequest != null) {
            mClosestCloudlet = mMatchingEngine.findCloudlet(findCloudletRequest,
                    host, port, 10000, mFindCloudletMode);
            if(mClosestCloudlet.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
                someText = "findCloudlet Failed. Error: " + mClosestCloudlet.getStatus();
                Log.e(TAG, someText);
                Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
                return false;
            }
            Log.i(TAG, "REQ_FIND_CLOUDLET mClosestCloudlet fqdn=" + mClosestCloudlet.getFqdn()+" location="+mClosestCloudlet.getCloudletLocation().getLatitude()+","+mClosestCloudlet.getCloudletLocation().getLongitude());
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

    private boolean verifyLocation(Location location, String host, int port) throws InterruptedException, IOException, ExecutionException {
        // Location Verification (Blocking, or use verifyLocationFuture):
        AppClient.VerifyLocationRequest verifyRequest =
                mMatchingEngine.createDefaultVerifyLocationRequest(mContext, location).setCarrierName(mCarrierName).build();
        if (verifyRequest != null) {
            AppClient.VerifyLocationReply verifiedLocation =
                    mMatchingEngine.verifyLocation(verifyRequest, host, port, 10000);
            someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                    ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                    ", Location Accuracy: " + verifiedLocation.getGpsLocationAccuracyKm() + " ]\n";

            if (mMatchingEngineResultsInterface != null) {
                mMatchingEngineResultsInterface.onVerifyLocation(verifiedLocation.getGpsLocationStatus(),
                        verifiedLocation.getGpsLocationAccuracyKm());
            }
        } else {
            someText = "Cannot create VerifyLocationRequest object.\n";
            Log.e(TAG, someText);
            Snackbar.make(mView, someText, Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private boolean getQosPositionKpi(ArrayList<AppClient.QosPosition> positions, String host, int port) throws ExecutionException, InterruptedException {

        Log.i(TAG, "me="+mMatchingEngine+" host="+host+" port="+port);
        AppClient.QosPositionRequest request = mMatchingEngine.createDefaultQosPositionRequest(positions, 0, null).build();
        ChannelIterator<AppClient.QosPositionKpiReply> responseIterator = mMatchingEngine.getQosPositionKpi(request,
                host, port, 10000);
        // A stream of QosPositionKpiReply(s), with a non-stream block of responses.
        long total = 0;
        while (responseIterator.hasNext()) {
            AppClient.QosPositionKpiReply aR = responseIterator.next();
            for (int i = 0; i < aR.getPositionResultsCount(); i++) {
                Log.i(TAG, aR.getPositionResults(i).toString());
            }
            total += aR.getPositionResultsCount();
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
}
