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

package com.mobiledgex.sdkdemo;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.mobiledgex.matchingengine.performancemetrics.NetTest;
import com.mobiledgex.matchingengine.performancemetrics.Site;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

import static com.mobiledgex.sdkdemo.MainActivity.DEFAULT_SPEED_TEST_PORT;

public class Cloudlet implements Serializable {
    private static final String TAG = "Cloudlet";

    // For data transfer measurements, we use base 10. "Mbits/sec" for displayed units.
    public static final int BITS_TO_MBITS = 1000*1000;

    private String mCloudletName;
    private String mAppName;
    private String mCarrierName;
    private double mLatitude;
    private double mLongitude;
    private double mDistance;
    private boolean mBestMatch;
    private transient Marker mMarker;

    private double latencyMin=9999;
    private double latencyAvg=0;
    private double latencyMax=0;
    private double latencyStddev=0;
    private double latencyTotal=0;
    private BigDecimal downloadMbps = BigDecimal.valueOf(0);
    private BigDecimal uploadMbps = BigDecimal.valueOf(0);
    private int latencyTestProgress = 0;
    private int mSpeedTestDownloadProgress = 0;
    private int mSpeedTestUploadProgress = 0;
    private long startTime;
    private double timeDifference;
    private int mNumPackets = 4;
    private int mNumBytes = 1048576;
    private boolean runningOnEmulator = false;
    private boolean pingFailed = false;

    private SpeedTestResultsInterface mSpeedTestResultsInterface;

    private String mHostName;
    private int mOpenPort = DEFAULT_SPEED_TEST_PORT;
    private final int socketTimeout = 3000;
    private boolean latencyTestTaskRunning = false;
    private boolean speedTestDownloadTaskRunning = false;
    private boolean speedTestUploadTaskRunning = false;
    private String speedTestDownloadErrorMessage = "";
    private String speedTestUploadErrorMessage = "";
    private String mFqdnPrefix;
    private String mIpAddress;
    private String mFqdn;
    private String mScheme;
    private CloudletListHolder.LatencyTestMethod mLatencyTestMethod;
    private boolean mLatencyTestMethodForced = false;
    private Context mContext;
    private boolean mRemoved;

    public Cloudlet(String cloudletName, String appName, String carrierName, LatLng gpsLocation, double distance, String fqdn, String fqdnPrefix, boolean tls, Marker marker, int port) {
        Log.d(TAG, "Cloudlet contructor. cloudletName="+cloudletName);
        mCloudletName = cloudletName;
        mAppName = appName;
        mCarrierName = carrierName;
        mLatitude = gpsLocation.latitude;
        mLongitude = gpsLocation.longitude;
        mDistance = distance;
        mMarker = marker;
        setUri(fqdnPrefix, fqdn, tls, port);

        if(CloudletListHolder.getLatencyTestAutoStart()) {
            //All AsyncTask instances are run on the same thread, so this queues up the tasks.
            startLatencyTest();
        } else {
            Log.i(TAG, "LatencyTestAutoStart is disabled");
        }

        mLatencyTestMethod = CloudletListHolder.getLatencyTestMethod();
    }

    /**
     * From the given parameters, create the hostname that will be pinged.
     * @param fqdnPrefix
     * @param fqdn
     * @param tls
     * @param port
     */
    public void setUri(String fqdnPrefix, String fqdn, boolean tls, int port) {
        Log.i(TAG, "mCarrierName="+mCarrierName+ " setUri("+fqdnPrefix+fqdn+":"+port+")");
        mOpenPort = port;
        mFqdnPrefix = fqdnPrefix;
        mHostName = fqdnPrefix+fqdn;
        mFqdn = fqdn;
        if (tls) {
            mScheme = "https";
        } else {
            mScheme = "http";
        }
    }

    public String getHostName() {
        return mHostName;
    }
    public String getFqdn() {
        return mFqdn;
    }

    /**
     * Build the download URI based on the download size preference.
     * @return  The URI.
     */
    private String getDownloadUri() {
        mNumBytes = CloudletListHolder.getNumBytesDownload();
        return mScheme+"://"+ mHostName +":"+ mOpenPort +"/getdata/?numbytes="+ mNumBytes;
    }

