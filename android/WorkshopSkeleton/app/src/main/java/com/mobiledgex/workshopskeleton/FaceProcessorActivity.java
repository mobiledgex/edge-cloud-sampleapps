/**
 * Copyright 2019-2021 MobiledgeX, Inc. All rights and licenses reserved.
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

import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;
import android.util.Log;

import com.mobiledgex.matchingengine.AppConnectionManager;
import com.mobiledgex.matchingengine.MatchingEngine;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;

public class FaceProcessorActivity extends AppCompatActivity {

    private FaceProcessorFragment mFaceProcessorFragment;

    private static final String TAG = "FaceProcessorFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_face_processor);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (null == savedInstanceState) {
            exampleDeveloperWorkflowBackground();
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
        ////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to follow example developer workflow and get FaceDetection started.
        return null;
        ////////////////////////////////////////////////////////////////////////////////////////////
    }

    private void exampleDeveloperWorkflowBackground() {
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
