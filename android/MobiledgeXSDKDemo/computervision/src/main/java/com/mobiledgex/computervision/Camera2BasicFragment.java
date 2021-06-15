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

package com.mobiledgex.computervision;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

/**
 * Provides normalized image frames (rotated and scaled) from the front or rear camera,
 * in portrait or landscape mode. Can also provide frames from an included video file.
 */
public class Camera2BasicFragment extends Fragment
        implements View.OnClickListener {

    private static final String TAG = "Camera2BasicFragment";
    private int mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_FRONT;

    private String mDebugInfo;

    private boolean mRunningBenchmark;
    private CountDownTimer mBenchmarkTimer;
    private String mBenchmarkType;
    private long mBenchmarkStartTime;
    private long mBenchmarkDurationMillis = 1*60*1000; //1 minute
    private long mBenchmarkTickMillis = 200;
    private int mBenchmarkFrameCount;
    private long mFrameLastTime;
    private long mFrameStartTime;
    private int mFrameCount;

    private MediaPlayer mMediaPlayer;
    private TextureView mVideoView;
    private boolean mVideoMode;
    private boolean mLegacyCamera;

    private ProgressBar mProgressBar;

    private int mImageSendWidth = 240;
    private int mImageSendHeight = 180;

    private ImageProviderInterface mImageProviderInterface;

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
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        private DecimalFormat decFor = new DecimalFormat("#.###");

        @Override
        public void onImageAvailable(ImageReader reader) {
//            Log.d(TAG, "onImageAvailable");
            // How to manipulate preview frames:
            // https://stackoverflow.com/questions/25462277/camera-preview-image-data-processing-with-android-l-and-camera2-api

            Image image = reader.acquireLatestImage();
            if(image == null) {
                return;
            }

            mFrameCount++;
            long now = System.currentTimeMillis();
            if(mFrameStartTime == 0) {
                mFrameStartTime = now;
            }
            long totalSeconds = (now- mFrameStartTime)/1000;
            if(mFrameLastTime > 0) {
                Log.i(TAG, "elapsed="+(now- mFrameLastTime)+" FPS="+(decFor.format((float) mFrameCount /totalSeconds)));
            }
            mFrameLastTime = now;

            byte[] bytes = ImageUtil.imageToByteArray(image);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

            Rect rect = new Rect(mTextureView.getLeft(),
                    mTextureView.getTop(), mTextureView.getRight(), mTextureView.getBottom());
            Log.d(TAG, "mTextureView rect="+rect.toShortString()+" ImageReader.OnImageAvailable");
            if(mImageProviderInterface != null) {
                mImageProviderInterface.onBitmapAvailable(prepareImage(bitmap), rect);
            }

            if (image != null) {
                image.close();
            }
        }
    };

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
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "onSurfaceTextureAvailable: mVideoMode="+mVideoMode+" "+width+","+height);
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
     /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mVideoSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(TAG, "Video onSurfaceTextureAvailable: mVideoMode="+mVideoMode+" "+width+","+height);
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

    public void setImageProviderInterface(ImageProviderInterface imageProviderInterface) {
        mImageProviderInterface = imageProviderInterface;
    }

    public void showDebugInfo() {
        Log.i(TAG, "mDebugInfo=\n"+ mDebugInfo);
        TextView showText = new TextView(getActivity());
        showText.setText(mDebugInfo);
        showText.setTextIsSelectable(true);
        int horzPadding = (int) (25 * getResources().getDisplayMetrics().density);
        showText.setPadding(horzPadding, 0,horzPadding,0);
        new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                .setView(showText)
                .setTitle("Camera Info")
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Rotate and scale a given bitmap, based on orientation and active camera.
     * @param bitmap
     * @return  The rotated and scaled bitmap.
     */
    public Bitmap prepareImage(Bitmap bitmap) {
        Matrix matrix = new Matrix();

        if(bitmap == null) {
            return null;
        }

        int deg = 0;
        int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        deg = getOrientation(displayRotation);

        boolean swapDimensions = false;
        Bitmap scaledBitmap;
        if(mVideoMode) {
            deg = 0;
            if (displayRotation == Surface.ROTATION_0) {
                swapDimensions = true;
            }

        } else {

            if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
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

            // For legacy camera mode, instead of pulling the image from an ImageReader instance,
            // which is standard, we pull it from the preview texture. The rotation needed
            // to send the correct image to the server is different. Handle that here.
            if(mLegacyCamera) {
                swapDimensions = true;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                        deg = 0;
                        break;
                    case Surface.ROTATION_180:
                        deg = 180;
                        break;
                    case Surface.ROTATION_90:
                        deg = 270;
                        break;
                    case Surface.ROTATION_270:
                        deg = 90;
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }
            }
        }

        if(swapDimensions) {
            // Swap width and height.
            scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                    mImageSendHeight, mImageSendWidth, true);
        } else {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                    mImageSendWidth, mImageSendHeight, true);
        }

        if(deg != 0) {
            matrix.postRotate(deg);
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        }
        return scaledBitmap;
    }

    /**
     * This method runs the face detection process on an included video file.
     * Because battery stats are no longer available to apps on non-rooted
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
    public void runBenchmark(Context context, String benchmarkType) {
        mBenchmarkType = benchmarkType;
        mRunningBenchmark = true;
        mBenchmarkFrameCount = 0;
        mBenchmarkStartTime = System.currentTimeMillis();
        mBenchmarkTimer = new CountDownTimer(mBenchmarkDurationMillis, mBenchmarkTickMillis) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = (mBenchmarkDurationMillis - (System.currentTimeMillis() - mBenchmarkStartTime))/1000;
                mImageProviderInterface.setStatus(convertSecondsToHMmSs(secondsRemaining) + " remains");
            }
            @Override
            public void onFinish() {
                //Write the results to a timestamped text file.
                String data = mImageProviderInterface.getStatsText();
                DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
                String fileName = df.format(new Date())+"_results.txt";
                Log.i(TAG, "Writing benchmark results to "+fileName);
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

                getActivity().finish();
            }
        }.start();
    }

    public int getCameraLensFacingDirection() {
        return mCameraLensFacingDirection;
    }

    public void setCameraLensFacingDirection(int cameraLensFacingDirection) {
        mCameraLensFacingDirection = cameraLensFacingDirection;
    }

    public static String convertSecondsToHMmSs(long seconds) {
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;
        return String.format("%d:%02d:%02d", h,m,s);
    }

    private Range<Integer> getRange() {
        CameraCharacteristics chars = null;
        try {
            CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
            chars = manager.getCameraCharacteristics(mCameraId);
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Range<Integer> result = null;
            for (Range<Integer> range : ranges) {
                int upper = range.getUpper();
                // 10 - min range upper for my needs
                if (upper >= 10) {
                    if (result == null || upper < result.getUpper().intValue()) {
                        result = range;
                    }
                }
            }
            if (result == null) {
                result = ranges[0];
            }
            return result;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

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
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated savedInstanceState="+savedInstanceState);
        if(savedInstanceState != null) {
            mVideoMode = savedInstanceState.getBoolean("VIDEO_MODE", false);
        }
        Log.i(TAG, "savedInstanceState mVideoMode="+mVideoMode);

        mProgressBar = view.findViewById(R.id.progressBar);
        mTextureView = view.findViewById(R.id.textureView);
        mVideoView = view.findViewById(R.id.videoView);
        mVideoView.setOnClickListener(this);
    }

    public void switchCamera() {
        if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_BACK) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_FRONT;
            closeCamera();
            reopenCamera();

        } else if (mCameraLensFacingDirection == CameraCharacteristics.LENS_FACING_FRONT) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_BACK;
            closeCamera();
            reopenCamera();
        }
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
    public void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
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
            mVideoView.setSurfaceTextureListener(mVideoSurfaceTextureListener);
        }

        if(mRunningBenchmark) {
            closeCamera();
            stopBackgroundThread();
        }

    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        closeCamera();
        stopBackgroundThread();
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop()");
        closeCamera();
        stopBackgroundThread();
        if(mBenchmarkTimer != null) {
            mBenchmarkTimer.cancel();
        }
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
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

                // Only set up the camera we have currently selected.
                // Unless the device only has 1 camera, then select it always.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != mCameraLensFacingDirection
                        && manager.getCameraIdList().length != 1) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Log.i(TAG, "map.getOutputSizes(ImageFormat.JPEG)="+Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)));
                /*
                platos Galaxy S5:
                Front: [1920x1080, 1440x1080, 1280x960, 1280x720, 960x720, 720x480, 640x480, 320x240]
                Rear: [5312x2988, 3984x2988, 3264x2448, 3264x1836, 2560x1920, 2048x1152, 1920x1080, 1280x960, 1280x720, 800x480, 640x480, 320x240]
                platos Galaxy S8:
                Front: [3264x2448, 3264x1836, 2880x2160, 2560x1920, 2560x1440, 2560x1080, 2448x2448, 2160x2160, 2048x1152, 1920x1080, 1440x1080, 1280x960, 1280x720, 720x480, 640x480, 320x240, 176x144]
                Rear: [4032x3024, 4032x2268, 3984x2988, 3264x2448, 3264x1836, 3024x3024, 2976x2976, 2880x2160, 2560x1920, 2560x1440, 2560x1080, 2448x2448, 2160x2160, 2048x1152, 1920x1080, 1440x1080, 1280x960, 1280x720, 720x480, 640x480, 320x240, 176x144]
                 */
                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                Log.i(TAG, "JPEG largest="+largest+" aspect="+((float)largest.getWidth()/(float)largest.getHeight()));

                mDebugInfo = "Output sizes for JPEG: "+Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
                mDebugInfo += "\nLargest chosen: "+largest;

                //We only want a 4:3 aspect ratio for the camera.
                if((float)largest.getWidth()/(float)largest.getHeight() > 1.34) {
                    largest = new Size((int) (largest.getHeight()*1.33333334), largest.getHeight());
                    mDebugInfo += "\nLargest after forcing 4:3 aspect ratio: "+largest;
                }
                Log.i(TAG, "new largest="+largest);

                Size desiredImageSize = new Size(mImageSendWidth, mImageSendHeight);
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
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

                Log.i(TAG, "mPreviewSize="+mPreviewSize+" mTextureView="+mTextureView.getRatioWidth()+","+mTextureView.getRatioHeight());

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;

                mDebugInfo += "\ndisplayRotation="+displayRotation+" SensorOrientation="+mSensorOrientation;
                mDebugInfo += "\nmPreviewSize="+mPreviewSize+" mTextureView="+mTextureView.getRatioWidth()+","+mTextureView.getRatioHeight();
                mDebugInfo += "\nmFlashSupported="+mFlashSupported+" mCameraId="+mCameraId;

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
            Log.i(TAG, "texture="+texture+" mPreviewSize="+mPreviewSize.getWidth()+","+mPreviewSize.getHeight());

            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            if(mLegacyCamera) {
                Log.i(TAG, "Legacy camera support. Not adding ImageReader.");
                mBackgroundHandler.post(mLegacyCameraRunnableCode);
            } else {
                mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
            }

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
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());

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
                            showToast("Camera configuration failed");
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
        int i = view.getId();
        if (i == R.id.videoView) {
            if (mMediaPlayer == null) {
                return;
            }
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            } else {
                mMediaPlayer.start();
            }
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

    private Runnable mLegacyCameraRunnableCode = new Runnable() {
        @Override
        public void run() {
            if(mTextureView.getRatioWidth() == 0 || mTextureView.getRatioHeight() == 0) {
                return;
            }
            Bitmap bitmap = mTextureView.getBitmap();

            Rect rect = new Rect(mTextureView.getLeft(),
                    mTextureView.getTop(), mTextureView.getRight(), mTextureView.getBottom());
            Log.i(TAG, "mTextureView rect="+rect.toShortString()+" mLegacyCamera preview");
            if(mImageProviderInterface != null) {
                mImageProviderInterface.onBitmapAvailable(prepareImage(bitmap), rect);
            }
            mBackgroundHandler.postDelayed(this, 66);
        }
    };

    private Runnable mVideoRunnableCode = new Runnable() {
        @Override
        public void run() {
            getCurrentVideoFrame();
            mBackgroundHandler.postDelayed(this, 66);
        }
    };

    public void startVideo(String filename, boolean isFullUrl) {
        Log.i(TAG, "startVideo("+filename+","+isFullUrl+")");
        mTextureView.setVisibility(View.INVISIBLE);
        mImageSendWidth = 320; //Widescreen: 1.77:1
        // Hide the status bar.
        View decorView = getActivity().getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        prepareVideo(filename, isFullUrl);
    }

    /**
     * Select appropriate video file and start it playing on mVideoView's surface.
     * A background thread is started to pull the current video frame every 66 ms.
     *
     * @param filename  The video file name.
     */
    private void prepareVideo(String filename, boolean isFullUrl) {
        Log.i(TAG, "prepareVideo()");
        String videoFileName;
        final Uri videoUrl;
        mVideoMode = true;
        closeCamera();
        if(mLegacyCamera) {
            stopBackgroundThread();
            startBackgroundThread();
        }

        if (isFullUrl) {
            videoUrl = Uri.parse(filename);
        } else {
            // In this case, it is one of the standard videos that we maintain.
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                videoFileName = "landscape/"+filename;
            } else {
                videoFileName = "portrait/"+filename;
            }
            videoUrl = Uri.parse("http://opencv.facetraining.mobiledgex.net/videos/"+videoFileName);
        }

        try {
            Surface videoSurface = new Surface(mVideoView.getSurfaceTexture());
            Log.i(TAG, "videoSurface="+videoSurface);

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(getContext(), videoUrl);
            mMediaPlayer.setSurface(videoSurface);
            mMediaPlayer.setLooping(true);
            mProgressBar.setVisibility(View.VISIBLE);
            mMediaPlayer.prepareAsync();

            // Play video when the media source is ready for playback.
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mBackgroundHandler.post(mVideoRunnableCode);
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    ErrorDialog.newInstance("Error playing video from URL "+videoUrl)
                            .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                    return true;
                }
            });

        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
            showToast(e.getMessage());
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
            showToast(e.getMessage());
        } catch (IllegalStateException e) {
            Log.d(TAG, e.getMessage());
            showToast(e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            showToast(e.getMessage());
        }
    }

    public void getCurrentVideoFrame() {
        if(mMediaPlayer == null) {
            Log.i(TAG, "mMediaPlayer is null. Abort getCurrentVideoFrame()");
            return;
        }

        int currentPosition = mMediaPlayer.getCurrentPosition(); //in millisecond
        Log.i(TAG, "bitmap from video: currentPosition="+currentPosition);

        int pos = currentPosition * 1000;   //unit in microsecond

        long start = System.currentTimeMillis();
        Bitmap bitmap = mVideoView.getBitmap();
        long now = System.currentTimeMillis();
        Log.i(TAG, "getBitmap took "+(now-start)+" ms");

        start = System.currentTimeMillis();
        Rect rect = new Rect(mVideoView.getLeft(),
                mVideoView.getTop(), mVideoView.getRight(), mVideoView.getBottom());
        Log.i(TAG, "mVideoView rect="+rect.toShortString()+" getCurrentVideoFrame()");
        if(mImageProviderInterface != null) {
            mImageProviderInterface.onBitmapAvailable(prepareImage(bitmap), rect);
        }
        now = System.currentTimeMillis();
        Log.i(TAG, "processImage took "+(now-start)+" ms");
    }

    public boolean isVideoMode() {
        return mVideoMode;
    }

    public boolean isLegacyCamera() {
        return mLegacyCamera;
    }

    public void setLegacyCamera(boolean legacyCamera) {
        mLegacyCamera = legacyCamera;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("VIDEO_MODE", mVideoMode);
    }

}
