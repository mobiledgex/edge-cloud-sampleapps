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
import android.graphics.Bitmap;
import android.graphics.Rect;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobiledgex.matchingenginehelper.EventLogViewer;
import com.mobiledgex.matchingenginehelper.MatchingEngineHelper;

import org.json.JSONArray;

import static com.mobiledgex.matchingenginehelper.MatchingEngineHelper.DEF_HOSTNAME_PLACEHOLDER;

public class ObjectProcessorFragment extends EdgeOnlyImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface {
    private static final String TAG = "ObjectProcessorFragment";
    private static final String VIDEO_FILE_NAME = "objects.mp4";
    private ObjectClassRenderer mObjectClassRenderer;

    public static ObjectProcessorFragment newInstance() {
        return new ObjectProcessorFragment();
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

        mEdgeLatencyFull = view.findViewById(R.id.full_latency);
        mEdgeLatencyNet = view.findViewById(R.id.network_latency);
        mEdgeStdFull = view.findViewById(R.id.full_std_dev);
        mEdgeStdNet = view.findViewById(R.id.network_std_dev);
        mStatusText = view.findViewById(R.id.statusTextView);
        mStatusText.setVisibility(View.GONE);
        mObjectClassRenderer = view.findViewById(R.id.object_class_renderer);

        RecyclerView eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        FloatingActionButton logExpansionButton = view.findViewById(R.id.fab);
        mEventLogViewer = new EventLogViewer(getActivity(), logExpansionButton, eventsRecyclerView);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, ALL_PREFS);

        Intent intent = getActivity().getIntent();
        getCommonIntentExtras(intent);

        mCameraMode = ImageSender.CameraMode.OBJECT_DETECTION;
        mCameraToolbar.setTitle(R.string.title_activity_object_detection);

        mVideoFilename = VIDEO_FILE_NAME;

        meHelper = new MatchingEngineHelper.Builder()
                .setActivity(getActivity())
                .setMeHelperInterface(this)
                .setView(mObjectClassRenderer)
                .setTestPort(FACE_DETECTION_HOST_PORT)
                .build();

        setAppNameForGpu();

        if (mGpuHostNameOverride) {
            mEdgeHostList.clear();
            mEdgeHostListIndex = 0;
            mEdgeHostList.add(mHostDetectionEdge);
            showMessage("Overriding GPU host. Host=" + mHostDetectionEdge);
            restartImageSenderEdge();
        } else {
            meHelper.findCloudletInBackground();
        }
    }
}
