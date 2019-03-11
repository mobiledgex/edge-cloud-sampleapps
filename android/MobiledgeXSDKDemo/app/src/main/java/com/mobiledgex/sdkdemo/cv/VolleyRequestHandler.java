package com.mobiledgex.sdkdemo.cv;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mobiledgex.sdkdemo.Account;
import com.mobiledgex.sdkdemo.CloudletListHolder;
import com.mobiledgex.sdkdemo.R;

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
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mobiledgex.sdkdemo.cv.ImageProcessorFragment.CloudletType.EDGE;
import static com.mobiledgex.sdkdemo.cv.ImageProcessorFragment.CloudletType.CLOUD;

public class VolleyRequestHandler {
    private static final String TAG = "VolleyRequestHandler";
    /**
     * For every N face detection requests, do a network latency test.
     */
    public static final int PING_INTERVAL = 4;
    public static final int TRAINING_COUNT_TARGET = 10;

    private RequestQueue queue;
    private final ImageServerInterface mImageServerInterface;

    private boolean doNetLatency = true;
    private final int rollingAvgSize = 100;

    public static final String DEF_FACE_HOST_EDGE = "facedetection.defaultedge.mobiledgex.net";
    public static final String DEF_FACE_HOST_CLOUD = "facedetection.defaultcloud.mobiledgex.net";

    //Bruce's private test environment
//    public static String DEF_FACE_HOST_CLOUD = "acrotopia.com";
//    public static String DEF_FACE_HOST_EDGE = "192.168.1.86";
//    public static String DEF_FACE_HOST_EDGE = "10.157.107.83";

    private String cloudHost;
    private String edgeHost;

    public ImageSender cloudImageSender;
    public ImageSender edgeImageSender;

    //Variables for latency test
    public CloudletListHolder.LatencyTestMethod latencyTestMethod;
    private final int socketTimeout = 3000;
    private String mSubject = "";

    public String getStatsText() {
        return edgeImageSender.latencyFullProcessRollingAvg.getStatsText() + "\n\n" +
                edgeImageSender.latencyNetOnlyRollingAvg.getStatsText() + "\n\n" +
                cloudImageSender.latencyFullProcessRollingAvg.getStatsText() + "\n\n" +
                cloudImageSender.latencyNetOnlyRollingAvg.getStatsText();
    }

    enum CameraMode {
        FACE_DETECTION,
        FACE_RECOGNITION,
        FACE_TRAINING,
        FACE_UPDATING_SERVER,
        POSE_DETECTION
    }

    public static int getFaceServerPort(String hostName) {
        int port;
        port = 8008;
        Log.i(TAG, "getFaceServerPort("+hostName+")="+port);
        return port;
    }

    public VolleyRequestHandler(ImageServerInterface imageServerInterface, Activity activity) {
        mImageServerInterface = imageServerInterface;

        latencyTestMethod = CloudletListHolder.getSingleton().getLatencyTestMethod();
        if(Account.getSingleton().isSignedIn()) {
            mSubject = Account.getSingleton().getGoogleSignInAccount().getDisplayName();
        }

        // Get hosts from preferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        cloudHost = prefs.getString(activity.getResources().getString(R.string.preference_fd_host_cloud), DEF_FACE_HOST_CLOUD);
        edgeHost = prefs.getString(activity.getResources().getString(R.string.preference_fd_host_edge), DEF_FACE_HOST_EDGE);

        Log.i(TAG, "cloudHost="+cloudHost);
        Log.i(TAG, "edgeHost="+edgeHost);

        cloudImageSender = new ImageSender(cloudHost, CLOUD);
        edgeImageSender = new ImageSender(edgeHost, EDGE);

        // Instantiate the RequestQueue.
        queue = Volley.newRequestQueue(activity);
    }

    public void setCameraMode(CameraMode mode) {
        cloudImageSender.setCameraMode(mode);
        edgeImageSender.setCameraMode(mode);
    }

    public void sendImage(Bitmap image) {
        cloudImageSender.sendImage(image);
        edgeImageSender.sendImage(image);
    }

    public void setSubjectName(String subjectName) {
        Log.d(TAG, "setSubjectName="+subjectName);
        mSubject = subjectName;
    }

