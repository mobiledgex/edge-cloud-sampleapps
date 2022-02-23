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

package com.mobiledgex.tritonlib;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobiledgex.computervision.Camera2BasicFragment;
import com.mobiledgex.computervision.GpuImageProcessorFragment;
import com.mobiledgex.computervision.ImageProviderInterface;
import com.mobiledgex.computervision.ImageSender;
import com.mobiledgex.computervision.ImageServerInterface;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingenginehelper.ConnectionTester;
import com.mobiledgex.matchingenginehelper.EventLogViewer;
import com.mobiledgex.matchingenginehelper.MatchingEngineHelper;
import com.mobiledgex.tritonlib.databinding.FragmentInceptionProcessorBinding;

import org.json.JSONArray;

import static com.mobiledgex.tritonlib.Yolov4ProcessorFragment.TRITON_HTTP_PORT;

public class InceptionProcessorFragment extends GpuImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface {
    private static final String TAG = "InceptionProcessorFragment";
    private static final String VIDEO_FILE_NAME = "objects.mp4";
    private InceptionClassRenderer mInceptionClassRenderer;

    private FragmentInceptionProcessorBinding viewBinding;

    private String mModelName;

    public static InceptionProcessorFragment newInstance() {
        return new InceptionProcessorFragment();
    }

    public void restartImageSenderEdge() {
        if (!mAttached) {
            Log.w(TAG, "Fragment is detached. Aborting restartImageSenderEdge()");
            return;
        }
        String message;
        if (mImageSenderEdge == null) {
            message = "Starting " + mCameraToolbar.getTitle() + " on EDGE host " + mHostDetectionEdge;
            mEventLogViewer.collapseAfter(3000);
        } else {
            message = "Restarting " + mCameraToolbar.getTitle() + " on EDGE host " + mHostDetectionEdge;
            mImageSenderEdge.closeConnection();
        }
        Log.i(TAG, message);
        showMessage(message);

        boolean tls = mTlsEdge;
        if (mEdgeHostNameOverride) {
            tls = mEdgeHostNameTls;
        }
        mImageSenderEdge = new ImageSenderTriton.Builder()
                .setActivity(getActivity())
                .setImageServerInterface(this)
                .setCloudLetType(CloudletType.EDGE)
                .setTls(tls)
                .setHost(mHostDetectionEdge)
                .setPort(TRITON_HTTP_PORT)
                .setCameraMode(mCameraMode)
                .setModelName(mModelName)
                .build();
    }

    /**
     * Update the object coordinates.
     *
     * @param cloudletType  The cloudlet type. Not used for Inception full image classification.
     * @param objectsJsonArray  Not used for Inception full image classification.
     * @param className  The class name inferred for the image.
     */
    @Override
    public void updateOverlay(CloudletType cloudletType, final JSONArray objectsJsonArray, String className) {
        Log.i(TAG, "updateOverlay objects("+cloudletType+","+className);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) {
                    //Happens during screen rotation
                    Log.e(TAG, "updateOverlay abort - null activity");
                    return;
                }

                boolean mirrored = mCamera2BasicFragment.getCameraLensFacingDirection() ==
                        CameraCharacteristics.LENS_FACING_FRONT
                        && !mCamera2BasicFragment.isLegacyCamera()
                        && !mCamera2BasicFragment.isVideoMode();

                if(mCamera2BasicFragment.isVideoMode()) {
                    mirrored = false;
                }

                mInceptionClassRenderer.setDisplayParms(mImageRect, mServerToDisplayRatioX, mServerToDisplayRatioY, mirrored);
                mInceptionClassRenderer.setClassName(className);
                mInceptionClassRenderer.invalidate();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView Yolov4ProcessorFragment");
        viewBinding = FragmentInceptionProcessorBinding.inflate(inflater, container, false);
        View view = viewBinding.getRoot();
        return view;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated savedInstanceState="+savedInstanceState);

        mCamera2BasicFragment = new Camera2BasicFragment();
        mCamera2BasicFragment.setImageProviderInterface(this);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.child_camera_fragment_container, mCamera2BasicFragment).commit();

        mCameraToolbar = viewBinding.cameraToolbar;
        ((AppCompatActivity)getActivity()).setSupportActionBar(mCameraToolbar);

        mEdgeLatencyFull = viewBinding.fullLatency;
        mEdgeLatencyNet = viewBinding.networkLatency;
        mEdgeStdFull = viewBinding.fullStdDev;
        mEdgeStdNet = viewBinding.networkStdDev;
        mStatusText = viewBinding.statusTextView;
        mStatusText.setVisibility(View.GONE);
        mInceptionClassRenderer = viewBinding.inceptionClassRenderer;

        RecyclerView eventsRecyclerView = viewBinding.eventsRecyclerView;
        FloatingActionButton logExpansionButton = view.findViewById(R.id.fab);
        mEventLogViewer = new EventLogViewer(getActivity(), logExpansionButton, eventsRecyclerView);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, "ALL");

        Intent intent = getActivity().getIntent();

        mCameraMode = ImageSender.CameraMode.OBJECT_DETECTION;
        mModelName = "ensemble_dali_inception";

        mVideoFilename = VIDEO_FILE_NAME;

        // Initialize all values to 0, otherwise we would see the formatting string "%d" all over the UI.
        mEdgeLatencyFull.setText(getResources().getString(R.string.full_process_latency_label, 0));
        mEdgeStdFull.setText(getResources().getString(R.string.stddev_label, 0));
        mEdgeLatencyNet.setText(getResources().getString(R.string.edge_label, 0));
        mEdgeStdNet.setText(getResources().getString(R.string.stddev_label, 0));

        fullLatencyEdgeLabel = R.string.full_process_latency_label;
        networkLatencyEdgeLabel = R.string.network_latency_label;

        meHelper = new MatchingEngineHelper.Builder()
                .setActivity(getActivity())
                .setMeHelperInterface(this)
                .setView(mInceptionClassRenderer)
                .build();

        if (mEdgeHostNameOverride) {
            mEdgeHostList.clear();
            mEdgeHostListIndex = 0;
            mEdgeHostList.add(mHostDetectionEdge);
            showMessage("Overriding Edge host. Host=" + mHostDetectionEdge);
            restartImageSenderEdge();
        } else {
            meHelper.findCloudletInBackground();
        }

    }

    @Override
    public ConnectionTester makeConnectionTester(boolean tls) {
        int testConnectionPort = TRITON_HTTP_PORT;
        String testUrl = "/v2/models/inception_graphdef/versions/1/config";
        String expectedResponse = "\"name\":\"inception_graphdef\"";
        String scheme = tls ? "https" : "http";
        String appInstUrl = scheme+"://"+meHelper.mClosestCloudlet.getFqdn()+":"+testConnectionPort+testUrl;

        return new ConnectionTester(appInstUrl, expectedResponse);
    }
}
