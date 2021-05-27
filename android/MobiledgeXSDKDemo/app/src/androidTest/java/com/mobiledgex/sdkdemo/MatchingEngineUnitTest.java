package com.mobiledgex.sdkdemo;

import android.app.UiAutomation;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mobiledgex.matchingengine.ChannelIterator;
import com.mobiledgex.matchingengine.DmeDnsException;
import com.mobiledgex.matchingengine.MatchingEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import distributed_match_engine.LocOuterClass;


@RunWith(AndroidJUnit4.class)
public class MatchingEngineUnitTest {
    private static final String TAG = "MatchingEngineUnitTest";

    public static final long GRPC_TIMEOUT_MS = 15000;

    // MatchingEngine variables
    public static final String orgName = "MobiledgeX";
    public static final String appName = "MobiledgeX SDK Demo";
    public static final String appVers = "2.0";
    public static final String carrierName = "wifi";
    public static final String authToken = null;
    public static final int cellID = 0;
    public static final int lteCategory = 0;
    public static final AppClient.BandSelection bandSelection = null;

    // Lat and long for San Jose
    public static final double latitude = 37.33;
    public static final double longitude = 121.88;

    @Before
    public void LooperEnsure() {
        // SubscriberManager needs a thread. Start one:
        if (Looper.myLooper()==null)
            Looper.prepare();
    }

