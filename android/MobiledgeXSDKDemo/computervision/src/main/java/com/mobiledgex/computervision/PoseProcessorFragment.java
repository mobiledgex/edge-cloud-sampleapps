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

package com.mobiledgex.computervision;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobiledgex.matchingenginehelper.EventLogViewer;
import com.mobiledgex.matchingenginehelper.MatchingEngineHelper;

import org.json.JSONArray;

public class PoseProcessorFragment extends GpuImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface {
    private static final String TAG = "PoseProcessorFragment";
    public static final String EXTRA_POSE_STROKE_WIDTH = "EXTRA_POSE_STROKE_WIDTH";
    public static final String EXTRA_POSE_JOINT_RADIUS = "EXTRA_POSE_JOINT_RADIUS";
    private static final String VIDEO_FILE_NAME = "Jason.mp4";
    private PoseRenderer mPoseRenderer;

    public static PoseProcessorFragment newInstance() {
        return new PoseProcessorFragment();
    }

    /**
     * Update the body poses.
     *
     * @param cloudletType  The cloudlet type. Not used for Poses.
     * @param posesJsonArray  An array of skeleton coordinates for each pose detected.
     * @param subject  Should be null or empty for Poses.
     */
    @Override
    public void updateOverlay(CloudletType cloudletType, final JSONArray posesJsonArray, String subject) {
        Log.i(TAG, "updateOverlay Poses("+cloudletType+","+posesJsonArray.toString());
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) {
                    //Happens during screen rotation
                    Log.e(TAG, "updatePoses abort - null activity");
                    return;
                }
                if (posesJsonArray.length() == 0) {
                    Log.d(TAG, "Empty poses array received. Discarding.");
                    return;
                }

                boolean mirrored = mCamera2BasicFragment.getCameraLensFacingDirection() ==
                        CameraCharacteristics.LENS_FACING_FRONT
                        && !mCamera2BasicFragment.isLegacyCamera()
                        && !mCamera2BasicFragment.isVideoMode();

                if(mCamera2BasicFragment.isVideoMode()) {
                    mirrored = false;
                }

                mPoseRenderer.setDisplayParms(mImageRect, mServerToDisplayRatioX, mServerToDisplayRatioY, mirrored);
                mPoseRenderer.setPoses(posesJsonArray);
                mPoseRenderer.invalidate();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView PoseProcessorFragment");
        return inflater.inflate(R.layout.fragment_pose_processor, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated PoseProcessorFragment savedInstanceState="+savedInstanceState);

        mCamera2BasicFragment = new Camera2BasicFragment();
        mCamera2BasicFragment.setImageProviderInterface(this);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.child_camera_fragment_container, mCamera2BasicFragment).commit();

        mCameraToolbar = view.findViewById(R.id.cameraToolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mCameraToolbar);

        mEdgeLatencyFull = view.findViewById(R.id.full_latency);
        mEdgeLatencyNet = view.findViewById(R.id.network_latency);
        mEdgeStdFull = view.findViewById(R.id.full_std_dev);
        mEdgeStdNet = view.findViewById(R.id.network_std_dev);
        mStatusText = view.findViewById(R.id.statusTextView);
        mStatusText.setVisibility(View.GONE);
        mPoseRenderer = view.findViewById(R.id.poseSkeleton);

        RecyclerView eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        FloatingActionButton logExpansionButton = view.findViewById(R.id.fab);
        mEventLogViewer = new EventLogViewer(getActivity(), logExpansionButton, eventsRecyclerView);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, ALL_PREFS);

        Intent intent = getActivity().getIntent();
        getCommonIntentExtras(intent);

        // Check for other optional parameters
        int strokeWidth = intent.getIntExtra(EXTRA_POSE_STROKE_WIDTH, PoseRenderer.DEFAULT_STROKE_WIDTH);
        int jointRadius = intent.getIntExtra(EXTRA_POSE_JOINT_RADIUS, PoseRenderer.DEFAULT_JOINT_RADIUS);
        mPoseRenderer.setStrokeWidth(strokeWidth);
        mPoseRenderer.setJointRadius(jointRadius);

        mCameraMode = ImageSender.CameraMode.POSE_DETECTION;
        mCameraToolbar.setTitle(R.string.title_activity_pose_detection);

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
                .setView(mPoseRenderer)
                .setTestPort(FACE_DETECTION_HOST_PORT)
                .build();

        setAppNameForGpu();
        getProvisioningData();
    }
}
