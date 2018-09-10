package com.mobiledgex.sdkdemo;


import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class Cloudlet implements Serializable {
    private static final String TAG = "Cloudlet";
    public static final int BYTES_TO_MBYTES = 1024*1024;

    private String mCloudletName;
    private String mAppName;
    private String mCarrierName;
    private double mLatitude;
    private double mLongitude;
    private double mDistance;
    private boolean bestMatch;
    private transient Marker mMarker;

    private double latencyMin=9999;
    private double latencyAvg=0;
    private double latencyMax=0;
    private double latencyStddev=0;
    private double latencyTotal=0;
    private BigDecimal mbps = BigDecimal.valueOf(0);
    private int latencyTestProgress = 0;
    private int speedTestProgress = 0;
    private long startTime;
    private long timeDifference;
    private int mNumPackets = 4;
    private int mNumBytes = 1048576;
    private boolean runningOnEmulator = false;
    private boolean pingFailed = false;

    private SpeedTestResultsListener mSpeedTestResultsListener;

    private String downloadUri;
    private String hostName;
    private int openPort = 7777;
    private final int socketTimeout = 3000;
    private boolean latencyTestTaskRunning = false;
    private boolean speedTestTaskRunning = false;
    private String uri;

    public Cloudlet(String cloudletName, String appName, String carrierName, LatLng gpsLocation, double distance, String uri, Marker marker, int numBytes, int numPackets) {
        Log.d(TAG, "Cloudlet contructor. cloudletName="+cloudletName);
        update(cloudletName, appName, carrierName, gpsLocation, distance, uri, marker, numBytes, numPackets);

        //All AsyncTask instances are run on the same thread, so this queues up the tasks.
        startLatencyTest();
    }

    public void update(String cloudletName, String appName, String carrierName, LatLng gpsLocation, double distance, String uri, Marker marker, int numBytes, int numPackets) {
        Log.d(TAG, "Cloudlet update. cloudletName="+cloudletName);
        mCloudletName = cloudletName;
        mAppName = appName;
        mCarrierName = carrierName;
        mLatitude = gpsLocation.latitude;
        mLongitude = gpsLocation.longitude;
        mDistance = distance;
        mMarker = marker;
        mNumBytes = numBytes;
        mNumPackets = numPackets;
        setUri(uri);
    }

    /**
     * From the given string, create the hostname that will be pinged,
     * and the URI that will be downloaded from.
     * @param uri
     */
    public void setUri(String uri) {
        this.hostName = "mobiledgexsdkdemo."+uri;
        this.downloadUri = "http://"+hostName+":7777/getdata?numbytes="+ mNumBytes;
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public String toString() {
        return "mCarrierName="+mCarrierName+" mCloudletName="+mCloudletName+" mLatitude="+mLatitude+" mLongitude="+mLongitude+" mDistance="+mDistance;
    }

    public void setSpeedTestResultsListener(SpeedTestResultsListener speedTestResultsListener) {
        this.mSpeedTestResultsListener = speedTestResultsListener;
    }

    public void startLatencyTest() {
        Log.d(TAG, "startLatencyTest()");
        if(latencyTestTaskRunning) {
            Log.d(TAG, "LatencyTest already running");
            return;
        }

        latencyMin=9999;
        latencyAvg=0;
        latencyMax=0;
        latencyStddev=0;
        latencyTotal=0;

        String latencyTestMethod = CloudletListHolder.getSingleton().getLatencyTestMethod();
        if(latencyTestMethod.equals("socket")) { //TODO: Use enum instead of string
            new LatencyTestTaskSocket().execute();
        } else if(latencyTestMethod.equals("ping")) {
            new LatencyTestTaskPing().execute();
        } else {
            Log.e(TAG, "Unknown latencyTestMethod: "+latencyTestMethod);
        }
    }

    public void startBandwidthTest() {
        if(speedTestTaskRunning) {
            Log.d(TAG, "SpeedTest already running");
            return;
        }
        Log.d(TAG, "downloadUri=" + downloadUri + " speedTestTaskRunning="+speedTestTaskRunning);
        if(!speedTestTaskRunning) {
            new SpeedTestTask().execute();
        }
    }

    private static boolean isReachable(String addr, int openPort, int timeOutMillis) {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
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

    public class LatencyTestTaskSocket extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            latencyTestTaskRunning = true;
            pingFailed = false;
            double sumSquare = 0;
            int countFail = 0;
            int countSuccess = 0;
            boolean reachable;
            //First time may be slower because of DNS lookup. Run once before it counts.
            isReachable(hostName, openPort, socketTimeout);
            for(int i = 0; i < mNumPackets; i++) {
                startTime = System.currentTimeMillis();
                reachable = isReachable(hostName, openPort, socketTimeout);
                if(reachable) {
                    long endTime = System.currentTimeMillis();
                    timeDifference = endTime - startTime;
                    Log.d(TAG, hostName+" reachable="+reachable+" Latency=" + timeDifference + " ms.");
                    latencyTotal += timeDifference;
                    latencyAvg = latencyTotal/(i+1);
                    if(timeDifference < latencyMin) { latencyMin = timeDifference; }
                    if(timeDifference > latencyMax) { latencyMax = timeDifference; }
                    sumSquare += Math.pow((timeDifference - latencyAvg),2);
                    latencyStddev = Math.sqrt( sumSquare/(i+1) );
                    countSuccess++;
                } else {
                    countFail++;
                }
                publishProgress((int)((i+1.0f)/mNumPackets*100));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            if(countFail == mNumPackets) {
                Log.w(TAG, "ping failed");
                pingFailed = true;
            }

            // Summary logs to match ping output
            // 10 packets transmitted, 10 packets received, 0.0% packet loss
            // round-trip min/avg/max/stddev = 202.167/219.318/335.734/38.879 ms
            String percent = String.format("%.1f", (countFail/(float)mNumPackets*100));
            String stddev = String.format("%.3f", (latencyStddev));
            Log.i(TAG, hostName+" "+mNumPackets+" packets transmitted, "+countSuccess+" packets received, "+percent+"% packet loss");
            Log.i(TAG, hostName+" round-trip min/avg/max/stddev = "+latencyMin+"/"+latencyAvg+"/"+latencyMax+"/"+stddev+" ms");

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            latencyTestProgress = pingFailed ? 0 : 100;
            latencyTestTaskRunning = false;
            if(mSpeedTestResultsListener != null) {
                mSpeedTestResultsListener.onLatencyProgress();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            latencyTestProgress = progress[0];
            if(mSpeedTestResultsListener != null) {
                mSpeedTestResultsListener.onLatencyProgress();
            }
        }

    }

    public class LatencyTestTaskPing extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            //ping can't run on an emulator, so detect that case.
            Log.i(TAG, "PRODUCT="+ Build.PRODUCT);
            if (Build.PRODUCT.equalsIgnoreCase("sdk_gphone_x86")
                    || Build.PRODUCT.equalsIgnoreCase("sdk_google_phone_x86")) {
                runningOnEmulator = true;
                Log.i(TAG, "YES, I am an emulator. Skipping ping test.");
                return null;
            } else {
                Log.i(TAG, "NO, I am NOT an emulator. Running ping test.");
            }

            String pingCommand = "/system/bin/ping -c "+ mNumPackets +" " + hostName;
            String inputLine = "";

            String regex = "time=(\\d+.\\d+) ms";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher;

            latencyTestTaskRunning = true;

            Log.d(TAG, mCloudletName+ " executing "+pingCommand);
            try {
                // execute the command on the environment interface
                Process process = Runtime.getRuntime().exec(pingCommand);
                // gets the input stream to get the output of the executed command
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                double linesTotal = mNumPackets;
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
                        }
                        Log.d(TAG, "linesRead="+linesRead+" linesTotal="+linesTotal+" "+(linesRead/linesTotal*100)+" "+(int)(linesRead/linesTotal*100));
                        publishProgress((int)(linesRead/linesTotal*100));
                    }
                    if (inputLine.length() > 0 && inputLine.contains("avg")) {  // when we get to the last line of executed ping command
                        break;
                    }
                    if (inputLine.length() > 0 && inputLine.contains("100% packet loss")) {  // when we get to the last line of executed ping command (all packets lost)
                        break;
                    }
                    inputLine = bufferedReader.readLine();
                }
            }
            catch (IOException e){
                Log.e(TAG, "getLatency: EXCEPTION");
                e.printStackTrace();
            }

            if(inputLine == null || inputLine.contains("100% packet loss")) {
                Log.w(TAG, "ping failed");
                pingFailed = true;
            } else {
                // Extract the average round trip time from the inputLine string
                regex = "(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+)\\/(\\d+\\.\\d+) ms";
                pattern = Pattern.compile(regex);
                matcher = pattern.matcher(inputLine);
                if (matcher.find()) {
                    Log.i(TAG, "output=" + matcher.group(0));
                    latencyMin = Double.parseDouble(matcher.group(1));
                    latencyAvg = Double.parseDouble(matcher.group(2));
                    latencyMax = Double.parseDouble(matcher.group(3));
                    latencyStddev = Double.parseDouble(matcher.group(4));
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            latencyTestProgress = pingFailed ? 0 : 100;
            latencyTestTaskRunning = false;
            if(mSpeedTestResultsListener != null) {
                mSpeedTestResultsListener.onLatencyProgress();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            latencyTestProgress = progress[0];
            if(mSpeedTestResultsListener != null) {
                mSpeedTestResultsListener.onLatencyProgress();
            }
        }

    }

    public class SpeedTestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            speedTestTaskRunning = true;
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();

            // add a listener to wait for speedtest completion and progress
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(final SpeedTestReport report) {
                    // called when download/upload is finished
                    Log.v(TAG, "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
                    BigDecimal divisor = new BigDecimal(BYTES_TO_MBYTES);
                    mbps = report.getTransferRateBit().divide(divisor);
                    speedTestProgress = 100;
                    speedTestTaskRunning = false;
                    mSpeedTestResultsListener.onBandwidthProgress();
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a download/upload error occur
                }

                @Override
                public void onProgress(final float percent, final SpeedTestReport report) {
                    // called to notify download/upload progress
                    Log.v(TAG, "[PROGRESS] "+percent + "% - rate in bit/s   : " + report.getTransferRateBit());
                    BigDecimal divisor = new BigDecimal(BYTES_TO_MBYTES);
                    mbps = report.getTransferRateBit().divide(divisor);
                    speedTestProgress = (int) percent;
                    if(mSpeedTestResultsListener != null) {
                        mSpeedTestResultsListener.onBandwidthProgress();
                    }
                }
            });

            speedTestSocket.startDownload(downloadUri);

            return null;
        }
    }


    public String getCloudletName() {
        return mCloudletName;
    }

    public void setCloudletName(String mCloudletName) {
        this.mCloudletName = mCloudletName;
    }

    public String getCarrierName() {
        return mCarrierName;
    }

    public void setCarrierName(String mCarrierName) {
        this.mCarrierName = mCarrierName;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double Latitude) {
        this.mLatitude = Latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public double getDistance() {
        return mDistance;
    }

    public void setDistance(double mDistance) {
        this.mDistance = mDistance;
    }

    public Marker getMarker() { return mMarker; }

    public void setMarker(Marker mMarker) { this.mMarker = mMarker; }

    public boolean isBestMatch() { return bestMatch; }

    public void setBestMatch(boolean bestMatch) { this.bestMatch = bestMatch; }

    public double getLatencyMin() {
        return latencyMin;
    }

    public double getLatencyAvg() {
        return latencyAvg;
    }

    public double getLatencyMax() {
        return latencyMax;
    }

    public double getLatencyStddev() {
        return latencyStddev;
    }

    public BigDecimal getMbps() {
        return mbps;
    }

    public int getLatencyTestProgress() {
        return latencyTestProgress;
    }

    public int getSpeedTestProgress() {
        return speedTestProgress;
    }

    public boolean isPingFailed() {
        return pingFailed;
    }

    public void setPingFailed(boolean pingFailed) {
        this.pingFailed = pingFailed;
    }

    public boolean isLatencyTestTaskRunning() {
        return latencyTestTaskRunning;
    }

    public void setLatencyTestTaskRunning(boolean latencyTestTaskRunning) {
        this.latencyTestTaskRunning = latencyTestTaskRunning;
    }

    public String getAppName() {
        return mAppName;
    }

    public void setAppName(String mAppName) {
        this.mAppName = mAppName;
    }

    public int getNumPackets() { return mNumPackets; }

    public void setNumPackets(int mNumPings) { this.mNumPackets = mNumPings; }

    public int getNumBytes() { return mNumBytes; }

    public void setNumBytes(int mNumBytes) { this.mNumBytes = mNumBytes; }

}
