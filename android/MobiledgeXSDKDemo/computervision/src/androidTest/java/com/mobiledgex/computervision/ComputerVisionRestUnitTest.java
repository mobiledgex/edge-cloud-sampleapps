package com.mobiledgex.computervision;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;

import com.android.volley.toolbox.Volley;
import com.mobiledgex.matchingengine.DmeDnsException;
import com.mobiledgex.matchingengine.MatchingEngine;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;

@RunWith(AndroidJUnit4.class)
public class ComputerVisionRestUnitTest {

    public static final long GRPC_TIMEOUT_MS = 15000;
    public static final String TAG = "Computer Vision Rest APIs Unit Test";

    // Backup Host and Port of MobiledgeXSDK Demo backend (face detection server)
    public static String host = "mobiledgexsdkdemo-tcp.sdkdemo-app-cluster.hamburg-main.tdg.mobiledgex.net";
    public static int port = 8008;

    // MatchingEngine variables
    public static final String orgName = "MobiledgeX";
    public static final String appName = "MobiledgeX SDK Demo";
    public static final String appVers = "2.0";
    public static final String carrierName = "wifi";
    public static final String authToken = null;
    public static final int cellID = 0;
    public static final String uniqueIDType = null;
    public static final String uniqueID = null;
    public static final List<AppClient.Tag> tags = null;

    // Lat and long for San Jose
    public static final double latitude = 37.33;
    public static final double longitude = 121.88;

    public static final Boolean useBackupUrl = false;

    @Before
    public void LooperEnsure() {
        // SubscriberManager needs a thread. Start one:
        if (Looper.myLooper()==null)
            Looper.prepare();
    }

    /*
     * Return url of MobiledgeXSDK Demo backend
     */
    private String registerAndFindCloudlet(Context ctx) {

        MatchingEngine me = new MatchingEngine(ctx);

        me.setMatchingEngineLocationAllowed(true);
        me.setAllowSwitchIfNoSubscriberInfo(true);

        try {
            AppClient.RegisterClientRequest registerRequest = me.createRegisterClientRequest(ctx, orgName, appName, appVers, carrierName, authToken, cellID, uniqueIDType, uniqueID, tags);
            AppClient.RegisterClientReply registerReply = me.registerClient(registerRequest, GRPC_TIMEOUT_MS);

            if (registerReply == null) {
                Log.e(TAG, "Register Client reply is null");
                return null;
            }

            if (registerReply.getStatus() != AppClient.ReplyStatus.RS_SUCCESS) {
                Log.e(TAG, "Register Client reply status is " + registerReply.getStatus());
                return null;
            }

            Location location = new Location("MobiledgeX_Loc_Sim");
            location.setLatitude(latitude);
            location.setLongitude(longitude);

            AppClient.FindCloudletRequest findCloudletRequest = me.createDefaultFindCloudletRequest(ctx, location)
                        .build();
            AppClient.FindCloudletReply findCloudletReply = me.findCloudlet(findCloudletRequest, GRPC_TIMEOUT_MS);

            if (findCloudletReply == null) {
                Log.e(TAG, "Find Cloudlet reply is null");
                return null;
            }

            if (findCloudletReply.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
                Log.e(TAG, "Find Cloudlet find status is " + findCloudletReply.getStatus());
                return null;
            }

            String fqdn = findCloudletReply.getFqdn();
            Appcommon.AppPort appPort = findCloudletReply.getPorts(0);
            int publicPort = appPort.getPublicPort();
            String fqdnPrefix = appPort.getFqdnPrefix();
            return "http://" + fqdnPrefix + fqdn + ":" + publicPort;

        } catch (DmeDnsException dde) {
            Log.e(TAG, "ExecutionException registering client. " + dde.getMessage());
            return null;
        } catch (ExecutionException ee) {
            Log.e(TAG, "ExecutionException registering client. " + ee.getMessage());
            return null;
        } catch (InterruptedException ie) {
            Log.e(TAG, "InterruptedException registering client. " + ie.getMessage());
            return null;
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e(TAG, "InterruptedException registering client. " + nnfe.getMessage());
            return null;
        }
    }

