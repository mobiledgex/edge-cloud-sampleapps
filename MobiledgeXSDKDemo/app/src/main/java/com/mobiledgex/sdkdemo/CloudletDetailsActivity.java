package com.mobiledgex.sdkdemo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import static com.mobiledgex.sdkdemo.MainActivity.HOSTNAME;

public class CloudletDetailsActivity extends AppCompatActivity {

    public static final int BYTES_TO_MBYTES = 1024*1024;
    private static final String TAG = "CloudletDetailsActivity";
    private Intent intent;
    private TextView speedtestResults;
    private TextView latencyMinTv;
    private TextView latencyAvgTv;
    private TextView latencyMaxTv;
    private TextView latencyStddevTv;
    private Button buttonSpeedTest;
    private ProgressBar progressBarDownload;
    private ProgressBar progressBarLatency;
    private double latencyMin=9999;
    private double latencyAvg=0;
    private double latencyMax=0;
    private double latencyStddev=0;
    private double latencyTotal=0;
    private long startTime;
    private long timeDifference;
    private int numPings = 5; //TODO: Make preference
    private boolean runningOnEmulator = false;
    private String downloadUri = "http://ipv4.ikoula.testdebit.info/1M.iso";
    private String hostName = "ipv4.ikoula.testdebit.info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloudlet_details);

        setTitle("Cloudlet Details");

        intent = getIntent();

        Cloudlet cloudlet = (Cloudlet) intent.getSerializableExtra("cloudlet");

        TextView cloudletName = findViewById(R.id.cloudletName);
        TextView carrierName = findViewById(R.id.carrierName);
        TextView distance = findViewById(R.id.distance);
        TextView latitude = findViewById(R.id.latitude);
        TextView longitude = findViewById(R.id.longitude);
        speedtestResults = findViewById(R.id.speedtestResults);
        latencyMinTv = findViewById(R.id.latencyMin);
        latencyAvgTv = findViewById(R.id.latencyAvg);
        latencyMaxTv = findViewById(R.id.latencyMax);
        latencyStddevTv = findViewById(R.id.latencyStddev);
        progressBarDownload = findViewById(R.id.progressBarDownload);
        progressBarLatency = findViewById(R.id.progressBarLatency);

        cloudletName.setText(cloudlet.getCloudletName());
        carrierName.setText(cloudlet.getCarrierName());
        distance.setText(String.format("%.4f", cloudlet.getDistance()));
        latitude.setText(Double.toString(cloudlet.getLatitude()));
        longitude.setText(Double.toString(cloudlet.getLongitude()));

//        if(cloudlet.getCloudletName().equals("Austin1")) {
        if(cloudlet.isBestMatch()) {
//            downloadUri = "http://"+HOSTNAME+"/speedtest/1M.iso";
//            downloadUri = "http://"+HOSTNAME+"/speedtest/1M.txt";
            downloadUri = "http://"+HOSTNAME+":7777/getdata?numbytes=1048576";
            hostName = HOSTNAME;
        }

