/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobiledgex.matchingengine.MatchingEngine;

import org.json.JSONArray;

import java.text.DecimalFormat;

public class PoseProcessorFragment extends ImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface {
    private static final String TAG = "PoseProcessorFragment";
    public static final String EXTRA_POSE_STROKE_WIDTH = "EXTRA_POSE_STROKE_WIDTH";
    public static final String EXTRA_POSE_JOINT_RADIUS = "EXTRA_POSE_JOINT_RADIUS";
    private static final String VIDEO_FILE_NAME = "Jason.mp4";
    private PoseRenderer mPoseRenderer;

    private TextView mLatencyFull;
    private TextView mLatencyNet;
    private TextView mStdFull;
    private TextView mStdNet;

    public static final String DEF_OPENPOSE_HOST_EDGE = "posedetection.defaultedge.mobiledgex.net";

    public static PoseProcessorFragment newInstance() {
        return new PoseProcessorFragment();
    }

    public String getStatsText() {
        if (mImageSenderEdge != null) {
            return mImageSenderEdge.getStatsText();
        } else {
            return "Edge never initialized.";
        }
    }

    /**
     * Perform any processing of the given bitmap.
     *
     * @param bitmap  The bitmap from the camera or video.
     * @param imageRect  The coordinates of the image on the screen. Needed for scaling/offsetting
     *                   resulting pose skeleton coordinates.
     */
    @Override
    public void onBitmapAvailable(Bitmap bitmap, Rect imageRect) {
        if(bitmap == null) {
            return;
        }

        mImageRect = imageRect;
        mServerToDisplayRatioX = (float) mImageRect.width() / bitmap.getWidth();
        mServerToDisplayRatioY = (float) mImageRect.height() / bitmap.getHeight();

        Log.d(TAG, "mImageRect="+mImageRect.toShortString()+" mImageRect.height()="+mImageRect.height()+" bitmap.getWidth()="+bitmap.getWidth()+" bitmap.getHeight()="+bitmap.getHeight()+" mServerToDisplayRatioX=" + mServerToDisplayRatioX +" mServerToDisplayRatioY=" + mServerToDisplayRatioY);

        if (mImageSenderEdge != null) {
            mImageSenderEdge.sendImage(bitmap);
        } else {
            Log.d(TAG, "Waiting for mImageSenderEdge to be initialized.");
        }
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
    public void updateTrainingProgress(int trainingCount, ImageSender.CameraMode mode) {
        throw new UnsupportedOperationException();
    }

    public void updateFullProcessStats(final CloudletType cloudletType, RollingAverage rollingAverage) {
        final long stdDev = rollingAverage.getStdDev();
        final long latency;
        if(prefUseRollingAvg) {
            latency = rollingAverage.getAverage();
        } else {
            latency = rollingAverage.getCurrent();
        }
        if(getActivity() == null) {
            Log.w(TAG, "Activity has gone away. Abort UI update");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(cloudletType) {
                    case EDGE:
                    case CLOUD:
                        mLatencyFull.setText("Full Process Latency: " + String.valueOf(latency/1000000) + " ms");
                        mStdFull.setText("Stddev: " + new DecimalFormat("#.##").format(stdDev/1000000) + " ms");
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void updateNetworkStats(final CloudletType cloudletType, RollingAverage rollingAverage) {
        final long stdDev = rollingAverage.getStdDev();
        final long latency;
        if(prefUseRollingAvg) {
            latency = rollingAverage.getAverage();
        } else {
            latency = rollingAverage.getCurrent();
        }

        if(getActivity() == null) {
            Log.w(TAG, "Activity has gone away. Abort UI update");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(cloudletType) {
                    case EDGE:
                    case CLOUD:
                        mLatencyNet.setText("Network Only Latency: " + String.valueOf(latency/1000000) + " ms");
                        mStdNet.setText("Stddev: " + new DecimalFormat("#.##").format(stdDev/1000000) + " ms");
                        break;
                    default:
                        break;
                }
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

        mMatchingEngine = new MatchingEngine(getContext());

        mCamera2BasicFragment = new Camera2BasicFragment();
        mCamera2BasicFragment.setImageProviderInterface(this);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.child_camera_fragment_container, mCamera2BasicFragment).commit();

        mCameraToolbar = view.findViewById(R.id.cameraToolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mCameraToolbar);

        mLatencyFull = view.findViewById(R.id.full_latency);
        mLatencyNet = view.findViewById(R.id.network_latency);
        mStdFull = view.findViewById(R.id.full_std_dev);
        mStdNet = view.findViewById(R.id.network_std_dev);
        mStatusText = view.findViewById(R.id.statusTextView);
        mStatusText.setVisibility(View.GONE);
        mPoseRenderer = view.findViewById(R.id.poseSkeleton);

        setupLogViewer(view);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, "ALL");

        String prefKeyOpenPoseHostEdge = getResources().getString(R.string.preference_openpose_host_edge);
        mHostDetectionEdge = prefs.getString(prefKeyOpenPoseHostEdge, DEF_OPENPOSE_HOST_EDGE);
        Log.i(TAG, "prefKeyOpenPoseHostEdge="+prefKeyOpenPoseHostEdge+" mHostDetectionEdge="+ mHostDetectionEdge);

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

        if (mGpuHostNameOverride) {
            mEdgeHostList.clear();
            mEdgeHostListIndex = 0;
            mEdgeHostList.add(mHostDetectionEdge);
            showMessage("Overriding GPU host. Host=" + mHostDetectionEdge);
            restartImageSenderEdge();
        } else {
            findCloudletGpuInBackground();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(TAG, "onCreateOptionsMenu");
        mOptionsMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.camera_menu, menu);

        //Remove these menu items.
        menu.findItem(R.id.action_camera_training).setVisible(false);
        menu.findItem(R.id.action_camera_training_guest).setVisible(false);
        menu.findItem(R.id.action_camera_remove_training_data).setVisible(false);
        menu.findItem(R.id.action_camera_remove_training_guest_data).setVisible(false);

        //No Cloud available for benchmarking
        menu.findItem(R.id.action_benchmark_cloud).setVisible(false);

        // Declutter the menu, but keep the code in place in case we need it later.
        menu.findItem(R.id.action_camera_debug).setVisible(false);
    }

    @Override
    protected void toggleViews() {
        if(prefShowFullLatency) {
            mLatencyFull.setVisibility(View.VISIBLE);
        } else {
            mLatencyFull.setVisibility(View.INVISIBLE);
            mStdFull.setVisibility(View.GONE);
            mStdNet.setVisibility(View.GONE);
        }
        if(prefShowNetLatency) {
            mLatencyNet.setVisibility(View.VISIBLE);
            mStdNet.setVisibility(View.VISIBLE);
        } else {
            mLatencyNet.setVisibility(View.INVISIBLE);
            mStdNet.setVisibility(View.GONE);
        }
        if(prefShowStdDev) {
            if(prefShowNetLatency) {
                mStdFull.setVisibility(View.VISIBLE);
            }
            if(prefShowNetLatency) {
                mStdNet.setVisibility(View.VISIBLE);
            }
        } else {
            mStdFull.setVisibility(View.GONE);
            mStdNet.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "Pose Detection onSharedPreferenceChanged("+key+")");
        if(getContext() == null) {
            //Can happen during rapid screen rotations.
            return;
        }
        super.onSharedPreferenceChanged(sharedPreferences, key);

        String prefKeyHostGpuOverride = getResources().getString(R.string.pref_override_gpu_cloudlet_hostname);
        String prefKeyHostGpu = getResources().getString(R.string.preference_openpose_host_edge);

        if (key.equals(prefKeyHostGpuOverride) || key.equals("ALL")) {
            mGpuHostNameOverride = sharedPreferences.getBoolean(prefKeyHostGpuOverride, false);
            Log.i(TAG, "key="+key+" mGpuHostNameOverride="+ mGpuHostNameOverride);
            if (mGpuHostNameOverride) {
                mHostDetectionEdge = sharedPreferences.getString(prefKeyHostGpu, DEF_OPENPOSE_HOST_EDGE);
                Log.i(TAG, "key="+key+" mHostDetectionEdge="+ mHostDetectionEdge);
            }
        }
        if (key.equals(prefKeyHostGpu) || key.equals("ALL")) {
            mHostDetectionEdge = sharedPreferences.getString(prefKeyHostGpu, DEF_OPENPOSE_HOST_EDGE);
            Log.i(TAG, "key="+key+" mHostDetectionEdge="+ mHostDetectionEdge);
        }

        if (key.equals(prefKeyHostGpu) || key.equals(prefKeyHostGpuOverride)) {
            if (mGpuHostNameOverride) {
                mEdgeHostList.clear();
                mEdgeHostListIndex = 0;
                mEdgeHostList.add(mHostDetectionEdge);
                showMessage("mHostDetectionEdge set to " + mHostDetectionEdge);
                restartImageSenderEdge();
            }
        }
    }
}
