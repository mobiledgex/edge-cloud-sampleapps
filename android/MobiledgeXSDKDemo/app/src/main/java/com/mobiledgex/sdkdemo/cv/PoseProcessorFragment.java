package com.mobiledgex.sdkdemo.cv;

import android.content.Context;
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

import com.mobiledgex.sdkdemo.R;

import org.json.JSONArray;

import java.text.DecimalFormat;

public class PoseProcessorFragment extends ImageProcessorFragment implements ImageServerInterface,
        ImageProviderInterface {
    private static final String TAG = "PoseProcessorFragment";
    private PoseRenderer mPoseRenderer;

    private TextView mLatencyFull;
    private TextView mLatencyNet;
    private TextView mStdFull;
    private TextView mStdNet;

    public static PoseProcessorFragment newInstance() {
        return new PoseProcessorFragment();
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

        mImageSenderEdge.sendImage(bitmap);
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

                boolean mirrored = mCamera2BasicFragment.mCameraLensFacingDirection ==
                        CameraCharacteristics.LENS_FACING_FRONT && !mCamera2BasicFragment.mLegacyCamera;
                int widthOff = mImageRect.left;
                int heightOff = mImageRect.top;

                if(mCamera2BasicFragment.mVideoMode) {
                    mirrored = false;
                }

                mPoseRenderer.setDisplayParms(mImageRect.width(), mImageRect.height(),
                        widthOff, heightOff, mServerToDisplayRatioX, mServerToDisplayRatioY, mirrored);
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
        mPoseRenderer = view.findViewById(R.id.poseSkeleton);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, "ALL");

        //TODO: Make this a preference when we have GPU support on multiple servers.
        String host = "openpose.bonn-mexdemo.mobiledgex.net";
        mImageSenderEdge = new ImageSender(getActivity(),this, CloudletType.EDGE, host, FACE_DETECTION_HOST_PORT);

        //TODO: Revisit when we have GPU support on multiple servers.
        //The only GPU-enabled server we have doesn't support ping.
        mImageSenderEdge.mLatencyTestMethod = ImageSender.LatencyTestMethod.socket;
        mImageSenderEdge.setCameraMode(ImageSender.CameraMode.POSE_DETECTION);
        mCameraMode = ImageSender.CameraMode.POSE_DETECTION;
        mCameraToolbar.setTitle(R.string.title_activity_pose_detection);
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

        // Always disable this for Pose Detection.
        prefLocalProcessing = false;

    }

}
