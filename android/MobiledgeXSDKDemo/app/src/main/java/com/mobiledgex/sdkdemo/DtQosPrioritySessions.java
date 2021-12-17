package com.mobiledgex.sdkdemo;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mobiledgex.matchingenginehelper.MatchingEngineHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QoS for stable latency/throughput API. See:
 * https://staging-portal.hubraum.opsfactory.dev/de/products/617bd0928431ba00019948f4/documentation
 * https://staging-portal.hubraum.opsfactory.dev/de/products/617dda988431ba00019948ff/documentation
 */
public class DtQosPrioritySessions {
    private static final String TAG = "DtQosPrioritySessions";
    public static final String QOS_API_URL = "https://staging-api.developer.telecom.com";
    private static MainActivity mActivity;
    private static MatchingEngineHelper meHelper;

    protected static void createPrioritySession(MainActivity activity, MatchingEngineHelper helper) {
        mActivity = activity;
        meHelper = helper;
        Log.i(TAG, "meHelper.mQosSessionId="+meHelper.mQosSessionId+" meHelper.mQosProfileName="+meHelper.mQosProfileName);
        // Create dialog with QOS profile names.
        List<String> items = new ArrayList<>();
        items.add("LOW_LATENCY");
        items.add("THROUGHPUT_S");
        items.add("THROUGHPUT_M");
        items.add("THROUGHPUT_L");
        Log.i(TAG, "items="+items);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Update Session Priority");

        final ArrayAdapter<String> arrayAdapterItems = new ArrayAdapter<String>(
                mActivity.getApplicationContext(), android.R.layout.simple_list_item_single_choice, items);
        View content = mActivity.getLayoutInflater().inflate(R.layout.dt_qos_session_dialog, null);
        EditText textSesId = content.findViewById(R.id.text_ses_id);
        textSesId.setTextIsSelectable(true);
        EditText textProfile = content.findViewById(R.id.text_profile_name);
        textProfile.setTextIsSelectable(true);
        EditText textDuration = content.findViewById(R.id.text_duration);
        TextView tvMessage = content.findViewById(R.id.tv_msg);
        tvMessage.setText("Choose new QOS profile for session:");
        final ListView lvItems = content.findViewById(R.id.lv_items);
        lvItems.setAdapter(arrayAdapterItems);
        lvItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        builder.setView(content);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        content.findViewById(R.id.button_cancel).setOnClickListener(view -> {
            alertDialog.cancel();
        });

        Button buttonDelSession = content.findViewById(R.id.button_delete);
        buttonDelSession.setOnClickListener(view -> {
            String url = buildQosUrl(meHelper.mQosProfileName);
            url += "/"+meHelper.mQosSessionId;
            sendHttpRequest(Request.Method.DELETE, url, "", -1, "", "");
            alertDialog.cancel();
        });

        Button buttonSesDetails = content.findViewById(R.id.button_details);
        buttonSesDetails.setOnClickListener(view -> {
            String url = buildQosUrl(meHelper.mQosProfileName);
            url += "/"+meHelper.mQosSessionId;
            sendHttpRequest(Request.Method.GET, url, "", -1, "", "");
            alertDialog.cancel();
        });

        if (meHelper.mQosSessionId != null && !meHelper.mQosSessionId.isEmpty()) {
            textSesId.setText(meHelper.mQosSessionId);
        } else {
            buttonDelSession.setEnabled(false);
            buttonSesDetails.setEnabled(false);
        }
        if (meHelper.mQosProfileName != null && !meHelper.mQosProfileName.isEmpty()) {
            textProfile.setText(meHelper.mQosProfileName);
        }

        lvItems.setOnItemClickListener((parent, view, which, id1) -> {
            String selectedItemText = items.get(which);
            String requestBody = "";
            String url = buildQosUrl(selectedItemText);
            int duration = Integer.parseInt(textDuration.getText().toString());
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("duration", duration);
                jsonBody.put("asAddr", meHelper.mAppInstIp);
                jsonBody.put("ueAddr", meHelper.mDeviceIpv4);
                jsonBody.put("asAddr", meHelper.mAppInstIp);
                jsonBody.put("asPorts", "8008");
                jsonBody.put("protocolIn", "TCP");
                jsonBody.put("protocolOut", "TCP");
                jsonBody.put("qos", selectedItemText);
                requestBody = jsonBody.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            if (meHelper.mQosSessionId != null && !meHelper.mQosSessionId.isEmpty()) {
                // Delete the old, and include parms for a second request to be called afterwards to create the new.
                String deleteUrl = buildQosUrl(meHelper.mQosProfileName)+"/"+meHelper.mQosSessionId;
                Log.i(TAG, "Deleting old session via "+deleteUrl);
                sendHttpRequest(Request.Method.DELETE, deleteUrl, "", Request.Method.POST, url, requestBody);
            } else {
                // Create a new one from scratch
                sendHttpRequest(Request.Method.POST, url, requestBody, -1, "", "");
            }
            alertDialog.dismiss();
        });
    }

