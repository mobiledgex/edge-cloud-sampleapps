package com.mobiledgex.sdkdemo.cv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.mobiledgex.sdkdemo.R;

import org.json.JSONArray;
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

public class OpencvImageProcessorFragment extends ImageProcessorFragment {
    private static final String TAG = "OpencvImageProcessor";
    private CascadeClassifier mCascadeClassifier;

    RollingAverage localLatencyRollingAvg = new RollingAverage(ImageProcessorFragment.CloudletType.LOCAL_PROCESSING, "On-Device", 100);

    public static OpencvImageProcessorFragment newInstance() {
        return new OpencvImageProcessorFragment();
    }

    @Override
    public void onBitmapAvailable(Bitmap bitmap, Rect imageRect) {
        Log.i(TAG, "onBitmapAvailable() bitmap: "+bitmap.getWidth()+"x"+bitmap.getHeight());
        super.onBitmapAvailable(bitmap, imageRect);

        if(bitmap == null) {
            return;
        }

        if(!prefLocalProcessing || mCameraMode != ImageSender.CameraMode.FACE_DETECTION) {
            return;
        }

        mImageRect = imageRect;
        mServerToDisplayRatioX = (float) mImageRect.width() / bitmap.getWidth();
        mServerToDisplayRatioY = (float) mImageRect.height() / bitmap.getHeight();

        Log.d(TAG, "mImageRect="+mImageRect.toShortString()+" mImageRect.height()="+mImageRect.height()+" bitmap.getWidth()="+bitmap.getWidth()+" bitmap.getHeight()="+bitmap.getHeight()+" mServerToDisplayRatioX=" + mServerToDisplayRatioX +" mServerToDisplayRatioY=" + mServerToDisplayRatioY);

        processImageLocal(bitmap);
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

        updateOverlay(ImageProcessorFragment.CloudletType.LOCAL_PROCESSING, jsonArray, null);

        // Save image to local storage.
//            if (facesArray.length > 0) {
//                Imgproc.cvtColor(aInputFrame, aInputFrame, CvType.CV_8UC1);
//                Imgcodecs.imwrite(String.valueOf(mFile), aInputFrame);
//            }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_benchmark_local) {
            mCameraToolbar.setVisibility(View.GONE);
            mBenchmarkActive = true;
            prefLocalProcessing = true;
            prefRemoteProcessing = false;
            mCloudLatency.setVisibility(View.GONE);
            mCloudLatency2.setVisibility(View.GONE);
            mCloudStd.setVisibility(View.GONE);
            mCloudStd2.setVisibility(View.GONE);
            mEdgeLatency.setVisibility(View.GONE);
            mEdgeLatency2.setVisibility(View.GONE);
            mEdgeStd.setVisibility(View.GONE);
            mEdgeStd2.setVisibility(View.GONE);
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

}
