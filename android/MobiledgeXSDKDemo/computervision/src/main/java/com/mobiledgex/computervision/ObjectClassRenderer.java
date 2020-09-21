/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a set of rectangular coordinates with class names of detected objects in multiple colors.
 */
public class ObjectClassRenderer extends View {
    private static final String TAG = "ObjectClassRenderer";
    public static final int DEFAULT_STROKE_WIDTH = 6;
    private int mStrokeWidth = DEFAULT_STROKE_WIDTH;
    private int mTextSize = (int) (getResources().getDisplayMetrics().scaledDensity * 16);
    private JSONArray mObjects;
    private int mWidth;
    private int mHeight;
    private int mWidthOff;
    private int mHeightOff;
    private float mServerToDisplayRatioX;
    private float mServerToDisplayRatioY;
    private boolean mMirrored;
    private Animation mAlphaAnim;

    /**
     * Array for drawing each object with a different color.
     */
    private String[] colors = {
            "#238bc0", "#ff9209", "#32ab39", "#e03d34", "#a57ec8", "#9e6a5d", "#ea90cc", "#919191",
            "#c8c62b", "#00c8d8", "#bbd1ec", "#ffc689", "#a6e19b", "#ffaaa6", "#cfbfdd", "#cfaca5",
            "#fac4da", "#d1d1d1", "#e1e09e", "#ace0e9"
    };

    private List<Paint> mPaints = new ArrayList<>();
    private List<Paint> mFillPaints = new ArrayList<>();
    private Paint mTextPaint;

    public ObjectClassRenderer(Context context) {
        super(context);
        init(context);
    }

    public ObjectClassRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(mTextSize);

        mPaints.clear();
        for(int i = 0; i < colors.length; i++) {
            Paint paint = new Paint();
            paint.setColor(Color.parseColor(colors[i]));
            paint.setStrokeWidth(mStrokeWidth);
            paint.setStyle(Paint.Style.STROKE);
            mPaints.add(paint);
            Paint fillPaint = new Paint(paint);
            fillPaint.setAlpha(96); // out of 255
            fillPaint.setStyle(Paint.Style.FILL);
            mFillPaints.add(fillPaint);
        }

        mAlphaAnim = AnimationUtils.loadAnimation(context, R.anim.alpha);
        mAlphaAnim.setFillAfter(true);
        startAnimation(mAlphaAnim);
    }

    /**
     * Sets the array of detected object coordinates and class names.
     * @param objectsJsonArray The array of detected object coordinates and class names.
     */
    public void setObjects(JSONArray objectsJsonArray) {
        Log.d(TAG, "setObjects() "+getWidth()+","+getHeight()+" mObjects already exists = "+(mObjects !=null));
        mObjects = objectsJsonArray;
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
        Log.d(TAG, "onDraw() "+getWidth()+","+getHeight()+" mObjects="+(mObjects !=null));
        super.onDraw(canvas);

        if(mObjects == null) {
            return;
        }

        Log.d(TAG, "mObjects.length()="+ mObjects.length());

        int totalObjects = mObjects.length();
        if (totalObjects > colors.length) {
            // In testing, this is super-unlikely. 6 is about the most objects I've seen at once.
            Log.w(TAG, "Total number of objects supported is " + colors.length + ". " +
                    "Received " + totalObjects + ". Dropping extras.");
            totalObjects = colors.length;
        }
        try {
            for (int i = 0; i < totalObjects; i++) {
                JSONObject object = mObjects.getJSONObject(i);
                Log.d(TAG, i + " object=" + object);
                JSONArray jsonRect = object.getJSONArray("rect");
                Rect rect = new Rect();
                rect.left = jsonRect.getInt(0);
                rect.top = jsonRect.getInt(1);
                rect.right = jsonRect.getInt(2);
                rect.bottom = jsonRect.getInt(3);
                Log.d(TAG, "received rect=" + rect.toShortString());

                rect.left *= mServerToDisplayRatioX;
                rect.right *= mServerToDisplayRatioX;
                rect.top *= mServerToDisplayRatioY;
                rect.bottom *= mServerToDisplayRatioY;

                if (mMirrored) {
                    Log.d(TAG, "Mirroring!");
                    // The image that was processed is what the camera sees, but the image we want to
                    // overlay the rectangle onto is mirrored. So not only do we have to scale it,
                    // but we have to flip it horizontally.
                    rect.left = mWidth - rect.left;
                    rect.right = mWidth - rect.right;
                    int tmp = rect.left;
                    rect.left = rect.right;
                    rect.right = tmp;
                }

                Log.d(TAG, "jsonRect=" + jsonRect + " scaled rect=" + rect.toShortString());
                rect.offset(mWidthOff, mHeightOff);
                canvas.drawRect(rect, mFillPaints.get(i));
                canvas.drawRect(rect, mPaints.get(i));

                String className = object.getString("class");
                String confidence = object.getString("confidence");
                float confVal = Float.parseFloat(confidence)*100;
                String label = className+" "+String.format("%.1f", confVal)+"%";
                canvas.drawText(label, rect.left, rect.top+mTextSize, mTextPaint);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
