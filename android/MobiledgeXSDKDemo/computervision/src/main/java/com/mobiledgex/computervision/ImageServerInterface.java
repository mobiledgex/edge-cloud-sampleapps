package com.mobiledgex.computervision;

import org.json.JSONArray;

/**
 * Interface for clients of the Face/Pose Detection/Recognition Server.
 */
public interface ImageServerInterface {
    /**
     * Provide statistics values for the full image processing round-trip.
     * @param cloudletType  The cloudlet type.
     * @param rollingAverage  The RollingAverage instance which holds the statistic values.
     */
    void updateFullProcessStats(CloudletType cloudletType, RollingAverage rollingAverage);

    /**
     * Provide statistics values for network latency.
     * @param cloudletType  The cloudlet type.
     * @param rollingAverage  The RollingAverage instance which holds the statistic values.
     */
    void updateNetworkStats(CloudletType cloudletType, RollingAverage rollingAverage);

    /**
     * Provide the coordinates for the detected images so that the implementor can draw
     * markers over the original image.
     * @param cloudletType  The cloudlet type.
     * @param overlayData  Array or detected coordinates.
     * @param subject  Name of identified subject. Empty string should draw no text.
     */
    void updateOverlay(CloudletType cloudletType, JSONArray overlayData, String subject);

    /**
     * Provides feedback on the Face Training process progress.
     * @param trainingCount  The number of images that have been accepted by the Face Training Server.
     * @param mode  The current camera mode. Used to determine whether to move to to a new stage of
     *              the Face Training process.
     * @see com.mobiledgex.computervision.ImageSender.CameraMode
     */
    void updateTrainingProgress(int trainingCount, ImageSender.CameraMode mode);

    /**
     * Shows a message to the user.
     * @param message  The message to show.
     */
    void showMessage(String message);

    /**
     * The type of server that is processing the images.
     */
    enum CloudletType {
        EDGE,
        CLOUD,
        LOCAL_PROCESSING,
        PUBLIC
    }
}
