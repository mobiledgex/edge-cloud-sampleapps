package com.mobiledgex.computervision;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageSender {
    private static final String TAG = "ImageSender";
    public static final int TRAINING_COUNT_TARGET = 10;

    private ImageServerInterface mImageServerInterface;
    private RequestQueue mRequestQueue;

    private String mHost;
    private int mPort;
    private RollingAverage mLatencyFullProcessRollingAvg;
    private RollingAverage mLatencyNetOnlyRollingAvg;
    private int mTrainingCount;
    private boolean mBusy;
    private long mLatency = 0;
    private boolean mDoNetLatency = true;
    private final int mRollingAvgSize = 100;
    private CameraMode mCameraMode;
    private String djangoUrl = "/detector/detect/";
    private ImageServerInterface.CloudletType mCloudLetType;
    private Handler mHandler;

    //Variables for latency test
    private LatencyTestMethod mLatencyTestMethod = LatencyTestMethod.socket;
    private final int mSocketTimeout = 3000;

    private GoogleSignInAccount mAccount;
    private String mGuestName = "";

    public enum LatencyTestMethod {
        ping,
        socket
    }
    
    public enum CameraMode {
        FACE_DETECTION,
        FACE_RECOGNITION,
        FACE_TRAINING,
        FACE_UPDATING_SERVER,
        FACE_UPDATE_SERVER_COMPLETE,
        POSE_DETECTION
    }

    public ImageSender(Activity activity, ImageServerInterface imageServerInterface, ImageServerInterface.CloudletType cloudLetType, String host, int port) {
        mCloudLetType = cloudLetType;
        mHost = host;
        mPort = port;

        mImageServerInterface = imageServerInterface;

        mAccount = GoogleSignIn.getLastSignedInAccount(activity);
        if(mAccount != null) {
            Log.i(TAG, "mAccount=" + mAccount.getDisplayName()+" "+mAccount.getId());
        } else {
            Log.i(TAG, "mAccount=" + mAccount);
        }

        mLatencyFullProcessRollingAvg = new RollingAverage(cloudLetType, "Full Process", mRollingAvgSize);
        mLatencyNetOnlyRollingAvg = new RollingAverage(cloudLetType, "Network Only", mRollingAvgSize);
        HandlerThread handlerThread = new HandlerThread("BackgroundPinger"+cloudLetType);
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        // Instantiate the RequestQueue.
        mRequestQueue = Volley.newRequestQueue(activity);
    }

    public void setGuestName(String guestName) {
        Log.d(TAG, "setGuestName="+guestName);
        mGuestName = guestName;
    }

    public void setCameraMode(CameraMode mode) {
        mCameraMode = mode;
        if(mode == CameraMode.FACE_DETECTION) {
            djangoUrl = "/detector/detect/";
        } else if (mode == CameraMode.FACE_RECOGNITION){
            djangoUrl = "/recognizer/predict/";
        } else if (mode == CameraMode.FACE_TRAINING) {
            djangoUrl = "/trainer/add/";
            mTrainingCount = 0;
        } else if (mode == CameraMode.FACE_UPDATING_SERVER) {
            djangoUrl = "/trainer/predict/";
        } else if (mode == CameraMode.POSE_DETECTION){
            djangoUrl = "/openpose/detect/";
        } else {
            Log.e(TAG, "Invalid CameraMode: "+mode);
        }
        Log.i(TAG, "setCameraMode("+mCameraMode+") djangoUrl="+djangoUrl+" "+mCloudLetType+" host="+mHost);
    }

    public void setDoNetLatency(boolean doNetLatency) {
        this.mDoNetLatency = doNetLatency;
    }

    private Map<String,String> getUserParams() {
        Map<String, String> params = new HashMap<String, String>();
        if(isSignedIn()) {
            params.put("owner_name", mAccount.getDisplayName());
            params.put("owner_id", mAccount.getId());
            if(mGuestName.equals("")) {
                params.put("subject", mAccount.getDisplayName());
            } else {
                params.put("subject", mGuestName);
            }
        }
        Log.i(TAG, "getUserParams="+params);
        return params;
    }

    public boolean isSignedIn() {
        return mAccount != null;
    }

    //TODO: Get the port value from the appInst
    public static int getFaceDetectionServerPort(String hostName) {
        int port;
        port = 8008;
        Log.i(TAG, "getFaceDetectionServerPort("+hostName+")="+port);
        return port;
    }

    /**
     * Encode the bitmap and use Volley async to request face detection/recognition
     * coordinates. Decode the returned JSON string and update the rectangles
     * on the preview. Also time the transaction to calculate latency.
     *
     * @param bitmap  The image to encode and send.
     */
    public void sendImage(Bitmap bitmap) {
        if(mBusy) {
            return;
        }
        // Get a lock for the busy
        mBusy = true;
        if(mDoNetLatency) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    doSinglePing(mHost, mLatencyNetOnlyRollingAvg, mCloudLetType);
                }
            });
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 67, byteStream);
        //TODO: Add preferences for quality and to allow lossless Bitmap.CompressFormat.PNG

        byte[] bytes = byteStream.toByteArray();
        String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);

        final String requestBody = encoded;
        String url = "http://"+ mHost +":"+mPort + djangoUrl;
        Log.i(TAG, "url="+url+" length: "+requestBody.length());

        final long startTime = System.nanoTime();

        // Request a byte response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, mCloudLetType +" sendImage response="+response);
                        long endTime = System.nanoTime();
                        mBusy = false;
                        mLatency = endTime - startTime;
                        mLatencyFullProcessRollingAvg.add(mLatency);
                        mImageServerInterface.updateFullProcessStats(mCloudLetType, mLatencyFullProcessRollingAvg);
                        Log.i(TAG, mCloudLetType +" mCameraMode="+mCameraMode+" mLatency="+(mLatency /1000000.0));
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray rects;
                            String subject = null;
                            if(jsonObject.getBoolean("success")) {
                                if(mCameraMode == CameraMode.POSE_DETECTION) {
                                    JSONArray poses = jsonObject.getJSONArray("poses");
                                    mImageServerInterface.updateOverlay(mCloudLetType, poses, null);
                                }  else {
                                    if (jsonObject.has("subject")) {
                                        //This means it was from recognition mode
                                        subject = jsonObject.getString("subject");
                                        JSONArray rect = jsonObject.getJSONArray("rect");
                                        rects = new JSONArray();
                                        rects.put(rect);
                                    } else {
                                        //Default is from detection mode
                                        rects = jsonObject.getJSONArray("rects");
                                    }
                                    mImageServerInterface.updateOverlay(mCloudLetType, rects, subject);
                                }

                                if(mCameraMode == CameraMode.FACE_TRAINING) {
                                    mTrainingCount++;
                                    Log.i(TAG, mCloudLetType +" mTrainingCount="+ mTrainingCount);
                                    mImageServerInterface.updateTrainingProgress(mTrainingCount, mCameraMode);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mBusy = false;
                Log.e(TAG, "sendImage received error="+error);
            }
        }) {

            @Override
            protected Map<String,String> getParams(){
                Map<String, String> params = getUserParams();
                params.put("image", requestBody);
                return params;
            }
        };

        // Add the request to the RequestQueue.
        mRequestQueue.add(stringRequest);
    }

    /**
     * Sends request to the FaceDetectionServer to perform the update procedure.
     */
    public void recognizerUpdate() {
        Log.i(TAG, mCloudLetType +" recognizerUpdate mCameraMode="+mCameraMode);
        setCameraMode(CameraMode.FACE_RECOGNITION);

        String url = "http://"+ mHost +":"+mPort+"/recognizer/update/";
        Log.i(TAG, mCloudLetType +" url="+url);

        //Show indeterminate progress bar
        mImageServerInterface.updateTrainingProgress(mTrainingCount, CameraMode.FACE_UPDATING_SERVER);

        final long startTime = System.nanoTime();

        // Request a byte response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, mCloudLetType +" recognizerUpdate response="+response);
                        long endTime = System.nanoTime();
                        long elapsed = endTime - startTime;
                        Log.i(TAG, mCloudLetType +" recognizerUpdate elapsed="+(elapsed/1000000.0));
                        setCameraMode(CameraMode.FACE_RECOGNITION);
                        //Remove indeterminate progress bar
                        mImageServerInterface.updateTrainingProgress(mTrainingCount, CameraMode.FACE_UPDATE_SERVER_COMPLETE);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "recognizerUpdate received error="+error);
                //Even though this call failed, set these values to continue processing.
                mBusy = false;
                setCameraMode(CameraMode.FACE_RECOGNITION);
                //Remove indeterminate progress bar
                mImageServerInterface.updateTrainingProgress(mTrainingCount, CameraMode.FACE_UPDATE_SERVER_COMPLETE);
            }
        }) {
            @Override
            protected Map<String,String> getParams(){
                return getUserParams();
            }
        };

        // Add the request to the RequestQueue.
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(stringRequest);
    }

    /**
     * Sends request to the FaceTrainingServer to perform the train procedure.
     */
    public void trainerTrain() {
        Log.i(TAG, mCloudLetType +" trainerTrain mCameraMode="+mCameraMode);
        setCameraMode(CameraMode.FACE_UPDATING_SERVER);

        String url = "http://"+ mHost +":"+mPort+"/trainer/train/";
        Log.i(TAG, mCloudLetType +" url="+url);

        final long startTime = System.nanoTime();

        // Request a byte response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, mCloudLetType +" trainerTrain response="+response);
                        long endTime = System.nanoTime();
                        long elapsed = endTime - startTime;
                        Log.i(TAG, mCloudLetType +" trainerTrain elapsed="+(elapsed/1000000.0));
                        setCameraMode(CameraMode.FACE_RECOGNITION);
                        mImageServerInterface.updateTrainingProgress(mTrainingCount, mCameraMode);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mBusy = false;
                Log.e(TAG, "trainerTrain received error="+error);
            }
        }) {
            @Override
            protected Map<String,String> getParams(){
                return getUserParams();
            }
        };

        // Add the request to the RequestQueue.
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(stringRequest);
    }

    /**
     * Sends request to the FaceTrainingServer to remove training data.
     */
    public void trainerRemove() {
        String url = "http://"+ mHost +":"+mPort+"/trainer/remove/";
        Log.i(TAG, mCloudLetType +" url="+url);
        setCameraMode(CameraMode.FACE_UPDATING_SERVER);

        final long startTime = System.nanoTime();

        // Request a byte response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, mCloudLetType +" trainerRemove response="+response);
                        long endTime = System.nanoTime();
                        long elapsed = endTime - startTime;
                        Log.i(TAG, mCloudLetType +" trainerRemove elapsed="+(elapsed/1000000.0));
                        setGuestName("");
                        setCameraMode(CameraMode.FACE_RECOGNITION);
                        mImageServerInterface.updateTrainingProgress(mTrainingCount, mCameraMode);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mBusy = false;
                Log.e(TAG, "trainerRemove received error="+error);
            }
        }) {
            @Override
            protected Map<String,String> getParams(){
                return getUserParams();
            }
        };

        // Add the request to the RequestQueue.
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mRequestQueue.add(stringRequest);
    }

    /**
     * Depending on Latency Testing method preference, perform either a single "ping"
     * or a single socket open test.
     *
     * @param host
     * @param cloudletType
     */
    private void doSinglePing(String host, RollingAverage rollingAverage, ImageServerInterface.CloudletType cloudletType) {
        long latency = 0;
        if(mLatencyTestMethod.equals(LatencyTestMethod.ping)) {
            try {
                String pingCommand = "/system/bin/ping -c 1 " + host;
                String inputLine = "";

                String regex = "(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+) ms";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher;

                // execute the command on the environment interface
                Log.d(TAG, "ping command: "+pingCommand);
                Process process = Runtime.getRuntime().exec(pingCommand);
                // gets the input stream to get the output of the executed command
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                inputLine = bufferedReader.readLine();
                while ((inputLine != null)) {
                    Log.d(TAG, "ping inputLine=" + inputLine);
                    if (inputLine.contains("rtt min")) {
                        // Extract the average round trip time from the inputLine string
                        matcher = pattern.matcher(inputLine);
                        if (matcher.find()) {
                            Log.d(TAG, "ping output=" + matcher.group(0));
                            latency = (long) (Double.parseDouble(matcher.group(1)) * 1000000.0);
                            rollingAverage.add(latency);
                        }
                        break;

                    } else if (inputLine.contains("100% packet loss")) {  // when we get to the last line of executed ping command (all packets lost)
                        mLatencyTestMethod = LatencyTestMethod.socket;
                        mImageServerInterface.showMessage("Ping failed. Switching to socket latency test mode.");
                        break;
                    }
                    inputLine = bufferedReader.readLine();
                }
            }
            catch (IOException e){
                Log.e(TAG, "doSinglePing: EXCEPTION");
                e.printStackTrace();
            }

        } else {
            long startTime = System.nanoTime();
            boolean reachable = isReachable(host, mPort, mSocketTimeout);
            if (reachable) {
                long endTime = System.nanoTime();
                latency = endTime - startTime;
                rollingAverage.add(latency);
                Log.d(TAG, host + " reachable=" + reachable + " Latency=" + (latency / 1000000.0) + " ms.");
            } else {
                Log.d(TAG, host + " reachable=" + reachable);
            }
        }

        mImageServerInterface.updateNetworkStats(cloudletType, rollingAverage);
    }

    public void setLatencyTestMethod(LatencyTestMethod latencyTestMethod) {
        Log.i(TAG, "latencyTestMethod="+latencyTestMethod);
        this.mLatencyTestMethod = latencyTestMethod;
    }

    public String getStatsText() {
        return mLatencyFullProcessRollingAvg.getStatsText() + "\n\n" +
                mLatencyNetOnlyRollingAvg.getStatsText();
    }

    /**
     * Creates a socket and connects to the server with a specified timeout value.
     * @param addr  the Host name or IP address
     * @param openPort  The port number
     * @param timeOutMillis  the timeout value to be used in milliseconds
     * @return  True if socket connection is successfully made.
     */
    public static boolean isReachable(String addr, int openPort, int timeOutMillis) {
        // Any Open port on other machine
        // mOpenPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        try {
            try (Socket soc = new Socket()) {
                soc.connect(new InetSocketAddress(addr, openPort), timeOutMillis);
            }
            return true;
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

}