    /*
     * Grab specified resource, convert to byte array, and then convert to base64 string
     */
    private String getBase64EncodedResource(Context ctx, int id) {
        // Grab bitmap for faces.png image to be sent to server
        Bitmap bitmap = BitmapFactory.decodeResource(ctx.getResources(), id);
        // Convert to byte array
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 66, byteStream);
        final byte[] bytes = byteStream.toByteArray();
        // Convert bytes into Base64 string
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /*
     * Sends POST request to specified url
     * Implements onResponse and onErrorResponse callbacks
     * Returns StringRequest to be monitored
     */
    private void sendPostRequest(Context ctx, String url, final Map<String, String> params) {
        final Object lock = new Object();
        final String[] result = {"", ""};

        RequestQueue queue = Volley.newRequestQueue(ctx);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                synchronized (lock) {
                    result[0] = "success";
                    result[1] = response;
                    lock.notify();
                }
            }
            }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                synchronized (lock) {
                    result[0] = "error";
                    result[1] = error.toString();
                    lock.notify();
                }
            }
            }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        queue.add(request);

        synchronized (lock) {
            try {
                lock.wait(15000);
            } catch (InterruptedException ie) {
                assertTrue("Interrupted exception " + ie.getMessage(), false);
            }
        }

        String key = result[0];
        String response = result[1];

        assertTrue("Timed out", key != "");
        assertTrue("Error is " + response, key != "error");
        assertTrue("Response is empty", response != null && response != "");

        Log.i(TAG, "Response is " + response);
        try {
            JSONObject jsonObject = new JSONObject(response);
            assertTrue("Server unable to detect/recognize", jsonObject.getBoolean("success"));
        } catch (JSONException je) {
            assertTrue("Unable to convert response to JSON. Exception: " + je.getMessage(), false);
        }
    }

    /*
     * Face Detection: detector/detect/ endpoint
     * Used to send a face image to the server and get back a set of coordinates for any detected faces.
     */
    @Test()
    public void testDetectorDetectEndpoint() {
        Context ctx = InstrumentationRegistry.getTargetContext();

        String url = registerAndFindCloudlet(ctx);

        // If error with registerAndFindCloudlet, use backup
        if (url == null || useBackupUrl) {
            Log.e(TAG, "Unable to get url. Using backup host and port");
            url = "http://" + host + ":" + port;
        }

        String detectorDetectEnpoint = "/detector/detect/";
        url += detectorDetectEnpoint;
        Log.i(TAG, "url is " + url);

        final String requestBody = getBase64EncodedResource(ctx, R.drawable.faces);

        Map<String, String> params = new HashMap<>();
        params.put("image", requestBody);

        sendPostRequest(ctx, url, params);
    }

    /*
     * Face Recognition: recognizer/predict/ endpoint
     * Used to send a face image to the server and get back a set of coordinates for the recognized face.
     */
    @Test
    public void testRecognizerPredictEndpoint() {
        Context ctx = InstrumentationRegistry.getTargetContext();

        String url = registerAndFindCloudlet(ctx);

        // If error with registerAndFindCloudlet, use backup
        if (url == null || useBackupUrl) {
            Log.e(TAG, "Unable to get url. Using backup host and port");
            url = "http://" + host + ":" + port;
        }

        String detectorDetectEnpoint = "/recognizer/predict/";
        url += detectorDetectEnpoint;
        Log.i(TAG, "url is " + url);

        final String requestBody = getBase64EncodedResource(ctx, R.drawable.wonho);

        Map<String, String> params = new HashMap<>();
        params.put("image", requestBody);

        sendPostRequest(ctx, url, params);
    }

    /*
     * Face Recognition: recognizer/add/ endpoint
     * Used to send a face image to the server and add it to the set of training data.
     * The image is only added if a face is successfully detected.
     */
    @Test
    public void testRecognizerAddEndpoint() {
        Context ctx = InstrumentationRegistry.getTargetContext();

        String url = registerAndFindCloudlet(ctx);

        // If error with registerAndFindCloudlet, use backup
        if (url == null || useBackupUrl) {
            Log.e(TAG, "Unable to get url. Using backup host and port");
            url = "http://" + host + ":" + port;
        }

        String detectorDetectEnpoint = "/recognizer/add/";
        url += detectorDetectEnpoint;
        Log.i(TAG, "url is " + url);

        final String requestBody = getBase64EncodedResource(ctx, R.drawable.wonho);

        Map<String, String> params = new HashMap<>();
        params.put("subject", "Wonho Park");
        params.put("owner", "Guest");
        params.put("image", requestBody);

        sendPostRequest(ctx, url, params);
    }

    /*
     * Face Recognition: recognizer/add/ endpoint
     * Tells the server to read all training data images and rebuild database.
     */
    @Test
    public void testRecognizerTrainEndpoint() {
        Context ctx = InstrumentationRegistry.getTargetContext();

        String url = "http://opencv.facetraining.mobiledgex.net/8009";

        String detectorDetectEnpoint = "/recognizer/train/";
        url += detectorDetectEnpoint;
        Log.i(TAG, "url is " + url);

        sendPostRequest(ctx, url, null);
    }

    /*
     * Pose Detection: openpose/detect/ endpoint
     * Used to send a human body image to the server and get back a set of coordinates for any detected poses.
     */
    @Test
    public void testOpenposeDetectEndpoint() {
        Context ctx = InstrumentationRegistry.getTargetContext();

        String url = "http://posedetection.defaultedge.mobiledgex.net:8008";

        String detectorDetectEnpoint = "/openpose/detect/";
        url += detectorDetectEnpoint;
        Log.i(TAG, "url is " + url);

        final String requestBody = getBase64EncodedResource(ctx, R.drawable.pose);

        Map<String, String> params = new HashMap<>();
        params.put("image", requestBody);

        sendPostRequest(ctx, url, params);
    }

}
