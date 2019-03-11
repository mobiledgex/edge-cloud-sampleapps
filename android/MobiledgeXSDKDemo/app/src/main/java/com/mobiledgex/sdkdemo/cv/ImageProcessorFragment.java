package com.mobiledgex.sdkdemo.cv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
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
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiledgex.sdkdemo.Account;
import com.mobiledgex.sdkdemo.R;
import com.mobiledgex.sdkdemo.SettingsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessorFragment extends Fragment implements ImageServerInterface, ImageProviderInterface,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SharedPreferences.OnSharedPreferenceChangeListener,
        TrainGuestDialog.TrainGuestDialogListener {

    private static final String TAG = "ImageProcessorFragment";

    protected Camera2BasicFragment mCamera2BasicFragment;
    private static final int MAX_FACES = 8;
    protected Menu mOptionsMenu;
    private TextView mLatencyFullTitle;
    private TextView mLatencyNetTitle;
    private TextView mCloudLatency;
    private TextView mEdgeLatency;
    private TextView mCloudLatency2;
    private TextView mEdgeLatency2;
    private TextView mCloudStd;
    private TextView mEdgeStd;
    private TextView mCloudStd2;
    private TextView mEdgeStd2;
    private TextView mProgressText;
    private ProgressBar mProgressBarTraining;
    protected Toolbar mCameraToolbar;

    protected Rect mImageRect;
    private BoundingBox mCloudBB;
    private BoundingBox mEdgeBB;
    private BoundingBox mLocalBB;
    private List<BoundingBox> mCloudBBList = new ArrayList<>();
    private List<BoundingBox> mEdgeBBList = new ArrayList<>();
    private List<BoundingBox> mLocalBBList = new ArrayList<>();

    protected VolleyRequestHandler mVolleyRequestHandler;

    private boolean prefLegacyCamera;
    private boolean prefMultiFace;
    protected boolean prefShowFullLatency;
    protected boolean prefShowNetLatency;
    protected boolean prefShowStdDev;
    protected boolean prefUseRollingAvg;
    protected boolean prefLocalProcessing = false;
    private boolean prefRemoteProcessing = true;
    protected VolleyRequestHandler.CameraMode mCameraMode;
    protected float mServerToDisplayRatioX;
    protected float mServerToDisplayRatioY;
    private boolean mBenchmarkActive;

    protected enum CloudletType {
        EDGE,
        CLOUD,
        LOCAL_PROCESSING
    }

    VolleyRequestHandler.RollingAverage localLatencyRollingAvg = new VolleyRequestHandler.RollingAverage(CloudletType.LOCAL_PROCESSING, "On-Device", 100);

    public static final String EXTRA_FACE_RECOGNITION = "extra_face_recognition";

    private CascadeClassifier mCascadeClassifier;

    public String getStatsText() {
        return mVolleyRequestHandler.getStatsText();
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    @Override
    public void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Perform any processing of the given bitmap.
     *
     * @param bitmap  The bitmap from the camera or video.
     * @param imageRect  The coordinates of the image on the screen. Needed for scaling/offsetting
     *                   resulting face rectangles or pose skeleton coordinates.
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

        if(prefRemoteProcessing) {
            if(mBenchmarkActive) {
                mVolleyRequestHandler.cloudImageSender.busy = true;
            }
            mVolleyRequestHandler.sendImage(bitmap);
        }

        if(prefLocalProcessing) {
            processImageLocal(bitmap);
        }
    }

    @Override
    public void setMessageText(String messageText) {
        mProgressText.setVisibility(View.VISIBLE);
        mProgressText.setText(messageText);
    }

    /**
     * Via OpenCV library, use on-device processing to detect faces in the bitmap.
     *
     * @param bitmap
     */
    private void processImageLocal(Bitmap bitmap) {
        long startTime = System.nanoTime();

        Mat aInputFrame = null;
        MatOfRect faces = null;
        try {
            aInputFrame = new Mat();
            Utils.bitmapToMat(bitmap, aInputFrame);
            faces = new MatOfRect();
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "OpenCV libraries not yet loaded.");
            return;
        }

        int localOpenCvFaceSize = (int) (bitmap.getHeight() * 0.2);
        // Use the classifier to detect faces
        if (mCascadeClassifier != null) {
            mCascadeClassifier.detectMultiScale(aInputFrame, faces, 1.2, 2, 2,
                    new org.opencv.core.Size(localOpenCvFaceSize, localOpenCvFaceSize), new org.opencv.core.Size());
        }

        // If there are any faces found, build array of rectangles
        //Ex: [[64, 84, 175, 195], [50, 32, 193, 175]]
        JSONArray jsonArray = new JSONArray();
        org.opencv.core.Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            JSONArray jsonRect = new JSONArray();
            Imgproc.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
            jsonRect.put(facesArray[i].x);
            jsonRect.put(facesArray[i].y);
            jsonRect.put((int)facesArray[i].br().x);
            jsonRect.put((int)facesArray[i].br().y);
            jsonArray.put(jsonRect);
        }

        long endTime = System.nanoTime();
        long localLatency = endTime - startTime;
        localLatencyRollingAvg.add(localLatency);
        long localStdDev = localLatencyRollingAvg.getStdDev();
        long localAvg = localLatencyRollingAvg.getAverage();
        Log.i(TAG, "localLatency="+(localLatency/1000000.0)+" localAvg="+(localAvg/1000000.0)+" localStdDev="+(localStdDev/1000000.0));

        updateOverlay(CloudletType.LOCAL_PROCESSING, jsonArray, null);

        // Save image to local storage.
