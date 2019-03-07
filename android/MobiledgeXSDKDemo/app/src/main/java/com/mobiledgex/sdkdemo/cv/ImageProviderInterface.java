package com.mobiledgex.sdkdemo.cv;

import android.graphics.Bitmap;
import android.graphics.Rect;

public interface ImageProviderInterface {
    void onBitmapAvailable(Bitmap bitmap, Rect imageRect);
    void setMessageText(String messageText);
}
