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
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;

import java.text.DecimalFormat;

public class ObjectProcessorFragment extends ImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface {
    private static final String TAG = "ObjectProcessorFragment";
    private ObjectClassRenderer mObjectClassRenderer;

    private TextView mLatencyFull;
    private TextView mLatencyNet;
    private TextView mStdFull;
    private TextView mStdNet;

    public static final String DEF_OBJECT_DETECTION_HOST_EDGE = "posedetection.defaultedge.mobiledgex.net";

    public static ObjectProcessorFragment newInstance() {
        return new ObjectProcessorFragment();
    }

    public String getStatsText() {
        return mImageSenderEdge.getStatsText();
    }

    /**
     * Perform any processing of the given bitmap.
     *
     * @param bitmap  The bitmap from the camera or video.
     * @param imageRect  The coordinates of the image on the screen. Needed for scaling/offsetting
     *                   resulting object coordinates.
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

        mImageSenderEdge.sendImage(bitmap);
    }

    /**
     * Update the object coordinates.
     *
     * @param cloudletType  The cloudlet type. Not used for object detection.
     * @param objectsJsonArray  An array of rectangular coordinates and class names for each object detected.
     * @param subject  Should be null or empty for object detection.
     */
    @Override
    public void updateOverlay(CloudletType cloudletType, final JSONArray objectsJsonArray, String subject) {
        Log.i(TAG, "updateOverlay objects("+cloudletType+","+objectsJsonArray.toString());
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) {
                    //Happens during screen rotation
                    Log.e(TAG, "updateOverlay abort - null activity");
                    return;
                }
                if (objectsJsonArray.length() == 0) {
                    Log.d(TAG, "Empty objects array received. Discarding.");
                    return;
                }

                boolean mirrored = mCamera2BasicFragment.getCameraLensFacingDirection() ==
                        CameraCharacteristics.LENS_FACING_FRONT
                        && !mCamera2BasicFragment.isLegacyCamera()
                        && !mCamera2BasicFragment.isVideoMode();

                if(mCamera2BasicFragment.isVideoMode()) {
                    mirrored = false;
                }

                mObjectClassRenderer.setDisplayParms(mImageRect, mServerToDisplayRatioX, mServerToDisplayRatioY, mirrored);
                mObjectClassRenderer.setObjects(objectsJsonArray);
                mObjectClassRenderer.invalidate();
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
        Log.i(TAG, "onCreateView ObjectProcessorFragment");
        return inflater.inflate(R.layout.fragment_object_processor, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated ObjectProcessorFragment savedInstanceState="+savedInstanceState);

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
        mObjectClassRenderer = view.findViewById(R.id.object_class_renderer);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, "ALL");

        // TODO: Create separate preference for Object Detection Host
        String prefKeyOpenPoseHostEdge = getResources().getString(R.string.preference_openpose_host_edge);
        mHostDetectionEdge = prefs.getString(prefKeyOpenPoseHostEdge, DEF_OBJECT_DETECTION_HOST_EDGE);
        Log.i(TAG, "prefKeyOpenPoseHostEdge="+prefKeyOpenPoseHostEdge+" mHostDetectionEdge="+mHostDetectionEdge);

        // See if we have an Extra with the closest cloudlet passed in to override the preference.
        Intent intent = getActivity().getIntent();
        String edgeCloudletHostname = intent.getStringExtra(EXTRA_EDGE_CLOUDLET_HOSTNAME);
        Log.i(TAG, "Extra "+EXTRA_EDGE_CLOUDLET_HOSTNAME+"="+edgeCloudletHostname);
        if(edgeCloudletHostname != null) {
            Log.i(TAG, "Using Extra "+edgeCloudletHostname+" for mHostDetectionEdge.");
            mHostDetectionEdge = edgeCloudletHostname;
        }
        
        mImageSenderEdge = new ImageSender(getActivity(),this, CloudletType.EDGE, mHostDetectionEdge, FACE_DETECTION_HOST_PORT, PERSISTENT_TCP_PORT);

        //TODO: Revisit when we have GPU support on multiple servers.
        //The only GPU-enabled server we have doesn't support ping.
        mImageSenderEdge.setLatencyTestMethod(ImageSender.LatencyTestMethod.socket);
        mImageSenderEdge.setCameraMode(ImageSender.CameraMode.OBJECT_DETECTION);
        mCameraMode = ImageSender.CameraMode.OBJECT_DETECTION;
        mCameraToolbar.setTitle(R.string.title_activity_object_detection);
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

        menu.findItem(R.id.action_benchmark_edge).setVisible(false);
        menu.findItem(R.id.action_benchmark_local).setVisible(false);
        menu.findItem(R.id.action_benchmark_submenu).setVisible(false);

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

        // Always disable this for Object Detection.
        prefLocalProcessing = false;

    }

}
