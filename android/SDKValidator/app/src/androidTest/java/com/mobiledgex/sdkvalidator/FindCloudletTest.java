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
import com.mobiledgex.matchingengine.DmeDnsException;
import com.mobiledgex.matchingengine.MatchingEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import distributed_match_engine.AppClient;
import io.grpc.StatusRuntimeException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FindCloudletTest {
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

    private int getCellId(Context context, MatchingEngine me) {
        int cellId = 0;
        List<Pair<String, Long>> cellIdList = me.retrieveCellId(context);
        if (cellIdList != null && cellIdList.size() > 0) {
            cellId = cellIdList.get(0).second.intValue();
        }
        return cellId;
    }

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
                    .setAppVers(appVersion)
                    .setCellId(getCellId(context, me));
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
    public void findCloudletTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppClient.FindCloudletReply findCloudletReply1 = null;
        AppClient.FindCloudletReply findCloudletReply2 = null;
        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        try {
            Location location = getTestLocation( 47.6062,122.3321);

            String carrierName = me.retrieveNetworkCarrierName(context);
            registerClient(me);

            // Set orgName and location, then override the rest for testing:
            AppClient.FindCloudletRequest findCloudletRequest = me.createDefaultFindCloudletRequest(context, location)
                    .setCarrierName(findCloudletCarrierOverride)
                    .build();
            if (useHostOverride) {
                findCloudletReply1 = me.findCloudlet(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                findCloudletReply1 = me.findCloudlet(findCloudletRequest, GRPC_TIMEOUT_MS);
            }

            // Second try:
            me.setThreadedPerformanceTest(true);
            if (useHostOverride) {
                findCloudletReply2 = me.findCloudlet(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                findCloudletReply2 = me.findCloudlet(findCloudletRequest, GRPC_TIMEOUT_MS);
            }

        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("FindCloudlet: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.e(TAG, Log.getStackTraceString(ee));
            assertFalse("FindCloudlet: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.e(TAG, sre.getMessage());
            Log.e(TAG, Log.getStackTraceString(sre));
            assertFalse("FindCloudlet: StatusRunTimeException!", true);
        } catch (InterruptedException ie) {
            Log.e(TAG, Log.getStackTraceString(ie));
            assertFalse("FindCloudlet: InterruptedException!", true);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, Log.getStackTraceString(nnfe));
            assertFalse("FindCloudlet: PackageManager.NameNotFoundException!", true);
        }

        assertNotNull("FindCloudletReply1 is null!", findCloudletReply1);
        assertNotNull("FindCloudletReply2 is null!", findCloudletReply2);

        // Might also fail, since the network is not under test control:
        assertEquals("App's expected test cloudlet FQDN doesn't match.", "sdkdemo-app-cluster.dusseldorf-main.tdg.mobiledgex.net", findCloudletReply1.getFqdn());
    }

    @Test
    public void findCloudletBadCarrier() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppClient.FindCloudletReply findCloudletReply1 = null;
        AppClient.FindCloudletReply findCloudletReply2 = null;
        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        try {
            Location location = getTestLocation( 47.6062,122.3321);

            String carrierName = me.retrieveNetworkCarrierName(context);
            registerClient(me);

            // Set orgName and location, then override the rest for testing:
            AppClient.FindCloudletRequest findCloudletRequest = me.createDefaultFindCloudletRequest(context, location)
                    .setCarrierName("Leon's Fly-by-night Carrier")
                    .build();
            if (useHostOverride) {
                findCloudletReply1 = me.findCloudlet(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                findCloudletReply1 = me.findCloudlet(findCloudletRequest, GRPC_TIMEOUT_MS);
            }

            // Second try:
            me.setThreadedPerformanceTest(true);
            if (useHostOverride) {
                findCloudletReply2 = me.findCloudlet(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                findCloudletReply2 = me.findCloudlet(findCloudletRequest, GRPC_TIMEOUT_MS);
            }

        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("FindCloudlet: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.e(TAG, Log.getStackTraceString(ee));
            assertFalse("FindCloudlet: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            Log.e(TAG, sre.getMessage());
            Log.e(TAG, Log.getStackTraceString(sre));
            assertFalse("FindCloudlet: StatusRunTimeException!", true);
        } catch (InterruptedException ie) {
            Log.e(TAG, Log.getStackTraceString(ie));
            assertFalse("FindCloudlet: InterruptedException!", true);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, Log.getStackTraceString(nnfe));
            assertFalse("FindCloudlet: PackageManager.NameNotFoundException!", true);
        }

        Log.i(TAG, "findCloudletReply1="+findCloudletReply1);
        Log.i(TAG, "findCloudletReply2="+findCloudletReply2);
        assertNotNull("FindCloudletReply1 is null!", findCloudletReply1);
        assertNotNull("FindCloudletReply2 is null!", findCloudletReply2);

        assertEquals("status doesn't match", AppClient.FindCloudletReply.FindStatus.FIND_NOTFOUND, findCloudletReply1.getStatus());

    }

    // This test only tests "" any, and not subject to the global override.
    @Test
    public void findCloudletTestSetCarrierNameAnyOverride() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppClient.FindCloudletReply findCloudletReply = null;
        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        boolean expectedExceptionHit = false;
        try {
            Location location = getTestLocation( 47.6062,122.3321);

            String carrierName = me.retrieveNetworkCarrierName(context);
            registerClient(me);

            // Set NO carrier name, as if there's no SIM card. This should tell DME to return
            // any edge AppInst from any carrier, for this app, version, orgName keyset.
            AppClient.FindCloudletRequest findCloudletRequest2 = me.createDefaultFindCloudletRequest(context, location)
                    .setCarrierName("")
                    .build();
            if (useHostOverride) {
                findCloudletReply = me.findCloudlet(findCloudletRequest2, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                findCloudletReply = me.findCloudlet(findCloudletRequest2, GRPC_TIMEOUT_MS);
            }
            assertTrue(findCloudletReply != null);
            assertTrue(findCloudletReply.getStatus().equals(AppClient.FindCloudletReply.FindStatus.FIND_FOUND));
        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("FindCloudlet: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.e(TAG, Log.getStackTraceString(ee));
            assertFalse("FindCloudlet: ExecutionException!", true);
        } catch (StatusRuntimeException sre) {
            /* This is expected! */
            Log.e(TAG, sre.getMessage());
            Log.e(TAG, Log.getStackTraceString(sre));
            expectedExceptionHit = true;
            assertTrue("FindCloudlet: Expected StatusRunTimeException!", true);
        } catch (InterruptedException ie) {
            Log.e(TAG, Log.getStackTraceString(ie));
            assertFalse("FindCloudlet: InterruptedException!", true);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, Log.getStackTraceString(nnfe));
            assertFalse("FindCloudlet: NameNotFoundException!", true);
        }

        assertFalse("findCloudletTestSetAllOptionalDevAppNameVers: NO Expected StatusRunTimeException about 'NO PERMISSION'", expectedExceptionHit);
    }

    @Test
    public void findCloudletFutureTest() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Future<AppClient.FindCloudletReply> response;
        AppClient.FindCloudletReply findCloudletReply1 = null;
        AppClient.FindCloudletReply findCloudletReply2 = null;
        MatchingEngine me = new MatchingEngine(context);
        me.setUseWifiOnly(useWifiOnly);
        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        try {
            Location location = getTestLocation( 47.6062,122.3321);

            registerClient(me);

            // Cannot use the older API if overriding.
            AppClient.FindCloudletRequest findCloudletRequest = me.createDefaultFindCloudletRequest(context, location)
                    .setCarrierName(findCloudletCarrierOverride)
                    .build();

            if (useHostOverride) {
                response = me.findCloudletFuture(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                response = me.findCloudletFuture(findCloudletRequest, 10000);
            }
            findCloudletReply1 = response.get();

            // Second try:
            me.setThreadedPerformanceTest(true);
            if (useHostOverride) {
                response = me.findCloudletFuture(findCloudletRequest, hostOverride, portOverride, GRPC_TIMEOUT_MS);
            } else {
                response = me.findCloudletFuture(findCloudletRequest, GRPC_TIMEOUT_MS);
            }
            findCloudletReply2 = response.get();
        } catch (DmeDnsException dde) {
            Log.e(TAG, Log.getStackTraceString(dde));
            assertFalse("FindCloudletFuture: DmeDnsException", true);
        } catch (ExecutionException ee) {
            Log.e(TAG, Log.getStackTraceString(ee));
            assertFalse("FindCloudletFuture: ExecutionExecution!", true);
        } catch (InterruptedException ie) {
            Log.e(TAG, Log.getStackTraceString(ie));
            assertFalse("FindCloudletFuture: InterruptedException!", true);
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, Log.getStackTraceString(nnfe));
            assertFalse("FindCloudletFuture: PackageManager.NameNotFoundException!", true);
        } finally {
            enableMockLocation(context,false);
        }


        assertNotNull("FindCloudletReply1 is null!", findCloudletReply1);
        assertNotNull("FindCloudletReply2 is null!", findCloudletReply2);

        assertEquals(findCloudletReply1.getFqdn(), findCloudletReply2.getFqdn());

        // Might also fail, since the network is not under test control:
        assertEquals("App's expected test cloudlet FQDN doesn't match.", "sdkdemo-app-cluster.dusseldorf-main.tdg.mobiledgex.net", findCloudletReply1.getFqdn());
    }




}