//            if (facesArray.length > 0) {
//                Imgproc.cvtColor(aInputFrame, aInputFrame, CvType.CV_8UC1);
//                Imgcodecs.imwrite(String.valueOf(mFile), aInputFrame);
//            }
    }

    /**
     * Update the face rectangle coordinates and the UI.
     *
     * @param cloudletType
     * @param rectJsonArray
     * @param subject  The identified subject name.
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

                int widthOff = mImageRect.left;
                int heightOff = mImageRect.top;

                Log.d(TAG, "widthOff=" + widthOff + " heightOff=" + heightOff + " mServerToDisplayRatioX=" + mServerToDisplayRatioX +" mServerToDisplayRatioY=" + mServerToDisplayRatioY);

                int i;
                int totalFaces;
                JSONArray jsonRect;
                if(prefMultiFace) {
                    totalFaces = rectJsonArray.length();
                    if(totalFaces > MAX_FACES) {
                        totalFaces = MAX_FACES;
                        Log.w(TAG, "MAX_FACES ("+MAX_FACES+") exceeded. Ignoring additional");
                    }
                } else {
                    totalFaces = 1;
                }
                for(i = 0; i < totalFaces; i++) {
                    try {
                        jsonRect = rectJsonArray.getJSONArray(i);
                        Rect rect = new Rect();
                        rect.left = jsonRect.getInt(0);
                        rect.top = jsonRect.getInt(1);
                        rect.right = jsonRect.getInt(2);
                        rect.bottom = jsonRect.getInt(3);
                        Log.d(TAG, "received rect=" + rect.toShortString());
                        if (rect.top == 0 && rect.left == 0 && rect.right == 0 && rect.bottom == 0) {
                            Log.d(TAG, "Discarding empty rectangle");
                            continue;
                        }

                        //In case we received the exact same coordinates from both Edge and Cloud,
                        //offset only one of the rectangles so they will be distinct.
                        if (cloudletType == CloudletType.EDGE) {
                            rect.left -= 1;
                            rect.right += 1;
                            rect.top -= 1;
                            rect.bottom += 1;
                        }

                        rect.left *= mServerToDisplayRatioX;
                        rect.right *= mServerToDisplayRatioX;
                        rect.top *= mServerToDisplayRatioY;
                        rect.bottom *= mServerToDisplayRatioY;

                        if (mCamera2BasicFragment.mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT
                                && !prefLegacyCamera && !mCamera2BasicFragment.mVideoMode) {
                            Log.d(TAG, "Mirroring!");
                            // The image that was processed is what the camera sees, but the image we want to
                            // overlay the rectangle onto is mirrored. So not only do we have to scale it,
                            // but we have to flip it horizontally.
                            rect.left = mImageRect.width() - rect.left;
                            rect.right = mImageRect.width() - rect.right;
                            int tmp = rect.left;
                            rect.left = rect.right;
                            rect.right = tmp;
                        }

                        Log.d(TAG, "jsonRect="+jsonRect+" scaled rect=" + rect.toShortString());
                        rect.offset(widthOff, heightOff);

                        BoundingBox bb;
                        if (cloudletType == CloudletType.CLOUD) {
                            bb = mCloudBBList.get(i);
                        } else if (cloudletType == CloudletType.EDGE) {
                            bb = mEdgeBBList.get(i);
                        } else if (cloudletType == CloudletType.LOCAL_PROCESSING) {
                            bb = mLocalBBList.get(i);
                        } else {
                            Log.e(TAG, "Unknown cloudletType: "+cloudletType);
                            continue;
                        }
                        bb.rect = rect;
                        bb.invalidate();
                        bb.restartAnimation();
                        bb.setSubject(subject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void updateOverlay(final CloudletType cloudletType, final JSONArray posesJsonArray) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTrainingProgress(int cloudTrainingCount, int edgeTrainingCount) {
        Log.i(TAG, "updateTrainingProgress() edge="+mVolleyRequestHandler.edgeImageSender.mCameraMode+" cloud="+mVolleyRequestHandler.cloudImageSender.mCameraMode);
        mProgressBarTraining.setVisibility(View.VISIBLE);
        mProgressText.setVisibility(View.VISIBLE);
        //Find smaller value.
        int progress = cloudTrainingCount;
        if( edgeTrainingCount <= progress) {
            progress = edgeTrainingCount;
        }

        VolleyRequestHandler.CameraMode edgeMode = mVolleyRequestHandler.edgeImageSender.mCameraMode;
        VolleyRequestHandler.CameraMode cloudMode = mVolleyRequestHandler.cloudImageSender.mCameraMode;
        if(edgeMode == VolleyRequestHandler.CameraMode.FACE_TRAINING
                || cloudMode == VolleyRequestHandler.CameraMode.FACE_TRAINING) {
            mProgressBarTraining.setProgress(progress);
            mProgressText.setText("Collecting images... "+progress+"/"+VolleyRequestHandler.TRAINING_COUNT_TARGET);
        } else if(edgeMode == VolleyRequestHandler.CameraMode.FACE_UPDATING_SERVER
                && cloudMode == VolleyRequestHandler.CameraMode.FACE_UPDATING_SERVER) {
            mProgressBarTraining.setIndeterminate(true);
            mProgressText.setText("Updating server...");
        } else if(edgeMode == VolleyRequestHandler.CameraMode.FACE_RECOGNITION
                && cloudMode == VolleyRequestHandler.CameraMode.FACE_RECOGNITION) {
            mProgressBarTraining.setVisibility(View.GONE);
            mProgressText.setVisibility(View.GONE);
            guestTrainingMenuUncheck();
        }
    }

    @Override
    public void updateFullProcessStats(final CloudletType cloudletType, VolleyRequestHandler.RollingAverage rollingAverage) {
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
    public void updateNetworkStats(final CloudletType cloudletType, VolleyRequestHandler.RollingAverage rollingAverage) {
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

    @Override
    public void onSetGuestName(String guestName) {
        mVolleyRequestHandler.setSubjectName(guestName);
        mVolleyRequestHandler.setCameraMode(VolleyRequestHandler.CameraMode.FACE_TRAINING);
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

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies() {
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getActivity().getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Load the cascade classifier
            mCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "OpenCV: Error loading cascade", e);
        }

        // And we are ready to go
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(TAG, "onCreateOptionsMenu");
        mOptionsMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.camera_menu, menu);

        if(mCameraMode == VolleyRequestHandler.CameraMode.FACE_DETECTION) {
            menu.findItem(R.id.action_camera_training).setVisible(false);
            menu.findItem(R.id.action_camera_training_guest).setVisible(false);
        } else {
            menu.findItem(R.id.action_benchmark_edge).setVisible(false);
            menu.findItem(R.id.action_benchmark_local).setVisible(false);
            menu.findItem(R.id.action_benchmark_submenu).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_camera_swap) {
            mCamera2BasicFragment.switchCamera();
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
            mCameraToolbar.setVisibility(View.GONE);
            mCamera2BasicFragment.startVideo();
            return true;
        }

        if (id == R.id.action_camera_debug) {
            mCamera2BasicFragment.showDebugInfo();
            return true;
        }

        if (id == R.id.action_camera_training) {
            if(!Account.getSingleton().isSignedIn()) {
                new android.support.v7.app.AlertDialog.Builder(getContext())
                        .setTitle(R.string.sign_in_required_title)
                        .setMessage(R.string.sign_in_required_message)
                        .setPositiveButton("OK", null)
                        .show();
                return true;
            }
            mVolleyRequestHandler.setCameraMode(VolleyRequestHandler.CameraMode.FACE_TRAINING);
            return true;
        }

        if (id == R.id.action_camera_training_guest) {
            //Even in guest mode, the user must be signed in because they will be listed as the
            //owner of the guest images on the face training server.
            if(!Account.getSingleton().isSignedIn()) {
                new android.support.v7.app.AlertDialog.Builder(getContext())
                        .setTitle(R.string.sign_in_required_title)
                        .setMessage(R.string.sign_in_required_message)
                        .setPositiveButton("OK", null)
                        .show();
                return true;
            }
            if(item.isChecked()) {
                // If item already checked then uncheck it
                item.setChecked(false);
                mVolleyRequestHandler.setSubjectName(Account.getSingleton().getGoogleSignInAccount().getDisplayName());
            } else {
                item.setChecked(true);
                TrainGuestDialog trainGuestDialog = new TrainGuestDialog();
                trainGuestDialog.setTargetFragment(this, 1);
                trainGuestDialog.show(getActivity().getSupportFragmentManager(), "training_guest_dialog");
            }
            return true;
        }

        if (id == R.id.action_benchmark_edge) {
            mBenchmarkActive = true;
            prefLocalProcessing = false;
            prefRemoteProcessing = true;
            mCloudLatency.setVisibility(View.GONE);
            mCloudLatency2.setVisibility(View.GONE);
            mCloudStd.setVisibility(View.GONE);
            mCloudStd2.setVisibility(View.GONE);
            mCamera2BasicFragment.runBenchmark(getContext(), "Edge");
            return true;
        }
        if (id == R.id.action_benchmark_local) {
            mBenchmarkActive = true;
            prefLocalProcessing = true;
            prefRemoteProcessing = false;
            loadOpencvLibrary();
            mCamera2BasicFragment.runBenchmark(getContext(), "Local");
            return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        //Only load the OpenCV library if we are doing local processing.
        if(prefLocalProcessing) {
            loadOpencvLibrary();
        } else {
            Log.d(TAG, "Local processing not enabled. Skipping loading OpenCV library.");
        }
    }

    private void loadOpencvLibrary() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, getContext(), mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "onSharedPreferenceChanged("+key+")");
        if(getContext() == null) {
            //Can happen during rapid screen rotations.
            return;
        }
        String prefKeyFrontCamera = getResources().getString(R.string.preference_fd_front_camera);
        String prefKeyLegacyCamera = getResources().getString(R.string.preference_fd_legacy_camera);
        String prefKeyMultiFace = getResources().getString(R.string.preference_fd_multi_face);
        String prefKeyLocalProc = getResources().getString(R.string.preference_fd_local_proc);
        String prefKeyShowFullLatency = getResources().getString(R.string.preference_fd_show_full_latency);
        String prefKeyShowNetLatency = getResources().getString(R.string.preference_fd_show_net_latency);
        String prefKeyShowStdDev = getResources().getString(R.string.preference_fd_show_stddev);
        String prefKeyUseRollingAvg = getResources().getString(R.string.preference_fd_use_rolling_avg);
        String prefKeyHostCloud = getResources().getString(R.string.preference_fd_host_cloud);
        String prefKeyHostEdge = getResources().getString(R.string.preference_fd_host_edge);

        if(key.equals(prefKeyHostCloud) || key.equals(prefKeyHostEdge)) {
            Log.d(TAG, "Nothing to do for "+key);
            return;
        }

        if (key.equals(prefKeyFrontCamera) || key.equals("ALL")) {
            mCamera2BasicFragment.mCameraLensFacingDirection =
                    sharedPreferences.getInt(prefKeyFrontCamera, CameraCharacteristics.LENS_FACING_FRONT);
        }
        if (key.equals(prefKeyMultiFace) || key.equals("ALL")) {
            prefMultiFace = sharedPreferences.getBoolean(prefKeyMultiFace, true);
        }
        if (key.equals(prefKeyLegacyCamera) || key.equals("ALL")) {
            prefLegacyCamera = sharedPreferences.getBoolean(prefKeyLegacyCamera, true);
            mCamera2BasicFragment.mLegacyCamera = prefLegacyCamera;
        }
        if (key.equals(prefKeyLocalProc) || key.equals("ALL")) {
            prefLocalProcessing = sharedPreferences.getBoolean(prefKeyLocalProc, false);
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
        return inflater.inflate(R.layout.fragment_image_processor, container, false);
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

        //Find 1 CLoudBB and create MAX_FACES-1 more, using the same LayoutParams.
        mCloudBB = view.findViewById(R.id.cloudBB);
        mCloudBB.setColor(Color.RED);
        mCloudBB.setCloudletType(CloudletType.CLOUD);
        mCloudBBList.add(mCloudBB);
        for(int i = 1; i <= MAX_FACES; i++) {
            BoundingBox bb = new BoundingBox(getContext());
            bb.setColor(Color.RED);
            bb.setCloudletType(CloudletType.CLOUD);
            bb.setLayoutParams(mCloudBB.getLayoutParams());
            mCloudBBList.add(bb);
            frameLayout.addView(bb);
        }

        //Find 1 EdgeBB and create MAX_FACES-1 more, using the same LayoutParams.
        mEdgeBB = view.findViewById(R.id.edgeBB);
        mEdgeBB.setColor(Color.GREEN);
        mEdgeBB.setCloudletType(CloudletType.EDGE);
        mEdgeBBList.add(mEdgeBB);
        for(int i = 1; i <= MAX_FACES; i++) {
            BoundingBox bb = new BoundingBox(getContext());
            bb.setColor(Color.GREEN);
            bb.setCloudletType(CloudletType.EDGE);
            bb.setLayoutParams(mEdgeBB.getLayoutParams());
            mEdgeBBList.add(bb);
            frameLayout.addView(bb);
        }
        Log.i(TAG, "mEdgeBBList="+mEdgeBBList.size()+" mCloudBBList="+mCloudBBList.size()+" MAX_FACES="+MAX_FACES);

        //Find 1 LocalBB and create MAX_FACES-1 more, using the same LayoutParams.
        mLocalBB = view.findViewById(R.id.localBB);
        mLocalBB.setColor(Color.BLUE);
//        mLocalBB.shapeType = BoundingBox.ShapeType.OVAL;
        mLocalBBList.add(mLocalBB);
        for(int i = 1; i <= MAX_FACES; i++) {
            BoundingBox bb = new BoundingBox(getContext());
            bb.setColor(Color.BLUE);
//            bb.shapeType = BoundingBox.ShapeType.OVAL;
            bb.setLayoutParams(mLocalBB.getLayoutParams());
            mLocalBBList.add(bb);
            frameLayout.addView(bb);
        }

        mProgressBarTraining = view.findViewById(R.id.progressBarTraining);
        mProgressBarTraining.setProgress(0);
        mProgressBarTraining.setMax(VolleyRequestHandler.TRAINING_COUNT_TARGET);
        mProgressBarTraining.setVisibility(View.GONE);
        mProgressText = view.findViewById(R.id.progressTextView);
        mProgressText.setVisibility(View.GONE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        onSharedPreferenceChanged(prefs, "ALL");

        Intent intent = getActivity().getIntent();
        boolean faceRecognition = intent.getBooleanExtra(EXTRA_FACE_RECOGNITION, false);
        if (faceRecognition) {
            mVolleyRequestHandler.setCameraMode(VolleyRequestHandler.CameraMode.FACE_RECOGNITION);
            prefLocalProcessing = false;
            mCameraMode = VolleyRequestHandler.CameraMode.FACE_RECOGNITION;
            mCameraToolbar.setTitle(R.string.title_activity_face_recognition);
        } else {
            mVolleyRequestHandler.setCameraMode(VolleyRequestHandler.CameraMode.FACE_DETECTION);
            mCameraMode = VolleyRequestHandler.CameraMode.FACE_DETECTION;
            mCameraToolbar.setTitle(R.string.title_activity_face_detection);
        }
    }

    protected void toggleViews() {
        if(prefShowStdDev) {
            mEdgeStd.setVisibility(View.VISIBLE);
            mCloudStd.setVisibility(View.VISIBLE);
            mEdgeStd2.setVisibility(View.VISIBLE);
            mCloudStd2.setVisibility(View.VISIBLE);
        } else {
            mEdgeStd.setVisibility(View.GONE);
            mCloudStd.setVisibility(View.GONE);
            mEdgeStd2.setVisibility(View.GONE);
            mCloudStd2.setVisibility(View.GONE);
        }
        if(prefShowFullLatency) {
            mLatencyFullTitle.setVisibility(View.VISIBLE);
            mCloudLatency.setVisibility(View.VISIBLE);
            mEdgeLatency.setVisibility(View.VISIBLE);
        } else {
            mLatencyFullTitle.setVisibility(View.INVISIBLE);
            mCloudLatency.setVisibility(View.INVISIBLE);
            mEdgeLatency.setVisibility(View.INVISIBLE);
            mEdgeStd.setVisibility(View.GONE);
            mCloudStd.setVisibility(View.GONE);
        }
        if(prefShowNetLatency) {
            mLatencyNetTitle.setVisibility(View.VISIBLE);
            mCloudLatency2.setVisibility(View.VISIBLE);
            mEdgeLatency2.setVisibility(View.VISIBLE);
        } else {
            mLatencyNetTitle.setVisibility(View.INVISIBLE);
            mCloudLatency2.setVisibility(View.INVISIBLE);
            mEdgeLatency2.setVisibility(View.INVISIBLE);
            mEdgeStd2.setVisibility(View.GONE);
            mCloudStd2.setVisibility(View.GONE);
        }

        mVolleyRequestHandler.setDoNetLatency(prefShowNetLatency);
    }

    @Override
    public void onAttach(Context context) {
        Log.i(TAG, "onAttach("+context+")");
        super.onAttach(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        //We can't create the VolleyRequestHandler until we have a context available.
        mVolleyRequestHandler = new VolleyRequestHandler(this, this.getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCamera2BasicFragment = null;
    }

}