    public class ImageSender {
        public String host;
        private int port;
        private RollingAverage latencyFullProcessRollingAvg;
        private RollingAverage latencyNetOnlyRollingAvg;
        public int trainingCount;
        public boolean busy;
        private long latency = 0;
        public CameraMode mCameraMode;
        private String djangoUrl = "/detector/detect/";
        private ImageProcessorFragment.CloudletType cloudLetType;
        private Handler mHandler;


        public ImageSender(String host, ImageProcessorFragment.CloudletType cloudLetType) {
            this.host = host;
            this.cloudLetType = cloudLetType;
            latencyFullProcessRollingAvg = new RollingAverage(cloudLetType, "Full Process", rollingAvgSize);
            latencyNetOnlyRollingAvg = new RollingAverage(cloudLetType, "Network Only", rollingAvgSize);
            port = getFaceServerPort(host);
            HandlerThread handlerThread = new HandlerThread("BackgroundPinger"+cloudLetType);
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
        }

        public void setCameraMode(CameraMode mode) {
            mCameraMode = mode;
            if(mode == CameraMode.FACE_DETECTION || mode == CameraMode.FACE_UPDATING_SERVER) {
                djangoUrl = "/detector/detect/";
            } else if (mode == CameraMode.FACE_RECOGNITION){
                djangoUrl = "/recognizer/predict/";
            } else if (mode == CameraMode.FACE_TRAINING){
                djangoUrl = "/recognizer/add/";
                trainingCount = 0;
            } else if (mode == CameraMode.POSE_DETECTION){
                djangoUrl = "/openpose/detect/";
            } else {
                Log.e(TAG, "Invalid CameraMode: "+mode);
            }
            Log.i(TAG, "setCameraMode("+mCameraMode+") djangoUrl="+djangoUrl);
        }

