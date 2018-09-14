package com.mobiledgex.sdkdemo.com.mobiledgex.sdkdemo.camera;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import static com.mobiledgex.sdkdemo.com.mobiledgex.sdkdemo.camera.Camera2BasicFragment.CloudLetType.CLOUDLET_MEX;
import static com.mobiledgex.sdkdemo.com.mobiledgex.sdkdemo.camera.Camera2BasicFragment.CloudLetType.CLOUDLET_PUBLIC;

public class VolleyRequestHandler {
    private static final String TAG = "VolleyRequestHandler";

    private final Camera2BasicFragment mCamera2BasicFragment;
    public double edgeStdDev = 0;
    public double cloudStdDev = 0;
    private int N = 100;
    private int cn = 0;
    private int en = 0;
    private long[] cloudStdArr = new long[100];
    private long[] edgeStdArr = new long[100];

    public boolean cloudBusy = false;
    public boolean edgeBusy = false;

    private long cloudLatency = 0;
    private long edgeLatency = 0;

    private Rect cloudRect = new Rect(0,0,0,0);
    private Rect edgeRect = new Rect(0, 0, 0,0);

//    private static String cloudAPIEndpoint = "http://104.42.217.135:8000"; //west us
//    private static String edgeAPIEndpoint = "http://37.50.143.103:8000"; //Bonn
//    private static String edgeAPIEndpoint = "http://80.187.128.15:8000"; //Berlin

    //Bruce's private test environment
    private static String cloudAPIEndpoint = "http://acrotopia.com:8000";
    private static String edgeAPIEndpoint = "http://192.168.1.86:8000";

    RequestQueue queue;

    public VolleyRequestHandler(Camera2BasicFragment camera2BasicFragment) {
        mCamera2BasicFragment = camera2BasicFragment;
        // Instantiate the RequestQueue.
        queue = Volley.newRequestQueue(camera2BasicFragment.getActivity());
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
     * Encode the bitmap and use Volley async to request face detection
     * coordinates. Decode the returned JSON string and update the rectangle
     * on the preview. Also time the transaction to calculate latency.
     *
     * @param bitmap  The image to encode and send.
     */
    private void sendToCloud(Bitmap bitmap) {
        // Get a lock for the busy
        cloudBusy = true;

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] bytes = byteStream.toByteArray();
        String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);

        final String requestBody = encoded;
        String url = cloudAPIEndpoint+ "/detect2/";

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
                        cloudStdArr[cn] = cloudLatency;
                        cn += 1;
                        cn %= N;
                        cloudStdDev = stdDev(cloudStdArr, N);
                        try {
                            JSONObject jsonRect = new JSONObject(response);
                            cloudRect.left = jsonRect.getInt("left");
                            cloudRect.top = jsonRect.getInt("top");
                            cloudRect.right = jsonRect.getInt("right");
                            cloudRect.bottom = jsonRect.getInt("bottom");
                            mCamera2BasicFragment.updateRectangles(CLOUDLET_PUBLIC, cloudRect);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                cloudBusy = false;
                Log.e(TAG, "That didn't work! error="+error);
                long endTime = System.nanoTime();
                cloudLatency = endTime - startTime;
                cloudStdArr[cn] = cloudLatency;
                cn += 1;
                cn %= N;
                cloudStdDev = stdDev(cloudStdArr, N);
                cloudBusy = false;
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

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] bytes = byteStream.toByteArray();
        String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);

        final String requestBody = encoded;
        String url = edgeAPIEndpoint+ "/detect2/";

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
                        edgeStdArr[en] = edgeLatency;
                        cn += 1;
                        cn %= N;
                        edgeStdDev = stdDev(edgeStdArr, N);
                        try {
                            JSONObject jsonRect = new JSONObject(response);
                            edgeRect.left = jsonRect.getInt("left");
                            edgeRect.top = jsonRect.getInt("top");
                            edgeRect.right = jsonRect.getInt("right");
                            edgeRect.bottom = jsonRect.getInt("bottom");
                            mCamera2BasicFragment.updateRectangles(CLOUDLET_MEX, edgeRect);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                edgeBusy = false;
                Log.e(TAG, "That didn't work! error="+error);
                long endTime = System.nanoTime();
                edgeLatency = endTime - startTime;
                edgeStdArr[en] = edgeLatency;
                cn += 1;
                cn %= N;
                edgeStdDev = stdDev(edgeStdArr, N);
                edgeBusy = false;
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

    private double average(long[] arr, int len)
    {
        double sum = 0;
        for (int i = 0; i < len; i++) {
            sum += arr[i];
        }
        return sum /= len;
    }

    private double stdDev(long[] arr, int len)
    {
        double avg = average(arr, len);
        double sum = 0;

        for (int i = 0; i < len; i++) {
            sum += Math.pow(arr[i] - avg, 2);
        }

        return Math.sqrt(sum/len);
    }

    public long getCloudLatency()
    {
        return cloudLatency;
    }

    public long getEdgeLatency()
    {
        return edgeLatency;
    }

}
