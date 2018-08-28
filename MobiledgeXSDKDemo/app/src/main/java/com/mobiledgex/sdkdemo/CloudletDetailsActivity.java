package com.mobiledgex.sdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CloudletDetailsActivity extends AppCompatActivity implements SpeedTestResultsListener {

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
        Log.i(TAG, "cloudlet="+cloudlet+" "+cloudlet.getCloudletName());
        cloudlet.setSpeedTestResultsListener(this);

        cloudletNameTv = findViewById(R.id.cloudletName);
        appNameTv = findViewById(R.id.appName);
        carrierNameTv = findViewById(R.id.carrierName);
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
                cloudlet.startLatencyTest();
            }
        });

        updateUi();

    }

    @Override
    public void onLatencyProgress() {
        updateUi();
    }

    @Override
    public void onBandwidthProgress() {
        updateUi();
    }

    private void updateUi() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    private String formatValue(double value) {
        return String.format("%.3f", value);
    }

}
