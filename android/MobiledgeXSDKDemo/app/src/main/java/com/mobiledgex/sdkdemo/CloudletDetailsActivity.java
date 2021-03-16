/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
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

package com.mobiledgex.sdkdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CloudletDetailsActivity extends AppCompatActivity implements SpeedTestResultsInterface {

    private static final String TAG = "CloudletDetailsActivity";
    private Intent intent;
    private Cloudlet cloudlet;
    private TextView cloudletNameTv;
    private TextView appNameTv;
    private TextView speedtestDownloadResultsTv;
    private TextView speedtestUploadResultsTv;
    private TextView latencyMinTv;
    private TextView latencyAvgTv;
    private TextView latencyMaxTv;
    private TextView latencyStddevTv;
    private TextView latencyMessageTv;
    private TextView carrierNameTv;
    private TextView ipAddressTv;
    private TextView distanceTv;
    private TextView latitudeTv;
    private TextView longitudeTv;
    private Button buttonSpeedTestDownload;
    private Button buttonSpeedTestUpload;
    private Button buttonLatencyTest;
    private ProgressBar progressBarDownload;
    private ProgressBar progressBarUpload;
    private ProgressBar progressBarLatency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloudlet_details);

        setTitle("Cloudlet Details");

        intent = getIntent();

        String cloudletName = intent.getStringExtra("CloudletName");
        cloudlet = CloudletListHolder.getSingleton().getCloudletList().get(cloudletName);
        if(cloudlet == null) {
            Log.e(TAG, "cloudlet "+cloudletName+" not found in list. Aborting.");
            return;
        }
        Log.i(TAG, "cloudlet="+cloudlet+" "+cloudlet.getCloudletName());
        cloudlet.setSpeedTestResultsListener(this);
        cloudlet.setContext(getApplicationContext());

        cloudletNameTv = findViewById(R.id.cloudletName);
        appNameTv = findViewById(R.id.appName);
        carrierNameTv = findViewById(R.id.carrierName);
        ipAddressTv = findViewById(R.id.ipAddress);
        distanceTv = findViewById(R.id.distance);
        latitudeTv = findViewById(R.id.latitude);
        longitudeTv = findViewById(R.id.longitude);
        speedtestDownloadResultsTv = findViewById(R.id.speedtestDownloadResults);
        speedtestUploadResultsTv = findViewById(R.id.speedtestUploadResults);
        latencyMinTv = findViewById(R.id.latencyMin);
        latencyAvgTv = findViewById(R.id.latencyAvg);
        latencyMaxTv = findViewById(R.id.latencyMax);
        latencyStddevTv = findViewById(R.id.latencyStddev);
        latencyMessageTv = findViewById(R.id.latencyMessage);
        progressBarDownload = findViewById(R.id.progressBarDownload);
        progressBarUpload = findViewById(R.id.progressBarUpload);
        progressBarLatency = findViewById(R.id.progressBarLatency);

        cloudletNameTv.setText(cloudlet.getCloudletName());
        appNameTv.setText(cloudlet.getAppName());
        carrierNameTv.setText(cloudlet.getCarrierName());
        distanceTv.setText(String.format("%.4f", cloudlet.getDistance()));
        latitudeTv.setText(Double.toString(cloudlet.getLatitude()));
        longitudeTv.setText(Double.toString(cloudlet.getLongitude()));

        buttonSpeedTestDownload = findViewById(R.id.buttonSpeedtestDownload);
        buttonSpeedTestDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cloudlet.startSpeedTestDownload();
            }
        });
        buttonSpeedTestUpload = findViewById(R.id.buttonSpeedtestUpload);
        buttonSpeedTestUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cloudlet.startSpeedTestUpload();
            }
        });
        buttonLatencyTest = findViewById(R.id.buttonLatencytest);
        buttonLatencyTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                latencyMessageTv.setText("");
                cloudlet.startLatencyTest();
            }
        });

        updateUi();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cloudlet_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (id == R.id.action_copy_cloud_host) {
            String prefKeyHostCloud = getResources().getString(R.string.preference_fd_host_cloud);
            String prefKeyHostCloudOverride = getResources().getString(R.string.pref_override_cloud_cloudlet_hostname);
            String hostname = cloudlet.getHostName();
            Log.i(TAG, "Cloud hostname being set to: "+hostname);
            prefs.edit().putString(prefKeyHostCloud, hostname).apply();
            prefs.edit().putBoolean(prefKeyHostCloudOverride, true).apply();
            return true;
        }
        if (id == R.id.action_copy_edge_host) {
            String prefKeyHostEdge = getResources().getString(R.string.preference_fd_host_edge);
            String prefKeyHostEdgeOverride = getResources().getString(R.string.pref_override_edge_cloudlet_hostname);
            String hostname = cloudlet.getHostName();
            Log.i(TAG, "Edge hostname being set to: "+hostname);
            prefs.edit().putString(prefKeyHostEdge, hostname).apply();
            prefs.edit().putBoolean(prefKeyHostEdgeOverride, true).apply();
            return true;
        }
        if (id == R.id.action_speedtest_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.SpeedTestSettingsFragment.class.getName() );
            intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onLatencyProgress() {
        updateUi();
    }

    @Override
    public void onSpeedtestDownloadProgress() {
        updateUi();
    }

    @Override
    public void onSpeedtestUploadProgress() {
        updateUi();
    }

    @Override
    public void onIpAddressResolved() {
        updateUi();
    }

    private void updateUi() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CloudletListHolder.LatencyTestMethod latencyTestMethod = CloudletListHolder.getSingleton().getLatencyTestMethod();
                String latencyMessage = "Method: "+cloudlet.getLatencyTestMethod();
                if (cloudlet.getLatencyTestMethodForced()) {
                    latencyMessage += " (forced)";
                }
                latencyMessageTv.setText(latencyMessage);

                if(cloudlet.getLatencyMin() != 9999) {
                    latencyMinTv.setText(formatValue(cloudlet.getLatencyMin()) + " ms");
                } else {
                    latencyMinTv.setText(formatValue(0) + " ms");
                }
                if(cloudlet.isPingFailed()) {
                    if(latencyTestMethod == CloudletListHolder.LatencyTestMethod.ping) {
                        latencyMessageTv.setText("Ping failed");
                    } else if(latencyTestMethod == CloudletListHolder.LatencyTestMethod.socket) {
                        latencyMessageTv.setText("Socket test failed");
                    }
                }
                latencyAvgTv.setText(formatValue(cloudlet.getLatencyAvg())+" ms");
                latencyMaxTv.setText(formatValue(cloudlet.getLatencyMax())+" ms");
                latencyStddevTv.setText(formatValue(cloudlet.getLatencyStddev())+" ms");
                progressBarLatency.setProgress(cloudlet.getLatencyTestProgress());
                progressBarDownload.setProgress(cloudlet.getSpeedTestDownloadProgress());
                progressBarUpload.setProgress(cloudlet.getSpeedTestUploadProgress());
                speedtestDownloadResultsTv.setText(cloudlet.getSpeedTestDownloadResult());
                speedtestUploadResultsTv.setText(cloudlet.getSpeedTestUploadResult());
                distanceTv.setText(String.format("%.4f", cloudlet.getDistance()));
                ipAddressTv.setText(cloudlet.getIpAddress());
            }
        });
    }

    private String formatValue(double value) {
        return String.format("%.3f", value);
    }

}
