package com.mobiledgex.sdkdemo;

import android.util.ArrayMap;

/**
 * Singleton class to allow access to cloudlet list throughout the app.
 */
public class CloudletListHolder {
    private static final CloudletListHolder ourInstance = new CloudletListHolder();

    private ArrayMap<String, Cloudlet> mCloudletList = new ArrayMap<>();
    private LatencyTestMethod latencyTestMethod = LatencyTestMethod.ping;
    private DownloadTestType downloadTestType = DownloadTestType.dynamic;
    private boolean latencyTestAutoStart;
    private int numBytes;
    private int numPackets;

    public static CloudletListHolder getSingleton() {
        return ourInstance;
    }

    public int getNumBytes() {
        return numBytes;
    }

    public void setNumBytes(int numBytes) {
        this.numBytes = numBytes;
    }

    public int getNumPackets() {
        return numPackets;
    }

    public void setNumPackets(int numPackets) {
        this.numPackets = numPackets;
    }

    public enum LatencyTestMethod {
        ping,
        socket
    }

    public enum DownloadTestType {
        dynamic,
        staticFile
    }

    private CloudletListHolder() {
    }

    public ArrayMap<String, Cloudlet> getCloudletList() {
        return mCloudletList;
    }

    public void setCloudlets(ArrayMap<String, Cloudlet> mCloudlets) {
        this.mCloudletList = mCloudlets;
    }

    public DownloadTestType getDownloadTestType() {
        return downloadTestType;
    }

    public void setDownloadTestType(String downloadTestType) {
        this.downloadTestType = DownloadTestType.valueOf(downloadTestType);
        System.out.println("String DownloadTestType="+downloadTestType+" enum downloadTestType="+this.downloadTestType);
    }

    public boolean getLatencyTestAutoStart() {
        return latencyTestAutoStart;
    }

    public void setLatencyTestAutoStart(boolean latencyTestAutoStart) {
        this.latencyTestAutoStart = latencyTestAutoStart;
    }

    public LatencyTestMethod getLatencyTestMethod() {
        return latencyTestMethod;
    }

    public void setLatencyTestMethod(String latencyTestMethod) {
        this.latencyTestMethod = LatencyTestMethod.valueOf(latencyTestMethod);
        System.out.println("String latencyTestMethod="+latencyTestMethod+" enum latencyTestMethod="+this.latencyTestMethod);
    }
}
