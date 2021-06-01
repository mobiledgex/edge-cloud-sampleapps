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

package com.mobiledgex.matchingenginehelper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.eventbus.Subscribe;
import com.mobiledgex.matchingengine.ChannelIterator;
import com.mobiledgex.matchingengine.DmeDnsException;
import com.mobiledgex.matchingengine.EdgeEventsConnection;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingengine.edgeeventsconfig.EdgeEventsConfig;
import com.mobiledgex.matchingengine.edgeeventsconfig.FindCloudletEvent;
import com.mobiledgex.matchingengine.performancemetrics.NetTest;
import com.mobiledgex.matchingengine.performancemetrics.Site;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;
import distributed_match_engine.LocOuterClass;

import static distributed_match_engine.AppClient.ServerEdgeEvent.ServerEventType.EVENT_APPINST_HEALTH;

public class MatchingEngineHelper implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MatchingEngineHelper";
    private static final int DEFAULT_LATENCY_PORT = 8085;
    private Activity mActivity;
    private final View mView;
    private EdgeEventsSubscriber mEdgeEventsSubscriber;
    public int mDmePort = 50051;
    public static final String DEFAULT_DME_HOSTNAME = "wifi.dme.mobiledgex.net";
    public static final String DEFAULT_CARRIER_NAME = "";
    public static final String DEF_HOSTNAME_PLACEHOLDER = "Default";
    public static final String DEFAULT_FIND_CLOUDLET_MODE = "PROXIMITY";
    public static final int DEFAULT_APP_INSTANCES_LIMIT = 4;
    private String mDefaultCarrierName;
    private String mDefaultDmeHostname;
    public String mDmeHostname;
    public String mCarrierName;
    public String mAppName;
    public String mAppVersion;
    public String mOrgName;
    public  MatchingEngine.FindCloudletMode mFindCloudletMode;
    private boolean mNetworkSwitchingAllowed;
    protected int mAppInstancesLimit;

    public Location mLastKnownLocation;
    public Location mLocationInSimulator;
    public String mClosestCloudletHostname;
    protected boolean mRegisterClientComplete;
    public AppClient.FindCloudletReply mClosestCloudlet;
    public AppClient.AppInstListReply mAppInstanceReplyList;
    public boolean mAllowLocationSimulatorUpdate = false;

    private final MatchingEngineHelperInterface meHelperInterface;
    private Location mSpoofedLocation = null;

    private MatchingEngine me;
    private String someText = null;
    public String mAppInstHostname;
    public boolean mAppInstTls;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private boolean mGpsInitialized;
    private boolean mRunConnectionTests = true;

    public static class Builder {
        private Activity activity;
        private View view;
        private MatchingEngineHelperInterface meHelperInterface;

        public Builder setActivity(Activity activity) {
            this.activity = activity;
            return this;
        }
        public Builder setMeHelperInterface(MatchingEngineHelperInterface meHelperInterface) {
            this.meHelperInterface = meHelperInterface;
            return this;
        }
        public Builder setView(View view) {
            this.view = view;
            return this;
        }

        public MatchingEngineHelper build() {
            return new MatchingEngineHelper(this);
        }
    }

    public MatchingEngineHelper(final Builder builder) {
        Log.i(TAG, "MatchingEngineHelper()");
        mActivity = builder.activity;
        mView = builder.view;
        meHelperInterface = builder.meHelperInterface;
        me = new MatchingEngine(mActivity);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(builder.activity);
        Resources resources = mActivity.getResources();

        // Reuse the onSharedPreferenceChanged code to initialize anything dependent on these prefs:
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_matching_engine_location_verification));
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_app_name));
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_app_version));
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_org_name));
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_default_dme_hostname));
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_default_operator_name));
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_find_cloudlet_mode));
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_app_instances_limit));
        onSharedPreferenceChanged(prefs, resources.getString(R.string.pref_default_app_definition));

        Log.i(TAG, "mDmeHostname="+ mDmeHostname +" networkSwitchingAllowed="+mNetworkSwitchingAllowed+" mCarrierName="+mCarrierName);

        // Watch for any updated preferences:
        prefs.registerOnSharedPreferenceChangeListener(this);

        mEdgeEventsSubscriber = new EdgeEventsSubscriber();
        me.getEdgeEventsBus().register(mEdgeEventsSubscriber);

        // Set a default Edge Events config.
        // There is also a parameterized version to further customize.
        EdgeEventsConfig backgroundEdgeEventsConfig = me.createDefaultEdgeEventsConfig();
        backgroundEdgeEventsConfig.latencyTestType = NetTest.TestType.CONNECT;
        // This is the internal port, that has not been remapped to a public port for a particular appInst.
        backgroundEdgeEventsConfig.latencyInternalPort = DEFAULT_LATENCY_PORT;
        backgroundEdgeEventsConfig.latencyUpdateConfig.maxNumberOfUpdates = 10; // Or Long.MAX_VALUE if you want. Default is 0.
        backgroundEdgeEventsConfig.latencyUpdateConfig.updateIntervalSeconds = 7; // The default is 30.
        backgroundEdgeEventsConfig.latencyThresholdTrigger = 300;
        backgroundEdgeEventsConfig.latencyTriggerTestMode = MatchingEngine.FindCloudletMode.PROXIMITY;

        me.startEdgeEvents(backgroundEdgeEventsConfig);
        String message = "Subscribed to ServerEdgeEvents";
        Log.i(TAG, message);
        meHelperInterface.showMessage(message);
    }

    /**
     * This method performs several actions with the matching engine in the background,
     * one after the other:
     * <ol>
     *     <li>registerClient</li>
     *     <li>getAppInstList</li>
     *     <li>verifyLocation</li>
     *     <li>findCloudlet</li>
     * </ol>
     *
     */
    public void doEnhancedLocationUpdateInBackground() {
        new Thread(() -> {
            try {
                if (mRegisterClientComplete || registerClient()) {
                    mRegisterClientComplete = true;
                } else {
                    Log.e(TAG, "registerClient failed. aborting doEnhancedLocationUpdateInBackground");
                    return;
                }
                // Do all actions in order as long as each one is successful.
                if (!getAppInstList()) {
                    Log.e(TAG, "getAppInstList failed. aborting doEnhancedLocationUpdateInBackground");
                    return;
                }
                if (!verifyLocation()) {
                    Log.e(TAG, "verifyLocation failed. aborting doEnhancedLocationUpdateInBackground");
                    return;
                }
                if (!findCloudlet()) {
                    Log.e(TAG, "findCloudlet failed. aborting doEnhancedLocationUpdateInBackground");
                    return;
                }

            } catch (ExecutionException | InterruptedException
                    | PackageManager.NameNotFoundException | IOException e) {
                Log.e(TAG, "Exception in getAppInstListInBackground() for "+
                        mAppName+", "+mAppVersion+", "+mOrgName);
                e.printStackTrace();
                meHelperInterface.showError(e.getLocalizedMessage());
            }
        }).start();
    }

    private boolean verifyMeRequirements() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean locationVerificationAllowed = prefs.getBoolean(mActivity.getResources().getString(R.string.pref_matching_engine_location_verification), false);
        if (!locationVerificationAllowed) {
            Snackbar snackbar = Snackbar.make(mView, "Enhanced Location not enabled", Snackbar.LENGTH_LONG);
            snackbar.setAction("Settings", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mActivity, SettingsActivity.class);
                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.MatchingEngineSettingsFragment.class.getName());
                    intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                    mActivity.startActivity(intent);
                }
            });
            snackbar.show();
            return false;
        }

        // In a real Edge-enabled app, if no carrierName, or active Subscription networks,
        // the app should use the public cloud instead.
        // For this demo app, we may want to test over wifi, which we can allow by disabling
        // the "Network Switching Enabled" preference.
        boolean netSwitchingAllowed = prefs.getBoolean(mActivity.getResources().getString(R.string.pref_net_switching_allowed), false);
        if (netSwitchingAllowed) {
            boolean carrierNameFound = false;
            List<SubscriptionInfo> subList = me.getActiveSubscriptionInfoList();
            if (subList != null && subList.size() > 0) {
                for (SubscriptionInfo info : subList) {
                    if (info.getCarrierName().length() > 0) {
                        carrierNameFound = true;
                    }
                }
            }
            if (!carrierNameFound) {
                Snackbar snackbar = Snackbar.make(mView, "No valid SIM card. Disable \"Network Switching Enabled\" to continue.", Snackbar.LENGTH_LONG);
                snackbar.setAction("Settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mActivity, SettingsActivity.class);
                        intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.MatchingEngineSettingsFragment.class.getName());
                        intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                        mActivity.startActivity(intent);
                    }
                });
                snackbar.show();
                return false;
            }
        }
        return true;
    }

    public boolean registerClient() throws ExecutionException, InterruptedException,
            io.grpc.StatusRuntimeException, PackageManager.NameNotFoundException {
        if (!verifyMeRequirements()) {
            String message = "Matching engine requirements not met. Please allow app permissions " +
                    "and enable Enhanced Location Verification in Matching Engine Settings";
            Log.e(TAG, message);
            meHelperInterface.showError(message);
            return false;
        }
        AppClient.RegisterClientRequest registerClientRequest;
        Future<AppClient.RegisterClientReply> registerReplyFuture;
        registerClientRequest = me.createDefaultRegisterClientRequest(mActivity, mOrgName)
                .setAppName(mAppName).setAppVers(mAppVersion).setCarrierName(mCarrierName).build();
        registerReplyFuture = me.registerClientFuture(registerClientRequest, mDmeHostname, mDmePort,10000);
        Log.i(TAG, "registerClientRequest="+registerClientRequest);
        Log.i(TAG, "registerClientRequest: "
                + " getAppName()=" + registerClientRequest.getAppName()
                + " getAppVers()=" + registerClientRequest.getAppVers()
                + " getOrgName()=" + registerClientRequest.getOrgName()
                + " getCarrierName()=" + registerClientRequest.getCarrierName());

        AppClient.RegisterClientReply reply = registerReplyFuture.get();
        Log.i(TAG, "registerStatus.getStatus()="+reply.getStatus());

        if (reply.getStatus() != AppClient.ReplyStatus.RS_SUCCESS) {
            String registerStatusText = "registerClient Failed. Error: " + reply.getStatus();
            Log.e(TAG, registerStatusText);
            meHelperInterface.showError(registerStatusText);
            return false;
        }
        Log.i(TAG, "SessionCookie:" + reply.getSessionCookie());
        meHelperInterface.onRegister();
        mRegisterClientComplete = true;
        return true;
    }

    public void registerClientInBackground() {
        new Thread(() -> {
            try {
                registerClient();
            } catch (ExecutionException | InterruptedException
                    | PackageManager.NameNotFoundException | IllegalArgumentException e) {
                Log.e(TAG, "Exception in findCloudletInBackground() for "+
                        mAppName+", "+mAppVersion+", "+mOrgName);
                e.printStackTrace();
                meHelperInterface.showError(e.getLocalizedMessage());
            }
        }).start();
    }

    public void findCloudletInBackground() {
        new Thread(() -> {
            try {
                if (mRegisterClientComplete || registerClient()) {
                    mRegisterClientComplete = true;
                } else {
                    return;
                }
                findCloudlet();
            } catch (ExecutionException | InterruptedException
                    | PackageManager.NameNotFoundException | IllegalArgumentException e) {
                Log.e(TAG, "Exception in findCloudletInBackground() for "+
                        mAppName+", "+mAppVersion+", "+mOrgName);
                e.printStackTrace();
                meHelperInterface.showError(e.getLocalizedMessage());
            }
        }).start();
    }

    public void getAppInstListInBackground() {
        Log.i(TAG, "getAppInstListInBackground mAppName="+mAppName+" mAppVersion="+mAppVersion+" mOrgName="+mOrgName);

        meHelperInterface.showMessage("Get App Instances for app "+mAppName+"...");
        new Thread(() -> {
            try {
                if (mRegisterClientComplete || registerClient()) {
                    mRegisterClientComplete = true;
                } else {
                    return;
                }
                getAppInstList();
            } catch (ExecutionException | InterruptedException | PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Exception in getAppInstListInBackground() for "+
                        mAppName+", "+mAppVersion+", "+mOrgName);
                e.printStackTrace();
                meHelperInterface.showError(e.getLocalizedMessage());
            }
        }).start();
    }

    public void verifyLocationInBackground() {
        new Thread(() -> {
            try {
                if (mRegisterClientComplete || registerClient()) {
                    mRegisterClientComplete = true;
                } else {
                    return;
                }
                verifyLocation();
            } catch (ExecutionException | InterruptedException
                    | PackageManager.NameNotFoundException
                    | IllegalArgumentException | IOException e) {
                Log.e(TAG, "Exception in findCloudletInBackground() for "+
                        mAppName+", "+mAppVersion+", "+mOrgName);
                e.printStackTrace();
                meHelperInterface.showError(e.getLocalizedMessage());
            }
        }).start();
    }

    public void doQosRequestInBackground(ArrayList<AppClient.QosPosition> kpiPositions) {
        new Thread(() -> {
            try {
                if (mRegisterClientComplete || registerClient()) {
                    mRegisterClientComplete = true;
                } else {
                    return;
                }
                getQosPositionKpi(kpiPositions);
            } catch (ExecutionException | InterruptedException
                    | PackageManager.NameNotFoundException | IllegalArgumentException e) {
                Log.e(TAG, "Exception in findCloudletInBackground() for "+
                        mAppName+", "+mAppVersion+", "+mOrgName);
                e.printStackTrace();
                meHelperInterface.showError(e.getLocalizedMessage());
            }
        }).start();
    }

    public boolean findCloudlet() throws ExecutionException, InterruptedException, IllegalArgumentException {
        AppClient.FindCloudletRequest findCloudletRequest;
        findCloudletRequest = me.createDefaultFindCloudletRequest(mActivity, getLocationForMatching()).setCarrierName(mCarrierName).build();
        Future<AppClient.FindCloudletReply> reply = me.findCloudletFuture(findCloudletRequest,
                mDmeHostname, mDmePort,10000, mFindCloudletMode);

        mClosestCloudlet = reply.get();

        Log.i(TAG, "findCloudlet mClosestCloudlet="+mClosestCloudlet);
        if(mClosestCloudlet.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
            String findCloudletStatusText = "findCloudlet Failed. Error: " + mClosestCloudlet.getStatus();
            Log.e(TAG, findCloudletStatusText);
            return false;
        }
        Log.i(TAG, "mClosestCloudlet.getFqdn()=" + mClosestCloudlet.getFqdn());

        //Find fqdnPrefix from Port structure.
        String fqdnPrefix = "";
        List<Appcommon.AppPort> ports = mClosestCloudlet.getPortsList();
        // Get data only from first port.
        Appcommon.AppPort aPort = ports.get(0);
        if (aPort != null) {
            Log.i(TAG, "Got port "+aPort+" TLS="+aPort.getTls()+" fqdnPrefix="+fqdnPrefix);
            fqdnPrefix = aPort.getFqdnPrefix();
            mAppInstTls = aPort.getTls();
            Log.i(TAG, "Setting TLS="+ mAppInstTls);
        }
        // Build full hostname.
        if (!mAppInstTls) {
            mAppInstHostname = fqdnPrefix + mClosestCloudlet.getFqdn();
        } else {
            // TODO: Revert this to prepend fqdnPrefix after EDGECLOUD-3634 is fixed.
            // Note that Docker deployments don't even have FqdnPrefix, so this workaround
            // is only needed for k8s, but the same code will work for both.
            mAppInstHostname = mClosestCloudlet.getFqdn();
        }
        meHelperInterface.showMessage("findCloudlet successful. Host=" + mAppInstHostname);
        onFindCloudlet(mClosestCloudlet);
        return true;
    }

    private void onFindCloudlet(AppClient.FindCloudletReply closestCloudlet) {
        mClosestCloudlet = closestCloudlet;
        meHelperInterface.onFindCloudlet(closestCloudlet);
        mRunConnectionTests = true;
        new Thread(new Runnable() {
            @Override public void run() {
                ConnectionTester tester = new ConnectionTester(mClosestCloudlet.getFqdn(),
                        mAppName, DEFAULT_LATENCY_PORT);

                if (tester.testConnection()) {
                    meHelperInterface.showMessage("Successfully connected to app inst on "+tester.mUrl);
                } else {
                    meHelperInterface.showError("Failed to connect to app inst on "+tester.mUrl);
                }
            }
        }).start();
    }

    private boolean getAppInstList() throws InterruptedException, ExecutionException {
        AppClient.AppInstListRequest appInstListRequest
                = me.createDefaultAppInstListRequest(mActivity, getLocationForMatching()).setCarrierName(mCarrierName).setLimit(mAppInstancesLimit).build();
        if(appInstListRequest != null) {
            AppClient.AppInstListReply cloudletList = me.getAppInstList(appInstListRequest,
                    mDmeHostname, mDmePort, 10000);
            Log.i(TAG, "cloudletList.getStatus()="+cloudletList.getStatus());
            if (cloudletList.getStatus() != AppClient.AppInstListReply.AIStatus.AI_SUCCESS) {
                String message = "getAppInstList failed. Status="+cloudletList.getStatus();
                Log.e(TAG, message);
                meHelperInterface.showError(message);
                return false;
            }
            meHelperInterface.onGetCloudletList(cloudletList);
            return true;

        } else {
            String message = "Cannot create AppInstListRequest object";
            Log.e(TAG, message);
            meHelperInterface.showError(message);
            return false;
        }
    }

    private boolean verifyLocation() throws InterruptedException, IOException, ExecutionException {
        // Location Verification (Blocking, or use verifyLocationFuture):
        AppClient.VerifyLocationRequest verifyRequest =
                me.createDefaultVerifyLocationRequest(mActivity, getLocationForMatching())
                        .setCarrierName(mCarrierName).build();
        if (verifyRequest != null) {
            AppClient.VerifyLocationReply verifiedLocation =
                    me.verifyLocation(verifyRequest, mDmeHostname, mDmePort, 10000);
            someText = "[Location Verified: Tower: " + verifiedLocation.getTowerStatus() +
                    ", GPS LocationStatus: " + verifiedLocation.getGpsLocationStatus() +
                    ", Location Accuracy: " + verifiedLocation.getGpsLocationAccuracyKm() + " ]\n";

            if (meHelperInterface != null) {
                meHelperInterface.onVerifyLocation(verifiedLocation.getGpsLocationStatus(),
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

    private void getQosPositionKpi(ArrayList<AppClient.QosPosition> positions) throws ExecutionException, InterruptedException {
        Log.i(TAG, "getQosPositionKpi me=" + me);
        AppClient.QosPositionRequest request = me.createDefaultQosPositionRequest(positions, 0, null).build();
        ChannelIterator<AppClient.QosPositionKpiReply> responseIterator = me.getQosPositionKpi(request,
                mDmeHostname, mDmePort, 10000);
        // A stream of QosPositionKpiReply(s), with a non-stream block of responses.
        long total = 0;
        while (responseIterator.hasNext()) {
            AppClient.QosPositionKpiReply aR = responseIterator.next();
            for (int i = 0; i < aR.getPositionResultsCount(); i++) {
                Log.i(TAG, aR.getPositionResults(i).toString());
            }
            total += aR.getPositionResultsCount();
        }
    }

    public MatchingEngine getMatchingEngine() {
        return me;
    }

    public void setMatchingEngine(MatchingEngine mMatchingEngine) {
        this.me = mMatchingEngine;
    }

    public Location getSpoofedLocation() {
        return mSpoofedLocation;
    }

    public void setSpoofedLocation(Location spoofedLocation) {
        Log.i(TAG, "setSpoofedLocation(" + spoofedLocation + ")");
        if (mClosestCloudlet != null) {
            postLocationToEdgeEvents(spoofedLocation);
        }
        mSpoofedLocation = spoofedLocation;
    }

    public void setSpoofedLocation(double latitude, double longitude) {
        Location location = new Location("MEX");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        setSpoofedLocation(location);
    }

    public void onDestroy() {
        if (me != null) {
            me.close();
            me = null;
        }
        mEdgeEventsSubscriber = null;
        mActivity = null;
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, String key) {
        Log.d(TAG, "onSharedPreferenceChanged("+key+")");
        if (mActivity == null) {
            Log.e(TAG, "mActivity is null, possibly after onDestroy()");
            return;
        }
        String prefKeyAllowMatchingEngineLocation = mActivity.getResources().getString(R.string.pref_matching_engine_location_verification);
        String prefKeyAllowNetSwitch = mActivity.getResources().getString(R.string.pref_net_switching_allowed);
        String prefKeyDmeHostname = mActivity.getResources().getString(R.string.pref_dme_hostname);
        String prefKeyOperatorName = mActivity.getResources().getString(R.string.pref_operator_name);
        String prefKeyDefaultDmeHostname = mActivity.getResources().getString(R.string.pref_default_dme_hostname);
        String prefKeyDefaultOperatorName = mActivity.getResources().getString(R.string.pref_default_operator_name);
        String prefKeyFindCloudletMode = mActivity.getResources().getString(R.string.pref_find_cloudlet_mode);
        String prefKeyAppInstancesLimit = mActivity.getResources().getString(R.string.pref_app_instances_limit);
        String prefKeyDefaultAppInfo = mActivity.getResources().getString(R.string.pref_default_app_definition);
        String prefKeyAppName = mActivity.getResources().getString(R.string.pref_app_name);
        String prefKeyAppVersion = mActivity.getResources().getString(R.string.pref_app_version);
        String prefKeyOrgName = mActivity.getResources().getString(R.string.pref_org_name);
        boolean appInfoChanged = false;

        if (key.equals(prefKeyAllowMatchingEngineLocation)) {
            boolean matchingEngineLocationAllowed = prefs.getBoolean(prefKeyAllowMatchingEngineLocation, false);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+matchingEngineLocationAllowed);
            MatchingEngine.setMatchingEngineLocationAllowed(matchingEngineLocationAllowed);
            if (matchingEngineLocationAllowed) {
                meHelperInterface.getCloudlets(true);
            }
        }

        if (key.equals(prefKeyAllowNetSwitch)) {
            mNetworkSwitchingAllowed = prefs.getBoolean(prefKeyAllowNetSwitch, false);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+mNetworkSwitchingAllowed);
            me.setNetworkSwitchingEnabled(mNetworkSwitchingAllowed);
        }

        if (key.equals(prefKeyDefaultDmeHostname)) {
            boolean useDefault = prefs.getBoolean(prefKeyDefaultDmeHostname, true);
            if (useDefault) {
                new Thread(() -> {
                    try {
                        mDefaultDmeHostname = me.generateDmeHostAddress();
                        String prefKeyValueDefaultDmeHostname = mActivity.getResources().getString(R.string.pref_value_default_dme_hostname);
                        prefs.edit().putString(prefKeyValueDefaultDmeHostname, mDefaultDmeHostname).apply();
                        setDmeHostname(mDefaultDmeHostname);
                    } catch (DmeDnsException e) {
                        mDefaultDmeHostname = DEFAULT_DME_HOSTNAME;
                        me.setUseWifiOnly(true);
                    }
                    Log.i(TAG, "mDefaultCarrierName="+mDefaultCarrierName+" mDefaultDmeHostname="+mDefaultDmeHostname);
                }).start();

            } else {
                // Change the key name so the normal DME hostname handling code will be used below.
                key = prefKeyDmeHostname;
            }
        }

        if (key.equals(prefKeyDefaultOperatorName)) {
            boolean useDefault = prefs.getBoolean(prefKeyDefaultOperatorName, true);
            if (useDefault) {
                // This will try to get the MccMnc from the device, unless wifiOnly is set.
                mDefaultCarrierName = me.getCarrierName(mActivity);
                Log.i(TAG, "mDefaultCarrierName=" + mDefaultCarrierName);
                String prefKeyValueDefaultOperatorName = mActivity.getResources().getString(R.string.pref_value_default_operator_name);
                prefs.edit().putString(prefKeyValueDefaultOperatorName, mDefaultCarrierName).apply();
                setCarrierName(mDefaultCarrierName);
            } else {
                // Change the key name so the normal Operator name handling code will be used below.
                key = prefKeyOperatorName;
            }
        }

        if (key.equals(prefKeyDmeHostname)) {
            String hostAndPort = prefs.getString(prefKeyDmeHostname, DEFAULT_DME_HOSTNAME+":"+"50051");
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+hostAndPort);
            String dmeHostname;

            try {
                dmeHostname = parseDmeHost(hostAndPort);
            } catch (HostParseException e) {
                String message = e.getLocalizedMessage()+" Default value will be used.";
                Log.e(TAG, message);
                Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
                dmeHostname = DEFAULT_DME_HOSTNAME;
            }

            Log.i(TAG, "dmeHostname from preferences: "+dmeHostname);
            setDmeHostname(dmeHostname);
        }

        if (key.equals(prefKeyOperatorName)) {
            String carrierName = prefs.getString(prefKeyOperatorName, DEFAULT_CARRIER_NAME);
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+carrierName);
            setCarrierName(carrierName);
        }

        if (key.equals(prefKeyFindCloudletMode)) {
            String findCloudletMode = prefs.getString(prefKeyFindCloudletMode, DEFAULT_FIND_CLOUDLET_MODE);
            mFindCloudletMode = MatchingEngine.FindCloudletMode.valueOf(findCloudletMode);
            Log.i(TAG, "findCloudletMode="+findCloudletMode+" mFindCloudletMode="+mFindCloudletMode);
        }

        if (key.equals(prefKeyAppInstancesLimit)) {
            String appInstancesLimit = prefs.getString(key, ""+DEFAULT_APP_INSTANCES_LIMIT);
            try {
                mAppInstancesLimit = Integer.parseInt(appInstancesLimit);
            } catch (NumberFormatException e) {
                mAppInstancesLimit = DEFAULT_APP_INSTANCES_LIMIT;
            }
            Log.i(TAG, "appInstancesLimit="+appInstancesLimit+" mAppInstancesLimit="+mAppInstancesLimit);
            appInfoChanged = true;
        }

        if (key.equals(prefKeyDefaultAppInfo)) {
            boolean useDefault = prefs.getBoolean(prefKeyDefaultAppInfo, true);
            if (useDefault) {
                mAppName = mActivity.getResources().getString(R.string.dme_app_name);
                mAppVersion = mActivity.getResources().getString(R.string.app_version);
                mOrgName = mActivity.getResources().getString(R.string.org_name);
            } else {
                mAppName = prefs.getString(prefKeyAppName, mActivity.getResources().getString(R.string.dme_app_name));
                mAppVersion = prefs.getString(prefKeyAppVersion, mActivity.getResources().getString(R.string.app_version));
                mOrgName = prefs.getString(prefKeyOrgName, mActivity.getResources().getString(R.string.org_name));
                Log.i(TAG, "onSharedPreferenceChanged("+key+")=false. Custom values: appName="+mAppName+" appVersion="+mAppVersion+" orgName="+mOrgName);
            }
            appInfoChanged = true;
        }

        if (key.equals(prefKeyAppName)) {
            mAppName = prefs.getString(key, mActivity.getResources().getString(R.string.dme_app_name));
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+mAppName);
            appInfoChanged = true;
        }
        if (key.equals(prefKeyAppVersion)) {
            mAppVersion = prefs.getString(key, mActivity.getResources().getString(R.string.app_version));
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+mAppVersion);
            appInfoChanged = true;
        }
        if (key.equals(prefKeyOrgName)) {
            mOrgName = prefs.getString(key, mActivity.getResources().getString(R.string.org_name));
            Log.i(TAG, "onSharedPreferenceChanged("+key+")="+mOrgName);
            appInfoChanged = true;
        }

        if (appInfoChanged) {
            meHelperInterface.getCloudlets(true);
        }
    }

    public void setCarrierName(String carrierName) {
        mCarrierName = carrierName;
        mClosestCloudletHostname = null;
        meHelperInterface.getCloudlets(true);
    }

    public void setDmeHostname(String hostname) {
        mRegisterClientComplete = false;
        mDmeHostname = hostname;
        mClosestCloudletHostname = null;
        meHelperInterface.getCloudlets(true);
        checkForLocSimulator(mDmeHostname);
    }

    public static String parseDmeHost(String hostAndPort) throws HostParseException {
        //Value is in this format: eu-mexdemo.dme.mobiledgex.net:50051
        String domainAndPortRegex = "^(((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}):\\d+$";
        Pattern domainAndPortPattern = Pattern.compile(domainAndPortRegex);
        Matcher matcher = domainAndPortPattern.matcher(hostAndPort);
        if(matcher.find()) {
            return matcher.group(1);
        } else {
            String message = "Invalid DME hostname and port: "+hostAndPort;
            throw new HostParseException(message);
        }
    }

    /**
     * Makes a request to the location simulator address for the corresponding DME hostname.
     * If the call is successful, we will allow location simulator updates in the UI.
     *
     * @param hostname  The DME hostname.
     */
    private void checkForLocSimulator(String hostname) {
        mAllowLocationSimulatorUpdate = false; // Default unless successful.
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(mActivity);
        String hostName = hostname.replace("dme", "locsim");
        String url = "http://"+hostName+":8888"; // Just check the index.
        Log.i(TAG, "checkForLocSimulator url="+url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "checkForLocSimulator response="+response);
                        mAllowLocationSimulatorUpdate = true;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Non-200 response for checkForLocSimulator. error="+error+". " +
                        "This is OK and just means there's no location simulator for this DME.");
                mAllowLocationSimulatorUpdate = false;
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    /**
     * If GPS spoof has been peformed, return that value, otherwise return the actual GPS location.
     * @return  location to be used for matching.
     */
    public Location getLocationForMatching() {
        if(mSpoofedLocation == null) {
            return mLastKnownLocation;
        } else {
            return mSpoofedLocation;
        }
    }

    /**
     * Post location into edgeEvents updater.
     *
     * @param location
     */
    public void postLocationToEdgeEvents(Location location) {
        Log.i(TAG, "postLocationToEdgeEvents("+location+")");
        me.getEdgeEventsConnectionFuture()
                .thenApply(connection -> {
                    if (connection != null) {
                        Log.i(TAG, "Posting location to DME");
                        DecimalFormat decFor = new DecimalFormat("#.###");
                        meHelperInterface.showMessage("Posting location to DME: "
                                + decFor.format(location.getLatitude()) + ", "
                                + decFor.format(location.getLongitude()));
                        connection.postLocationUpdate(location);
                    } else {
                        Log.e(TAG, "No connection for postLocationUpdate()");
                    }
                    return null;
                });
    }

    // (Guava EventBus Interface)
    // This class encapsulates what an app might implement to watch for edge events. Not every event
    // needs to be implemented. If you just want FindCloudlet, just @Subscribe to FindCloudletEvent.
    //
    class EdgeEventsSubscriber {

        // Subscribe to error handlers!
        @Subscribe
        public void onMessageEvent(EdgeEventsConnection.EdgeEventsError error) {
            Log.d(TAG, "EdgeEvents error reported, reason: " + error);
            if (error.toString().equals("eventTriggeredButCurrentCloudletIsBest")) {
                return;
            }
            meHelperInterface.showError(error.toString());
        }

        // Subscribe to FindCloudletEvent updates to appInst. Reasons to do so include
        // the AppInst Health and Latency spec for this application.
        @Subscribe
        public void onMessageEvent(FindCloudletEvent findCloudletEvent) {
            Log.i(TAG, "Cloudlet update, reason: " + findCloudletEvent.trigger);

            // Connect to new Cloudlet in the event here, preferably in a background task.
            Log.i(TAG, "FindCloudletEvent. Cloudlet: " + findCloudletEvent.newCloudlet);

            someText = findCloudletEvent.newCloudlet.getFqdn();
            meHelperInterface.showMessage("FindCloudletEvent. FQDN: "+someText);

            Log.i(TAG, "Received: Server pushed a new FindCloudletReply to switch to: " + findCloudletEvent);
            handleFindCloudletServerPush(findCloudletEvent);
        }

        void handleEventLatencyResults(AppClient.ServerEdgeEvent event) {
            meHelperInterface.showMessage("Received: " + event.getEventType());
            LocOuterClass.Statistics s = event.getStatistics();
            DecimalFormat decFor = new DecimalFormat("#.##");
            String message = "Latency results: min/avg/max/stddev="
                    + decFor.format(s.getMin()) + "/" + decFor.format(s.getAvg()) + "/"
                    + decFor.format(s.getMax()) + "/" + decFor.format(s.getStdDev());
            meHelperInterface.showMessage(message);
        }

        void handleFindCloudletServerPush(FindCloudletEvent event) {
            meHelperInterface.showMessage("Received: FindCloudletEvent");

            // In this demo case, use our existing interface to display the newly selected cloudlet on the map.
            mClosestCloudlet = event.newCloudlet;
            onFindCloudlet(mClosestCloudlet);
        }

        void handleAppInstHealth(AppClient.ServerEdgeEvent event) {
            if (event.getEventType() != EVENT_APPINST_HEALTH) {
                return;
            }

            meHelperInterface.showMessage("Received: " + event.getEventType()
                    + event.getHealthCheck().name());

            switch (event.getHealthCheck()) {
                case HEALTH_CHECK_FAIL_ROOTLB_OFFLINE:
                case HEALTH_CHECK_FAIL_SERVER_FAIL:
                    doEnhancedLocationUpdateInBackground();
                    break;
                case HEALTH_CHECK_OK:
                    Log.i(TAG, "AppInst Health is OK");
                    break;
                case UNRECOGNIZED:
                    // fall through
                default:
                    Log.i(TAG, "AppInst Health event: " + event.getHealthCheck());
            }
        }

        void handleCloudletMaintenance(AppClient.ServerEdgeEvent event) {
            if (event.getEventType() != AppClient.ServerEdgeEvent.ServerEventType.EVENT_CLOUDLET_MAINTENANCE) {
                return;
            }

            meHelperInterface.showMessage("Received: " + event.getEventType()
                    + event.getMaintenanceState().name());

            switch (event.getMaintenanceState()) {
                case NORMAL_OPERATION:
                    Log.i(TAG, "Maintenance state is all good!");
                    break;
                default:
                    Log.i(TAG, "Server maintenance: " + event.getMaintenanceState());
            }
        }

        void handleCloudletState(AppClient.ServerEdgeEvent event) {
            if (event.getEventType() != AppClient.ServerEdgeEvent.ServerEventType.EVENT_CLOUDLET_STATE) {
                return;
            }

            meHelperInterface.showMessage("Received: " + event.getEventType()
                    + event.getCloudletState().name());

            switch (event.getCloudletState()) {
                case CLOUDLET_STATE_INIT:
                    Log.i(TAG, "Cloudlet is not ready yet. Wait or FindCloudlet again.");
                    break;
                case CLOUDLET_STATE_NOT_PRESENT:
                    Log.i(TAG, "Cloudlet is not present.");
                case CLOUDLET_STATE_UPGRADE:
                    Log.i(TAG, "Cloudlet is upgrading");
                case CLOUDLET_STATE_OFFLINE:
                    Log.i(TAG, "Cloudlet is offline");
                case CLOUDLET_STATE_ERRORS:
                    Log.i(TAG, "Cloudlet is offline");
                case CLOUDLET_STATE_READY:
                    // Timer Retry or just retry.
                    doEnhancedLocationUpdateInBackground();
                    break;
                case CLOUDLET_STATE_NEED_SYNC:
                    Log.i(TAG, "Cloudlet data needs to sync.");
                    break;
                default:
                    Log.i(TAG, "Not handled");
            }
        }

        // Only the app knows with any certainty which AppPort (and internal port array)
        // it wants to test, so this is in the application.
        void handleLatencyRequest(AppClient.ServerEdgeEvent event) {
            if (event.getEventType() != AppClient.ServerEdgeEvent.ServerEventType.EVENT_LATENCY_REQUEST) {
                return;
            }

            meHelperInterface.showMessage("Received: " + event.getEventType());

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    // NetTest
                    // Local copy:
                    NetTest netTest = new NetTest();

                    // You must have performed FindCloudlet before you can do a NetTest.
                    if (mClosestCloudlet == null) {
                        meHelperInterface.showError("You must first perform Find Cloudlet.");
                        return;
                    }

                    // Assuming some knowledge of your own internal un-remapped server port
                    // discover, and test with the PerformanceMetrics API:
                    int publicPort;
                    HashMap<Integer, Appcommon.AppPort> ports = me.getAppConnectionManager().getTCPMap(mClosestCloudlet);
                    Appcommon.AppPort anAppPort = ports.get(DEFAULT_LATENCY_PORT);
                    if (anAppPort == null) {
                        Log.e(TAG, "The expected server (or port) doesn't seem to be here!");
                        return;
                    }

                    // Test with default network in use:
                    publicPort = anAppPort.getPublicPort();
                    String host = me.getAppConnectionManager().getHost(mClosestCloudlet, anAppPort);
                    Site site = new Site(mActivity, NetTest.TestType.CONNECT, 5, host, publicPort);
                    netTest.addSite(site);
                    netTest.testSites(netTest.TestTimeoutMS); // Test the one we just added.

                    me.getEdgeEventsConnection().postLatencyUpdate(netTest.getSite(host),
                            getLocationForMatching());
                    meHelperInterface.showMessage("Latency results posted to DME: Avg="+site.average);
                }
            });
        }
    }

    /**
     * See documentation for Google's FusedLocationProviderClient for additional usage information.
     */
    public void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    int interval = 5000; // Initially, 5 second interval to get the first update quickly
                    Log.i(TAG, "mFusedLocationClient.getLastLocation()="+mFusedLocationClient.getLastLocation()+" interval="+interval);

                    mLocationRequest = new LocationRequest();
                    mLocationRequest.setSmallestDisplacement(5);
                    mLocationRequest.setInterval(interval);
                    mLocationRequest.setFastestInterval(interval);
                    mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    Log.i(TAG, "mFusedLocationClient.requestLocationUpdates() called");
                } catch (SecurityException se) {
                    se.printStackTrace();
                    Log.i(TAG, "App should Request location permissions during onResume().");
                }
            }
        });
    }

    public void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i(TAG, "onLocationResult() Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastKnownLocation = locationResult.getLastLocation();

                if (mClosestCloudlet != null) {
                    // If we are currently spoofing the GPS, don't post the actual location.
                    if (getSpoofedLocation() == null) {
                        Log.e(TAG, "Posting real location to DME");
                        postLocationToEdgeEvents(location);
                    }
                }

                if(!mGpsInitialized) {
                    meHelperInterface.getCloudlets(true);
                    mGpsInitialized = true;
                }

                if(mLocationRequest.getInterval() < 60000) {
                    // Now that we got our first update, make interval longer to save battery.
                    Log.i(TAG, "Slowing down location request interval");
                    mLocationRequest.setInterval(60000); // one minute interval
                    mLocationRequest.setFastestInterval(60000);
                }
            }
        }
    };
}
