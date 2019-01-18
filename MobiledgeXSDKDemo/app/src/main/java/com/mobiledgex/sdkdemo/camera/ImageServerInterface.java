package com.mobiledgex.sdkdemo.camera;

import org.json.JSONArray;

/**
 * Interface for clients of the Face/Pose detection/recognition Server.
 */
public interface ImageServerInterface {
    void updateFullProcessStats(Camera2BasicFragment.CloudLetType cloudletType, long latency, long stdDev);
    void updateNetworkStats(Camera2BasicFragment.CloudLetType cloudletType, long latency, long stdDev);
    void updateOverlay(Camera2BasicFragment.CloudLetType cloudletType, JSONArray overlayData);
    void updateOverlay(Camera2BasicFragment.CloudLetType cloudletType, JSONArray overlayData, String subject);
    void updateTrainingProgress(int cloudTrainingCount, int edgeTrainingCount);
    void showToast(String message);
}
