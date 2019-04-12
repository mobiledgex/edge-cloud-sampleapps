package com.mobiledgex.computervision;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Interface for classes that process images from the camera.
 */
public interface ImageProviderInterface {
    /**
     * Callback that is called when a new bitmap image is available from the camera or video.
     *
     * @param bitmap  The bitmap image.
     * @param imageRect  The coordinates of the TextureView that is showing the preview image.
     *                   This is used for calculating ratios and offsets needed to correctly draw
     *                   any image overlays (e.g. face rectangles, pose skeletons).
     */
    void onBitmapAvailable(Bitmap bitmap, Rect imageRect);

    /**
     * Shows a status message to the user.
     * @param status  The status to show.
     */
    void setStatus(String status);
}
