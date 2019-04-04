package com.mobiledgex.sdkdemo.cv;

import org.json.JSONArray;

/**
 * Interface for clients of the Face/Pose detection/recognition Server.
 */
public interface ImageServerInterface {
    void updateFullProcessStats(ImageProcessorFragment.CloudletType cloudletType, RollingAverage rollingAverage);
    void updateNetworkStats(ImageProcessorFragment.CloudletType cloudletType, RollingAverage rollingAverage);
    void updateOverlay(ImageProcessorFragment.CloudletType cloudletType, JSONArray overlayData, String subject);
    void updateTrainingProgress(int trainingCount, ImageSender.CameraMode mode);
    void showToast(String message);
}
