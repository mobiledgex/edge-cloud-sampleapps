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

package com.mobiledgex.tritonlib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a set of rectangular coordinates with class names of detected objects in multiple colors.
 */
public class InceptionClassRenderer extends View {
    private static final String TAG = "InceptionClassRenderer";
    public static final int DEFAULT_STROKE_WIDTH = 6;
    private int mStrokeWidth = DEFAULT_STROKE_WIDTH;
    private int mTextSize = (int) (getResources().getDisplayMetrics().scaledDensity * 16);
    private int mWidth;
    private int mHeight;
    private int mWidthOff;
    private int mHeightOff;
    private float mServerToDisplayRatioX;
    private float mServerToDisplayRatioY;
    private boolean mMirrored;
    private Animation mAlphaAnim;

    private Paint mTextPaint;
    private String mClassName;

    public InceptionClassRenderer(Context context) {
        super(context);
        init(context);
    }

    public InceptionClassRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(mTextSize);

        mAlphaAnim = AnimationUtils.loadAnimation(context, R.anim.alpha);
        mAlphaAnim.setFillAfter(true);
        startAnimation(mAlphaAnim);
    }

    public void setClassName(String className) {
        Log.d(TAG, "setClasses() "+getWidth()+","+getHeight());
        mClassName = className;
        restartAnimation();
    }

    /**
     * Sets display parameters used to determine ratios and offsets to correctly draw the object rectangles.
     * @param imageRect  The coordinates of the TextureView that is showing the preview image.
     * @param serverToDisplayRatioX  The ratio of the width of the image sent to the server vs.
     *                               the width of the preview image being displayed.
     * @param serverToDisplayRatioY  The ratio of the height of the image sent to the server vs.
     *                               the height of the preview image being displayed.
     * @param mirrored  Whether the preview image is being mirrored.
     */
    public void setDisplayParms(Rect imageRect,
                                float serverToDisplayRatioX, float serverToDisplayRatioY, boolean mirrored) {
        mWidth = imageRect.width();
        mHeight = imageRect.height();
        mWidthOff = imageRect.left;;
        mHeightOff = imageRect.top;
        mServerToDisplayRatioX = serverToDisplayRatioX;
        mServerToDisplayRatioY = serverToDisplayRatioY;
        mMirrored = mirrored;
    }

    /**
     * Reset the fade-out animation back to the beginning.
     */
    public void restartAnimation() {
        startAnimation(mAlphaAnim);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw() "+getWidth()+","+getHeight());
        super.onDraw(canvas);

        if(mClassName == null) {
            return;
        }

        Log.d(TAG, "mClassName="+ mClassName);

        String label = mClassName;
        int x = 200;
        int y = getHeight()-mTextSize*7;
        canvas.drawText(label, x, y, mTextPaint);
    }
}