    @Before
    public void grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            UiAutomation uiAutomation = getInstrumentation().getUiAutomation();
            uiAutomation.grantRuntimePermission(
                    getInstrumentation().getTargetContext().getPackageName(),
                    "android.permission.READ_PHONE_STATE");
            uiAutomation.grantRuntimePermission(
                    getInstrumentation().getTargetContext().getPackageName(),
                    "android.permission.ACCESS_COARSE_LOCATION");
        }
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = getInstrumentation().getTargetContext();
        assertEquals("com.mobiledgex.sdkdemo", appContext.getPackageName());
    }

    /*
     * Call this before each API call in order to get session cookie
     */
    private void registerClient(MatchingEngine me, Context ctx) {
        try {
            AppClient.RegisterClientRequest request = me.createDefaultRegisterClientRequest(ctx, orgName)
                    .setAppName(appName)
                    .setAppVers(appVers)
                    .build();
            AppClient.RegisterClientReply reply = me.registerClient(request, GRPC_TIMEOUT_MS);

            assertTrue("Unable to get RegisterClientReply", reply != null);
            assertEquals("Reply status is " + reply.getStatus(), AppClient.ReplyStatus.RS_SUCCESS, reply.getStatus());
            assertTrue("Session cookie is " + reply.getSessionCookie(), reply.getSessionCookie() != null && reply.getSessionCookie() != "");

        } catch (NameNotFoundException nnfe) {
            assertTrue("NameNotFoundException creating register client request. " + nnfe.getMessage(), false);
        } catch (DmeDnsException dde) {
            assertTrue("ExecutionException registering client. " + dde.getMessage(), false);
        } catch (ExecutionException ee) {
            assertTrue("ExecutionException registering client. " + ee.getMessage(), false);
        } catch (InterruptedException ie) {
            assertTrue("InterruptedException registering client. " + ie.getMessage(), false);
        }
    }

    @Test
    public void testRegisterClient() {
        Context ctx = getInstrumentation().getTargetContext();
        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        registerClient(me, ctx);
    }

    @Test
    public void testFindCloudlet() {

        Context ctx = getInstrumentation().getTargetContext();
        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        registerClient(me, ctx);

        Location location = new Location("MobiledgeX_Loc_Sim");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        try {
            AppClient.FindCloudletRequest request = me.createDefaultFindCloudletRequest(ctx, location)
                    .build();
            AppClient.FindCloudletReply reply = me.findCloudlet(request, GRPC_TIMEOUT_MS);

            assertTrue("Unable to get FindCloudletReply", reply != null);
            assertEquals("FindCloudlet status is " + reply.getStatus(), AppClient.FindCloudletReply.FindStatus.FIND_FOUND, reply.getStatus());
            assertTrue("Fqdn in FindCloudletReply is " + reply.getFqdn(),reply.getFqdn() != null && reply.getFqdn() != "");
            Log.i(TAG, "cloudlet location="+reply.getCloudletLocation().getLatitude()+","+reply.getCloudletLocation().getLongitude());
            assertNotEquals("Latitude should not be 0.0, but it is", 0.0, reply.getCloudletLocation().getLatitude());
            assertNotEquals("Longitude should not be 0.0, but it is", 0.0, reply.getCloudletLocation().getLongitude());

        } catch (DmeDnsException dde) {
            assertTrue("ExecutionException finding cloudlet. " + dde.getMessage(), false);
        } catch (ExecutionException ee) {
            assertTrue("ExecutionException finding cloudlet. " + ee.getMessage(), false);
        } catch (InterruptedException ie) {
            assertTrue("InterruptedException finding cloudlet. " + ie.getMessage(),  false);
        }
    }

    @Test
    public void testFindCloudletPerformance() {

        Context ctx = getInstrumentation().getTargetContext();
        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        registerClient(me, ctx);

        Location location = new Location("MobiledgeX_Loc_Sim");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        try {
            AppClient.FindCloudletRequest request = me.createDefaultFindCloudletRequest(ctx, location).build();
            AppClient.FindCloudletReply reply = me.findCloudlet(request, GRPC_TIMEOUT_MS, MatchingEngine.FindCloudletMode.PERFORMANCE);

            assertTrue("Unable to get FindCloudletReply", reply != null);
            assertEquals("FindCloudlet status is " + reply.getStatus(), AppClient.FindCloudletReply.FindStatus.FIND_FOUND, reply.getStatus());
            assertTrue("Fqdn in FindCloudletReply is " + reply.getFqdn(),reply.getFqdn() != null && reply.getFqdn() != "");
            Log.i(TAG, "cloudlet location="+reply.getCloudletLocation().getLatitude()+","+reply.getCloudletLocation().getLongitude());
            assertNotEquals("Latitude should not be 0.0, but it is", 0.0, reply.getCloudletLocation().getLatitude());
            assertNotEquals("Longitude should not be 0.0, but it is", 0.0, reply.getCloudletLocation().getLongitude());

        } catch (DmeDnsException dde) {
            assertTrue("ExecutionException finding cloudlet. " + dde.getMessage(), false);
        } catch (ExecutionException ee) {
            assertTrue("ExecutionException finding cloudlet. " + ee.getMessage(), false);
        } catch (InterruptedException ie) {
            assertTrue("InterruptedException finding cloudlet. " + ie.getMessage(),  false);
        }
    }

    @Test
    public void testVerifyLocation() {

        Context ctx = getInstrumentation().getTargetContext();
        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        registerClient(me, ctx);

        Location location = new Location("MobiledgeX_Loc_Sim");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        try {
            AppClient.VerifyLocationRequest request = me.createDefaultVerifyLocationRequest(ctx, location).build();
            AppClient.VerifyLocationReply reply = me.verifyLocation(request, GRPC_TIMEOUT_MS);

            assertTrue("Unable to get VerifyLocation", reply != null);
            assertEquals("VerifyLocation Tower status is " + reply.getTowerStatus(), AppClient.VerifyLocationReply.TowerStatus.CONNECTED_TO_SPECIFIED_TOWER, reply.getTowerStatus());
            assertEquals("VerifyLocation GPS Location status is " + reply.getGpsLocationStatus(), AppClient.VerifyLocationReply.GPSLocationStatus.LOC_VERIFIED,  reply.getGpsLocationStatus());

        } catch (DmeDnsException dde) {
            assertTrue("DmeDnsException verifying location. " + dde.getMessage(), false);
        } catch (ExecutionException ee) {
            assertTrue("ExecutionException verifying location. " + ee.getMessage(), false);
        } catch (InterruptedException ie) {
            assertTrue("InterruptedException verifying location. " + ie.getMessage(), false);
        } catch (IOException ioe) {
            assertTrue("IOException verifying location. " + ioe.getMessage(), false);
        }
    }

    @Test
    public void testGetAppInstList() {

        Context ctx = getInstrumentation().getTargetContext();
        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        registerClient(me, ctx);

        Location location = new Location("MobiledgeX_Loc_Sim");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        try {
            AppClient.AppInstListRequest request = me.createDefaultAppInstListRequest(ctx, location).build();
            AppClient.AppInstListReply reply = me.getAppInstList(request, GRPC_TIMEOUT_MS);

            assertTrue("Unable to get AppInstListReply", reply != null);
            assertEquals("AppInstListReply status is " + reply.getStatus(), AppClient.AppInstListReply.AIStatus.AI_SUCCESS, reply.getStatus());

        } catch (DmeDnsException dde) {
            assertTrue("ExecutionException getting app inst list. " + dde.getMessage(), false);
        } catch (ExecutionException ee) {
            assertTrue("ExecutionException getting app inst list. " + ee.getMessage(), false);
        } catch (InterruptedException ie) {
            assertTrue("InterruptedException getting app inst list. " + ie.getMessage(), false);
        }
    }

    @Test
    public void testGetQosPositionKpi() {

        Context ctx = getInstrumentation().getTargetContext();
        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        registerClient(me, ctx);

        LocOuterClass.Loc loc = LocOuterClass.Loc.newBuilder()
                .setLongitude(latitude)
                .setLatitude(longitude)
                .build();
        List<AppClient.QosPosition> qosPositions = generateQosPositionList(loc, 45, 200, 1);

        try {
            AppClient.QosPositionRequest request = me.createDefaultQosPositionRequest(qosPositions, lteCategory, bandSelection).build();
            ChannelIterator<AppClient.QosPositionKpiReply> responseIterator = me.getQosPositionKpi(request, GRPC_TIMEOUT_MS);

            assertTrue("No QosPositionKpi reply", responseIterator.hasNext());
            while (responseIterator.hasNext()) {
                AppClient.QosPositionKpiReply reply = responseIterator.next();
                AppClient.ReplyStatus status = reply.getStatus();
                assertEquals("QosPositionKpi reply status is " + status, AppClient.ReplyStatus.RS_SUCCESS, status);
            }

        } catch (DmeDnsException dde) {
            assertTrue("ExecutionException getting app inst list. " + dde.getMessage(), false);
        } catch (ExecutionException ee) {
            assertTrue("ExecutionException getting app inst list. " + ee.getMessage(), false);
        } catch (InterruptedException ie) {
            assertTrue("InterruptedException getting app inst list. " + ie.getMessage(), false);
        }
    }

    private List<AppClient.QosPosition> generateQosPositionList(LocOuterClass.Loc loc, double direction_degrees, double totalDistanceKm, double increment) {
        List<AppClient.QosPosition> positions = new ArrayList<>();
        long positionId = 1;

        AppClient.QosPosition firstPosition = AppClient.QosPosition.newBuilder()
                .setPositionid(positionId)
                .setGpsLocation(loc)
                .build();
        positions.add(firstPosition);

        LocOuterClass.Loc lastLocation = LocOuterClass.Loc.newBuilder()
                .setLongitude(loc.getLongitude())
                .setLatitude(loc.getLatitude())
                .build();

        double kmPerDegreeLat = 110.57; //at Equator
        double kmPerDegreeLong = 111.32; //at Equator
        double addLatitude = (Math.sin(direction_degrees * (Math.PI/180)) * increment)/kmPerDegreeLat;
        double addLongitude = (Math.cos(direction_degrees * (Math.PI/180)) * increment)/kmPerDegreeLong;
        for (double traverse = 0; traverse + increment < totalDistanceKm; traverse += increment) {
            LocOuterClass.Loc next = LocOuterClass.Loc.newBuilder()
                    .setLongitude(lastLocation.getLongitude() + addLongitude)
                    .setLatitude(lastLocation.getLatitude() + addLatitude)
                    .build();
            AppClient.QosPosition nextPosition = AppClient.QosPosition.newBuilder()
                    .setPositionid(++positionId)
                    .setGpsLocation(next)
                    .build();

            positions.add(nextPosition);
            lastLocation = next;
        }
        return positions;
    }
}