        /**
         * Encode the bitmap and use Volley async to request face detection
         * coordinates. Decode the returned JSON string and update the rectangles
         * on the preview. Also time the transaction to calculate latency.
         *
         * @param bitmap  The image to encode and send.
         */
        private void sendImage(Bitmap bitmap) {
            if(busy) {
                return;
            }
            // Get a lock for the busy
            busy = true;
            if(doNetLatency) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            doSinglePing(host, latencyNetOnlyRollingAvg, cloudLetType);
                        }
                    });
            }

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 67, byteStream);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            byte[] bytes = byteStream.toByteArray();
            String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);

            final String requestBody = encoded;
            String url = "http://"+host+":"+port + djangoUrl;
            Log.i(TAG, "url="+url+" length: "+requestBody.length());

            final long startTime = System.nanoTime();

            // Request a byte response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, cloudLetType+" sendImage response="+response);
                            long endTime = System.nanoTime();
                            busy = false;
                            latency = endTime - startTime;
                            latencyFullProcessRollingAvg.add(latency);
                            mImageServerInterface.updateFullProcessStats(cloudLetType, latencyFullProcessRollingAvg);
                            Log.i(TAG, cloudLetType+" mCameraMode="+mCameraMode+" latency="+(latency/1000000.0));
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                JSONArray rects;
                                String subject = null;
                                if(jsonObject.getBoolean("success")) {
                                    if(mCameraMode == CameraMode.POSE_DETECTION) {
                                        JSONArray poses = jsonObject.getJSONArray("poses");
                                        mImageServerInterface.updateOverlay(cloudLetType, poses);
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
                                        mImageServerInterface.updateOverlay(cloudLetType, rects, subject);
                                    }

                                    if(mCameraMode == CameraMode.FACE_TRAINING) {
                                        trainingCount++;
                                        Log.i(TAG, cloudLetType+" trainingCount="+trainingCount);
                                        CameraMode mode = CameraMode.FACE_TRAINING;
                                        if(trainingCount >= TRAINING_COUNT_TARGET) {
                                            updateTraining();
                                        }
                                        mImageServerInterface.updateTrainingProgress(cloudImageSender.trainingCount, edgeImageSender.trainingCount);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    busy = false;
                    Log.e(TAG, "That didn't work! error="+error);
                }
            }) {

                @Override
                protected Map<String,String> getParams(){
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("subject", mSubject);
                    params.put("image", requestBody);
                    if(Account.getSingleton().isSignedIn()) {
                        if(mSubject != Account.getSingleton().getGoogleSignInAccount().getDisplayName()) {
                            params.put("owner", Account.getSingleton().getGoogleSignInAccount().getDisplayName());
                        }
                    }
                    return params;
                }
            };

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        }

        private void updateTraining() {
            Log.i(TAG, cloudLetType+" updateTraining mCameraMode="+mCameraMode);
            if(mCameraMode == CameraMode.FACE_UPDATING_SERVER) {
                return;
            }
            setCameraMode(CameraMode.FACE_UPDATING_SERVER);

            String url = "http://"+host+":"+port + "/recognizer/train/";
            Log.i(TAG, cloudLetType+" url="+url);

            final long startTime = System.nanoTime();

            // Request a byte response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, cloudLetType+" updateTraining response="+response);
                            long endTime = System.nanoTime();
                            long elapsed = endTime - startTime;
                            Log.i(TAG, cloudLetType+" updateTraining elapsed="+(elapsed/1000000.0));
                            if(response.startsWith("OK")) {
                                Log.i(TAG, cloudLetType+" updateTraining successful");
                                setCameraMode(CameraMode.FACE_RECOGNITION);
                            } else {
                                Log.i(TAG, cloudLetType+" updateTraining failed!");
                            }
                            mImageServerInterface.updateTrainingProgress(cloudImageSender.trainingCount, edgeImageSender.trainingCount);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    busy = false;
                    Log.e(TAG, "That didn't work! error="+error);
                }
            }) {
                @Override
                protected Map<String,String> getParams(){
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("subject", mSubject);
                    return params;
                }
            };

            // Add the request to the RequestQueue.
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(stringRequest);
        }

        /**
         * Depending on Latency Testing method preference, perform either a single "ping"
         * or a single socket open test.
         *
         * @param host
         * @param cloudletType
         */
        private void doSinglePing(String host, RollingAverage rollingAverage, ImageProcessorFragment.CloudletType cloudletType) {
            long latency = 0;
            if(latencyTestMethod.equals(CloudletListHolder.LatencyTestMethod.ping)) {
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
                            latencyTestMethod = CloudletListHolder.LatencyTestMethod.socket;
                            mImageServerInterface.showToast("Ping failed. Switching to socket latency test mode.");
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
                boolean reachable = isReachable(host, port, socketTimeout);
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

    }

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

    public void setDoNetLatency(boolean doNetLatency) {
        this.doNetLatency = doNetLatency;
    }

    public static class RollingAverage {

        private final long[] window;
        private float sum = 0f;
        private int fill;
        private int position;
        private ImageProcessorFragment.CloudletType cloudLetType;
        private String name;
        private long current;

        public RollingAverage(ImageProcessorFragment.CloudletType cloudLetType, String name, int size) {
            this.cloudLetType = cloudLetType;
            this.name = name;
            this.window=new long[size];
        }

        public void add(long number) {
            current = number;

            if(fill==window.length){
                sum-=window[position];
            }else{
                fill++;
            }

            sum+=number;
            window[position++]=number;

            if(position == window.length){
                position=0;
            }

        }

        public long getCurrent() { return current; }

        public long getAverage() {
            return (long) (sum / fill);
        }

        /**
         * Get the standard deviation of the entire set.
         * @return Population Standard Deviation, σ
         */
        public long getStdDev() {
            double avg = getAverage();
            double sum = 0;

            for (int i = 0; i < fill; i++) {
                sum += Math.pow(window[i] - avg, 2);
            }

            return (long) Math.sqrt(sum/fill);
        }

        /**
         * Get a textual summary of the stats.
         * @return  String to display in network stats window.
         */
        public String getStatsText() {
            if(fill == 0) {
                return "";
            }
            String stats = cloudLetType + " " + name + " Latency:\n";
            DecimalFormat decFor = new DecimalFormat("#.##");
            long min = Integer.MAX_VALUE;
            long max = -1;
            for(int i = 0; i < fill; i++) {
//                stats += i + ". time=" + decFor.format(window[i] / 1000000) + " ms\n";
                if(window[i] / 1000000 < min) {
                    min = window[i] / 1000000;
                }
                if(window[i] / 1000000 > max) {
                    max = window[i] / 1000000;
                }
            }
            String avg = decFor.format(getAverage() / 1000000);
            String stdDev = decFor.format(getStdDev() / 1000000);
            stats += "min/avg/max/stddev = "+decFor.format(min)+"/"+avg+"/"+decFor.format(max)+"/"+stdDev+" ms";
            return stats;
        }
    }
}
