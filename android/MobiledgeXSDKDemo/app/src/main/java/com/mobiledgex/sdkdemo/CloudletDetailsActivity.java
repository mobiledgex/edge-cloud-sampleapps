package com.mobiledgex.sdkdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CloudletDetailsActivity extends AppCompatActivity implements SpeedTestResultsInterface {

    public static final int BYTES_TO_MBYTES = 1024*1024;
    private static final String TAG = "CloudletDetailsActivity";
    private Intent intent;
    private Cloudlet cloudlet;
    private TextView cloudletNameTv;
    private TextView appNameTv;
    private TextView speedtestResultsTv;
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
    private Button buttonSpeedTest;
    private Button buttonLatencyTest;
    private ProgressBar progressBarDownload;
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

        cloudletNameTv = findViewById(R.id.cloudletName);
        appNameTv = findViewById(R.id.appName);
        carrierNameTv = findViewById(R.id.carrierName);
        ipAddressTv = findViewById(R.id.ipAddress);
        distanceTv = findViewById(R.id.distance);
        latitudeTv = findViewById(R.id.latitude);
        longitudeTv = findViewById(R.id.longitude);
        speedtestResultsTv = findViewById(R.id.speedtestResults);
        latencyMinTv = findViewById(R.id.latencyMin);
        latencyAvgTv = findViewById(R.id.latencyAvg);
        latencyMaxTv = findViewById(R.id.latencyMax);
        latencyStddevTv = findViewById(R.id.latencyStddev);
        latencyMessageTv = findViewById(R.id.latencyMessage);
        progressBarDownload = findViewById(R.id.progressBarDownload);
        progressBarLatency = findViewById(R.id.progressBarLatency);

        cloudletNameTv.setText(cloudlet.getCloudletName());
        appNameTv.setText(cloudlet.getAppName());
        carrierNameTv.setText(cloudlet.getCarrierName());
        distanceTv.setText(String.format("%.4f", cloudlet.getDistance()));
        latitudeTv.setText(Double.toString(cloudlet.getLatitude()));
        longitudeTv.setText(Double.toString(cloudlet.getLongitude()));

        buttonSpeedTest = findViewById(R.id.buttonSpeedtest);
        buttonSpeedTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cloudlet.startBandwidthTest();
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
            // Convert Demo app hostname to Face app hostname. Example:
            // mobiledgexsdkdemo-tcp.mobiledgexsdkdemomobiledgexsdkdemo10.mexdemo-centralus-cloudlet.azure.mobiledgex.net
            // becomes
            // facedetectiondemo-tcp.mobiledgexsdkdemofacedetectiondemo10.mexdemo-centralus-cloudlet.azure.mobiledgex.net
            // Change appName, but keep devName the same.
            // TODO: Find the appInst for the face app and pull the FQDN directly from there instead converting here.
            String hostname = cloudlet.getHostName();
            Log.i(TAG, "Cloud hostname before conversion: "+hostname);
            hostname = hostname.replaceAll("mobiledgexsdkdemo-tcp", "facedetectiondemo-tcp");
            hostname = hostname.replaceAll("mobiledgexsdkdemo10", "facedetectiondemo10");
            Log.i(TAG, "Cloud hostname after conversion: "+hostname);
            prefs.edit().putString(prefKeyHostCloud, hostname).apply();
            return true;
        }
        if (id == R.id.action_copy_edge_host) {
            String prefKeyHostEdge = getResources().getString(R.string.preference_fd_host_edge);
            String hostname = cloudlet.getHostName();
            Log.i(TAG, "Edge hostname before conversion: "+hostname);
            hostname = hostname.replaceAll("mobiledgexsdkdemo-tcp", "facedetectiondemo-tcp");
            hostname = hostname.replaceAll("mobiledgexsdkdemo10", "facedetectiondemo10");
            Log.i(TAG, "Edge hostname after conversion: "+hostname);
            prefs.edit().putString(prefKeyHostEdge, hostname).apply();
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
    public void onBandwidthProgress() {
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
                if(cloudlet.getCarrierName().equalsIgnoreCase("azure")) {
                    if(latencyTestMethod == CloudletListHolder.LatencyTestMethod.ping) {
                        latencyMessageTv.setText("Socket test forced");
                    }
                }

                if(cloudlet.getLatencyMin() != 9999) {
                    latencyMinTv.setText(formatValue(cloudlet.getLatencyMin()) + " ms");
                } else {
                    latencyMinTv.setText(formatValue(0) + " ms");
                }
                if(cloudlet.isPingFailed()) {
                    latencyMessageTv.setText("Ping failed");
                }
                latencyAvgTv.setText(formatValue(cloudlet.getLatencyAvg())+" ms");
                latencyMaxTv.setText(formatValue(cloudlet.getLatencyMax())+" ms");
                latencyStddevTv.setText(formatValue(cloudlet.getLatencyStddev())+" ms");
                progressBarLatency.setProgress(cloudlet.getLatencyTestProgress());
                progressBarDownload.setProgress(cloudlet.getSpeedTestProgress());
                speedtestResultsTv.setText(String.format("%.2f", cloudlet.getMbps())+" Mbits/sec");
                distanceTv.setText(String.format("%.4f", cloudlet.getDistance()));
                ipAddressTv.setText(cloudlet.getIpAddress());
            }
        });
    }

    private String formatValue(double value) {
        return String.format("%.3f", value);
    }

}
