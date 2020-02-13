/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
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

package com.mobiledgex.workshopskeleton;

import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.util.Log;

import com.mobiledgex.matchingengine.AppConnectionManager;
import com.mobiledgex.matchingengine.DmeDnsException;
import com.mobiledgex.matchingengine.MatchingEngine;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;

public class FaceProcessorActivity extends AppCompatActivity {

    private static final String TAG = "FaceProcessorActivity";
    private FaceProcessorFragment mFaceProcessorFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_face_processor);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (null == savedInstanceState) {
            exampleDevloperWorkflowBackground();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        // Rotating the device creates a new instance of the fragment. Update reference here.
        if (fragment instanceof FaceProcessorFragment)
            mFaceProcessorFragment = (FaceProcessorFragment) fragment;
    }

    @Override
    public void finish() {
        Intent resultIntent = new Intent();
        String stats = mFaceProcessorFragment.getStatsText();
        resultIntent.putExtra("STATS", stats);
        setResult(RESULT_OK, resultIntent);
        super.finish();
    }

    public Socket exampleDeveloperWorkflow() {
        MatchingEngine me = new MatchingEngine(this);
        AppConnectionManager appConnect = me.getAppConnectionManager();

        String appName = "MobiledgeX SDK Demo";
        String appVersion = "2.0";
        String carrierName = "wifi";
        String devName = "MobiledgeX";
        Location location = new Location("MEX");
        location.setLatitude(52.52);
        location.setLongitude(13.4040);    //Berlin

        Future<AppClient.FindCloudletReply> future = me.registerAndFindCloudlet(this, devName, appName, appVersion, carrierName, location, "", 0, "", "", null);
        AppClient.FindCloudletReply findCloudletReply;
        try {
            findCloudletReply = future.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "RegisterAndFindCloudlet error " + e.getMessage());
            return null;
        }

        HashMap<Integer, Appcommon.AppPort> portMap = appConnect.getTCPMap(findCloudletReply);
        Appcommon.AppPort one = portMap.get(8008); // 8011 for persistent tcp

        me.setNetworkSwitchingEnabled(true);
        Future<Socket> fs = appConnect.getTcpSocket(findCloudletReply, one, one.getPublicPort(), 15000);
        me.setNetworkSwitchingEnabled(false); // if using wifi only

        if (fs == null) {
            Log.e(TAG, "Socket future didnt return anything");
            return null;
        }

        Socket socket;
        try {
            socket = fs.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Cannot get socket from future. Exception: " + e.getMessage());
            return null;
        }
        return socket;
    }

    private void exampleDevloperWorkflowBackground() {
        // Creates new BackgroundRequest object which will call exampleDeveloperWorkflow
        new ExampleDeveloperWorkflowBackgroundRequest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class ExampleDeveloperWorkflowBackgroundRequest extends AsyncTask<Object, Void, Socket> {
        @Override
        protected Socket doInBackground(Object... params) {
            return exampleDeveloperWorkflow();
        }

        @Override
        protected void onPostExecute(Socket socket) {
            if (socket != null) {
                mFaceProcessorFragment = FaceProcessorFragment.newInstance();
                mFaceProcessorFragment.mHost = socket.getInetAddress().getHostName();
                mFaceProcessorFragment.mPort = socket.getPort();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, mFaceProcessorFragment)
                        .commit();
                try {
                    socket.close();
                } catch (IOException ioe) {
                    Log.e(TAG, "Unable to close socket. IOException: " + ioe.getMessage());
                }
            }
        }
    }

}
