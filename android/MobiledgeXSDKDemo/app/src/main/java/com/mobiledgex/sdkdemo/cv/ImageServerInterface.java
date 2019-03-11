package com.mobiledgex.sdkdemo.cv;

import org.json.JSONArray;

/**
 * Interface for clients of the Face/Pose detection/recognition Server.
 */
public interface ImageServerInterface {
    void updateFullProcessStats(ImageProcessorFragment.CloudletType cloudletType, VolleyRequestHandler.RollingAverage rollingAverage);
    void updateNetworkStats(ImageProcessorFragment.CloudletType cloudletType, VolleyRequestHandler.RollingAverage rollingAverage);
    void updateOverlay(ImageProcessorFragment.CloudletType cloudletType, JSONArray overlayData);
    void updateOverlay(ImageProcessorFragment.CloudletType cloudletType, JSONArray overlayData, String subject);
    void updateTrainingProgress(int cloudTrainingCount, int edgeTrainingCount);
    void showToast(String message);
}