    /**
     * Build the download URI.
     * @return  The URI.
     */
    private String getUploadUri() {
        return mScheme+"://"+ mHostName +":"+ mOpenPort +"/uploaddata/";
    }

    public String toString() {
        return "mCarrierName="+mCarrierName+" mCloudletName="+mCloudletName+" mLatitude="+mLatitude
                +" mLongitude="+mLongitude+" mDistance="+mDistance+" mScheme="+mScheme+" hostName="+ mHostName;
    }

    public void setSpeedTestResultsListener(SpeedTestResultsInterface speedTestResultsInterface) {
        this.mSpeedTestResultsInterface = speedTestResultsInterface;

        // Now that we have a listener, start this task.
        new ResolveHostNameTask().execute();
    }

    public void startLatencyTest() {
        Log.d(TAG, "startLatencyTest()");
        if(latencyTestTaskRunning) {
            Log.d(TAG, "LatencyTest already running");
            return;
        }

        mNumPackets = CloudletListHolder.getNumPackets();

        latencyMin=9999;
        latencyAvg=0;
        latencyMax=0;
        latencyStddev=0;
        latencyTotal=0;

        //ping can't run on an emulator, so detect that case.
        Log.i(TAG, "PRODUCT="+ Build.PRODUCT);
        if (Build.PRODUCT.equalsIgnoreCase("sdk_gphone_x86")
                || Build.PRODUCT.equalsIgnoreCase("sdk_google_phone_x86")) {
            runningOnEmulator = true;
            Log.i(TAG, "YES, I am an emulator.");
        } else {
            runningOnEmulator = false;
            Log.i(TAG, "NO, I am NOT an emulator.");
        }

        mLatencyTestMethod = CloudletListHolder.getLatencyTestMethod();
        if(mCarrierName.equalsIgnoreCase("azure")) {
            Log.i(TAG, "Socket test forced for Azure");
            mLatencyTestMethod = CloudletListHolder.LatencyTestMethod.socket;
            mLatencyTestMethodForced = true;
        }
        if(runningOnEmulator) {
            Log.i(TAG, "Socket test forced for emulator");
            mLatencyTestMethod = CloudletListHolder.LatencyTestMethod.socket;
            mLatencyTestMethodForced = true;
        }

        if (mLatencyTestMethod == CloudletListHolder.LatencyTestMethod.socket) {
            new LatencyTestTaskSocket().execute();
        } else if (mLatencyTestMethod == CloudletListHolder.LatencyTestMethod.ping) {
            new LatencyTestTaskPing().execute();
        } else if (mLatencyTestMethod == CloudletListHolder.LatencyTestMethod.NetTest) {
            new LatencyTestTaskNetTest().execute();
        } else {
            Log.e(TAG, "Unknown mLatencyTestMethod: " + mLatencyTestMethod);
        }
    }

    public void startSpeedTestDownload() {
        Log.d(TAG, "downloadUri=" + getDownloadUri() + " speedTestDownloadTaskRunning="+ speedTestDownloadTaskRunning);
        if(speedTestDownloadTaskRunning) {
            Log.d(TAG, "Download SpeedTest already running");
            return;
        }
        new SpeedTestDownloadTask().execute();
    }

