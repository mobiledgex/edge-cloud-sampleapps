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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobiledgex.computervision.Camera2BasicFragment;
import com.mobiledgex.computervision.FaceBoxRenderer;
import com.mobiledgex.computervision.ImageProviderInterface;
import com.mobiledgex.computervision.ImageSender;
import com.mobiledgex.computervision.ImageServerInterface;
import com.mobiledgex.computervision.RollingAverage;

import org.json.JSONArray;

import java.text.DecimalFormat;

public class FaceProcessorFragment extends com.mobiledgex.computervision.ImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface {
    private FaceBoxRenderer mFaceBoxRenderer;

    private static final String TAG = "FaceProcessorFragment";
    private TextView mLatencyFull;
    private TextView mLatencyNet;
    private TextView mStdFull;
    private TextView mStdNet;
    private TextView mStatusText;
    private Toolbar mCameraToolbar;

    public String mHost;
    public int mPort;

    public static FaceProcessorFragment newInstance() {
        return new FaceProcessorFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView FaceProcessorFragment");
        return inflater.inflate(R.layout.fragment_face_processor, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated FaceProcessorFragment savedInstanceState="+savedInstanceState);

        mCameraToolbar = view.findViewById(R.id.cameraToolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mCameraToolbar);

        mLatencyFull = view.findViewById(R.id.full_latency);
        mLatencyNet = view.findViewById(R.id.network_latency);
        mStdFull = view.findViewById(R.id.full_std_dev);
        mStdNet = view.findViewById(R.id.network_std_dev);
        mStatusText = view.findViewById(R.id.statusTextView);

        /////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to access preferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, "ALL");

        /////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to define a Camera2BasicFragment
        mCamera2BasicFragment = new Camera2BasicFragment();
        mCamera2BasicFragment.setImageProviderInterface(this);
        String prefKeyFrontCamera = getResources().getString(R.string.preference_fd_front_camera);
        mCamera2BasicFragment.setCameraLensFacingDirection(prefs.getInt(prefKeyFrontCamera, CameraCharacteristics.LENS_FACING_FRONT));
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(com.mobiledgex.computervision.R.id.child_camera_fragment_container, mCamera2BasicFragment).commit();

        /////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to define an ImageSender
        mImageSenderEdge = new ImageSender(getActivity(), this, CloudletType.CLOUD, mHost, mPort, PERSISTENT_TCP_PORT); // mHost and mPort come from GetConnectionWorkflow in FaceProcessorActivity
        mImageSenderEdge.setCameraMode(ImageSender.CameraMode.FACE_DETECTION);
        mCameraMode = ImageSender.CameraMode.FACE_DETECTION;
        mCameraToolbar.setTitle("Face Detection");

        /////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to define a FaceBoxRenderer
        mFaceBoxRenderer = view.findViewById(R.id.edgeFaceBoxRender);
        mFaceBoxRenderer.setShapeType(FaceBoxRenderer.ShapeType.OVAL);
        mFaceBoxRenderer.setColor(Color.CYAN);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: To get rid of the "Not implemented" message, hide mStatusText or set the value to an empty string.
        mStatusText.setText("");

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.face_detection_menu, menu);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Copy/paste the code to implement methods defined in the ImageProcessorInterface.
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

        mImageSenderEdge.sendImage(bitmap);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Copy/paste the code to implement methods defined in the ImageServerInterface.
    /**
     * Update the face rectangle coordinates.
     *
     * @param cloudletType  The cloudlet type determines which FaceBoxRender to use.
     * @param rectJsonArray  An array of rectangular coordinates for each face detected.
     * @param subject  The Recognized subject name. Null or empty for Face Detection.
     */
    @Override
    public void updateOverlay(final CloudletType cloudletType, final JSONArray rectJsonArray, final String subject) {
        Log.i(TAG, "updateOverlay Rectangles("+cloudletType+","+rectJsonArray.toString()+","+subject+")");
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

                Log.d(TAG, "mirrored=" + mirrored + " mImageRect=" + mImageRect.toShortString() + " mServerToDisplayRatioX=" + mServerToDisplayRatioX +" mServerToDisplayRatioY=" + mServerToDisplayRatioY);

                mFaceBoxRenderer.setDisplayParms(mImageRect, mServerToDisplayRatioX, mServerToDisplayRatioY, mirrored, prefMultiFace);
                mFaceBoxRenderer.setRectangles(rectJsonArray, subject);
                mFaceBoxRenderer.invalidate();
                mFaceBoxRenderer.restartAnimation();
            }
        });
    }

    public void updateFullProcessStats(final CloudletType cloudletType, RollingAverage rollingAverage) {
        final long stdDev = rollingAverage.getStdDev();
        final long latency = rollingAverage.getAverage();
        if(getActivity() == null) {
            Log.w(TAG, "Activity has gone away. Abort UI update");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLatencyFull.setText("Full Process Latency: " + String.valueOf(latency/1000000) + " ms");
                mStdFull.setText("Stddev: " + new DecimalFormat("#.##").format(stdDev/1000000) + " ms");
            }
        });
    }

    @Override
    public void updateNetworkStats(final CloudletType cloudletType, RollingAverage rollingAverage) {
        final long stdDev = rollingAverage.getStdDev();
        final long latency;
        latency = rollingAverage.getAverage();

        if(getActivity() == null) {
            Log.w(TAG, "Activity has gone away. Abort UI update");
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLatencyNet.setText("Network Only Latency: " + String.valueOf(latency/1000000) + " ms");
                mStdNet.setText("Stddev: " + new DecimalFormat("#.##").format(stdDev/1000000) + " ms");            }
        });
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Copy/paste the onOptionsItemSelected code to define menu selection actions.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_camera_swap) {
            mCamera2BasicFragment.switchCamera();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            String prefKeyFrontCamera = getResources().getString(R.string.preference_fd_front_camera);
            prefs.edit().putInt(prefKeyFrontCamera, mCamera2BasicFragment.getCameraLensFacingDirection()).apply();
            return true;
        } else if (id == R.id.action_camera_video) {
            mCameraToolbar.setVisibility(View.GONE);
            mCamera2BasicFragment.startVideo();
            return true;
        } else if (id == R.id.action_camera_debug) {
            mCamera2BasicFragment.showDebugInfo();
            return true;
        }
        return false;
    }

    protected void toggleViews() {
        //We are overriding the this method just so the superclass version doesn't try to toggle
        //the visibility of a widget we don't have in our layout.
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Copy/paste the getStatsText code to return actual stat values.
    /**
     * Return statistics from the latency measurements.
     * @return
     */
    public String getStatsText() {
        return mImageSenderEdge.getStatsText();
    }
}
