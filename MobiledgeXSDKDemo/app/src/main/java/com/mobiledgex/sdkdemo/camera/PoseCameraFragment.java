package com.mobiledgex.sdkdemo.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobiledgex.sdkdemo.CloudletListHolder;
import com.mobiledgex.sdkdemo.R;

import org.json.JSONArray;

import java.text.DecimalFormat;

public class PoseCameraFragment extends Camera2BasicFragment implements ImageServerInterface {
    private static final String TAG = "PoseCameraFragment";
    private PoseRenderer mPoseRenderer;

    private TextView mLatencyFull;
    private TextView mLatencyNet;
    private TextView mStdFull;
    private TextView mStdNet;

    public static PoseCameraFragment newInstance() {
        return new PoseCameraFragment();
    }

    /**
     * Update the body poses.
     *
     * @param cloudletType
     * @param posesJsonArray
     */
    public void updateOverlay(final Camera2BasicFragment.CloudLetType cloudletType, final JSONArray posesJsonArray) {
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

                boolean mirrored = mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT;
                int widthOff = (int) mTextureView.getX();
                int heightOff = (int) mTextureView.getY();

                if(mVideoMode) {
                    widthOff = (int) mVideoView.getX();
                    heightOff = (int) mVideoView.getY();
                    mirrored = false;
                }

                mPoseRenderer.setDisplayParms(mTextureView.getWidth(), mTextureView.getHeight(),
                        widthOff, heightOff, serverToDisplayRatioX, serverToDisplayRatioY, mirrored);
                mPoseRenderer.setPoses(posesJsonArray);
                mPoseRenderer.invalidate();
            }
        });
    }

    @Override
    public void updateOverlay(Camera2BasicFragment.CloudLetType cloudletType, JSONArray overlayData, String subject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTrainingProgress(int cloudTrainingCount, int edgeTrainingCount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void showToast(String message) {
        super.showToast(message);
    }

    public void updateFullProcessStats(final CloudLetType cloudletType, VolleyRequestHandler.RollingAverage rollingAverage) {
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
    public void updateNetworkStats(final CloudLetType cloudletType, VolleyRequestHandler.RollingAverage rollingAverage) {
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
    public void onAttach(Context context) {
        super.onAttach(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        //We can't create the VolleyRequestHandler until we have a context available.
        mVolleyRequestHandler = new VolleyRequestHandler(this, this.getActivity());
        mVolleyRequestHandler.cloudImageSender.busy = true; //TODO: Revisit this
        mVolleyRequestHandler.edgeImageSender.host = "openpose.bonn-mexdemo.mobiledgex.net";

        //TODO: Revisit when we have GPU support on multiple servers.
        //The only GPU-enabled server we have doesn't support ping.
        mVolleyRequestHandler.latencyTestMethod = CloudletListHolder.LatencyTestMethod.socket;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView PoseCameraFragment");
        return inflater.inflate(R.layout.fragment_pose_camera, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i("BDA11", "onViewCreated PoseCameraFragment savedInstanceState="+savedInstanceState);
        if(savedInstanceState != null) {
            mVideoMode = savedInstanceState.getBoolean("VIDEO_MODE", false);
        }
        Log.i("BDA11", "savedInstanceState mVideoMode="+mVideoMode);
        mCameraToolbar = view.findViewById(R.id.cameraToolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mCameraToolbar);

        mTextureView = view.findViewById(R.id.textureView);
        mVideoView = view.findViewById(R.id.videoView);
        mVideoView.setOnClickListener(this);
        mLatencyFull = view.findViewById(R.id.full_latency);
        mLatencyNet = view.findViewById(R.id.network_latency);
        mStdFull = view.findViewById(R.id.full_std_dev);
        mStdNet = view.findViewById(R.id.network_std_dev);
        mPoseRenderer = view.findViewById(R.id.poseSkeleton);

        mVolleyRequestHandler.setCameraMode(VolleyRequestHandler.CameraMode.POSE_DETECTION);
        mCameraMode = VolleyRequestHandler.CameraMode.POSE_DETECTION;
        mCameraToolbar.setTitle(R.string.title_activity_pose_detection);

        if(mVideoMode) {
            mActiveTextureView = mVideoView;
        } else {
            mActiveTextureView = mTextureView;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        onSharedPreferenceChanged(prefs, "ALL");

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

        // Always disable this for Pose Detection.
        prefLocalProcessing = false;

    }

}