    public void startSpeedTestUpload() {
        Log.d(TAG, "uploadUri=" + getUploadUri() + " speedTestUploadTaskRunning="+ speedTestUploadTaskRunning);
        if(speedTestUploadTaskRunning) {
            Log.d(TAG, "Upload SpeedTest already running");
            return;
        }
        new SpeedTestUploadTask().execute();
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

    public void setContext(Context context) {
        mContext = context;
    }

    public void setRemoved(boolean removed) {
        this.mRemoved = removed;
    }

    public boolean isRemoved() {
        return mRemoved;
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
            isReachable(mHostName, mOpenPort, socketTimeout);
            for(int i = 0; i < mNumPackets; i++) {
                startTime = System.nanoTime();
                reachable = isReachable(mHostName, mOpenPort, socketTimeout);
                if(reachable) {
                    long endTime = System.nanoTime();
                    timeDifference = (endTime - startTime)/1000000.0;
                    Log.d(TAG, mHostName +" reachable="+reachable+" Latency=" + timeDifference + " ms.");
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
            String avg = String.format("%.3f", (latencyAvg));
            String stddev = String.format("%.3f", (latencyStddev));
            Log.i(TAG, mHostName +" "+mNumPackets+" packets transmitted, "+countSuccess+" packets received, "+percent+"% packet loss");
            Log.i(TAG, mHostName +" round-trip min/avg/max/stddev = "+latencyMin+"/"+avg+"/"+latencyMax+"/"+stddev+" ms");

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            latencyTestProgress = pingFailed ? 0 : 100;
            latencyTestTaskRunning = false;
            if(mSpeedTestResultsInterface != null) {
                mSpeedTestResultsInterface.onLatencyProgress();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            latencyTestProgress = progress[0];
            if(mSpeedTestResultsInterface != null) {
                mSpeedTestResultsInterface.onLatencyProgress();
            }
        }

    }

    public class LatencyTestTaskPing extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... voids) {
            pingFailed = false;
            String pingCommand = "/system/bin/ping -c "+ mNumPackets +" " + mHostName;
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
            if(mSpeedTestResultsInterface != null) {
                mSpeedTestResultsInterface.onLatencyProgress();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            latencyTestProgress = progress[0];
            if(mSpeedTestResultsInterface != null) {
                mSpeedTestResultsInterface.onLatencyProgress();
            }
        }

    }

    public class SpeedTestDownloadTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            speedTestDownloadTaskRunning = true;
            speedTestDownloadErrorMessage = "";
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();

            // add a listener to wait for speedtest completion and progress
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(final SpeedTestReport report) {
                    // called when download is finished
                    Log.v(TAG, "[DOWNLOAD COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
                    BigDecimal divisor = new BigDecimal(BITS_TO_MBITS);
                    downloadMbps = report.getTransferRateBit().divide(divisor);
                    speedTestDownloadTaskRunning = false;
                    mSpeedTestResultsInterface.onSpeedtestDownloadProgress();
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a download error occurs
                    Log.e(TAG, "Download speedTestError="+speedTestError+" errorMessage="+errorMessage);
                    speedTestDownloadErrorMessage = speedTestError.toString();
                    speedTestDownloadTaskRunning = false;
                    downloadMbps = BigDecimal.valueOf(0);
                    if(mSpeedTestResultsInterface != null) {
                        mSpeedTestResultsInterface.onSpeedtestUploadProgress();
                    }
                }

                @Override
                public void onProgress(final float percent, final SpeedTestReport report) {
                    // called to notify download progress
                    Log.v(TAG, "[DOWNLOAD PROGRESS] "+percent + "% - rate in bit/s   : " + report.getTransferRateBit());
                    BigDecimal divisor = new BigDecimal(BITS_TO_MBITS);
                    downloadMbps = report.getTransferRateBit().divide(divisor);
                    mSpeedTestDownloadProgress = (int) percent;
                    if(mSpeedTestResultsInterface != null) {
                        mSpeedTestResultsInterface.onSpeedtestDownloadProgress();
                    }
                }
            });

            speedTestSocket.startDownload(getDownloadUri());

            return null;
        }
    }

    public class SpeedTestUploadTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            speedTestUploadTaskRunning = true;
            speedTestUploadErrorMessage = "";
            uploadMbps = BigDecimal.valueOf(0);
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();

            // add a listener to wait for speedtest completion and progress
            speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

                @Override
                public void onCompletion(final SpeedTestReport report) {
                    // called when upload is finished
                    speedTestUploadTaskRunning = false;
                    Log.v(TAG, "[UPLOAD COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
                    // Do not update the transfer rate here because the POST response can take long
                    // enough to receive that it can skew the results, possibly dropping by more than 50 %.
                }

                @Override
                public void onError(SpeedTestError speedTestError, String errorMessage) {
                    // called when a upload error occurs
                    Log.e(TAG, "Upload speedTestError="+speedTestError+" errorMessage="+errorMessage);
                    // A second error may occur as SOCKET_TIMEOUT, which would overwrite a more useful
                    // INVALID_HTTP_RESPONSE error, so only keep the first error per run.
                    if (speedTestUploadErrorMessage.isEmpty()) {
                        speedTestUploadErrorMessage = speedTestError.toString();
                    }
                    speedTestUploadTaskRunning = false;
                    uploadMbps = BigDecimal.valueOf(0);
                    if(mSpeedTestResultsInterface != null) {
                        mSpeedTestResultsInterface.onSpeedtestUploadProgress();
                    }
                }

                @Override
                public void onProgress(final float percent, final SpeedTestReport report) {
                    // called to notify upload progress
                    Log.v(TAG, "[UPLOAD PROGRESS] "+percent + "% - rate in bit/s   : " + report.getTransferRateBit());
                    // When first started, the upload value may spike to an unrealistic value.
                    // Don't show anything until it's had time to settle.
                    if (percent > 25) {
                        BigDecimal divisor = new BigDecimal(BITS_TO_MBITS);
                        uploadMbps = report.getTransferRateBit().divide(divisor);
                    }
                    mSpeedTestUploadProgress = (int) percent;
                    if(mSpeedTestResultsInterface != null) {
                        mSpeedTestResultsInterface.onSpeedtestUploadProgress();
                    }
                }
            });

            mNumBytes = CloudletListHolder.getNumBytesUpload();
            speedTestSocket.startUpload(getUploadUri(), mNumBytes);

            return null;
        }
    }

    private class ResolveHostNameTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                InetAddress address = InetAddress.getByName(mHostName);
                mIpAddress = address.getHostAddress();
                return null;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            mIpAddress = "Could not resolve";
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            mSpeedTestResultsInterface.onIpAddressResolved();
        }
    }

    private class LatencyTestTaskNetTest extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            NetTest netTest = new NetTest();
            Site site = new Site(mContext, NetTest.TestType.CONNECT, mNumPackets, mHostName, mOpenPort);
            netTest.addSite(site);
            netTest.testSites(netTest.TestTimeoutMS);

            // Values for the UI to display
            latencyMin = site.min();
            latencyAvg = site.average;
            latencyMax = site.max();
            latencyStddev = site.stddev;

            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            latencyTestProgress = pingFailed ? 0 : 100;
            latencyTestTaskRunning = false;
            if(mSpeedTestResultsInterface != null) {
                mSpeedTestResultsInterface.onLatencyProgress();
            }
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

    public boolean isBestMatch() { return mBestMatch; }

    public void setBestMatch(boolean bestMatch) { this.mBestMatch = bestMatch; }

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

    public String getSpeedTestDownloadResult() {
        if (speedTestDownloadErrorMessage.isEmpty()) {
            return String.format("%.2f", downloadMbps) + " Mbits/sec";
        } else {
            return speedTestDownloadErrorMessage;
        }
    }

    public String getSpeedTestUploadResult() {
        if (speedTestUploadErrorMessage.isEmpty()) {
            return String.format("%.2f", uploadMbps) + " Mbits/sec";
        } else {
            return speedTestUploadErrorMessage;
        }
    }

    public int getLatencyTestProgress() {
        return latencyTestProgress;
    }

    public int getSpeedTestDownloadProgress() {
        return mSpeedTestDownloadProgress;
    }

    public int getSpeedTestUploadProgress() {
        return mSpeedTestUploadProgress;
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

    public String getIpAddress() {
        return mIpAddress;
    }

    public String getFqdnPrefix() {
        return mFqdnPrefix;
    }

    public CloudletListHolder.LatencyTestMethod getLatencyTestMethod() {
        return mLatencyTestMethod;
    }

    public boolean getLatencyTestMethodForced() {
        return mLatencyTestMethodForced;
    }


}
