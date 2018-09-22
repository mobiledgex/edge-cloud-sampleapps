package com.mobiledgex.sdkdemo.com.mobiledgex.sdkdemo.camera;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mobiledgex.sdkdemo.CloudletListHolder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mobiledgex.sdkdemo.com.mobiledgex.sdkdemo.camera.Camera2BasicFragment.CloudLetType.CLOUDLET_MEX;
import static com.mobiledgex.sdkdemo.com.mobiledgex.sdkdemo.camera.Camera2BasicFragment.CloudLetType.CLOUDLET_PUBLIC;

public class VolleyRequestHandler {
    private static final String TAG = "VolleyRequestHandler";
    /**
     * For every N face detection requests, do a network latency test.
     */
    public static final int PING_INTERVAL = 4;

    private RequestQueue queue;
    private final Camera2BasicFragment mCamera2BasicFragment;

    public double edgeStdDev = 0;
    public double cloudStdDev = 0;
    private long cloudCount = 0;
    private long edgeCount = 0;

    private boolean doNetLatency = true;
    public boolean cloudBusy = false;
    public boolean edgeBusy = false;
    private long cloudLatency = 0;
    private long edgeLatency = 0;
    private final int rollingAvgSize = 100;
    private RollingAverage cloudLatencyRollingAvg = new RollingAverage(rollingAvgSize);
    private RollingAverage edgeLatencyRollingAvg = new RollingAverage(rollingAvgSize);
    private RollingAverage cloudLatencyNetOnlyRollingAvg = new RollingAverage(rollingAvgSize);
    private RollingAverage edgeLatencyNetOnlyRollingAvg = new RollingAverage(rollingAvgSize);

    private static int port = 8000;
    private static String cloudHost = "104.42.217.135"; //West US
    private static String edgeHost = "37.50.143.103"; //Bonn
//    private static String edgeHost = "80.187.128.15"; //Berlin

    //Bruce's private test environment
//    private static String cloudHost = "acrotopia.com";
//    private static String edgeHost = "192.168.1.86";

    private static String cloudAPIEndpoint = "http://"+cloudHost+":"+port;
    private static String edgeAPIEndpoint = "http://"+edgeHost+":"+port;

    //Variables for latency test
    private CloudletListHolder.LatencyTestMethod latencyTestMethod;
    private Handler mHandler;
    private final int socketTimeout = 3000;
    private boolean useRollingAverage = false;

    public VolleyRequestHandler(Camera2BasicFragment camera2BasicFragment) {
        mCamera2BasicFragment = camera2BasicFragment;

        latencyTestMethod = CloudletListHolder.getSingleton().getLatencyTestMethod();

        // Instantiate the RequestQueue.
        queue = Volley.newRequestQueue(camera2BasicFragment.getActivity());

        HandlerThread handlerThread = new HandlerThread("BackgroundPinger");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

    }

    public void sendImage(Bitmap image)
    {
        if (!cloudBusy) {
            sendToCloud(image);
        }

        if (!edgeBusy) {
            sendToEdge(image);
        }
    }

    /**
     * Depending on Latency Testing method preference, perform either a single "ping"
     * or a single socket open test.
     *
     * @param host
     * @param cloudletType
     */
    private void doSinglePing(String host, RollingAverage rollingAverage, Camera2BasicFragment.CloudLetType cloudletType) {
        long latency = 0;
        if(latencyTestMethod.equals(CloudletListHolder.LatencyTestMethod.ping)) {
            try {
                String pingCommand = "/system/bin/ping -c 1 " + host;
                String inputLine = "";

                String regex = "time=(\\d+.\\d+) ms";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher;

                // execute the command on the environment interface
                Process process = Runtime.getRuntime().exec(pingCommand);
                // gets the input stream to get the output of the executed command
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                inputLine = bufferedReader.readLine();
                while ((inputLine != null)) {
                    Log.d(TAG, "inputLine=" + inputLine);
                    if (inputLine.contains("rtt min")) {
                        // Extract the average round trip time from the inputLine string
                        regex = "(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+) ms";
                        pattern = Pattern.compile(regex);
                        matcher = pattern.matcher(inputLine);
                        if (matcher.find()) {
                            Log.d(TAG, "output=" + matcher.group(0));
                            latency = (long) (Double.parseDouble(matcher.group(1)) * 1000000.0);
                            rollingAverage.add(latency);
                        }
                        break;

                    } else if (inputLine.contains("100% packet loss")) {  // when we get to the last line of executed ping command (all packets lost)
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

        mCamera2BasicFragment.updatePing(cloudletType, latency, rollingAverage.getStdDev());
    }

    /**
     * Encode the bitmap and use Volley async to request face detection
     * coordinates. Decode the returned JSON string and update the rectangle
     * on the preview. Also time the transaction to calculate latency.
     *
     * @param bitmap  The image to encode and send.
     */
    private void sendToCloud(Bitmap bitmap) {
        // Get a lock for the busy
        cloudBusy = true;
        if(doNetLatency) {
            cloudCount++;
            if (cloudCount == 1 || cloudCount % PING_INTERVAL == 0) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        doSinglePing(cloudHost, cloudLatencyNetOnlyRollingAvg, CLOUDLET_PUBLIC);
                        cloudBusy = false;
                    }
                });
                return;
            }
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] bytes = byteStream.toByteArray();
        String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);

