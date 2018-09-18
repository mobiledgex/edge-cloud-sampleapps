package com.mobiledgex.sdkdemo;

import android.util.ArrayMap;

/**
 * Singleton class to allow access to cloudlet list throughout the app.
 */
public class CloudletListHolder {
    private static final CloudletListHolder ourInstance = new CloudletListHolder();

    private ArrayMap<String, Cloudlet> mCloudletList = new ArrayMap<>();
    private LatencyTestMethod latencyTestMethod = LatencyTestMethod.ping;

    public static CloudletListHolder getSingleton() {
        return ourInstance;
    }

    private CloudletListHolder() {
    }

    public ArrayMap<String, Cloudlet> getCloudletList() {
        return mCloudletList;
    }

    public void setCloudlets(ArrayMap<String, Cloudlet> mCloudlets) {
        this.mCloudletList = mCloudlets;
    }

    public enum LatencyTestMethod {
        ping,
        socket
    }

    public LatencyTestMethod getLatencyTestMethod() {
        return latencyTestMethod;
    }

    public void setLatencyTestMethod(String latencyTestMethod) {
        this.latencyTestMethod = LatencyTestMethod.valueOf(latencyTestMethod);
        System.out.println("String latencyTestMethod="+latencyTestMethod+" enum latencyTestMethod="+this.latencyTestMethod);
    }
}
