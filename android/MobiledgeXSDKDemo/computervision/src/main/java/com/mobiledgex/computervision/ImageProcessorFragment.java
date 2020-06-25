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

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mobiledgex.matchingengine.MatchingEngine;

import org.json.JSONArray;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;

import static com.mobiledgex.computervision.EventItem.EventType.ERROR;
import static com.mobiledgex.computervision.EventItem.EventType.INFO;

public class ImageProcessorFragment extends Fragment implements ImageServerInterface, ImageProviderInterface,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SharedPreferences.OnSharedPreferenceChangeListener,
        TrainGuestDialog.TrainGuestDialogListener {

    private static final String TAG = "ImageProcessorFragment";
    public static final String EXTRA_FACE_STROKE_WIDTH = "EXTRA_FACE_STROKE_WIDTH";
    private static final String VIDEO_FILE_NAME = "Jason.mp4";

    private MatchingEngine mMatchingEngine;
    //The following defaults may be overridden by passing EXTRA values to the Intent.
    public String mAppName = "MobiledgeX SDK Demo";
    public String mAppVersion = "2.0";
    public String mOrgName = "MobiledgeX";
    public String mCarrierName = "GDDT";
    public String mDmeHostname = "wifi.dme.mobiledgex.net";
    public int mDmePort = 50051;
    private double mLatitude;
    private double mLongitude;
    private AppClient.FindCloudletReply mClosestCloudlet;

    protected Camera2BasicFragment mCamera2BasicFragment;
    protected Menu mOptionsMenu;
    private TextView mLatencyFullTitle;
    private TextView mLatencyNetTitle;
    protected TextView mCloudLatency;
    protected TextView mEdgeLatency;
    protected TextView mCloudLatency2;
    protected TextView mEdgeLatency2;
    protected TextView mCloudStd;
    protected TextView mEdgeStd;
    protected TextView mCloudStd2;
    protected TextView mEdgeStd2;
    protected TextView mStatusText;
    protected RecyclerView mEventsRecyclerView;
    private TextView mProgressText;
    private ProgressBar mProgressBarTraining;
    protected Toolbar mCameraToolbar;
    public List<EventItem> mEventItemList = new ArrayList<>();

    protected Rect mImageRect;
    private FaceBoxRenderer mCloudFaceBoxRenderer;
    private FaceBoxRenderer mEdgeFaceBoxRenderer;
    private FaceBoxRenderer mLocalFaceBoxRenderer;

    protected boolean prefLegacyCamera;
    protected boolean prefMultiFace;
    protected boolean prefShowFullLatency;
    protected boolean prefShowNetLatency;
    protected boolean prefShowStdDev;
    protected boolean prefUseRollingAvg;
    protected boolean prefAutoFailover;
    protected boolean prefShowCloudOutput;
    protected int prefCameraLensFacingDirection;
    protected ImageSender.CameraMode mCameraMode;
    protected float mServerToDisplayRatioX;
    protected float mServerToDisplayRatioY;
    private String defaultLatencyMethod = "socket";
    private String defaultConnectionMode = "REST";

    public static final int FACE_DETECTION_HOST_PORT = 8008;
    private static final int FACE_TRAINING_HOST_PORT = 8009;
    protected static final int PERSISTENT_TCP_PORT = 8011;
    public static final String DEF_FACE_HOST_EDGE = "facedetection.defaultedge.mobiledgex.net";
    public static final String DEF_FACE_HOST_CLOUD = "facedetection.defaultcloud.mobiledgex.net";
    public static final String DEF_FACE_HOST_TRAINING = "opencv.facetraining.mobiledgex.net";
    protected ImageSender mImageSenderEdge;
    private ImageSender mImageSenderCloud;
    private ImageSender mImageSenderTraining;

    private String mHostDetectionCloud;
    protected String mHostDetectionEdge;
    private String mHostTraining;
    private List<String> mEdgeHostList = new ArrayList<>();
    private int mEdgeHostListIndex;

    public static final String EXTRA_FACE_RECOGNITION = "EXTRA_FACE_RECOGNITION";
    public static final String EXTRA_EDGE_CLOUDLET_HOSTNAME = "EXTRA_EDGE_CLOUDLET_HOSTNAME";
    public static final String EXTRA_FIND_CLOUDLET_MODE = "EXTRA_FIND_CLOUDLET_MODE";
    public static final String EXTRA_APP_NAME = "EXTRA_APP_NAME";
    public static final String EXTRA_APP_VERSION = "EXTRA_APP_VERSION";
    public static final String EXTRA_ORG_NAME = "EXTRA_ORG_NAME";
    public static final String EXTRA_DME_HOSTNAME = "EXTRA_DME_HOSTNAME";
    public static final String EXTRA_CARRIER_NAME = "EXTRA_CARRIER_NAME";
    public static final String EXTRA_LATITUDE = "EXTRA_LATITUDE";
    public static final String EXTRA_LONGITUDE = "EXTRA_LONGITUDE";
    protected String mVideoFilename;
    private MyEventRecyclerViewAdapter mEventRecyclerViewAdapter;
    private FloatingActionButton mLogExpansionButton;
    private MatchingEngine.FindCloudletMode mFindCloudletMode;
    private boolean registerClientComplete;
    private boolean isLogExpanded = false;
    private int mLogViewHeight;

    /**
     * Return statistics information to be displayed in dialog after activity -- a combination
     * of the Cloud and Edge stats.
     * @return  The statistics text.
     */
    public String getStatsText() {
        return mImageSenderEdge.getStatsText() + "\n\n" +
                mImageSenderCloud.getStatsText();
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show.
     */
    @Override
    public void showMessage(final String text) {
        addEventItem(INFO, text);
    }

    @Override
    public void showError(final String text) {
        addEventItem(ERROR, text);
    }

    public void addEventItem(EventItem.EventType type, String text) {
        if (!isLogExpanded) {
            logViewAnimate(0, mLogViewHeight);
            isLogExpanded = true;
        }

        mEventItemList.add(new EventItem(type, text));
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mEventRecyclerViewAdapter.itemAdded();
                }
            });
        }
    }

    @Override
    public void reportConnectionError(String text, ImageSender imageSender) {
        Log.i(TAG, "reportConnectionError from "+imageSender.getHost()+": "+text);
        showError(text);
        if (imageSender == mImageSenderEdge) {
            mImageSenderEdge.setInactive(true);
            RollingAverage ra = new RollingAverage(CloudletType.EDGE, "Error", 1);
            updateFullProcessStats(CloudletType.EDGE, ra);
            updateNetworkStats(CloudletType.EDGE, ra);
            if (prefAutoFailover) {
                Log.i(TAG, "Restarting mImageSenderEdge due to reportConnectionError: "+text);
                mEdgeHostListIndex++;
                if (mEdgeHostList.size() > mEdgeHostListIndex) {
                    mHostDetectionEdge = mEdgeHostList.get(mEdgeHostListIndex);
                    restartImageSenderEdge();
                } else {
                    findCloudletInBackground();
                }
            } else {
                String message = "Please perform 'Find Closest Cloudlet' or 'Find App Instances'.";
                Log.e(TAG, message);
                showError(message);
            }
        }
    }

    public void restartImageSenderEdge() {
        String message = "Restarting mImageSenderEdge on host: " + mHostDetectionEdge;
        Log.i(TAG, message);
        showMessage(message);
        mImageSenderEdge.closeConnection();
        mImageSenderEdge = new ImageSender.Builder()
                .setActivity(getActivity())
                .setImageServerInterface(this)
                .setCloudLetType(CloudletType.EDGE)
                .setHost(mHostDetectionEdge)
                .setPort(FACE_DETECTION_HOST_PORT)
                .setPersistentTcpPort(PERSISTENT_TCP_PORT)
                .build();
        mImageSenderEdge.setCameraMode(mCameraMode);
    }

    /**
     * Perform any processing of the given bitmap.
     *
     * @param bitmap  The bitmap from the camera or video.
     * @param imageRect  The coordinates of the image on the screen. Needed for scaling/offsetting
     *                   resulting face rectangle coordinates.
     */
    @Override
    public void onBitmapAvailable(Bitmap bitmap, Rect imageRect) {
        if(bitmap == null) {
            return;
        }
        Log.d(TAG, "onBitmapAvailable mCameraMode="+mCameraMode);

        mImageRect = imageRect;
        mServerToDisplayRatioX = (float) mImageRect.width() / bitmap.getWidth();
        mServerToDisplayRatioY = (float) mImageRect.height() / bitmap.getHeight();

        Log.d(TAG, "mImageRect="+mImageRect.toShortString()+" mImageRect.height()="+mImageRect.height()+" bitmap.getWidth()="+bitmap.getWidth()+" bitmap.getHeight()="+bitmap.getHeight()+" mServerToDisplayRatioX=" + mServerToDisplayRatioX +" mServerToDisplayRatioY=" + mServerToDisplayRatioY);

        // Determine which ImageSenders should handle this image.
        if(mCameraMode == ImageSender.CameraMode.FACE_TRAINING
            || mCameraMode == ImageSender.CameraMode.FACE_UPDATING_SERVER) {
            mImageSenderTraining.sendImage(bitmap);
        } else {
            mImageSenderEdge.sendImage(bitmap);                 
            mImageSenderCloud.sendImage(bitmap);
        }
    }

    /**
     * Show a status message from the ImageProvider.
     * @param status  The status to show.
     */
    @Override
    public void setStatus(String status) {
//        mEventItemList.add(new EventItem(INFO, status));
        if (status.isEmpty()) {
            mStatusText.setVisibility(View.GONE);
        } else {
            mStatusText.setVisibility(View.VISIBLE);
        }
        mStatusText.setText(status);
    }

    /**
     * Update the face rectangle coordinates and the UI.
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
                if (rectJsonArray.length() == 0) {
                    Log.d(TAG, "Empty rectangle received. Discarding.");
                    return;
                }

                boolean mirrored = mCamera2BasicFragment.getCameraLensFacingDirection() ==
                        CameraCharacteristics.LENS_FACING_FRONT
                        && !mCamera2BasicFragment.isLegacyCamera()
                        && !mCamera2BasicFragment.isVideoMode();

                Log.d(TAG, "mirrored=" + mirrored + " mImageRect=" + mImageRect.toShortString() + " mServerToDisplayRatioX=" + mServerToDisplayRatioX +" mServerToDisplayRatioY=" + mServerToDisplayRatioY);

                FaceBoxRenderer faceBoxRenderer;
                if (cloudletType == CloudletType.CLOUD) {
                    faceBoxRenderer = mCloudFaceBoxRenderer;
                } else if (cloudletType == CloudletType.EDGE) {
                    faceBoxRenderer = mEdgeFaceBoxRenderer;
                } else if (cloudletType == CloudletType.LOCAL_PROCESSING) {
                    faceBoxRenderer = mLocalFaceBoxRenderer;
                } else if (cloudletType == CloudletType.PUBLIC) {
                    faceBoxRenderer = mLocalFaceBoxRenderer; //Borrow the local processing renderer.
                    faceBoxRenderer.setColor(Color.GRAY);//TODO: Create a separate training-in-progress renderer.
                } else {
                    Log.e(TAG, "Unknown cloudletType: "+cloudletType);
                    return;
                }
                faceBoxRenderer.setDisplayParms(mImageRect, mServerToDisplayRatioX, mServerToDisplayRatioY, mirrored, prefMultiFace);
                faceBoxRenderer.setRectangles(rectJsonArray, subject);
                faceBoxRenderer.invalidate();
                faceBoxRenderer.restartAnimation();
            }
        });
    }

    @Override
    public void updateTrainingProgress(int trainingCount, ImageSender.CameraMode mode) {
        Log.i(TAG, "updateTrainingProgress() mTrainingCount="+trainingCount+" mode="+mode);
        mProgressBarTraining.setVisibility(View.VISIBLE);
        mProgressText.setVisibility(View.VISIBLE);
        mCameraMode = mode;
        int progress = trainingCount;

        if(mode == ImageSender.CameraMode.FACE_TRAINING) {
            mProgressBarTraining.setProgress(progress);
            mProgressText.setText("Collecting images... "+progress+"/"+ImageSender.TRAINING_COUNT_TARGET);
            if(trainingCount >= ImageSender.TRAINING_COUNT_TARGET) {
                mImageSenderTraining.trainerTrain();
                mProgressBarTraining.setIndeterminate(true);
                mProgressText.setText("Updating server...");
            }

        } else if(mode == ImageSender.CameraMode.FACE_UPDATING_SERVER) {
            mProgressBarTraining.setIndeterminate(true);
            mProgressText.setText("Updating server...");
        } else if(mode == ImageSender.CameraMode.FACE_RECOGNITION) {
            // Back to normal
        } else if(mode == ImageSender.CameraMode.FACE_UPDATE_SERVER_COMPLETE) {
            mProgressBarTraining.setVisibility(View.GONE);
            mProgressText.setVisibility(View.GONE);
            guestTrainingMenuUncheck();
            mCameraMode = ImageSender.CameraMode.FACE_RECOGNITION;
        }
    }

    @Override
    public void updateFullProcessStats(final CloudletType cloudletType, RollingAverage rollingAverage) {
        Log.i(TAG, "updateFullProcessStats "+cloudletType+" "+rollingAverage.getCurrent());
        final long stdDev = rollingAverage.getStdDev();
        final long latency;
        if (rollingAverage.getCurrent() == 0) {
            latency = (long)9999*1000*1000; //to indicate connection error.
        } else {
            if (prefUseRollingAvg) {
                latency = rollingAverage.getAverage();
            } else {
                latency = rollingAverage.getCurrent();
            }
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
                        mEdgeLatency.setText("Edge: " + String.valueOf(latency / 1000000) + " ms");
                        mEdgeStd.setText("Stddev: " + new DecimalFormat("#.##").format(stdDev / 1000000) + " ms");
                        break;
                    case CLOUD:
                        mCloudLatency.setText("Cloud: " + String.valueOf(latency / 1000000) + " ms");
                        mCloudStd.setText("Stddev: " + new DecimalFormat("#.##").format(stdDev / 1000000) + " ms");
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void updateNetworkStats(final CloudletType cloudletType, RollingAverage rollingAverage) {
        Log.i(TAG, "updateNetworkStats "+cloudletType+" "+rollingAverage.getCurrent());
        final long stdDev = rollingAverage.getStdDev();
        final long latency;
        if (rollingAverage.getCurrent() == 0) {
            latency = (long)9999*1000*1000; //to indicate connection error.
        } else {
            if (prefUseRollingAvg) {
                latency = rollingAverage.getAverage();
            } else {
                latency = rollingAverage.getCurrent();
            }
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
                        mEdgeLatency2.setText("Edge: " + String.valueOf(latency / 1000000) + " ms");
                        mEdgeStd2.setText("Stddev: " + new DecimalFormat("#.##").format(stdDev / 1000000) + " ms");
                        break;
                    case CLOUD:
                        mCloudLatency2.setText("Cloud: " + String.valueOf(latency / 1000000) + " ms");
                        mCloudStd2.setText("Stddev: " + new DecimalFormat("#.##").format(stdDev / 1000000) + " ms");
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * Called by the TrainGuestDialog whenever a guest name is entered. This starts training mode or
     * data removal depending on which request code the dialog was using.
     *
     * @param guestName  The name of the guest.
     * @param requestCode  Determines if the request is to start training, or to remove data.
     */
    @Override
    public void onSetGuestName(String guestName, int requestCode) {
        Log.i(TAG, "onSetGuestName("+guestName+", "+requestCode+")");
        switch (requestCode) {
            case TrainGuestDialog.RC_START_TRAINING:
                mCameraMode = ImageSender.CameraMode.FACE_TRAINING;
                mImageSenderTraining.setCameraMode(mCameraMode);
                mImageSenderTraining.setGuestName(guestName);
                updateTrainingProgress(0, mCameraMode);
                break;
            case TrainGuestDialog.RC_REMOVE_DATA:
                mCameraMode = ImageSender.CameraMode.FACE_UPDATING_SERVER;
                mImageSenderTraining.setCameraMode(mCameraMode);
                mImageSenderTraining.setGuestName(guestName);
                mImageSenderTraining.trainerRemove();
                updateTrainingProgress(0, mCameraMode);
                break;
        }

    }

    @Override
    public void onCancelTrainGuestDialog() {
        guestTrainingMenuUncheck();
    }

    public void guestTrainingMenuUncheck() {
        if(mOptionsMenu != null) {
            mOptionsMenu.findItem(R.id.action_camera_training_guest).setChecked(false);
        }
    }

    public static ImageProcessorFragment newInstance() {
        return new ImageProcessorFragment();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(TAG, "onCreateOptionsMenu mCameraMode="+mCameraMode);
        mOptionsMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.camera_menu, menu);

        if(mCameraMode == ImageSender.CameraMode.FACE_DETECTION) {
            //Hide all training stuff
            menu.findItem(R.id.action_camera_training).setVisible(false);
            menu.findItem(R.id.action_camera_remove_training_data).setVisible(false);
            menu.findItem(R.id.action_camera_training_guest).setVisible(false);
            menu.findItem(R.id.action_camera_remove_training_guest_data).setVisible(false);
        }

        // Declutter the menu, but keep the code in place in case we need it later.
        menu.findItem(R.id.action_camera_debug).setVisible(false);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_camera_swap) {
            mCamera2BasicFragment.switchCamera();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            String prefKeyFrontCamera = getResources().getString(R.string.preference_fd_front_camera);
            prefs.edit().putInt(prefKeyFrontCamera, mCamera2BasicFragment.getCameraLensFacingDirection()).apply();
            return true;
        }

        if (id == R.id.action_camera_settings) {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.FaceDetectionSettingsFragment.class.getName() );
            intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_camera_video) {
//            mCameraToolbar.setVisibility(View.GONE);
            mCamera2BasicFragment.startVideo(mVideoFilename);
            return true;
        }

        if (id == R.id.action_camera_debug) {
            mCamera2BasicFragment.showDebugInfo();
            return true;
        }

        if (id == R.id.action_find_cloudlet) {
            findCloudletInBackground();
            return true;
        }

        if (id == R.id.action_get_app_inst_list) {
            getAppInstListInBackground();
            return true;
        }

        if (id == R.id.action_manual_failover) {
            new Thread(new Runnable() {
                @Override public void run() {
                    reportConnectionError("Manual Failover", mImageSenderEdge);
                }
            }).start();
            return true;
        }

        if (id == R.id.action_camera_training) {
            if(!verifySignedIn()) {
                return true;
            }
            mImageSenderTraining.setCameraMode(ImageSender.CameraMode.FACE_TRAINING);
            mCameraMode = ImageSender.CameraMode.FACE_TRAINING;
            updateTrainingProgress(0, mCameraMode);
            return true;
        }

        if (id == R.id.action_camera_training_guest) {
            //Even in guest mode, the user must be signed in because they will be listed as the
            //owner of the guest images on the face training server.
            if(!verifySignedIn()) {
                return true;
            }
            if(item.isChecked()) {
                // If item already checked then uncheck it
                item.setChecked(false);
                mImageSenderTraining.setGuestName("");
            } else {
                item.setChecked(true);
                TrainGuestDialog trainGuestDialog = new TrainGuestDialog();
                trainGuestDialog.setRequestCode(TrainGuestDialog.RC_START_TRAINING);
                trainGuestDialog.setTargetFragment(this, 1);
                trainGuestDialog.show(getActivity().getSupportFragmentManager(), "training_guest_dialog");
            }
            return true;
        }

        if (id == R.id.action_camera_remove_training_data) {
            if(!verifySignedIn()) {
                return true;
            }
            //Show a dialog to verify the user really wants to delete their data.
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle(R.string.verify_delete_title)
                    .setMessage(R.string.verify_delete_message)
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mCameraMode = ImageSender.CameraMode.FACE_UPDATING_SERVER;
                            mImageSenderTraining.setCameraMode(mCameraMode);
                            mImageSenderTraining.setGuestName("");
                            mImageSenderTraining.trainerRemove();
                            updateTrainingProgress(0, mCameraMode);
                        }
                    })
                    .show();
            return true;
        }

        if (id == R.id.action_camera_remove_training_guest_data) {
            if(!verifySignedIn()) {
                return true;
            }
            TrainGuestDialog trainGuestDialog = new TrainGuestDialog();
            trainGuestDialog.setRequestCode(TrainGuestDialog.RC_REMOVE_DATA);
            trainGuestDialog.setTargetFragment(this, 1);
            trainGuestDialog.show(getActivity().getSupportFragmentManager(), "training_guest_dialog");
            return true;
        }

        if (id == R.id.action_benchmark_edge) {
            mCameraToolbar.setVisibility(View.GONE);
            if (mImageSenderCloud != null) {
                mImageSenderCloud.setInactiveBenchmark(true);
                mCloudLatency.setVisibility(View.GONE);
                mCloudLatency2.setVisibility(View.GONE);
                mCloudStd.setVisibility(View.GONE);
                mCloudStd2.setVisibility(View.GONE);
            }
            mCamera2BasicFragment.startVideo(mVideoFilename);
            mCamera2BasicFragment.runBenchmark(getContext(), "Edge");
            return true;
        }

        if (id == R.id.action_benchmark_cloud) {
            mCameraToolbar.setVisibility(View.GONE);
            if (mImageSenderEdge != null) {
                mImageSenderEdge.setInactiveBenchmark(true);
                mEdgeLatency.setVisibility(View.GONE);
                mEdgeLatency2.setVisibility(View.GONE);
                mEdgeStd.setVisibility(View.GONE);
                mEdgeStd2.setVisibility(View.GONE);
            }
            mCamera2BasicFragment.startVideo(mVideoFilename);
            mCamera2BasicFragment.runBenchmark(getContext(), "Cloud");
            return true;
        }

        return false;
    }

    public boolean verifySignedIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        if(account == null) {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle(R.string.sign_in_required_title)
                    .setMessage(R.string.sign_in_required_message)
                    .setPositiveButton("OK", null)
                    .show();
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged("+key+")");
        if(getContext() == null) {
            //Can happen during rapid screen rotations.
            return;
        }
        String prefKeyLatencyMethod = getResources().getString(R.string.fd_latency_method);
        String prefKeyConnectionMode = getResources().getString(R.string.preference_fd_connection_mode);
        String prefKeyFrontCamera = getResources().getString(R.string.preference_fd_front_camera);
        String prefKeyLegacyCamera = getResources().getString(R.string.preference_fd_legacy_camera);
        String prefKeyMultiFace = getResources().getString(R.string.preference_fd_multi_face);
        String prefKeyShowFullLatency = getResources().getString(R.string.preference_fd_show_full_latency);
        String prefKeyShowNetLatency = getResources().getString(R.string.preference_fd_show_net_latency);
        String prefKeyShowStdDev = getResources().getString(R.string.preference_fd_show_stddev);
        String prefKeyUseRollingAvg = getResources().getString(R.string.preference_fd_use_rolling_avg);
        String prefKeyAutoFailover = getResources().getString(R.string.preference_fd_auto_failover);
        String prefKeyShowCloudOutput = getResources().getString(R.string.preference_fd_show_cloud_output);
        String prefKeyHostCloud = getResources().getString(R.string.preference_fd_host_cloud);
        String prefKeyHostEdge = getResources().getString(R.string.preference_fd_host_edge);
        String prefKeyHostTraining = getResources().getString(R.string.preference_fd_host_training);

        if (key.equals(prefKeyHostCloud) || key.equals("ALL")) {
            mHostDetectionCloud = sharedPreferences.getString(prefKeyHostCloud, DEF_FACE_HOST_CLOUD);
            Log.i(TAG, "prefKeyHostCloud="+prefKeyHostCloud+" mHostDetectionCloud="+mHostDetectionCloud);
        }
        if (key.equals(prefKeyHostEdge) || key.equals("ALL")) {
            mHostDetectionEdge = sharedPreferences.getString(prefKeyHostEdge, DEF_FACE_HOST_EDGE);
            Log.i(TAG, "prefKeyHostEdge="+prefKeyHostEdge+" mHostDetectionEdge1="+ mHostDetectionEdge);
        }
        if (key.equals(prefKeyHostTraining) || key.equals("ALL")) {
            mHostTraining = sharedPreferences.getString(prefKeyHostTraining, DEF_FACE_HOST_TRAINING);
            Log.i(TAG, "prefKeyHostTraining="+prefKeyHostTraining+" mHostTraining="+mHostTraining);
        }

        if (key.equals(prefKeyFrontCamera) || key.equals("ALL")) {
            if(mCamera2BasicFragment != null) {
                prefCameraLensFacingDirection = sharedPreferences.getInt(prefKeyFrontCamera, CameraCharacteristics.LENS_FACING_FRONT);
                mCamera2BasicFragment.setCameraLensFacingDirection(prefCameraLensFacingDirection);
            }
        }
        if (key.equals(prefKeyLatencyMethod) || key.equals("ALL")) {
            String latencyTestMethodString = sharedPreferences.getString(prefKeyLatencyMethod, defaultLatencyMethod);
            Log.i(TAG, "latencyTestMethod=" + latencyTestMethodString+" mImageSenderCloud="+mImageSenderCloud);
            if(mImageSenderCloud != null) {
                mImageSenderCloud.setLatencyTestMethod(ImageSender.LatencyTestMethod.valueOf(latencyTestMethodString));
            }
            if(mImageSenderEdge != null) {
                mImageSenderEdge.setLatencyTestMethod(ImageSender.LatencyTestMethod.valueOf(latencyTestMethodString));
            }
        }
        if (key.equals(prefKeyConnectionMode) || key.equals("ALL")) {
            String connectionModeString = sharedPreferences.getString(prefKeyConnectionMode, defaultConnectionMode);
            Log.i(TAG, "connectionMode=" + connectionModeString+" mImageSenderEdge="+mImageSenderEdge+" mImageSenderCloud="+mImageSenderCloud);
            ImageSender.setPreferencesConnectionMode(ImageSender.ConnectionMode.valueOf(connectionModeString), mImageSenderEdge, mImageSenderCloud);
        }
        if (key.equals(prefKeyMultiFace) || key.equals("ALL")) {
            prefMultiFace = sharedPreferences.getBoolean(prefKeyMultiFace, true);
        }
        if (key.equals(prefKeyLegacyCamera) || key.equals("ALL")) {
            prefLegacyCamera = sharedPreferences.getBoolean(prefKeyLegacyCamera, true);
            if(mCamera2BasicFragment != null) {
                mCamera2BasicFragment.setLegacyCamera(prefLegacyCamera);
            }
        }
        if (key.equals(prefKeyShowFullLatency) || key.equals("ALL")) {
            prefShowFullLatency = sharedPreferences.getBoolean(prefKeyShowFullLatency, true);
        }
        if (key.equals(prefKeyShowNetLatency) || key.equals("ALL")) {
            prefShowNetLatency = sharedPreferences.getBoolean(prefKeyShowNetLatency, true);
        }
        if (key.equals(prefKeyShowStdDev) || key.equals("ALL")) {
            prefShowStdDev = sharedPreferences.getBoolean(prefKeyShowStdDev, false);
        }
        if (key.equals(prefKeyUseRollingAvg) || key.equals("ALL")) {
            prefUseRollingAvg = sharedPreferences.getBoolean(prefKeyUseRollingAvg, false);
        }
        if (key.equals(prefKeyAutoFailover) || key.equals("ALL")) {
            prefAutoFailover = sharedPreferences.getBoolean(prefKeyAutoFailover, true);
        }
        if (key.equals(prefKeyShowCloudOutput) || key.equals("ALL")) {
            prefShowCloudOutput = sharedPreferences.getBoolean(prefKeyShowCloudOutput, true);
        }

        if(mImageSenderCloud != null) {
            mImageSenderCloud.setDoNetLatency(prefShowNetLatency);
        }
        if(mImageSenderEdge != null) {
            mImageSenderEdge.setDoNetLatency(prefShowNetLatency);
        }
        if(mImageSenderTraining != null) {
            mImageSenderTraining.setDoNetLatency(prefShowNetLatency);
        }

        toggleViews();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        final View root = inflater.inflate(R.layout.fragment_image_processor, container, false);


        return root;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated savedInstanceState="+savedInstanceState);

        mMatchingEngine = new MatchingEngine(getContext());

        mCameraToolbar = view.findViewById(R.id.cameraToolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(mCameraToolbar);

        FrameLayout frameLayout = view.findViewById(R.id.container);
        mLatencyFullTitle = view.findViewById(R.id.network_latency);
        mLatencyNetTitle = view.findViewById(R.id.latency_title2);
        mCloudLatency = view.findViewById(R.id.cloud_latency);
        mCloudLatency2 = view.findViewById(R.id.cloud_latency2);
        mCloudLatency.setTextColor(Color.RED);
        mCloudLatency2.setTextColor(Color.RED);
        mEdgeLatency = view.findViewById(R.id.edge_latency);
        mEdgeLatency2 = view.findViewById(R.id.edge_latency2);
        mEdgeLatency.setTextColor(Color.GREEN);
        mEdgeLatency2.setTextColor(Color.GREEN);
        mCloudStd = view.findViewById(R.id.cloud_std_dev);
        mCloudStd.setTextColor(Color.RED);
        mEdgeStd = view.findViewById(R.id.edge_std_dev);
        mEdgeStd.setTextColor(Color.GREEN);
        mCloudStd2 = view.findViewById(R.id.cloud_std_dev2);
        mCloudStd2.setTextColor(Color.RED);
        mEdgeStd2 = view.findViewById(R.id.edge_std_dev2);
        mEdgeStd2.setTextColor(Color.GREEN);

        mCloudFaceBoxRenderer = view.findViewById(R.id.cloudFaceBoxRender);
        mCloudFaceBoxRenderer.setColor(Color.RED);
        mCloudFaceBoxRenderer.setCloudletType(CloudletType.CLOUD);

        mEdgeFaceBoxRenderer = view.findViewById(R.id.edgeFaceBoxRender);
        mEdgeFaceBoxRenderer.setColor(Color.GREEN);
        mEdgeFaceBoxRenderer.setCloudletType(CloudletType.EDGE);

        mLocalFaceBoxRenderer = view.findViewById(R.id.localFaceBoxRender);
        mLocalFaceBoxRenderer.setColor(Color.BLUE);
        mLocalFaceBoxRenderer.setCloudletType(CloudletType.LOCAL_PROCESSING);
        mLocalFaceBoxRenderer.setShapeType(FaceBoxRenderer.ShapeType.OVAL);

        mProgressBarTraining = view.findViewById(R.id.progressBarTraining);
        mProgressBarTraining.setProgress(0);
        mProgressBarTraining.setMax(ImageSender.TRAINING_COUNT_TARGET);
        mProgressBarTraining.setVisibility(View.GONE);
        mProgressText = view.findViewById(R.id.progressTextView);
        mProgressText.setVisibility(View.GONE);

        mStatusText = view.findViewById(R.id.statusTextView);
        mStatusText.setVisibility(View.GONE);
        mStatusText.setText("");

        setupLogViewer(view);

        mCamera2BasicFragment = new Camera2BasicFragment();
        mCamera2BasicFragment.setImageProviderInterface(this);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.child_camera_fragment_container, mCamera2BasicFragment).commit();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        // Get preferences for everything we've instantiated so far.
        onSharedPreferenceChanged(prefs, "ALL");

        Intent intent = getActivity().getIntent();
        getCommonIntentExtras(intent);

        // Check for other optional parameters
        int strokeWidth = intent.getIntExtra(EXTRA_FACE_STROKE_WIDTH, FaceBoxRenderer.DEFAULT_STROKE_WIDTH);
        mCloudFaceBoxRenderer.setStrokeWidth(strokeWidth);
        mEdgeFaceBoxRenderer.setStrokeWidth(strokeWidth);
        mLocalFaceBoxRenderer.setStrokeWidth(strokeWidth);

        mImageSenderCloud = new ImageSender.Builder()
                .setActivity(getActivity())
                .setImageServerInterface(this)
                .setCloudLetType(CloudletType.CLOUD)
                .setHost(mHostDetectionCloud)
                .setPort(FACE_DETECTION_HOST_PORT)
                .setPersistentTcpPort(PERSISTENT_TCP_PORT)
                .build();
        mImageSenderEdge = new ImageSender.Builder()
                .setActivity(getActivity())
                .setImageServerInterface(this)
                .setCloudLetType(CloudletType.EDGE)
                .setHost(mHostDetectionEdge)
                .setPort(FACE_DETECTION_HOST_PORT)
                .setPersistentTcpPort(PERSISTENT_TCP_PORT)
                .build();
        mImageSenderTraining = new ImageSender.Builder()
                .setActivity(getActivity())
                .setImageServerInterface(this)
                .setCloudLetType(CloudletType.PUBLIC)
                .setHost(mHostTraining)
                .setPort(FACE_TRAINING_HOST_PORT)
                .setPersistentTcpPort(PERSISTENT_TCP_PORT)
                .build();

        boolean faceRecognition = intent.getBooleanExtra(EXTRA_FACE_RECOGNITION, false);
        if (faceRecognition) {
            mCameraMode = ImageSender.CameraMode.FACE_RECOGNITION;
            mCameraToolbar.setTitle(R.string.title_activity_face_recognition);
        } else {
            mCameraMode = ImageSender.CameraMode.FACE_DETECTION;
            mCameraToolbar.setTitle(R.string.title_activity_face_detection);
        }
        mImageSenderCloud.setCameraMode(mCameraMode);
        mImageSenderEdge.setCameraMode(mCameraMode);
        showMessage("Starting " + mCameraToolbar.getTitle() + " on CLOUD host " + mHostDetectionCloud);
        showMessage("Starting " + mCameraToolbar.getTitle() + " on EDGE host " + mHostDetectionEdge);

        mVideoFilename = VIDEO_FILE_NAME;

        //One more call to get preferences for ImageSenders
        onSharedPreferenceChanged(prefs, "ALL");
    }

    protected void setupLogViewer(View view) {
        mLogViewHeight = (int) (getResources().getDisplayMetrics().heightPixels*.4);
        mEventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        final LinearLayoutManager layout = new LinearLayoutManager(view.getContext());
        mEventsRecyclerView.setLayoutManager(layout);
        mEventRecyclerViewAdapter = new MyEventRecyclerViewAdapter(mEventItemList);
        mEventRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                layout.smoothScrollToPosition(mEventsRecyclerView, null, mEventRecyclerViewAdapter.getItemCount());
            }

        });
        mEventsRecyclerView.setAdapter(mEventRecyclerViewAdapter);
        mLogExpansionButton = view.findViewById(R.id.fab);
        mLogExpansionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "mEventsRecyclerView.getLayoutParams().height="+mEventsRecyclerView.getLayoutParams().height);
                Log.i(TAG, "mLogExpansionButton.getHeight()="+mLogExpansionButton.getHeight());
                Log.i(TAG, "isLogExpanded="+isLogExpanded);
                layout.smoothScrollToPosition(mEventsRecyclerView, null, mEventRecyclerViewAdapter.getItemCount());
                if(!isLogExpanded){
                    logViewAnimate(0, mLogViewHeight);
                    isLogExpanded = true;
                }
                else{
                    logViewAnimate(mLogViewHeight, 0);
                    isLogExpanded = false;
                }
            }
        });
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isLogExpanded) {
                    logViewAnimate(mLogViewHeight, 0);
                    isLogExpanded = false;
                }
            }
        }, 2000);
    }

    protected void logViewAnimate(final int start, final int end) {
        Log.i(TAG, "logViewAnimate start="+start+" end="+end);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ValueAnimator va = ValueAnimator.ofInt(start, end);
                va.setDuration(500);
                va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Integer value = (Integer) animation.getAnimatedValue();
                        Log.i(TAG, "value="+value);
                        mEventsRecyclerView.getLayoutParams().height = value.intValue();
                        mEventsRecyclerView.requestLayout();
                    }
                });
                va.start();
            }
        });
    }

    protected void getCommonIntentExtras(Intent intent) {
        // See if we have an Extra with the closest cloudlet passed in to override the preference.
        String edgeCloudletHostname = intent.getStringExtra(EXTRA_EDGE_CLOUDLET_HOSTNAME);
        if(edgeCloudletHostname != null) {
            Log.i(TAG, "Using Extra "+edgeCloudletHostname+" for mHostDetectionEdge.");
            mHostDetectionEdge = edgeCloudletHostname;
        }
        mAppName = intent.getStringExtra(EXTRA_APP_NAME);
        mAppVersion = intent.getStringExtra(EXTRA_APP_VERSION);
        mOrgName = intent.getStringExtra(EXTRA_ORG_NAME);
        mCarrierName = intent.getStringExtra(EXTRA_CARRIER_NAME);
        mDmeHostname = intent.getStringExtra(EXTRA_DME_HOSTNAME);
        mLatitude = intent.getDoubleExtra(EXTRA_LATITUDE, 1);
        mLongitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 1);

        String findCloudletMode = intent.getStringExtra(EXTRA_FIND_CLOUDLET_MODE);
        Log.i(TAG, "EXTRA_FIND_CLOUDLET_MODE: "+findCloudletMode);
        if (findCloudletMode != null && !findCloudletMode.isEmpty()) {
            mFindCloudletMode = MatchingEngine.FindCloudletMode.valueOf(findCloudletMode);
        } else {
            mFindCloudletMode = MatchingEngine.FindCloudletMode.PROXIMITY;
        }
        Log.i(TAG, "mFindCloudletMode: "+mFindCloudletMode);
    }

    protected void toggleViews() {
        Log.i(TAG, "toggleViews prefShowCloudOutput=" + prefShowCloudOutput);
        if (prefShowCloudOutput) {
            if (mImageSenderCloud != null) {
                mImageSenderCloud.setInactive(false);
            }
            mCloudLatency.setVisibility(View.VISIBLE);
            mCloudLatency2.setVisibility(View.VISIBLE);
            mCloudStd.setVisibility(View.VISIBLE);
            mCloudStd2.setVisibility(View.VISIBLE);
        } else {
            if (mImageSenderCloud != null) {
                mImageSenderCloud.setInactive(true);
            }
            mCloudLatency.setVisibility(View.GONE);
            mCloudLatency2.setVisibility(View.GONE);
            mCloudStd.setVisibility(View.GONE);
            mCloudStd2.setVisibility(View.GONE);
        }
        if(prefShowStdDev) {
            mEdgeStd.setVisibility(View.VISIBLE);
            mEdgeStd2.setVisibility(View.VISIBLE);
            if (prefShowCloudOutput) {
                mCloudStd.setVisibility(View.VISIBLE);
                mCloudStd2.setVisibility(View.VISIBLE);
            }
        } else {
            mEdgeStd.setVisibility(View.GONE);
            mEdgeStd2.setVisibility(View.GONE);
            mCloudStd.setVisibility(View.GONE);
            mCloudStd2.setVisibility(View.GONE);
        }
        if(prefShowFullLatency) {
            mLatencyFullTitle.setVisibility(View.VISIBLE);
            mEdgeLatency.setVisibility(View.VISIBLE);
            if (prefShowCloudOutput) {
                mCloudLatency.setVisibility(View.VISIBLE);
            }
        } else {
            mLatencyFullTitle.setVisibility(View.GONE);
            mEdgeLatency.setVisibility(View.GONE);
            mEdgeStd.setVisibility(View.GONE);
            mCloudLatency.setVisibility(View.GONE);
            mCloudStd.setVisibility(View.GONE);
        }
        if(prefShowNetLatency) {
            mLatencyNetTitle.setVisibility(View.VISIBLE);
            mEdgeLatency2.setVisibility(View.VISIBLE);
            if (prefShowCloudOutput) {
                mCloudLatency2.setVisibility(View.VISIBLE);
            }
        } else {
            mLatencyNetTitle.setVisibility(View.GONE);
            mEdgeLatency2.setVisibility(View.GONE);
            mEdgeStd2.setVisibility(View.GONE);
            mCloudLatency2.setVisibility(View.GONE);
            mCloudStd2.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Context context) {
        Log.i(TAG, "onAttach("+context+")");
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "onDetach()");
        super.onDetach();
        mCamera2BasicFragment = null;
        if (mImageSenderEdge != null) {
            mImageSenderEdge.closeConnection();
        }
        if (mImageSenderCloud != null) {
            mImageSenderCloud.closeConnection();
        }
    }

    public class DistanceComparator implements Comparator<AppClient.CloudletLocation>
    {
        public int compare(AppClient.CloudletLocation left, AppClient.CloudletLocation right) {
            return (int) (left.getDistance() - right.getDistance());
        }
    }

    public boolean registerClient() throws ExecutionException, InterruptedException,
            io.grpc.StatusRuntimeException, PackageManager.NameNotFoundException {

        AppClient.RegisterClientRequest registerClientRequest;
        Future<AppClient.RegisterClientReply> registerReplyFuture;
        AppClient.RegisterClientReply reply = null;
        registerClientRequest = mMatchingEngine.createDefaultRegisterClientRequest(getActivity(), mOrgName)
                .setAppName(mAppName).setAppVers(mAppVersion).setCarrierName(mCarrierName).build();
        registerReplyFuture = mMatchingEngine.registerClientFuture(registerClientRequest, mDmeHostname, mDmePort,10000);
        reply = registerReplyFuture.get();

        Log.i(TAG, "registerClientRequest="+registerClientRequest);
        Log.i(TAG, "registerStatus.getStatus()="+reply.getStatus());

        if (reply.getStatus() != AppClient.ReplyStatus.RS_SUCCESS) {
            String registerStatusText = "registerClient Failed. Error: " + reply.getStatus();
            Log.e(TAG, registerStatusText);
            showError(registerStatusText);
            return false;
        }
        Log.i(TAG, "SessionCookie:" + reply.getSessionCookie());
        showMessage("registerClient successful.");
        return true;
    }

    protected void findCloudletInBackground() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    if (registerClientComplete || registerClient()) {
                        registerClientComplete = true;
                        findCloudlet();
                    }
                } catch (ExecutionException | InterruptedException
                        | PackageManager.NameNotFoundException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    protected void getAppInstListInBackground() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    if (registerClientComplete || registerClient()) {
                        registerClientComplete = true;
                        getAppInstList();
                    }
                } catch (ExecutionException | InterruptedException | PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public boolean findCloudlet() throws ExecutionException, InterruptedException, IllegalArgumentException {
        Location location = new Location("MEX");
        location.setLatitude(mLatitude);
        location.setLongitude(mLongitude);
        Log.i(TAG, "findCloudlet location="+location);

        AppClient.FindCloudletRequest findCloudletRequest;
        findCloudletRequest = mMatchingEngine.createDefaultFindCloudletRequest(getContext(), location).build();
        Future<AppClient.FindCloudletReply> reply = mMatchingEngine.findCloudletFuture(findCloudletRequest, mDmeHostname, mDmePort,10000, mFindCloudletMode);

        mClosestCloudlet = reply.get();

        Log.i(TAG, "findCloudlet mClosestCloudlet="+mClosestCloudlet);
        if(mClosestCloudlet.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
            String findCloudletStatusText = "findCloudlet Failed. Error: " + mClosestCloudlet.getStatus();
            Log.e(TAG, findCloudletStatusText);
            return false;
        }
        Log.i(TAG, "mClosestCloudlet.getFqdn()=" + mClosestCloudlet.getFqdn());

        // Extract cloudlet name from FQDN
        String[] parts = mClosestCloudlet.getFqdn().split("\\.");
        String cloudletNameTvStr = parts[0];

        //Find fqdnPrefix from Port structure.
        String fqdnPrefix = "";
        List<Appcommon.AppPort> ports = mClosestCloudlet.getPortsList();
        for (Appcommon.AppPort aPort : ports) {
            fqdnPrefix = aPort.getFqdnPrefix();
        }
        // Build full hostname.
        mHostDetectionEdge = fqdnPrefix+mClosestCloudlet.getFqdn();
        mEdgeHostList.clear();
        mEdgeHostListIndex = 0;
        mEdgeHostList.add(mHostDetectionEdge);

        showMessage("findCloudlet successful. Host=" + mHostDetectionEdge);

        restartImageSenderEdge();
        return true;
    }

    private boolean getAppInstList() throws InterruptedException, ExecutionException {
        Location location = new Location("MEX");
        location.setLatitude(mLatitude);
        location.setLongitude(mLongitude);
        Log.i(TAG, "getAppInstList location="+location);
        AppClient.AppInstListRequest appInstListRequest
                = mMatchingEngine.createDefaultAppInstListRequest(getContext(), location).setCarrierName(mCarrierName).setLimit(6).build();
        // TODO: Make setLimit value a preference.
        if(appInstListRequest != null) {
            AppClient.AppInstListReply cloudletList = mMatchingEngine.getAppInstList(appInstListRequest,
                    mDmeHostname, mDmePort, 10000);
            Log.i(TAG, "cloudletList.getStatus()="+cloudletList.getStatus());
            if (cloudletList.getStatus() != AppClient.AppInstListReply.AIStatus.AI_SUCCESS) {
                String message = "getAppInstList failed. Status="+cloudletList.getStatus();
                Log.e(TAG, message);
                showError(message);
                return false;
            }

            mEdgeHostList.clear();
            mEdgeHostListIndex = 0;
            List<AppClient.CloudletLocation> sortableCloudletList = new ArrayList<>(cloudletList.getCloudletsList());
            Collections.sort(sortableCloudletList, new Comparator<AppClient.CloudletLocation>() {
                @Override public int compare(AppClient.CloudletLocation c1, AppClient.CloudletLocation c2) {
                    return (int) (c1.getDistance() - c2.getDistance());
                }
            });
            for (AppClient.CloudletLocation cloudlet : sortableCloudletList) {
                AppClient.Appinstance appInst = cloudlet.getAppinstances(0);
                Log.i(TAG, cloudlet.getCloudletName()+" Distance="+cloudlet.getDistance());
                //Find fqdnPrefix from Port structure.
                String fqdnPrefix = "";
                List<Appcommon.AppPort> ports = appInst.getPortsList();
                for (Appcommon.AppPort aPort : ports) {
                    fqdnPrefix = aPort.getFqdnPrefix();
                }
                String hostname = fqdnPrefix + appInst.getFqdn();
                mEdgeHostList.add(hostname);
                showMessage("Found " + hostname);
            }
            Log.i(TAG, "getAppInstList cloudletList.getCloudletsCount()=" + cloudletList.getCloudletsCount());
            Log.i(TAG, "getAppInstList mEdgeHostList=" + mEdgeHostList);

            // TODO: Remove
//            mEdgeHostList.clear();
//            mEdgeHostList.add("192.168.1.66");
//            mEdgeHostList.add("acrotopia.com");

            mHostDetectionEdge = mEdgeHostList.get(mEdgeHostListIndex);
            Log.i(TAG, "getAppInstList mHostDetectionEdge=" + mHostDetectionEdge);
            restartImageSenderEdge();
            return true;

        } else {
            String message = "Cannot create AppInstListRequest object";
            Log.e(TAG, message);
            showError(message);
            return false;
        }
    }


    public ImageSender getImageSenderEdge() {
        return mImageSenderEdge;
    }

    public ImageSender getImageSenderCloud() {
        return mImageSenderCloud;
    }

    public ImageSender getImageSenderTraining() {
        return mImageSenderTraining;
    }
}
