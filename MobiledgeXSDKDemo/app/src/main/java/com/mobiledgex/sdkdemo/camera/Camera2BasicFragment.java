/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiledgex.sdkdemo.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Camera2BasicFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback,
        SharedPreferences.OnSharedPreferenceChangeListener,
        TrainGuestDialog.TrainGuestDialogListener, ImageServerInterface {

    private static final String TAG = "Camera2BasicFragment";
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

    private BoundingBox mCloudBB;
    private BoundingBox mEdgeBB;
    private BoundingBox mLocalBB;
    private List<BoundingBox> mCloudBBList = new ArrayList<>();
    private List<BoundingBox> mEdgeBBList = new ArrayList<>();
    private List<BoundingBox> mLocalBBList = new ArrayList<>();

    private PoseRenderer mPoseRenderer;

    protected VolleyRequestHandler mVolleyRequestHandler;
    protected int mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_FRONT;

    private boolean prefMultiFace;
    protected boolean prefShowFullLatency;
    protected boolean prefShowNetLatency;
    protected boolean prefShowStdDev;
    private boolean prefUseRollingAvg;
    protected boolean prefLocalProcessing = true;
    private boolean prefRemoteProcessing = true;
    private List<String> mAssetImageFileList = new ArrayList<>();
    private String mAssetImagePath;
    private int mAssetImageFileIndex;
    private boolean mRunningBenchmark;
    private CountDownTimer mBenchmarkTimer;
    private long mBenchmarkStartTime;
    private long mBenchmarkDurationMillis = 15*60*1000; //15 minutes
    private long mBenchmarkTickMillis = 200;
    private int mBenchmarkFrameCount;
    private boolean mBenchmarkMaxCpuFlag = false;
    protected VolleyRequestHandler.CameraMode mCameraMode;

    public String getStatsText() {
        return mVolleyRequestHandler.getStatsText();
    }

    enum CloudLetType {
        EDGE,
        CLOUD,
        LOCAL_PROCESSING
    }

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_INTERNET_PERMISSION = 2;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    //TODO: Perform profiling with these factor values swapped.
    private int factor = 1; //TODO: Possibly calculate this based on camera resolution.
    private int mImageReaderFactor = 12;

    protected float serverToDisplayRatio;

    private CascadeClassifier cascadeClassifier;
    private int absoluteFaceSize;
    VolleyRequestHandler.RollingAverage localLatencyRollingAvg = new VolleyRequestHandler.RollingAverage(CloudLetType.LOCAL_PROCESSING, "On-Device", 100);

    public static final String EXTRA_BENCH_EDGE = "extra_bench_edge";
    public static final String EXTRA_BENCH_LOCAL = "extra_bench_local";
    public static final String EXTRA_FACE_RECOGNITION = "extra_face_recognition";
    public static final String EXTRA_POSE_DETECTION = "extra_pose_detection";

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        //We can't create the VolleyRequestHandler until we have a context available.
        mVolleyRequestHandler = new VolleyRequestHandler(this, this.getActivity());
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

    /**
     * This method runs the face detection process on a fixed set of images, cycling through them
     * one after the other. Because battery stats are no longer available to apps on non-rooted
     * devices, the way to measure battery use is to reset the stats through ADB, run the benchmark,
     * then view battery usage either through the on-phone App Manager, or with the Battery Historian
     * tool.
     *
     * Sample ADB commands:
     * adb shell dumpsys batterystats --reset
     * adb bugreport bugreport_s8_edge_01.zip
     *
     * @param context
     */
    public void runBenchmark(Context context) {
        //TODO Disable camera swap menu item
        closeCamera();
        stopBackgroundThread();

        mRunningBenchmark = true;
        mAssetImageFileIndex = 0;
        mAssetImagePath = "images/singleface";
        try {
            AssetManager assetManager = context.getAssets();
            mAssetImageFileList.addAll(Arrays.asList(assetManager.list(mAssetImagePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBenchmarkFrameCount = 0;
        mBenchmarkStartTime = System.currentTimeMillis();
        mBenchmarkTimer = new CountDownTimer(mBenchmarkDurationMillis, mBenchmarkTickMillis) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = (mBenchmarkDurationMillis - (System.currentTimeMillis() - mBenchmarkStartTime))/1000;
                mLatencyFullTitle.setText(convertSecondsToHMmSs(secondsRemaining)+" remaining");
                if(!mBenchmarkMaxCpuFlag) {
                    loadImageAsset();
                }
            }
            @Override
            public void onFinish() {
                mBenchmarkMaxCpuFlag = false;
                Log.i(TAG, "mBenchmarkFrameCount = " + mBenchmarkFrameCount);
                mVolleyRequestHandler.cloudImageSender.busy = false;

                //Write some results to a timestamped text file.
                String benchmarkType;
                if(prefRemoteProcessing) {
                    benchmarkType = "Edge";
                } else {
                    benchmarkType = "Local";
                }
                String data = benchmarkType+" mBenchmarkFrameCount = " + mBenchmarkFrameCount;
                DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
                String fileName = df.format(new Date())+"_results.txt";
                final File file = new File(getContext().getExternalFilesDir(null), fileName);
                try {
                    file.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(file);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append(data);
                    myOutWriter.close();
                    fOut.flush();
                    fOut.close();
                }
                catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }

                //Completely quit out of this app so that it doesn't use additional CPU/battery.
                getActivity().moveTaskToBack(true);
                getActivity().finish();
            }
        }.start();

        // Comment out the next 2 lines to use 200ms throttled "apples to apples" method.
        mBenchmarkMaxCpuFlag = true;
        new maxCpuLoop().execute();
    }

    private class maxCpuLoop extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            while(mBenchmarkMaxCpuFlag) {
                loadImageAsset();
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "maxCpuLoop done");
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    public void loadImageAsset() {
        try {
            if(getContext() == null) {
                Log.w(TAG, "getContext() is null. Aborting image load.");
                return;
            }
            AssetManager assetManager = getContext().getAssets();
            String imageFile = mAssetImageFileList.get(mAssetImageFileIndex);
            InputStream is = assetManager.open(mAssetImagePath +"/"+imageFile);
            Bitmap mTestBitmap = BitmapFactory.decodeStream(is);
            serverToDisplayRatio = (float) mTextureView.getWidth() / mTestBitmap.getWidth();
            processImage(mTestBitmap);

            // Draw the image on the screen using the camera preview TextureView.
            Canvas canvas = mTextureView.lockCanvas();
            if(canvas == null) {
                Log.i("BDA", "canvas is null :(");
            } else {
                //TODO: Pre-calculate these
                Rect src = new Rect(0, 0, mTestBitmap.getWidth(), mTestBitmap.getHeight());
                Rect dst = new Rect(0, 0, mTextureView.getWidth(), mTextureView.getHeight());

                if(mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                    // Mirror the image
                    Matrix m = new Matrix();
                    m.preScale(-1, 1);
                    mTestBitmap = Bitmap.createBitmap(mTestBitmap, 0, 0, mTestBitmap.getWidth(), mTestBitmap.getHeight(), m, false);
                    // so you got to move your bitmap back to its place. otherwise you will not see it
                    m.postTranslate(canvas.getWidth(), 0);
                }
                canvas.drawBitmap(mTestBitmap, src, dst, null);
                mTextureView.unlockCanvasAndPost(canvas);
            }

//            mAssetImageFileIndex++;
            if (mAssetImageFileIndex >= mAssetImageFileList.size()) {
                mAssetImageFileIndex = 0;
            }
            mBenchmarkFrameCount++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String convertSecondsToHMmSs(long seconds) {
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;
        return String.format("%d:%02d:%02d", h,m,s);
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    protected AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
//            Log.d(TAG, "onImageAvailable");
            // How to manipulate preview frames:
            // https://stackoverflow.com/questions/25462277/camera-preview-image-data-processing-with-android-l-and-camera2-api

            Image image = reader.acquireLatestImage();
            if(image == null) {
//                Log.d(TAG, "null image. bail out.");
                return;
            }

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

            Matrix matrix = new Matrix();

            Activity activity = getActivity();

            int width = 0;
            int height = 0;
            try {
                width = image.getWidth() / factor;
                height = image.getHeight() / factor;
            } catch (IllegalStateException e) {
//                Log.d(TAG, "image already closed. bail out.");
                return;
            }
//            Log.d(TAG, "image size="+image.getWidth()+","+image.getHeight()+"/factor("+factor+")="+width+","+height);
            // The faces will be a 20% of the height of the screen
            absoluteFaceSize = (int) (height * 0.2);

            int deg = 0;
            int rotation2 = 0;
            try {
                CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics("" + mCameraDevice.getId());
                rotation2 = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                deg = getOrientation(rotation);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                        deg = 270;
                        break;
                    case Surface.ROTATION_180:
                        deg = 90;
                        break;
                    case Surface.ROTATION_90:
                        deg = 0;
                        break;
                    case Surface.ROTATION_270:
                        deg = 180;
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }
            }

            if(deg != 0) {
                matrix.postRotate(deg);
            }

            //There appears to be a race condition when the camera is closed during this process.
            //Do one final check if the image still exists.
            if(image == null) {
                Log.w(TAG, "image has become null. Abort processing.");
                return;
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapImage,
                    image.getWidth() / factor, image.getHeight() / factor, true);

            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth() , scaledBitmap.getHeight(), matrix, true);

            serverToDisplayRatio = (float) mTextureView.getWidth() / rotatedBitmap.getWidth();

            processImage(rotatedBitmap);

            if (image != null) {
                image.close();
            }
        }
    };

    public void processImage(Bitmap bitmap) {
        if(prefRemoteProcessing) {
            mVolleyRequestHandler.sendImage(bitmap);
        }

        if(prefLocalProcessing) {
            long startTime = System.nanoTime();
            Mat aInputFrame = new Mat();
            Utils.bitmapToMat(bitmap, aInputFrame);
            MatOfRect faces = new MatOfRect();

            // Use the classifier to detect faces
            if (cascadeClassifier != null) {
                cascadeClassifier.detectMultiScale(aInputFrame, faces, 1.2, 2, 2,
                        new org.opencv.core.Size(absoluteFaceSize, absoluteFaceSize), new org.opencv.core.Size());
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
            Log.i("BDA", "localLatency="+(localLatency/1000000.0)+" localAvg="+(localAvg/1000000.0)+" localStdDev="+(localStdDev/1000000.0));

            updateOverlay(CloudLetType.LOCAL_PROCESSING, jsonArray, null);

            // Save image to local storage.
//            if (facesArray.length > 0) {
//                Imgproc.cvtColor(aInputFrame, aInputFrame, CvType.CV_8UC1);
//                Imgcodecs.imwrite(String.valueOf(mFile), aInputFrame);
//            }
        }

    }

    /**
     * Update the face rectangle coordinates and the UI.
     *
     * @param cloudletType
     * @param rectJsonArray
     * @param subject  The identified subject name.
     */
    @Override
    public void updateOverlay(final CloudLetType cloudletType, final JSONArray rectJsonArray, final String subject) {
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

                int widthOff = (int) mTextureView.getX();
                int heightOff = (int) mTextureView.getY();

                Log.d(TAG, "widthOff=" + widthOff + " heightOff=" + heightOff + " serverToDisplayRatio=" + serverToDisplayRatio);
                Rect textureRect = new Rect(1, 1, mTextureView.getWidth(), mTextureView.getHeight());
                textureRect.offset(widthOff, heightOff);
                Log.d(TAG, "textureRect=" + textureRect);

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

                        rect.left *= serverToDisplayRatio;
                        rect.right *= serverToDisplayRatio;
                        rect.top *= serverToDisplayRatio;
                        rect.bottom *= serverToDisplayRatio;

                        if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                            // The image that was processed is what the camera sees, but the image we want to
                            // overlay the rectangle onto is mirrored. So not only do we have to scale it,
                            // but we have to flip it horizontally.
                            rect.left = mTextureView.getWidth() - rect.left;
                            rect.right = mTextureView.getWidth() - rect.right;
                            int tmp = rect.left;
                            rect.left = rect.right;
                            rect.right = tmp;
                        }

                        rect.offset(widthOff, heightOff);

                        Log.d(TAG, "jsonRect="+jsonRect+" scaled rect=" + rect.toShortString() + " mTextureView size =" + mTextureView.getWidth() + "," + mTextureView.getHeight());

                        if (textureRect.contains(rect)) {
                            Log.d(TAG, "Adding "+rect);
                            BoundingBox bb;
                            if (cloudletType == CloudLetType.CLOUD) {
                                bb = mCloudBBList.get(i);
                            } else if (cloudletType == CloudLetType.EDGE) {
                                bb = mEdgeBBList.get(i);
                            } else if (cloudletType == CloudLetType.LOCAL_PROCESSING) {
                                bb = mLocalBBList.get(i);
                            } else {
                                Log.e(TAG, "Unknown cloudletType: "+cloudletType);
                                continue;
                            }
                            bb.rect = rect;
                            bb.invalidate();
                            bb.restartAnimation();
                            bb.setSubject(subject);
                        } else {
                            Log.w(TAG, "invalid "+cloudletType+" rectangle received: " + jsonRect.toString()+" converted to: " + rect.toShortString());
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void updateOverlay(final CloudLetType cloudletType, final JSONArray posesJsonArray) {
        throw new UnsupportedOperationException();
    }

    public void updateTrainingProgress(int cloudTrainingCount, int edgeTrainingCount) {
        Log.i("BDA7", "updateTrainingProgress() edge="+mVolleyRequestHandler.edgeImageSender.mCameraMode+" cloud="+mVolleyRequestHandler.cloudImageSender.mCameraMode);
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

    public void updateFullProcessStats(final CloudLetType cloudletType, final long latency, VolleyRequestHandler.RollingAverage rollingAverage) {
        final long stdDev = rollingAverage.getStdDev();

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

    public void updateNetworkStats(final CloudLetType cloudletType, final long latency, VolleyRequestHandler.RollingAverage rollingAverage) {
        final long stdDev = rollingAverage.getStdDev();

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
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "OpenCV: Error loading cascade", e);
        }

        // And we are ready to go
    }


    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback ()
    {
        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
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
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
            int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                    option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(TAG, "onCreateOptionsMenu");
        mOptionsMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.camera_menu, menu);

        if(mCameraMode == VolleyRequestHandler.CameraMode.FACE_DETECTION) {
            MenuItem item = menu.findItem(R.id.action_camera_training);
            item.setVisible(false);
            item = menu.findItem(R.id.action_camera_training_guest);
            item.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_camera_swap) {
            switchCamera();
            return true;
        }

        if (id == R.id.action_camera_settings) {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.FaceDetectionSettingsFragment.class.getName() );
            intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
            startActivity(intent);
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
            if(item.isChecked()) {
                // If item already checked then uncheck it
                item.setChecked(false);
                if(Account.getSingleton().isSignedIn()) {
                    mVolleyRequestHandler.setSubjectName(Account.getSingleton().getGoogleSignInAccount().getDisplayName());
                } else {
                    mVolleyRequestHandler.setSubjectName("");
                }
            } else {
                item.setChecked(true);
                TrainGuestDialog trainGuestDialog = new TrainGuestDialog();
                trainGuestDialog.setTargetFragment(this, 1);
                trainGuestDialog.show(getActivity().getSupportFragmentManager(), "training_guest_dialog");
            }
            return true;
        }

        return false;
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
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        Toolbar cameraToolbar = view.findViewById(R.id.cameraToolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(cameraToolbar);

        FrameLayout frameLayout = view.findViewById(R.id.container);
        mTextureView = view.findViewById(R.id.textureView);
        mTextureView.setOnClickListener(this);
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
        mCloudBB.setCloudletType(CloudLetType.CLOUD);
        mCloudBBList.add(mCloudBB);
        for(int i = 1; i <= MAX_FACES; i++) {
            BoundingBox bb = new BoundingBox(getContext());
            bb.setColor(Color.RED);
            bb.setCloudletType(CloudLetType.CLOUD);
            bb.setLayoutParams(mCloudBB.getLayoutParams());
            mCloudBBList.add(bb);
            frameLayout.addView(bb);
        }

        //Find 1 EdgeBB and create MAX_FACES-1 more, using the same LayoutParams.
        mEdgeBB = view.findViewById(R.id.edgeBB);
        mEdgeBB.setColor(Color.GREEN);
        mEdgeBB.setCloudletType(CloudLetType.EDGE);
        mEdgeBBList.add(mEdgeBB);
        for(int i = 1; i <= MAX_FACES; i++) {
            BoundingBox bb = new BoundingBox(getContext());
            bb.setColor(Color.GREEN);
            bb.setCloudletType(CloudLetType.EDGE);
            bb.setLayoutParams(mEdgeBB.getLayoutParams());
            mEdgeBBList.add(bb);
            frameLayout.addView(bb);
        }
        Log.i("BDA1", "mEdgeBBList="+mEdgeBBList.size()+" mCloudBBList="+mCloudBBList.size()+" MAX_FACES="+MAX_FACES);

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

        mPoseRenderer = view.findViewById(R.id.poseSkeleton);

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
            cameraToolbar.setTitle(R.string.title_activity_face_recognition);
        } else {
            mVolleyRequestHandler.setCameraMode(VolleyRequestHandler.CameraMode.FACE_DETECTION);
            mCameraMode = VolleyRequestHandler.CameraMode.FACE_DETECTION;
            cameraToolbar.setTitle(R.string.title_activity_face_detection);
        }
        boolean benchEdge = intent.getBooleanExtra(EXTRA_BENCH_EDGE, false);
        boolean benchLocal = intent.getBooleanExtra(EXTRA_BENCH_LOCAL, false);
        if(benchEdge) {
            prefLocalProcessing = false;
            prefRemoteProcessing = true;
            mVolleyRequestHandler.cloudImageSender.busy = true;
        } else if(benchLocal) {
            prefLocalProcessing = true;
            prefRemoteProcessing = false;
        }

        //Short delay to allow camera setup to complete, then start the benchmark
        if(benchEdge || benchLocal) {
            Handler handler = new Handler();
            Runnable startBenchmark = new Runnable() {
                @Override
                public void run() {
                    runBenchmark(getContext());
                }
            };
            handler.postDelayed(startBenchmark, 1000);
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

        mVolleyRequestHandler.setUseRollingAverage(prefUseRollingAvg);
        mVolleyRequestHandler.setDoNetLatency(prefShowNetLatency);

    }

    private void switchCamera() {
        if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_BACK) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_FRONT;
            closeCamera();
            reopenCamera();

        } else if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_BACK;
            closeCamera();
            reopenCamera();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String prefKeyFrontCamera = getResources().getString(R.string.preference_fd_front_camera);
        prefs.edit().putInt(prefKeyFrontCamera, mCameraLensFacingDirection).apply();
    }

    private void reopenCamera() {
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");
        Log.i(TAG, "mFile="+mFile.getAbsolutePath());
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

        //Only load the OpenCV library if we are doing local processing.
        if(prefLocalProcessing) {
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, getContext(), mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        } else {
            Log.d(TAG, "Local processing not enabled. Skipping loading OpenCV library.");
        }

        if(mRunningBenchmark) {
            closeCamera();
            stopBackgroundThread();
        }

    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        mBenchmarkMaxCpuFlag = false;
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");
        mBenchmarkMaxCpuFlag = false;
        closeCamera();
        stopBackgroundThread();
        if(mBenchmarkTimer != null) {
            mBenchmarkTimer.cancel();
        }
        super.onStop();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        if (shouldShowRequestPermissionRationale(Manifest.permission.INTERNET)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_camera_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != mCameraLensFacingDirection) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth()/mImageReaderFactor, largest.getHeight()/mImageReaderFactor,
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Opens the camera specified by {@link Camera2BasicFragment#mCameraId}.
     */
    private void openCamera(int width, int height) {
        Log.i(TAG, "openCamera("+width+","+height+")");
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        Log.d(TAG, "stopBackgroundThread()");
        if(mBackgroundThread == null) {
            Log.w(TAG, "mBackgroundThread is null. aborting stopBackgroundThread()");
            return;
        }
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        if(mImageReader == null) {
            Log.e(TAG, "mImageReader is null. Aborting createCameraPreviewSession");
            return;
        }
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();

            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);
                    Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        Log.w(TAG, "cloudBusy="+mVolleyRequestHandler.cloudImageSender.busy+" edgeBusy="+mVolleyRequestHandler.edgeImageSender.busy);
        switch (view.getId()) {
            /*case R.id.picture: {
                takePicture();
                break;
            }
            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }*/
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];

            /**Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            bitmapImage = toGrayscale(bitmapImage);
            bitmapImage.copyPixelsToBuffer(buffer);**/

            buffer.get(bytes);
            FileOutputStream output = null;

            try {
                Log.i(TAG, "mFile="+mFile.getAbsolutePath());
                output = new FileOutputStream(mFile);
                output.write(bytes);
                Log.i(TAG, "success");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_camera_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "Camera2BasicFragment onSharedPreferenceChanged("+key+")");
        if(getContext() == null) {
            //Can happen during rapid screen rotations.
            return;
        }
        String prefKeyFrontCamera = getResources().getString(R.string.preference_fd_front_camera);
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
            mCameraLensFacingDirection = sharedPreferences.getInt(prefKeyFrontCamera, CameraCharacteristics.LENS_FACING_FRONT);
        }
        if (key.equals(prefKeyMultiFace) || key.equals("ALL")) {
            prefMultiFace = sharedPreferences.getBoolean(prefKeyMultiFace, true);
        }
        if (key.equals(prefKeyLocalProc) || key.equals("ALL")) {
            prefLocalProcessing = sharedPreferences.getBoolean(prefKeyLocalProc, true);
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

}
