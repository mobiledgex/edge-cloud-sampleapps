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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;

import java.text.DecimalFormat;

import static com.mobiledgex.matchingenginehelper.MatchingEngineHelper.DEF_HOSTNAME_PLACEHOLDER;

/**
 * This class is used for image processing on a GPU-equipped Edge cloudlet
 * app instance (displayed as "Edge"). It is abstract an must be extended
 * a more specific image processor fragment.
 */
public abstract class GpuImageProcessorFragment extends ImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface {
    private static final String TAG = "GpuImageProcessorFragment";

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
    public abstract void updateOverlay(CloudletType cloudletType, final JSONArray posesJsonArray, String subject);

    @Override
    public void updateTrainingProgress(int trainingCount, ImageSender.CameraMode mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState);

    @Override
    public abstract void onViewCreated(final View view, Bundle savedInstanceState);

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
            mEdgeLatencyFull.setVisibility(View.VISIBLE);
        } else {
            mEdgeLatencyFull.setVisibility(View.INVISIBLE);
            mEdgeStdFull.setVisibility(View.GONE);
            mEdgeStdNet.setVisibility(View.GONE);
        }
        if(prefShowNetLatency) {
            mEdgeLatencyNet.setVisibility(View.VISIBLE);
            mEdgeStdNet.setVisibility(View.VISIBLE);
        } else {
            mEdgeLatencyNet.setVisibility(View.INVISIBLE);
            mEdgeStdNet.setVisibility(View.GONE);
        }
        if(prefShowStdDev) {
            if(prefShowNetLatency) {
                mEdgeStdFull.setVisibility(View.VISIBLE);
            }
            if(prefShowNetLatency) {
                mEdgeStdNet.setVisibility(View.VISIBLE);
            }
        } else {
            mEdgeStdFull.setVisibility(View.GONE);
            mEdgeStdNet.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged("+key+")");
        if(getContext() == null) {
            //Can happen during rapid screen rotations.
            return;
        }
        super.onSharedPreferenceChanged(sharedPreferences, key);

        String prefKeyHostEdgeOverride = getResources().getString(R.string.pref_override_edge_cloudlet_hostname);
        String prefKeyHostEdge = getResources().getString(R.string.pref_cv_host_edge);
        String prefKeyHostEdgeTls = getResources().getString(R.string.pref_cv_host_edge_tls);

        // Edge Hostname handling
        if (key.equals(prefKeyHostEdgeOverride) || key.equals(ALL_PREFS)) {
            mEdgeHostNameOverride = sharedPreferences.getBoolean(prefKeyHostEdgeOverride, false);
            Log.i(TAG, "key="+key+" mEdgeHostNameOverride="+ mEdgeHostNameOverride);
            if (mEdgeHostNameOverride) {
                mHostDetectionEdge = sharedPreferences.getString(prefKeyHostEdge, DEF_HOSTNAME_PLACEHOLDER);
                Log.i(TAG, "key="+key+" mHostDetectionEdge="+ mHostDetectionEdge);
            }
            mEdgeHostNameTls = sharedPreferences.getBoolean(prefKeyHostEdgeTls, false);
            Log.i(TAG, "prefKeyHostEdgeTls="+prefKeyHostEdgeTls+" mEdgeHostNameTls="+ mEdgeHostNameTls);
        }
        if (key.equals(prefKeyHostEdge) || key.equals(ALL_PREFS)) {
            mHostDetectionEdge = sharedPreferences.getString(prefKeyHostEdge, DEF_HOSTNAME_PLACEHOLDER);
            Log.i(TAG, "prefKeyHostEdge="+prefKeyHostEdge+" mHostDetectionEdge="+ mHostDetectionEdge);
        }
        if (key.equals(prefKeyHostEdgeTls) || key.equals(ALL_PREFS)) {
            mEdgeHostNameTls = sharedPreferences.getBoolean(prefKeyHostEdgeTls, false);
            Log.i(TAG, "prefKeyHostEdgeTls="+prefKeyHostEdgeTls+" mEdgeHostNameTls="+ mEdgeHostNameTls);
        }
        if (key.equals(prefKeyHostEdge) || key.equals(prefKeyHostEdgeOverride)) {
            if (mEdgeHostNameOverride) {
                mEdgeHostList.clear();
                mEdgeHostListIndex = 0;
                mEdgeHostList.add(mHostDetectionEdge);
                showMessage("mHostDetectionEdge set to " + mHostDetectionEdge);
                restartImageSenderEdge();
            }
        }
    }
}
