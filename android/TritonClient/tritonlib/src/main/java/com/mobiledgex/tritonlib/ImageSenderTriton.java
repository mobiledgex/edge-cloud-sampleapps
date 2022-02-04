/**
 * Copyright 2018-2021 MobiledgeX, Inc. All rights and licenses reserved.
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

package com.mobiledgex.tritonlib;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mobiledgex.computervision.ImageSender;
import com.mobiledgex.computervision.ImageServerInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ImageSenderTriton extends ImageSender {
    private static final String TAG = "ImageSenderTriton";
    public static final int INCEPTION_INFERENCE_HEADER_CONTENT_LENGTH = 235;
    private String mModelName;
    private String mTritonUrl;

    public static class Builder {
        private Activity activity;
        private ImageServerInterface imageServerInterface;
        private ImageServerInterface.CloudletType cloudLetType;
        private boolean tls = false;
        private String host;
        private String modelName;
        private int port;
        private CameraMode cameraMode;

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

        public Builder setTls(boolean tls) {
            this.tls = tls;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setModelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setCameraMode(CameraMode cameraMode) {
            this.cameraMode = cameraMode;
            return this;
        }

        public ImageSenderTriton build() {
            return new ImageSenderTriton(this);
        }
    }

    private ImageSenderTriton(final Builder builder) {
        Log.i(TAG, "mLatencyFullProcessRollingAvg="+mLatencyFullProcessRollingAvg);
        mCloudLetType = builder.cloudLetType;
        mTls = builder.tls;
        mHost = builder.host;
        mPort = builder.port;
        mModelName = builder.modelName;
        mContext = builder.activity;
        setCameraMode(builder.cameraMode);

        mImageServerInterface = builder.imageServerInterface;

        Log.i(TAG, "ImageSenderTriton "+mCloudLetType+" "+mHost+":"+mPort+" mTls="+mTls+" mCameraMode="+mCameraMode);
        super.init();
        Log.i(TAG, "mConnectionMode="+mConnectionMode);
    }

    /**
     * Encode the bitmap and use Volley async to request face detection/recognition
     * coordinates. Decode the returned JSON string and update the rectangles
     * on the preview. Also time the transaction to calculate latency.
     *
     * @param bitmap  The image to encode and send.
     */
    public void sendImage(Bitmap bitmap) {
        Log.d(TAG, mCloudLetType+" sendImage()");
        if(mBusy || mInactive || mInactiveBenchmark || mInactiveFailure) {
            return;
        }

        // Get a lock for the busy
        mBusy = true;
        if(mDoNetLatency) {
            if (mHandler == null) {
                // Too soon.
                return;
            }
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

        if(mConnectionMode == ConnectionMode.REST) {
            String bodyTemplate;
            mScheme =  mTls ? "https" : "http";
            if (mModelName.equals("ensemble_dali_inception")) {
                bodyTemplate = "{\"inputs\":[{\"name\":\"INPUT\",\"shape\":[1,$size]," +
                        "\"datatype\":\"UINT8\",\"parameters\":{\"binary_data_size\":$size}}]," +
                        "\"outputs\":[{\"name\":\"OUTPUT\",\"parameters\":{\"classification\":1," +
                        "\"binary_data\":true}}]}";
            } else if (mModelName.equals("ensemble_dali_yolov4")) {
                bodyTemplate = "{\"inputs\":[{\"name\":\"IMAGE\",\"shape\":[1,$size]," +
                        "\"datatype\":\"UINT8\",\"parameters\":{\"binary_data_size\":$size}}]," +
                        "\"outputs\":[{\"name\":\"OBJECTS_JSON\"}]}";
            } else {
                Log.e(TAG, "Unknown model name: "+mModelName);
                return;
            }
            String body = bodyTemplate.replace("$size", bytes.length+"");
            final int headerLength = body.length();
            Log.i(TAG, "body="+body+" headerLength="+headerLength);
            final String requestBody = body;

            mTritonUrl = "/v2/models/"+ mModelName +"/infer";
            String url = mScheme+"://"+ mHost +":"+mPort + mTritonUrl;
            Log.i(TAG, "url="+url+" length: "+requestBody.length());

            // Request a byte response from the provided URL.
            mStringRequestMain = new StringRequest(Request.Method.POST, url,
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
                    Log.e(TAG, message);
                    mImageServerInterface.reportConnectionError(error.toString(), ImageSenderTriton.this);
                }
            }) {
                byte[] concatenateByteArrays(byte[] a, byte[] b) {
                    byte[] result = new byte[a.length + b.length];
                    System.arraycopy(a, 0, result, 0, a.length);
                    System.arraycopy(b, 0, result, a.length, b.length);
                    return result;
                }

                @Override
                public byte[] getBody() {
                    return concatenateByteArrays(requestBody.getBytes(), bytes);
                }

                @Override
                public Map getHeaders() {
                    Map headers = new HashMap();
                    headers.put("Inference-Header-Content-Length", headerLength+"");
                    return headers;
                }
            };

            // Add the request to the RequestQueue.
            mRequestQueue.add(mStringRequestMain);
        } else {
            Log.e(TAG, "Unknown communication mode: "+ mConnectionMode);
        }
    }

    /**
     * Both the REST server and the GRPC server will return results in the same JSON
     * format. This method parses the results and updates the UI with the returned values.
     *
     * @param response
     * @param latency
     */
    private void handleResponse(String response, long latency) {
        Log.i(TAG, "handleResponse mModelName="+mModelName+" response="+response);
        if (mInactive) {
            Log.i(TAG, "Inactive, aborting update.");
            return;
        }
        try {
            if (mModelName.equals("ensemble_dali_yolov4")) {

                // Get just the outputs/OBJECTS_JSON data
                JSONObject jsonObject = new JSONObject(response);
                JSONArray outputs = jsonObject.getJSONArray("outputs");
                for (int i = 0; i < outputs.length(); i++) {
                    JSONObject output = outputs.getJSONObject(i);
                    String name = output.getString("name");
                    if (!name.equals("OBJECTS_JSON")) {
                        continue;
                    }
                    JSONArray data = output.getJSONArray("data");
                    String object = data.getString(0);
                    Log.i(TAG, "handleResponse object="+object);
                    response = object.toString();
                    jsonObject = new JSONObject(response);
                    JSONArray objects = jsonObject.getJSONArray("objects");
                    mImageServerInterface.updateOverlay(mCloudLetType, objects, null);
                }

            } else if (mModelName.equals("ensemble_dali_inception")) {
                Log.i(TAG, "ensemble_dali_inception response="+response);
                // Add 4 to ignore binary data before the actual inference data.
                String inference = response.substring(INCEPTION_INFERENCE_HEADER_CONTENT_LENGTH+4);
                Log.i(TAG, "inference="+inference);
                String[] parts = inference.split(":");
                Log.i(TAG, "parts="+parts[0]+" "+parts[1]+" "+parts[2]);
                float confidence = Float.parseFloat(parts[0]) * 100;
                String className = parts[2]+" "+String.format("%.1f", confidence)+"%";
                mImageServerInterface.updateOverlay(mCloudLetType, null, className);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        mLatencyFullProcessRollingAvg.add(latency / 1000000); //ns->ms
        mImageServerInterface.updateFullProcessStats(mCloudLetType, mLatencyFullProcessRollingAvg);
        Log.d(TAG, mCloudLetType + " mCameraMode=" + mCameraMode + " mLatency=" + (mLatency / 1000000.0)+" mHost="+mHost);
    }
}
