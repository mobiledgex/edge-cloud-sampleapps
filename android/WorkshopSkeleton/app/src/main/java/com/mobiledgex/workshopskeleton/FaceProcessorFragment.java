package com.mobiledgex.workshopskeleton;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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

import org.json.JSONArray;

import java.text.DecimalFormat;

public class FaceProcessorFragment extends Fragment {

    private static final String TAG = "FaceProcessorFragment";
    private TextView mLatencyFull;
    private TextView mLatencyNet;
    private TextView mStdFull;
    private TextView mStdNet;
    private TextView mStatusText;
    private Toolbar mCameraToolbar;
    private boolean prefShowFullLatency;
    private boolean prefShowNetLatency;
    private boolean prefShowStdDev;

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

        /////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to define a Camera2BasicFragment

        /////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to define an ImageSender

        /////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Copy/paste the code to define a FaceBoxRenderer

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: To get rid of the "Not implemented" message, hide mStatusText or set the value to an empty string.

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.face_detection_menu, menu);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Copy/paste the code to implement methods defined in the ImageProcessorInterface.

    /////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Copy/paste the code to implement methods defined in the ImageServerInterface.

    /////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Copy/paste the onOptionsItemSelected code to define menu selection actions.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

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

    /////////////////////////////////////////////////////////////////////////////////////////////
    // TODO: Copy/paste the getStatsText code to return actual stat values.
    /**
     * Return statistics from the latency measurements.
     * @return
     */
    public String getStatsText() {
        return "TODO";
    }
}
