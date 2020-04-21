/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
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

package com.mobiledgex.computervision;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

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

import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ImageSender {
    private static final String TAG = "ImageSender";
    public static final int TRAINING_COUNT_TARGET = 10;
    private static final double RECOGNITION_CONFIDENCE_THRESHOLD = 105;
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private ImageServerInterface mImageServerInterface;
    private RequestQueue mRequestQueue;

    private String mHost;
    private int mPort;
    private int mPersistentTcpPort;
    private RollingAverage mLatencyFullProcessRollingAvg;
    private RollingAverage mLatencyNetOnlyRollingAvg;
    private int mTrainingCount;
    private boolean mBusy;
    private boolean mInactiveBenchmark;
    private boolean mInactiveFailure;
    private long mLatency = 0;
    private boolean mDoNetLatency = true;
    private final int mRollingAvgSize = 100;
    private CameraMode mCameraMode;
    private String mDjangoUrl = "/detector/detect/";
    private ImageServerInterface.CloudletType mCloudLetType;
    private Handler mHandler;

    //Variables for latency test
    private LatencyTestMethod mLatencyTestMethod = LatencyTestMethod.socket;
    private final int mSocketTimeout = 3000;

    private GoogleSignInAccount mAccount;
    private String mGuestName = "";

    private static ConnectionMode preferencesConnectionMode = ConnectionMode.REST;
    private ConnectionMode mConnectionMode;
    private SocketClientTcp mSocketClientTcp;

    private long mStartTime;
    private int mOpcode;

    private OkHttpClient mWebSocketClient;
    private WebSocket mWebSocket;

    public enum ConnectionMode {
        REST,
        PERSISTENT_TCP,
        WEBSOCKET,
        QUIC
    }

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
        POSE_DETECTION,
        OBJECT_DETECTION
    }

    static class Builder {
        private Activity activity;
        private ImageServerInterface imageServerInterface;
        private ImageServerInterface.CloudletType cloudLetType;
        private String host;
        private int port;
        private int persistentTcpPort;
        private ImageSender.CameraMode cameraMode;

        public Builder setActivity(Activity activity) {
            this.activity = activity;
            return this;
        }

        public Builder setImageServerInterface(ImageServerInterface imageServerInterface) {
            this.imageServerInterface = imageServerInterface;
            return this;
        }

        public Builder setCloudLetType(ImageServerInterface.CloudletType cloudLetType) {
            this.cloudLetType = cloudLetType;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setPersistentTcpPort(int persistentTcpPort) {
            this.persistentTcpPort = persistentTcpPort;
            return this;
        }

        public Builder setCameraMode(ImageSender.CameraMode cameraMode) {
            this.cameraMode = cameraMode;
            return this;
        }

        public ImageSender build() {
            return new ImageSender(this);
        }
    }

    private ImageSender(final Builder builder) {
        mCloudLetType = builder.cloudLetType;
        mHost = builder.host;
        mPort = builder.port;
        mPersistentTcpPort = builder.persistentTcpPort;
        setCameraMode(builder.cameraMode);

        mImageServerInterface = builder.imageServerInterface;

        mAccount = GoogleSignIn.getLastSignedInAccount(builder.activity);
        if(mAccount != null) {
            Log.i(TAG, "mAccount=" + mAccount.getDisplayName()+" "+mAccount.getId());
        } else {
            Log.i(TAG, "mAccount=" + mAccount);
        }

        mLatencyFullProcessRollingAvg = new RollingAverage(mCloudLetType, "Full Process", mRollingAvgSize);
        mLatencyNetOnlyRollingAvg = new RollingAverage(mCloudLetType, "Network Only", mRollingAvgSize);
        HandlerThread handlerThread = new HandlerThread("BackgroundPinger"+mCloudLetType);
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        HandlerThread handlerThreadPersistentTcp = new HandlerThread("PersistentTcp"+mCloudLetType);
        handlerThreadPersistentTcp.start();

        // Instantiate the RequestQueue.
        mRequestQueue = Volley.newRequestQueue(builder.activity);

        Log.i(TAG, "preferencesConnectionMode="+ preferencesConnectionMode);
        if(mCloudLetType == ImageServerInterface.CloudletType.PUBLIC) {
            // The Face Training server only supports REST.
            mConnectionMode = ConnectionMode.REST;
        } else {
            mConnectionMode = preferencesConnectionMode;
        }

        if(mConnectionMode == ConnectionMode.PERSISTENT_TCP) {
            initTcpSocketConnection();
        }

        if (mConnectionMode == ConnectionMode.WEBSOCKET) {
            startWebSocketClient();
        }
    }

    private void initTcpSocketConnection() {
        mSocketClientTcp = null;
        // connect to the server
        ConnectTask connectTask = new ConnectTask();
        connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private final class ResultWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, okhttp3.Response response) {
            Log.i(TAG, "onOpen response="+response);
        }
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.i(TAG, "onMessage text="+text);
            mBusy = false;
            long endTime = System.nanoTime();
            mLatency = endTime - mStartTime;
            handleResponse(text, mLatency);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            Log.i(TAG, "Received bytes: " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            Log.i(TAG, "Closing: " + code + " / " + reason);
            mWebSocketClient.dispatcher().cancelAll();
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
            String message = "WebSocket Error: " + t.getMessage();
            Log.e(TAG, message);
            if (response != null && response.code() == 404) {
                mImageServerInterface.showError("WebSockets support not yet deployed to "+mCloudLetType+" server.");
                mInactiveFailure = true;
            } else {
                mImageServerInterface.showMessage(message, Toast.LENGTH_LONG);
            }
            mWebSocketClient.dispatcher().cancelAll();
        }
    }

    private void startWebSocketClient() {
        String url = "ws://"+mHost+":8008/ws"+mDjangoUrl;
        okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
        ResultWebSocketListener listener = new ResultWebSocketListener();
        mWebSocketClient = new OkHttpClient();
        mWebSocket = mWebSocketClient.newWebSocket(request, listener);
        mWebSocketClient.dispatcher().executorService().shutdown();
        Log.i(TAG, "Started WebSocket client. url: " + url);
    }

    /*receive the message from server with asyncTask*/
    public class ConnectTask extends AsyncTask<String,String,Void> {
        @Override
        protected Void doInBackground(String... message) {
            mSocketClientTcp = new SocketClientTcp(mHost, mPersistentTcpPort,
                    new SocketClientTcp.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    try {
                        if(message!=null) {
                            long endTime = System.nanoTime();
                            mBusy = false;
                            mLatency = endTime - mStartTime;
                            handleResponse(message, mLatency);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            Log.i(TAG, "mSocketClientTcp="+ mSocketClientTcp);
            mSocketClientTcp.run();
            return null;
        }
    }

    public void closeConnection() {
        Log.i(TAG, "Disconnecting socket for "+mCloudLetType+" mConnectionMode="+mConnectionMode);
        if (mWebSocket != null) {
            mWebSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
        }
        if (mSocketClientTcp != null) {
            mSocketClientTcp.stopClient();
        }
    }

    public void setGuestName(String guestName) {
        Log.d(TAG, "setGuestName="+guestName);
        mGuestName = guestName;
    }

    public void setCameraMode(CameraMode mode) {
        mCameraMode = mode;
        if(mode == CameraMode.FACE_DETECTION) {
            mOpcode = 1;
            mDjangoUrl = "/detector/detect/";
        } else if (mode == CameraMode.FACE_RECOGNITION){
            mOpcode = 2;
            mDjangoUrl = "/recognizer/predict/";
        } else if (mode == CameraMode.FACE_TRAINING) {
            mOpcode = 0;
            mDjangoUrl = "/trainer/add/";
            mTrainingCount = 0;
        } else if (mode == CameraMode.FACE_UPDATING_SERVER) {
            mOpcode = 0;
            mDjangoUrl = "/trainer/predict/";
        } else if (mode == CameraMode.POSE_DETECTION){
            mOpcode = 3;
            mDjangoUrl = "/openpose/detect/";
        } else if (mode == CameraMode.OBJECT_DETECTION){
            mOpcode = 4;
            mDjangoUrl = "/object/detect/";
        } else {
            Log.e(TAG, "Invalid CameraMode: "+mode);
        }
        Log.i(TAG, "setCameraMode("+mCameraMode+") mOpcode="+mOpcode+" mDjangoUrl="+ mDjangoUrl +" "+mCloudLetType+" host="+mHost);
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
        Log.i(TAG, "sendImage()");
        if(mBusy || mInactiveBenchmark || mInactiveFailure) {
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

        final byte[] bytes = byteStream.toByteArray();
        Log.d(TAG, mCloudLetType+" bytes.length="+bytes.length);

        mStartTime = System.nanoTime();

        // Depending on the connection mode, choose the appropriate way to send the image
        // data to the server.
        if(mConnectionMode == ConnectionMode.PERSISTENT_TCP) {
            if(mSocketClientTcp == null) {
                // The value may have been changed after starting the activity.
                Log.w(TAG, "mSocketClientTcp not initialized yet. Initializing now.");
                initTcpSocketConnection();
                // Try again next image frame that's received.
                mBusy = false;
                return;
            }
            mSocketClientTcp.send(mOpcode, bytes);

        } else if(mConnectionMode == ConnectionMode.REST) {
            final String requestBody = Base64.encodeToString(bytes, Base64.DEFAULT);
            String url = "http://"+ mHost +":"+mPort + mDjangoUrl;
            Log.i(TAG, "url="+url+" length: "+requestBody.length());

            // Request a byte response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, mCloudLetType + " sendImage response=" + response);
                            long endTime = System.nanoTime();
                            mBusy = false;
                            mLatency = endTime - mStartTime;
                            handleResponse(response, mLatency);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mBusy = false;
                    String message = "sendImage received error=" + error;
                    mImageServerInterface.showMessage(message, Toast.LENGTH_SHORT);
                    Log.e(TAG, message);
                }
            }) {

                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = getUserParams();
                    params.put("image", requestBody);
                    return params;
                }
            };

            // Add the request to the RequestQueue.
            mRequestQueue.add(stringRequest);
        } else if(mConnectionMode == ConnectionMode.WEBSOCKET) {
            mWebSocket.send(ByteString.of(bytes));
        } else {
            Log.e(TAG, "Unknown communication mode: "+ mConnectionMode);
        }
    }

    /**
     * Both the persistent TCP server and the REST server will return results in the same JSON
     * format. This method parses the results and updates the UI with the returned values.
     *
     * @param response
     * @param latency
     */
    private void handleResponse(String response, long latency) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray rects;
            String subject = null;
            if (jsonObject.getBoolean("success")) {
                if (mCameraMode == CameraMode.POSE_DETECTION) {
                    JSONArray poses = jsonObject.getJSONArray("poses");
                    mImageServerInterface.updateOverlay(mCloudLetType, poses, null);
                } else if (mCameraMode == CameraMode.OBJECT_DETECTION) {
                    JSONArray objects = jsonObject.getJSONArray("objects");
                    mImageServerInterface.updateOverlay(mCloudLetType, objects, null);
                } else {
                    if (jsonObject.has("subject")) {
                        //This means it was from recognition mode
                        subject = jsonObject.getString("subject");
                        double confidence = jsonObject.getDouble("confidence");
                        if (confidence > RECOGNITION_CONFIDENCE_THRESHOLD) {
                            subject = subject+"\n[DOUBTFUL]";
                        }
                        JSONArray rect = jsonObject.getJSONArray("rect");
                        rects = new JSONArray();
                        rects.put(rect);
                    } else {
                        //Default is from face detection mode
                        rects = jsonObject.getJSONArray("rects");
                    }
                    mImageServerInterface.updateOverlay(mCloudLetType, rects, subject);
                }

                if (mCameraMode == CameraMode.FACE_TRAINING) {
                    mTrainingCount++;
                    Log.i(TAG, mCloudLetType + " mTrainingCount=" + mTrainingCount);
                    mImageServerInterface.updateTrainingProgress(mTrainingCount, mCameraMode);
                }
            } else {
                Log.i(TAG, "None found in image");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mLatencyFullProcessRollingAvg.add(latency);
        mImageServerInterface.updateFullProcessStats(mCloudLetType, mLatencyFullProcessRollingAvg);
        Log.i(TAG, mCloudLetType + " mCameraMode=" + mCameraMode + " mLatency=" + (mLatency / 1000000.0)+" mHost="+mHost);
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
        Log.d(TAG, "doSinglePing mLatencyTestMethod="+mLatencyTestMethod+" cloudletType="+cloudletType);

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
                        mImageServerInterface.showMessage("Ping failed. Switching to socket latency test mode.", Toast.LENGTH_SHORT);
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

    /**
     * Sets the latency test method.
     * @param latencyTestMethod  Either ping or socket.
     */
    public void setLatencyTestMethod(LatencyTestMethod latencyTestMethod) {
        Log.i(TAG, "latencyTestMethod="+latencyTestMethod);
        this.mLatencyTestMethod = latencyTestMethod;
    }

    /**
     * Sets the static preferencesConnectionMode for the class, and if any non-null instances are
     * included, sets the mConnectionMode for each.
     *
     * @param preferencesConnectionMode  Either REST or PERSISTENT_TCP.
     * @param imageSenders Any ImageSender instances to set the instance variable value on.
     */
    public static void setPreferencesConnectionMode(ConnectionMode preferencesConnectionMode, ImageSender... imageSenders) {
        Log.i(TAG, "setPreferencesConnectionMode("+ preferencesConnectionMode +")");
        ImageSender.preferencesConnectionMode = preferencesConnectionMode;
        for(ImageSender imageSender: imageSenders) {
            Log.i(TAG, "setPreferencesConnectionMode imageSender="+imageSender);
            if(imageSender != null) {
                imageSender.mConnectionMode = preferencesConnectionMode;
            }
        }
    }

    /**
     * Return statistics information to be displayed in dialog after activity.
     * @return  The statistics text.
     */
    public String getStatsText() {
        if (mInactiveBenchmark || mInactiveFailure) {
            // We don't want any results in this case.
            return "";
        }
        String statsText = mCloudLetType +" hostname: "+mHost+ "\n" +
                "Connection mode="+mConnectionMode + "\n" +
                "Latency test method="+mLatencyTestMethod+"\n\n" +
                mLatencyFullProcessRollingAvg.getStatsText() + "\n\n" +
                mLatencyNetOnlyRollingAvg.getStatsText();
        Log.i(TAG, "getStatsText\n"+statsText);
        return statsText;
    }

    /**
     * Returns the hostname.
     * @return  the hostname
     */
    public String getHost() {
        return mHost;
    }

    /**
     * Set the hostname.
     * @param host  The new hostname to use.
     */
    public void setHost(String host) {
        mHost = host;
    }

    public boolean isInactiveBenchmark() {
        return mInactiveBenchmark;
    }

    public void setInactiveBenchmark(boolean inactiveBenchmark) {
        this.mInactiveBenchmark = inactiveBenchmark;
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