        buttonSpeedTest = findViewById(R.id.buttonSpeedtest);
        buttonSpeedTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "downloadUri=" + downloadUri);
                new SpeedTestTask().execute();
            }
        });

        //ping can't run on an emulator, so detect that case.
        Log.i(TAG, "PRODUCT="+Build.PRODUCT);
        if (Build.PRODUCT.equalsIgnoreCase("sdk_gphone_x86")) {
            runningOnEmulator = true;
            Log.i(TAG, "YES, I am an emulator");
        } else {
            Log.i(TAG, "NO, I am NOT an emulator");
            new LatencyTestTask().execute();
        }

    }

    public class LatencyTestTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            String pingCommand = "/system/bin/ping -c "+numPings+" " + hostName;
            String inputLine = "";

            String regex = "time=(\\d+.\\d+) ms";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    buttonSpeedTest.setEnabled(false);
                }
            });

            try {
                // execute the command on the environment interface
                Process process = Runtime.getRuntime().exec(pingCommand);
                // gets the input stream to get the output of the executed command
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                double linesTotal = numPings;
                double linesRead = 0;
                inputLine = bufferedReader.readLine();
                while ((inputLine != null)) {
                    Log.i(TAG, "inputLine="+inputLine);
                    if (inputLine.length() > 0 && inputLine.contains("time=")) {
                        linesRead++;
                        matcher = pattern.matcher(inputLine);
                        if(matcher.find()) {
                            double val = Double.parseDouble(matcher.group(1));
                            latencyTotal += val;
                            latencyAvg = latencyTotal/linesRead;
                            if(val < latencyMin) { latencyMin = val; }
                            if(val > latencyMax) { latencyMax = val; }
                            updateUi();
                        }
                        Log.i(TAG, "linesRead="+linesRead+" linesTotal="+linesTotal+" "+(linesRead/linesTotal*100)+" "+(int)(linesRead/linesTotal*100));
                        publishProgress((int)(linesRead/linesTotal*100));
                    }
                    if (inputLine.length() > 0 && inputLine.contains("avg")) {  // when we get to the last line of executed ping command
                        break;
                    }
                    inputLine = bufferedReader.readLine();
                }
            }
            catch (IOException e){
                Log.v(TAG, "getLatency: EXCEPTION");
                e.printStackTrace();
            }

            // Extracting the average round trip time from the inputLine string
            regex = "(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+) ms";
            pattern = Pattern.compile(regex);
            matcher = pattern.matcher(inputLine);
            if(matcher.find()) {
                Log.i(TAG, "output="+matcher.group(0));
                latencyMin = Double.parseDouble(matcher.group(1));
                latencyAvg = Double.parseDouble(matcher.group(2));
                latencyMax = Double.parseDouble(matcher.group(3));
                latencyStddev = Double.parseDouble(matcher.group(4));
                updateUi();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBarLatency.setProgress(100);
            buttonSpeedTest.setEnabled(true);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBarLatency.setProgress(progress[0]);
        }

    }

    private void updateUi() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                latencyMinTv.setText(""+latencyMin+" ms");
                latencyAvgTv.setText(String.format("%.3f", latencyAvg)+" ms");
                latencyMaxTv.setText(""+latencyMax+" ms");
                latencyStddevTv.setText(""+latencyStddev+" ms");
            }
        });
    }

    public class SpeedTestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            SpeedTestSocket speedTestSocket = new SpeedTestSocket();

            // add a listener to wait for speedtest completion and progress
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(final SpeedTestReport report) {
                    // called when download/upload is finished
//                    Log.v(TAG, "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
                    Log.v(TAG, "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
                    BigDecimal divisor = new BigDecimal(BYTES_TO_MBYTES);
                    final BigDecimal mbps = report.getTransferRateBit().divide(divisor);
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            speedtestResults.setText(String.format("%.2f", mbps)+" Mbits/sec");
                            progressBarDownload.setProgress(100);
                        }
                    });
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a download/upload error occur
                }

                @Override
                public void onProgress(final float percent, final SpeedTestReport report) {
                    // called to notify download/upload progress

                    if(runningOnEmulator) {
                        if (timeDifference == -1) {
                            long endTime = System.currentTimeMillis();
                            timeDifference = endTime - startTime;
                            Log.i(TAG, "Latency=" + timeDifference + " ms.");
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    latencyAvgTv.setText(timeDifference + " ms");
                                }
                            });
                        }
                    }

                    Log.v(TAG, "[PROGRESS] progress : " + percent + "%");
//                    Log.v(TAG, "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                    Log.v(TAG, "[PROGRESS] "+percent + "% - rate in bit/s   : " + report.getTransferRateBit());
                    BigDecimal divisor = new BigDecimal(BYTES_TO_MBYTES);
                    final BigDecimal mbps = report.getTransferRateBit().divide(divisor);
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            speedtestResults.setText(String.format("%.2f", mbps)+" Mbits/sec");
                            progressBarDownload.setProgress((int)percent);
                        }
                    });
                }
            });

            startTime = System.currentTimeMillis();
            timeDifference = -1;
            speedTestSocket.startDownload(downloadUri);

            return null;
        }
    }
}
