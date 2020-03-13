package com.mobiledgex.sdkdemo;

import android.app.UiAutomation;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.mobiledgex.matchingengine.DmeDnsException;
import com.mobiledgex.matchingengine.MatchingEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;


@RunWith(AndroidJUnit4.class)
public class MatchingEngineUnitTest {

    public static final String TAG = "MatchingEngineUnitTest";

    public static final long GRPC_TIMEOUT_MS = 15000;

    // MatchingEngine variables
    public static final String orgName = "MobiledgeX";
    public static final String appName = "MobiledgeX SDK Demo";
    public static final String appVers = "2.0";
    public static final String carrierName = "wifi";
    public static final String authToken = null;

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
                    InstrumentationRegistry.getTargetContext().getPackageName(),
                    "android.permission.READ_PHONE_STATE");
            uiAutomation.grantRuntimePermission(
                    InstrumentationRegistry.getTargetContext().getPackageName(),
                    "android.permission.ACCESS_COARSE_LOCATION"); // FINE_LOCATION??
        }
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("com.mobiledgex.sdkdemo", appContext.getPackageName());
    }

    // Call this before each API call in order to get session cookie
    public void registerClient(MatchingEngine me, Context ctx) {
        try {
            AppClient.RegisterClientRequest request = me.createDefaultRegisterClientRequest(ctx, orgName)
                    .setAppName(appName)
                    .setAppVers(appVers)
                    .build();
            AppClient.RegisterClientReply reply = me.registerClient(request, GRPC_TIMEOUT_MS);

            assertEquals(AppClient.ReplyStatus.RS_SUCCESS, reply.getStatus());
            assertTrue(reply.getSessionCookie() != null && reply.getSessionCookie() != "");

        } catch (NameNotFoundException nnfe) {
            assertTrue("NameNotFoundException creating register client request", false);
        } catch (DmeDnsException dde) {
            assertTrue("ExecutionException registering client.", false);
        } catch (ExecutionException ee) {
            assertTrue("ExecutionException registering client", false);
        } catch (InterruptedException ie) {
            assertTrue("InterruptedException registering client", false);
        }
    }

    @Test
    public void testRegisterClient() {
        Context ctx = InstrumentationRegistry.getTargetContext();
        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        registerClient(me, ctx);
    }

    @Test
    public void testFindCloudlet() {

        Context ctx = InstrumentationRegistry.getTargetContext();
        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        registerClient(me, ctx);

        Location location = new Location("MobiledgeX_Loc_Sim");
        location.setLatitude(37.33);
        location.setLongitude(121.88);

        try {
            AppClient.FindCloudletRequest request = me.createDefaultFindCloudletRequest(ctx, location)
                    .build();
            AppClient.FindCloudletReply reply = me.findCloudlet(request, GRPC_TIMEOUT_MS);

            assertEquals(AppClient.FindCloudletReply.FindStatus.FIND_FOUND, reply.getStatus());

        } catch (DmeDnsException dde) {
            assertTrue("ExecutionException registering client.", false);
        } catch (ExecutionException ee) {
            assertTrue("ExecutionException registering client", false);
        } catch (InterruptedException ie) {
            assertTrue("InterruptedException registering client", false);
        }
    }

    @Test
    public void testVerifyLocation() {

    }

    @Test
    public void testGetAppInstList() {

    }

    @Test
    public void testGetQosPositionKpi() {

    }
}
