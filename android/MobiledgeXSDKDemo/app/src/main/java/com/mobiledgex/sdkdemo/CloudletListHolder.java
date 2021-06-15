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

import android.util.ArrayMap;

/**
 * Singleton class to allow access to cloudlet list throughout the app.
 */
public class CloudletListHolder {
    private static final CloudletListHolder ourInstance = new CloudletListHolder();

    private ArrayMap<String, Cloudlet> mCloudletList = new ArrayMap<>();
    private LatencyTestMethod latencyTestMethod = LatencyTestMethod.ping;
    private boolean latencyTestAutoStart;
    private int numBytesDownload;
    private int numBytesUpload;
    private int numPackets;

    public static CloudletListHolder getSingleton() {
        return ourInstance;
    }

    public int getNumBytesDownload() {
        return numBytesDownload;
    }

    public void setNumBytesDownload(int numBytes) {
        this.numBytesDownload = numBytes;
    }

    public int getNumBytesUpload() {
        return numBytesUpload;
    }

    public void setNumBytesUpload(int numBytes) {
        this.numBytesUpload = numBytes;
    }

    public int getNumPackets() {
        return numPackets;
    }

    public void setNumPackets(int numPackets) {
        this.numPackets = numPackets;
    }

    public enum LatencyTestMethod {
        ping,
        socket,
        NetTest
    }

    private CloudletListHolder() {
    }

    public ArrayMap<String, Cloudlet> getCloudletList() {
        return mCloudletList;
    }

    public void setCloudlets(ArrayMap<String, Cloudlet> mCloudlets) {
        this.mCloudletList = mCloudlets;
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
