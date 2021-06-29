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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
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
import com.mobiledgex.computervision.ObjectClassRenderer;
import com.mobiledgex.matchingengine.MatchingEngine;
import com.mobiledgex.matchingenginehelper.ConnectionTester;
import com.mobiledgex.matchingenginehelper.EventLogViewer;
import com.mobiledgex.matchingenginehelper.MatchingEngineHelper;
import com.mobiledgex.matchingenginehelper.MatchingEngineHelperInterface;
import com.mobiledgex.tritonlib.databinding.FragmentObjectProcessorBinding;

import org.json.JSONArray;

public class Yolov4ProcessorFragment extends GpuImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface, MatchingEngineHelperInterface {
    private static final String TAG = "Yolov4ProcessorFragment";
    private static final String VIDEO_FILE_NAME = "objects.mp4";
    public static final float YOLOV4_SIZE = 608f;
    public static final int TRITON_HTTP_PORT = 8000;
    public static final int TRITON_GRPC_PORT = 8001;
    public static final int TRITON_METRICS_PORT = 8002;
    private ObjectClassRenderer mObjectClassRenderer;

    private FragmentObjectProcessorBinding viewBinding;

    private String mModelName;

    public static Yolov4ProcessorFragment newInstance() {
        return new Yolov4ProcessorFragment();
    }

    public void restartImageSenderEdge() {
        if (!mAttached) {
            Log.w(TAG, "Fragment is detached. Aborting restartImageSenderEdge()");
            return;
        }
        String message;
        if (mImageSenderEdge == null) {
            message = "Starting " + mCameraToolbar.getTitle() + " on EDGE host " + mHostDetectionEdge;
            mEventLogViewer.initialLogsComplete();
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

        // YOLOV4 resizes all input images to 608x608 before processing them, and bounding box
        // coordinates returned are based on that. This means we need to track 2 display ratios
        // and use them both to calculate where to actually draw the bounding boxes.
        // Example:
        // Display is 1080x1440. Resized image sent to Yolov4 is 180x240. Yolov4 resizes to 608x608.
        // mServerToDisplayRatioX = (1080/180)*(180/608) = 1.78
        // mServerToDisplayRatioY = (1440/240)*(240/608) = 2.37
        // For bounding boxes received, multiply x and y by these values for actual screen location.
        float displayToImageRatioX = (float) mImageRect.width() / bitmap.getWidth();
        float displayToImageRatioY = (float) mImageRect.height() / bitmap.getHeight();
        float imageToYolov4RatioX = bitmap.getWidth() / YOLOV4_SIZE;
        float imageToYolov4RatioY = bitmap.getHeight() / YOLOV4_SIZE;

        mServerToDisplayRatioX = displayToImageRatioX * imageToYolov4RatioX;
        mServerToDisplayRatioY = displayToImageRatioY * imageToYolov4RatioY;

        Log.d(TAG, "mImageRect="+mImageRect.toShortString()+" mImageRect.height()="+mImageRect.height()+" bitmap.getWidth()="+bitmap.getWidth()+" bitmap.getHeight()="+bitmap.getHeight()+" mServerToDisplayRatioX=" + mServerToDisplayRatioX +" mServerToDisplayRatioY=" + mServerToDisplayRatioY);

        if (mImageSenderEdge != null) {
            mImageSenderEdge.sendImage(bitmap);
        } else {
            Log.d(TAG, "Waiting for mImageSenderEdge to be initialized.");
        }
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView Yolov4ProcessorFragment");
        viewBinding = FragmentObjectProcessorBinding.inflate(inflater, container, false);
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

        mCameraToolbar = view.findViewById(R.id.cameraToolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mCameraToolbar);

        mEdgeLatencyFull = view.findViewById(R.id.full_latency);
        mEdgeLatencyNet = view.findViewById(R.id.network_latency);
        mEdgeStdFull = view.findViewById(R.id.full_std_dev);
        mEdgeStdNet = view.findViewById(R.id.network_std_dev);
        mStatusText = view.findViewById(R.id.status_text_view);
        mStatusText.setVisibility(View.GONE);
        mObjectClassRenderer = view.findViewById(R.id.object_class_renderer);

        RecyclerView eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        FloatingActionButton logExpansionButton = view.findViewById(R.id.fab);
        mEventLogViewer = new EventLogViewer(getActivity(), logExpansionButton, eventsRecyclerView);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, "ALL");

        mCameraMode = ImageSender.CameraMode.OBJECT_DETECTION;
        mModelName = "ensemble_dali_yolov4";

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
                .setView(mObjectClassRenderer)
                .build();
        MatchingEngine.setMatchingEngineLocationAllowed(true); //TODO: Add preference

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
        String testUrl = "/v2/models/yolov4/versions/1/config";
        String expectedResponse = "\"name\":\"yolov4\"";
        String scheme = tls ? "https" : "http";
        String appInstUrl = scheme+"://"+meHelper.mClosestCloudlet.getFqdn()+":"+testConnectionPort+testUrl;

        return new ConnectionTester(appInstUrl, expectedResponse);
    }
}
