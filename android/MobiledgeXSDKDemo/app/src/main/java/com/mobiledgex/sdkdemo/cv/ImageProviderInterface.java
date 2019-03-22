package com.mobiledgex.sdkdemo.cv;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Interface for classes that process images from the camera
 */
public interface ImageProviderInterface {
    void onBitmapAvailable(Bitmap bitmap, Rect imageRect);
    void setMessageText(String messageText);
}