        final String requestBody = encoded;
        String url = cloudAPIEndpoint+ "/detect3/";

        final long startTime = System.nanoTime();

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "sendToCloud response="+response);
                        long endTime = System.nanoTime();
                        cloudBusy = false;
                        cloudLatency = endTime - startTime;
                        cloudLatencyRollingAvg.add(cloudLatency);
                        cloudStdDev = cloudLatencyRollingAvg.getStdDev();
                        Log.i("BDA", "cloudLatency="+(cloudLatency/1000000.0)+" cloudStdDev="+(cloudStdDev/1000000.0));
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            mCamera2BasicFragment.updateRectangles(CLOUDLET_PUBLIC, jsonArray);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                cloudBusy = false;
                Log.e(TAG, "That didn't work! error="+error);
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.wtf(TAG, "Unsupported Encoding while trying to get the body bytes");
                    return null;
                }
            }
        };

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    /**
     * Encode the bitmap and use Volley async to request face detection
     * coordinates. Decode the returned JSON string and update the rectangle
     * on the preview. Also time the transaction to calculate latency.
     *
     * @param bitmap  The image to encode and send.
     */
    private void sendToEdge(Bitmap bitmap) {
        // Get a lock for the busy
        edgeBusy = true;
        if(doNetLatency) {
            edgeCount++;
            if (edgeCount == 1 || edgeCount % PING_INTERVAL == 0) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        doSinglePing(edgeHost, edgeLatencyNetOnlyRollingAvg, CLOUDLET_MEX);
                        edgeBusy = false;
                    }
                });
                return;
            }
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] bytes = byteStream.toByteArray();
        String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);

        final String requestBody = encoded;
        String url = edgeAPIEndpoint+ "/detect3/";

        final long startTime = System.nanoTime();

        // Request a byte response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "sendToEdge response="+response);
                        long endTime = System.nanoTime();
                        edgeBusy = false;
                        edgeLatency = endTime - startTime;
                        edgeLatencyRollingAvg.add(edgeLatency);
                        edgeStdDev = edgeLatencyRollingAvg.getStdDev();
                        Log.i("BDA", "edgeLatency="+(edgeLatency/1000000.0)+" edgeStdDev="+(edgeStdDev/1000000.0));
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            mCamera2BasicFragment.updateRectangles(CLOUDLET_MEX, jsonArray);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                edgeBusy = false;
                Log.e(TAG, "That didn't work! error="+error);
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.wtf(TAG, "Unsupported Encoding while trying to get the body bytes");
                    return null;
                }
            }
        };

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    private static boolean isReachable(String addr, int openPort, int timeOutMillis) {
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

    public long getCloudLatency()
    {
        if(useRollingAverage) {
            return cloudLatencyRollingAvg.getAverage();
        } else {
            return cloudLatency;
        }
    }

    public long getEdgeLatency()
    {
        if(useRollingAverage) {
            return edgeLatencyRollingAvg.getAverage();
        } else {
            return edgeLatency;
        }
    }

    public void setDoNetLatency(boolean doNetLatency) {
        this.doNetLatency = doNetLatency;
    }
    public void setUseRollingAverage(boolean useRollingAverage) {
        this.useRollingAverage = useRollingAverage;
    }

    public double getCloudStdDev() {
        return cloudStdDev;
    }

    public double getEdgeStdDev() {
        return edgeStdDev;
    }

    public class RollingAverage {

        private final long[] window;
        private float sum = 0f;
        private int fill;
        private int position;


        public RollingAverage(int size) {
            this.window=new long[size];
        }

        public void add(long number) {

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
    }
}
