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
 * This class holds static variables to allow access throughout the app.
 */
public class CloudletListHolder {
    private static ArrayMap<String, Cloudlet> mCloudletList = new ArrayMap<>();
    private static LatencyTestMethod latencyTestMethod = LatencyTestMethod.ping;
    private static boolean latencyTestAutoStart;
    private static int numBytesDownload;
    private static int numBytesUpload;
    private static int numPackets;

    public static int getNumBytesDownload() {
        return numBytesDownload;
    }

    public static void setNumBytesDownload(int numBytes) {
        numBytesDownload = numBytes;
    }

    public static int getNumBytesUpload() {
        return numBytesUpload;
    }

    public static void setNumBytesUpload(int numBytes) {
        numBytesUpload = numBytes;
    }

    public static int getNumPackets() {
        return numPackets;
    }

    public static void setNumPackets(int count) {
        numPackets = count;
    }

    public enum LatencyTestMethod {
        ping,
        socket,
        NetTest
    }

    private CloudletListHolder() {
    }

    public static ArrayMap<String, Cloudlet> getCloudletList() {
        return mCloudletList;
    }

    public static void setCloudlets(ArrayMap<String, Cloudlet> mCloudlets) {
        mCloudletList = mCloudlets;
    }

    public static boolean getLatencyTestAutoStart() {
        return latencyTestAutoStart;
    }

    public static void setLatencyTestAutoStart(boolean autoStart) {
        latencyTestAutoStart = autoStart;
    }

    public static LatencyTestMethod getLatencyTestMethod() {
        return latencyTestMethod;
    }

    public static void setLatencyTestMethod(String method) {
        latencyTestMethod = LatencyTestMethod.valueOf(method);
        System.out.println("String latencyTestMethod="+method+" enum latencyTestMethod="+latencyTestMethod);
    }
}
