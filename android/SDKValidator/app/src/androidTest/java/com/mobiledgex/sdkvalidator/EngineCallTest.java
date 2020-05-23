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

package com.mobiledgex.sdkvalidator;

import android.app.UiAutomation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.mobiledgex.matchingengine.AppConnectionManager;
import com.mobiledgex.matchingengine.ChannelIterator;
import com.mobiledgex.matchingengine.DmeDnsException;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.ssl.SSLPeerUnverifiedException;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon.AppPort;
import io.grpc.StatusRuntimeException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EngineCallTest {
    public static final String TAG = "EngineCallTest";
    public static final long GRPC_TIMEOUT_MS = 21000;

    public static final String organizationName = "MobiledgeX";
    // Other globals:
    public static final String applicationName = "MobiledgeX SDK Demo";
    public static final String appVersion = "2.0";

    FusedLocationProviderClient fusedLocationClient;

    public static String hostOverride = "wifi.dme.mobiledgex.net";
    public static int portOverride = 50051;
    public static String findCloudletCarrierOverride = "TDG"; // Allow "Any" if using "", but this likely breaks test cases.

    public boolean useHostOverride = true;

    // "useWifiOnly = true" also disables network switching, since the android default is WiFi.
    // Must be set to true if you are running tests without a SIM card.
    public boolean useWifiOnly = true;

    // Lat and long for San Jose
    public static final double latitude = 37.33;
    public static final double longitude = 121.88;

    private Location getTestLocation(double latitude, double longitude) {
        Location location = new Location("MobiledgeX_Test");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    @Before
    public void LooperEnsure() {
        // SubscriberManager needs a thread. Start one:
        if (Looper.myLooper()==null)
            Looper.prepare();
    }

    @Before
    public void grantPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
            uiAutomation.grantRuntimePermission(
                    InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName(),
                    "android.permission.READ_PHONE_STATE");
            uiAutomation.grantRuntimePermission(
                    InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName(),
                    "android.permission.ACCESS_COARSE_LOCATION");
            uiAutomation.grantRuntimePermission(
                    InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName(),
                    "android.permission.ACCESS_FINE_LOCATION"
            );
        }
    }
    // Mini test of wifi only:
    @Test
    public void testWiFiOnly() {
        useWifiOnly = true;

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        assertEquals(true, me.isUseWifiOnly());
        String overrideHost = "";
        try {
            overrideHost = me.generateDmeHostAddress();
        } catch (DmeDnsException dde) {
            assertTrue("Cannot set to use WiFi! DNS failure!", false);
        }
        assertEquals(me.wifiOnlyDmeHost, overrideHost);
        me.setUseWifiOnly(useWifiOnly = false);
        assertEquals(false, me.isUseWifiOnly());
    }

    /**
     * Enable or Disable MockLocation.
     * @param context
     * @param enableMock
     * @return
     */
    public boolean enableMockLocation(Context context, boolean enableMock) {
        if (fusedLocationClient == null) {
            fusedLocationClient = new FusedLocationProviderClient(context);
        }
        if (enableMock == false) {
            fusedLocationClient.setMockMode(false);
            return false;
        } else {
            fusedLocationClient.setMockMode(true);
            return true;
        }
    }

    /**
     * Utility Func. Single point mock location, fills in some extra fields. Does not calculate speed, nor update interval.
     * @param context
     * @param location
     */
    public void setMockLocation(Context context, Location location) throws InterruptedException {
        if (fusedLocationClient == null) {
            fusedLocationClient = new FusedLocationProviderClient(context);
        }

        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(1000);
        location.setAccuracy(3f);
        fusedLocationClient.setMockLocation(location);
        synchronized (location) {
            try {
                location.wait(1500); // Give Mock a bit of time to take effect.
            } catch (InterruptedException ie) {
                throw ie;
            }
        }
        fusedLocationClient.flushLocations();
    }

    // Every call needs registration to be called first at some point.
    public void registerClient(MatchingEngine me) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        AppClient.RegisterClientReply registerReply;
        AppClient.RegisterClientRequest regRequest;

        try {
            // The app version will be null, but we can build from scratch for test
            List<Pair<String, Long>> ids = me.retrieveCellId(context);
            AppClient.RegisterClientRequest.Builder regRequestBuilder = AppClient.RegisterClientRequest.newBuilder()
                    .setOrgName(organizationName)
                    .setAppName(applicationName)
                    .setAppVers(appVersion);
            if (ids != null && ids.size() > 0) {
                regRequestBuilder.setCellId(me.retrieveCellId(context).get(0).second.intValue());
            }
            regRequest = regRequestBuilder.build();
            if (useHostOverride) {
                registerReply = me.registerClient(regRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                registerReply = me.registerClient(regRequest, GRPC_TIMEOUT_MS);
            }
            // TODO: Validate JWT
        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertTrue("ExecutionException registering client.", false);
        } catch (ExecutionException ee) {
            Log.e(TAG, Log.getStackTraceString(ee));
            assertTrue("ExecutionException registering client", false);
        } catch (InterruptedException ioe) {
            Log.e(TAG, Log.getStackTraceString(ioe));
            assertTrue("InterruptedException registering client", false);
        }
    }

    @Test
    public void verifyLocationTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);
        AppClient.VerifyLocationReply verifyLocationReply = null;

        try {
            enableMockLocation(context, true);
            Location mockLoc = MockUtils.createLocation("verifyLocationTest", -96.994, 32.4824);
            setMockLocation(context, mockLoc);

            Location location = getTestLocation(-96.994, 32.4824);

            String carrierName = me.retrieveNetworkCarrierName(context);
            registerClient(me);

            AppClient.VerifyLocationRequest verifyLocationRequest = me.createDefaultVerifyLocationRequest(context, location)
                    .setCarrierName(carrierName)
                    .build();

            if (useHostOverride) {
                verifyLocationReply = me.verifyLocation(verifyLocationRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                verifyLocationReply = me.verifyLocation(verifyLocationRequest, GRPC_TIMEOUT_MS);
            }
            assert (verifyLocationReply != null);
        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("VerifyLocation: DmeDnsException", true);
        } catch (IOException ioe) {
            Log.e(TAG, Log.getStackTraceString(ioe));
            assertFalse("VerifyLocation: IOException!", true);
        } catch (ExecutionException ee) {
            Log.e(TAG, Log.getStackTraceString(ee));
            assertFalse("VerifyLocation: ExecutionExecution!", true);
        } catch (StatusRuntimeException sre) {
            Log.e(TAG, Log.getStackTraceString(sre));
            assertFalse("VerifyLocation: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.e(TAG, Log.getStackTraceString(ie));
            assertFalse("VerifyLocation: InterruptedException!", true);
        } finally {
            enableMockLocation(context, false);
        }


        // Temporary.
        assertEquals(0, verifyLocationReply.getVer());
        assertEquals(AppClient.VerifyLocationReply.TowerStatus.TOWER_UNKNOWN, verifyLocationReply.getTowerStatus());
        assertEquals(AppClient.VerifyLocationReply.GPSLocationStatus.LOC_ROAMING_COUNTRY_MATCH, verifyLocationReply.getGpsLocationStatus());
    }

    @Test
    public void verifyLocationFutureTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);
        AppClient.VerifyLocationReply verifyLocationReply = null;
        Future<AppClient.VerifyLocationReply> verifyLocationReplyFuture = null;

        try {
            Location location = getTestLocation( 47.6062,122.3321);

            String carrierName = me.retrieveNetworkCarrierName(context);
            registerClient(me);
            AppClient.VerifyLocationRequest verifyLocationRequest = me.createDefaultVerifyLocationRequest(context, location)
                    .setCarrierName(carrierName)
                    .build();
            if (useHostOverride) {
                verifyLocationReplyFuture = me.verifyLocationFuture(verifyLocationRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                verifyLocationReplyFuture = me.verifyLocationFuture(verifyLocationRequest, GRPC_TIMEOUT_MS);
            }
            verifyLocationReply = verifyLocationReplyFuture.get();
        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("verifyLocationFutureTest: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.e(TAG, Log.getStackTraceString(ee));
            assertFalse("verifyLocationFutureTest: ExecutionException Failed!", true);
        } catch (InterruptedException ie) {
            Log.e(TAG, Log.getStackTraceString(ie));
            assertFalse("verifyLocationFutureTest: InterruptedException!", true);
        }


        // Temporary.
        assertEquals(0, verifyLocationReply.getVer());
        assertEquals(AppClient.VerifyLocationReply.TowerStatus.TOWER_UNKNOWN, verifyLocationReply.getTowerStatus());
        assertEquals(AppClient.VerifyLocationReply.GPSLocationStatus.LOC_ROAMING_COUNTRY_MATCH, verifyLocationReply.getGpsLocationStatus());
    }


    /**
     * Mocked Location test. Note that responsibility of verifying location is in the MatchingEngine
     * on the server side, not client side.
     */
    @Test
    public void verifyMockedLocationTest_NorthPole() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        enableMockLocation(context,true);

        Location mockLoc = MockUtils.createLocation("verifyMockedLocationTest_NorthPole", 90d, 1d);

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        AppClient.VerifyLocationReply verifyLocationReply = null;
        try {
            Location location = getTestLocation( 47.6062,122.3321);

            String carrierName = me.retrieveNetworkCarrierName(context);
            registerClient(me);
            AppClient.VerifyLocationRequest verifyLocationRequest = me.createDefaultVerifyLocationRequest(context, location)
                    .setCarrierName(carrierName)
                    .build();
            if (useHostOverride) {
                verifyLocationReply = me.verifyLocation(verifyLocationRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                verifyLocationReply = me.verifyLocation(verifyLocationRequest, GRPC_TIMEOUT_MS);
            }
            assert(verifyLocationReply != null);
        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("verifyMockedLocationTest_NorthPole: DmeDnsException", true);
        } catch (IOException ioe) {
            Log.e(TAG, Log.getStackTraceString(ioe));
            assertFalse("verifyMockedLocationTest_NorthPole: IOException!", true);
        } catch (ExecutionException ee) {
            Log.e(TAG, Log.getStackTraceString(ee));
            assertFalse("verifyMockedLocationTest_NorthPole: ExecutionException!", true);
        } catch (InterruptedException ie) {
            Log.e(TAG, Log.getStackTraceString(ie));
            assertFalse("verifyMockedLocationTest_NorthPole: InterruptedException!", true);
        }

        // Temporary.
        assertEquals(0, verifyLocationReply.getVer());
        assertEquals(AppClient.VerifyLocationReply.TowerStatus.TOWER_UNKNOWN, verifyLocationReply.getTowerStatus());
        assertEquals(AppClient.VerifyLocationReply.GPSLocationStatus.LOC_ROAMING_COUNTRY_MATCH, verifyLocationReply.getGpsLocationStatus()); // Based on test data.

    }

    @Test
    public void getAppInstListTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        AppClient.AppInstListReply appInstListReply = null;

        try {
            Location location = getTestLocation( 47.6062,122.3321);

            registerClient(me);
            AppClient.AppInstListRequest appInstListRequest;
            AppClient.AppInstListReply list;
            appInstListRequest  = me.createDefaultAppInstListRequest(context, location)
                    .setCarrierName(findCloudletCarrierOverride)
                    .build();
            if (useHostOverride) {
                list = me.getAppInstList(appInstListRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                list = me.getAppInstList(appInstListRequest, GRPC_TIMEOUT_MS);
            }

            assertEquals(0, list.getVer());
            assertEquals(AppClient.AppInstListReply.AIStatus.AI_SUCCESS, list.getStatus());
            assertEquals(3, list.getCloudletsCount()); // NOTE: This is entirely test server dependent.
            for (int i = 0; i < list.getCloudletsCount(); i++) {
                Log.v(TAG, "Cloudlet: " + list.getCloudlets(i).toString());
            }

        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("getAppInstListTest: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.i(TAG, Log.getStackTraceString(ee));
            assertFalse("getAppInstListTest: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.i(TAG, Log.getStackTraceString(sre));
            Log.i(TAG, sre.getMessage());
            assertFalse("getAppInstListTest: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.i(TAG, Log.getStackTraceString(ie));
            assertFalse("getAppInstListTest: InterruptedException!", true);
        }
    }

    @Test
    public void getAppInstListFutureTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        try {
            Location location = getTestLocation( 47.6062,122.3321);

            registerClient(me);
            AppClient.AppInstListRequest appInstListRequest = me.createDefaultAppInstListRequest(context, location)
                    .setCarrierName(findCloudletCarrierOverride)
                    .build();

            Future<AppClient.AppInstListReply> listFuture;
            if (useHostOverride) {
                listFuture = me.getAppInstListFuture(appInstListRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                listFuture = me.getAppInstListFuture(appInstListRequest, GRPC_TIMEOUT_MS);
            }
            AppClient.AppInstListReply list = listFuture.get();

            assertEquals(0, list.getVer());
            assertEquals(AppClient.AppInstListReply.AIStatus.AI_SUCCESS, list.getStatus());
            assertEquals(3, list.getCloudletsCount()); // NOTE: This is entirely test server dependent.
            for (int i = 0; i < list.getCloudletsCount(); i++) {
                Log.v(TAG, "Cloudlet: " + list.getCloudlets(i).toString());
            }

        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("getAppInstListFutureTest: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.i(TAG, Log.getStackTraceString(ee));
            assertFalse("getAppInstListFutureTest: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.i(TAG, sre.getMessage());
            Log.i(TAG, Log.getStackTraceString(sre));
            assertFalse("getAppInstListFutureTest: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.i(TAG, Log.getStackTraceString(ie));
            assertFalse("getAppInstListFutureTest: InterruptedException!", true);
        }
    }

    @Test
    public void getQosPositionKpiTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        enableMockLocation(context,true);
        // The test must use a location where data exists on QOS server.
        Location location = MockUtils.createLocation("getQosPositionKpiTest", 8.5821, 50.11);

        ChannelIterator<AppClient.QosPositionKpiReply> responseIterator = null;
        try {
            registerClient(me);

            double totalDistanceKm = 20;
            double increment = 0.1;
            double direction = 45d;

            ArrayList<AppClient.QosPosition> kpiRequests = MockUtils.createQosPositionArray(location, direction, totalDistanceKm, increment);

            AppClient.QosPositionRequest request = me.createQoSPositionRequest(kpiRequests, 0, null, 0, null);
            assertFalse("SessionCookie must not be empty.", request.getSessionCookie().isEmpty());

            if (useHostOverride) {
                responseIterator = me.getQosPositionKpi(request, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                responseIterator = me.getQosPositionKpi(request, GRPC_TIMEOUT_MS);
            }
            // A stream of QosPositionKpiReply(s), with a non-stream block of responses.
            long total = 0;
            while (responseIterator.hasNext()) {
                AppClient.QosPositionKpiReply aR = responseIterator.next();
                for (int i = 0; i < aR.getPositionResultsCount(); i++) {
                    System.out.println(aR.getPositionResults(i));
                }
                total += aR.getPositionResultsCount();
            }
            responseIterator.shutdown();
            assertEquals((long)(kpiRequests.size()), total);
        } catch (DmeDnsException dde) {
            Log.i(TAG, Log.getStackTraceString(dde));
            assertFalse("queryQosKpiTest: DmeDnsException!", true);
        } catch (ExecutionException ee) {
            Log.i(TAG, Log.getStackTraceString(ee));
            assertFalse("queryQosKpiTest: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.i(TAG, sre.getMessage());
            Log.i(TAG, Log.getStackTraceString(sre));
            assertFalse("queryQosKpiTest: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.i(TAG, Log.getStackTraceString(ie));
            assertFalse("queryQosKpiTest: InterruptedException!", true);
        } finally {
            enableMockLocation(context,false);
            if (responseIterator != null) {
                responseIterator.shutdown();
            }
        }

    }

    @Test
    public void getQosPositionKpiFutureTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        enableMockLocation(context,true);
        // The test must use a location where data exists on QOS server.
        Location location = MockUtils.createLocation("getQosPositionKpiTest", 8.5821, 50.11);

        try {
            registerClient(me);

            double totalDistanceKm = 20;
            double increment = 0.1;
            double direction = 45d;

            ArrayList<AppClient.QosPosition> kpiRequests = MockUtils.createQosPositionArray(location, direction, totalDistanceKm, increment);

            AppClient.QosPositionRequest request = me.createQoSPositionRequest(kpiRequests, 0, null, 0, null);
            assertFalse("SessionCookie must not be empty.", request.getSessionCookie().isEmpty());

            Future<ChannelIterator<AppClient.QosPositionKpiReply>> replyFuture = null;
            if (useHostOverride) {
                replyFuture = me.getQosPositionKpiFuture(request, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                replyFuture = me.getQosPositionKpiFuture(request, GRPC_TIMEOUT_MS);
            }
            // A stream of QosPositionKpiReply(s), with a non-stream block of responses.
            // Wait for value with get().
            ChannelIterator<AppClient.QosPositionKpiReply> responseIterator = replyFuture.get();
            long total = 0;
            while (responseIterator.hasNext()) {
                AppClient.QosPositionKpiReply aR = responseIterator.next();
                for (int i = 0; i < aR.getPositionResultsCount(); i++) {
                    System.out.println(aR.getPositionResults(i));
                }
                total += aR.getPositionResultsCount();
            }
            responseIterator.shutdown();
            assertEquals((long)(kpiRequests.size()), total);
        } catch (DmeDnsException dde) {
            Log.i(TAG, Log.getStackTraceString(dde));
            assertFalse("getQosPositionKpiFutureTest: DmeDnsException!", true);
        } catch (ExecutionException ee) {
            Log.i(TAG, Log.getStackTraceString(ee));
            assertFalse("getQosPositionKpiFutureTest: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.i(TAG, sre.getMessage());
            Log.i(TAG, Log.getStackTraceString(sre));
            assertFalse("getQosPositionKpiFutureTest: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.i(TAG, Log.getStackTraceString(ie));
            assertFalse("getQosPositionKpiFutureTest: InterruptedException!", true);
        } finally {
            enableMockLocation(context,false);
        }

    }

    /**
     * Tests the MatchingEngine SDK supplied TCP connection to the edge cloudlet.
     *
     * This is a raw stream to a test echo server, so there are no explicit message delimiters.
     */
    @Test
    public void appConnectionTestTcp001() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        AppConnectionManager appConnect = me.getAppConnectionManager();

        enableMockLocation(context, true);

        Socket s = null;
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            // Test against Http Echo.
            String carrierName = "TDG";
            String appName = "HttpEcho";
            String orgName = "MobiledgeX";
            String appVersion = "20191204";

            // Exercise and override the default:
            // The app version will be null, but we can build from scratch for test
            AppClient.RegisterClientRequest regRequest = AppClient.RegisterClientRequest.newBuilder()
                    .setCarrierName(me.retrieveNetworkCarrierName(context))
                    .setOrgName(orgName)
                    .setAppName(appName)
                    .setAppVers(appVersion)
                    .build();

            AppClient.RegisterClientReply registerClientReply;
            if (true) {
                registerClientReply = me.registerClient(regRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                registerClientReply = me.registerClient(regRequest, GRPC_TIMEOUT_MS);
            }
            assertTrue("Register did not succeed for HttpEcho appInst", registerClientReply.getStatus() == AppClient.ReplyStatus.RS_SUCCESS);

            Location location = getTestLocation( 47.6062,122.3321);

            // Defaults:
            AppClient.FindCloudletRequest findCloudletRequest = me.createDefaultFindCloudletRequest(context, location)
                    .setCarrierName(findCloudletCarrierOverride)
                    .build();

            AppClient.FindCloudletReply findCloudletReply;
            if (true) {
                findCloudletReply = me.findCloudlet(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                findCloudletReply = me.findCloudlet(findCloudletRequest, GRPC_TIMEOUT_MS);
            }

            // Just using first one. This depends entirely on the server design.
            List<AppPort> appPorts = findCloudletReply.getPortsList();
            assertTrue("AppPorts is null", appPorts != null);
            assertTrue("AppPorts is empty!", appPorts.size() > 0);

            HashMap<Integer, AppPort> portMap = appConnect.getTCPMap(findCloudletReply);
            AppPort one = portMap.get(3001); // This internal port depends entirely the AppInst configuration/Docker image.

            assertTrue("EndPort is expected to be 0 for this AppInst", one.getEndPort() == 0 );
            // The actual mapped Public port, or one between getPublicPort() to getEndPort(), inclusive.
            Future<Socket> fs = appConnect.getTcpSocket(findCloudletReply, one, one.getPublicPort(), (int)GRPC_TIMEOUT_MS);

            // Interface bound TCP socket.
            s = fs.get(); // Nothing to do. Await value.
            try {
                bos = new BufferedOutputStream(s.getOutputStream());
                String data = "{\"Data\": \"food\"}";
                String rawpost = "POST / HTTP/1.1\r\n" +
                        "Host: 10.227.66.62:3000\r\n" +
                        "User-Agent: curl/7.54.0\r\n" +
                        "Accept: */*\r\n" +
                        "Content-Length: " + data.length() + "\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n" + data;
                bos.write(rawpost.getBytes());
                bos.flush();

                Object aMon = new Object(); // Some arbitrary object Monitor.
                synchronized (aMon) {
                    aMon.wait(1000);
                }

                bis = new BufferedInputStream(s.getInputStream());
                int available = bis.available();
                assertTrue("No bytes available in response.", available > 0); // Probably true.

                byte[] b = new byte[4096];
                int numRead = bis.read(b);
                assertTrue("Didn't get response!", numRead > 0);

                String output = new String(b);
                // Not an http client, so we're just going to get the substring of something stable:
                boolean found = output.indexOf("food") != -1 ? true : false;;
                assertTrue("Didn't find json data [" + data + "] in response!", found == true);

            } catch (IOException ioe) {
                assertTrue("Failed to get output stream for socket!", false);
            }

        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("appConnectionTestTcp001: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.i(TAG, Log.getStackTraceString(ee));
            assertFalse("appConnectionTestTcp001: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.i(TAG, sre.getMessage());
            Log.i(TAG, Log.getStackTraceString(sre));
            assertFalse("appConnectionTestTcp001: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.i(TAG, Log.getStackTraceString(ie));
            assertFalse("appConnectionTestTcp001: InterruptedException!", true);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.i(TAG, Log.getStackTraceString(nnfe));
            assertFalse("appConnectionTestTcp001: NameNotFoundException!", true);
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
                if (bos != null) {
                    bos.close();
                }
                if (s != null) {
                    s.close();
                }
            } catch (IOException ioe) {
                assertFalse("IO Exceptions trying to close socket.", true);
            }
            me.setNetworkSwitchingEnabled(true);
        }
    }

    /**
     * Tests the MatchingEngine SDK supplied HTTP connection to the edge cloudlet. FIXME: TLS Test with certs.
     */

    @Test
    public void appConnectionTestTcp002() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        AppConnectionManager appConnect = me.getAppConnectionManager();
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        OkHttpClient httpClient = null;
        // Test against Http Echo.
        String carrierName = "TDG";
        String appName = "HttpEcho";
        String orgName = "MobiledgeX";
        String appVersion = "20191204";
        try {
            String data = "{\"Data\": \"food\"}";

            AppClient.RegisterClientRequest req = me.createDefaultRegisterClientRequest(context, orgName)
                    .setCarrierName(carrierName)
                    .setAppName(appName)
                    .setAppVers(appVersion)
                    .build();

            AppClient.RegisterClientReply registerReply;
            // FIXME: Need/want a secondary cloudlet for this AppInst test.
            if (true) {
                registerReply = me.registerClient(req, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                registerReply = me.registerClient(req, GRPC_TIMEOUT_MS);
            }
            assertTrue("Register did not succeed for HttpEcho appInst", registerReply.getStatus() == AppClient.ReplyStatus.RS_SUCCESS);

            Location location = getTestLocation( 47.6062,122.3321);

            AppClient.FindCloudletRequest findCloudletRequest = me.createDefaultFindCloudletRequest(context, location)
                    .setCarrierName(carrierName)
                    .build();
            assertEquals("Session cookies don't match!", registerReply.getSessionCookie(), findCloudletRequest.getSessionCookie());

            AppClient.FindCloudletReply findCloudletReply;
            if (true) {
                findCloudletReply = me.findCloudlet(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                findCloudletReply = me.findCloudlet(findCloudletRequest, GRPC_TIMEOUT_MS);
            }

            // SSL:
            Future<OkHttpClient> httpClientFuture = null;
            httpClientFuture = appConnect.getHttpClient((int) GRPC_TIMEOUT_MS);

            // FIXME: UI Console exposes HTTP as TCP only, so test here use getTcpMap().
            String url = null;
            assertTrue("No AppPorts!", findCloudletReply.getPortsCount() > 0);
            HashMap<Integer, AppPort> portMap = appConnect.getTCPMap(findCloudletReply);
            // Choose the port that we happen to know the internal port for, 3001.
            AppPort one = portMap.get(3001);

            url = appConnect.createUrl(findCloudletReply, one, one.getPublicPort());
            assertTrue("URL for server seems very incorrect. ", url != null && url.length() > "http://:".length());

            assertFalse("Failed to get an SSL Socket!", httpClientFuture == null);

            // Interface bound TCP socket, has default timeout equal to NetworkManager.
            httpClient = httpClientFuture.get();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");


            RequestBody body = RequestBody.create(JSON, data);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            Response response = httpClient.newCall(request).execute();
            String output = response.body().string();
            boolean found = output.indexOf("food") !=-1 ? true : false;;
            assertTrue("Didn't find json data [" + data + "] in response!", found == true);
        } catch (PackageManager.NameNotFoundException nnfe) {

        } catch (IOException ioe) {
            Log.e(TAG, Log.getStackTraceString(ioe));
            assertFalse("appConnectionTestTcp002: IOException", true);
        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("appConnectionTestTcp002: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.i(TAG, Log.getStackTraceString(ee));
            assertFalse("appConnectionTestTcp002: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.i(TAG, sre.getMessage());
            Log.i(TAG, Log.getStackTraceString(sre));
            assertFalse("appConnectionTestTcp002: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.i(TAG, Log.getStackTraceString(ie));
            assertFalse("appConnectionTestTcp002: InterruptedException!", true);
        } finally {
            enableMockLocation(context,false);
        }
    }

    /**
     * NOTE: HttpEcho may only be installed on wifi.dme domain
     */
    @Test
    public void appConnectionTestTcp_Http_001() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        AppConnectionManager appConnect = me.getAppConnectionManager();
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        try {
            String data = "{\"Data\": \"food\"}";
            String carrierName = "TDG";
            String orgName = "MobiledgeX";
            String appName = "HttpEcho";
            String appVersion = "20191204";

            AppClient.RegisterClientRequest req = me.createDefaultRegisterClientRequest(context, orgName)
                    .setCarrierName(carrierName)
                    .setAppName(appName)
                    .setAppVers(appVersion)
                    .build();
            AppClient.RegisterClientReply registerClientReply;
            // FIXME: Need/want a secondary cloudlet for this AppInst test.
            if (true) {
                registerClientReply = me.registerClient(req, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                registerClientReply = me.registerClient(req, GRPC_TIMEOUT_MS);
            }
            assertTrue("Register did not succeed for HttpEcho appInst", registerClientReply.getStatus() == AppClient.ReplyStatus.RS_SUCCESS);

            Location location = getTestLocation( 47.6062,122.3321);

            AppClient.FindCloudletRequest findCloudletRequest = me.createDefaultFindCloudletRequest(context, location)
                    .setCarrierName(carrierName)
                    .build();
            // TODO: Validate JWT
            AppClient.FindCloudletReply findCloudletReply;
            if (true) {
                findCloudletReply = me.findCloudlet(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                findCloudletReply = me.findCloudlet(findCloudletRequest, GRPC_TIMEOUT_MS);
            }

            // SSL:
            Future<OkHttpClient> httpClientFuture = null;
            httpClientFuture = appConnect.getHttpClient(GRPC_TIMEOUT_MS);
            assertTrue("HttpClientFuture is NULL!", httpClientFuture != null);

            // FIXME: UI Console exposes HTTP as TCP only, so the test here uses getTcpList().
            String url = null;
            HashMap<Integer, AppPort> portMap = appConnect.getTCPMap(findCloudletReply);
            // Choose the TCP port, and we happen to know our server is on one port only: 3001.
            AppPort one = portMap.get(3001);
            assertTrue("Did not find server! ", one != null);
            url = appConnect.createUrl(findCloudletReply, one, one.getPublicPort());

            assertTrue("URL for server seems very incorrect. ", url != null && url.length() > "http://:".length());

            // Interface bound TCP socket, has default timeout equal to NetworkManager.
            OkHttpClient httpClient = httpClientFuture.get();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");


            RequestBody body = RequestBody.create(JSON, data);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            Response response = httpClient.newCall(request).execute();
            String output = response.body().string();
            boolean found = output.indexOf("food") != -1 ? true : false;
            assertTrue("Didn't find json data [" + data + "] in response!", found == true);


            Request mobiledgeXSiteRequest = new Request.Builder()
                    .url("https://mobiledgex.com")
                    .build();
            Response mexSiteResponse = httpClient.newCall(mobiledgeXSiteRequest).execute();
            int httpStatus = mexSiteResponse.code();
            assertEquals("Did not reach our home site. Status: ", 200, httpStatus);

            // This certificate goes to artifactory.mobiledgex.net, it *should* fail, but "connect" with
            // HTTP Status 200 OK.
            boolean failedVerification = false;
            mobiledgeXSiteRequest = new Request.Builder()
                    .url("https://mobiledgex.net")
                    .build();
            try {
                mexSiteResponse = httpClient.newCall(mobiledgeXSiteRequest).execute();
            } catch (SSLPeerUnverifiedException e) {
                failedVerification = true;
                httpStatus = mexSiteResponse.code();
                assertEquals("Should fail SSL Host verification, but still be 200 OK. Status: ", 200, httpStatus);
            }
            assertTrue("Did not fail hostname SSL verification!", failedVerification);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, nnfe.getMessage());
            Log.i(TAG, Log.getStackTraceString(nnfe));
            assertFalse("appConnectionTestTcp001: Package Info is missing!", true);
        } catch (IOException ioe) {
            Log.e(TAG, Log.getStackTraceString(ioe));
            assertFalse("appConnectionTestTcp001: IOException", true);
        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("appConnectionTestTcp001: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.i(TAG, Log.getStackTraceString(ee));
            assertFalse("appConnectionTestTcp001: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.e(TAG, sre.getMessage());
            Log.i(TAG, Log.getStackTraceString(sre));
            assertFalse("appConnectionTestTcp001: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.i(TAG, Log.getStackTraceString(ie));
            assertFalse("appConnectionTestTcp001: InterruptedException!", true);
        }
    }

    @Test
    public void testRegisterAndFindCloudlet_001() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        AppConnectionManager appConnectionManager = me.getAppConnectionManager();

        AppClient.RegisterClientReply registerClientReply = null;
        String carrierName = "TDG";
        String organizationName = "MobiledgeX";
        String appName = "HttpEcho";
        String appVersion = "20191204";

        Socket socket = null;
        try {
            Location location = getTestLocation( 47.6062,122.3321);

            Future<AppClient.FindCloudletReply> findCloudletReplyFuture = me.registerAndFindCloudlet(context, hostOverride, portOverride,
                    organizationName, appName,
                    appVersion, location, "",
                    0, null, null, null); // FIXME: These parameters should be overloaded or optional.
            // Just wait:
            AppClient.FindCloudletReply findCloudletReply = findCloudletReplyFuture.get();
            HashMap<Integer, AppPort> appTcpPortMap = appConnectionManager.getTCPMap(findCloudletReply);
            AppPort appPort = appTcpPortMap.get(3001);
            assertTrue(appPort != null); // There should be at least one for a connection to be made.
            Future<Socket> socketFuture = me.getAppConnectionManager().getTcpSocket(findCloudletReply, appPort, appPort.getPublicPort(), (int)GRPC_TIMEOUT_MS);
            socket = socketFuture.get();

            assertTrue("FindCloudletReply failed!", findCloudletReply != null);
        } catch (ExecutionException ee) {
            Log.i(TAG, Log.getStackTraceString(ee));
            assertFalse("testRegisterAndFindCloudlet_001: ExecutionException! "+ee.getLocalizedMessage(), true);
        } catch (StatusRuntimeException sre) {
            Log.i(TAG, Log.getStackTraceString(sre));
            assertFalse("testRegisterAndFindCloudlet_001: StatusRuntimeException!", true);
        } catch (InterruptedException ie) {
            Log.i(TAG, Log.getStackTraceString(ie));
            assertFalse("testRegisterAndFindCloudlet_001: InterruptedException!", true);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            enableMockLocation(context,false);
        }
    }

    @Test
    public void NetTestAPItest() {
        //TODO
    }

}