    private static String buildQosUrl(String profileName) {
        String qosType;
        if (profileName.indexOf("LATENCY") >= 0) {
            qosType = "latency";
        } else {
            qosType = "throughput";
        }
        return QOS_API_URL + "/5g-" +qosType+"/sessions";
    }

    protected static void sendHttpRequest(int method, String url, String body, int method2, String url2, String body2) {
        Log.i(TAG, "sendHttpRequest method="+method+" url="+url+" body="+body+" method2="+method2+" url2="+url2+" body2="+body2);
        final int[] statusCode = {0};
        final String[] methodNames = {"GET", "POST", "PUT", "DELETE"};
        String msg = "sendHttpRequest method="+methodNames[method]+" url="+url+" body="+body;
        Log.i(TAG, msg);
        mActivity.showMessage(msg);
        RequestQueue queue = Volley.newRequestQueue(mActivity.getApplicationContext());
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(method, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (method == Request.Method.DELETE) {
                            meHelper.mQosSessionId = "";
                            meHelper.mQosProfileName = "";
                            String msg = methodNames[method]+" QOS API session response="+statusCode[0];
                            if (!response.isEmpty()) {
                                msg += ": "+response;
                            }
                            Log.i(TAG, msg);
                            mActivity.showMessage(msg);
                            if (method2 >= 0) {
                                // Recursive call with the second set of parameters.
                                sendHttpRequest(method2, url2, body2, -1, "", "");
                            }
                            return; // DELETE call will have no body.
                        }
                        // POST or GET should have a JSON response.
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String msg = methodNames[method]+" QOS API session response="+statusCode[0]+":\n"+jsonObject.toString(2);
                            Log.i(TAG, msg);
                            mActivity.showMessage(msg);
                            meHelper.mQosSessionId = jsonObject.getString("id");
                            meHelper.mQosProfileName = jsonObject.getString("qos");
                            Log.i(TAG, "meHelper.mQosSessionId="+meHelper.mQosSessionId+" meHelper.mQosProfileName="+meHelper.mQosProfileName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            String msg = methodNames[method]+" QOS API session response="+statusCode[0]+": "+response;
                            Log.i(TAG, msg);
                            mActivity.showMessage(msg);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "That didn't work! error="+error);
                String msg = methodNames[method] + " API call Error: " + error.networkResponse.statusCode + " - " + new String(error.networkResponse.data, StandardCharsets.UTF_8) + " - " + error.getMessage();
                Log.e(TAG, msg);
                mActivity.showError(msg);
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }
            @Override
            public byte[] getBody() {
                try {
                    return body == "" ? null : body.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    Log.wtf(TAG, "Unsupported Encoding while trying to get the body bytes");
                    return null;
                }
            }
            @Override
            public Map<String, String> getHeaders() {
                // API key is stored in local.properties as dt_qos_sessions_api_key.
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Authorization", BuildConfig.DT_QOS_SESSIONS_API_KEY);
                params.put("accept", "application/json");
                return params;
            }
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                statusCode[0] = response.statusCode;
                return super.parseNetworkResponse(response);
            }
        };

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}
