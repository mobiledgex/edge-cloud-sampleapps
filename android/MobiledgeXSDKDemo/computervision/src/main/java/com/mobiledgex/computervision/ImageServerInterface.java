/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
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
