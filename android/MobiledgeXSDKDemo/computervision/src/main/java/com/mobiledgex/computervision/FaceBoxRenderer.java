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
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Draws a shape around detected faces, and if a "subject" value is specified, draws that text too.
 */
public class FaceBoxRenderer extends View {
    private static final String TAG = "FaceBoxRender";
    public static final int DEFAULT_STROKE_WIDTH = 10;
    private int mStrokeWidth = DEFAULT_STROKE_WIDTH;
    private boolean mMirrored;
    private boolean mMultiFace;
    private int mWidth;
    private int mHeight;
    private int mWidthOff;
    private int mHeightOff;
    private float mServerToDisplayRatioX;
    private float mServerToDisplayRatioY;

    private ImageServerInterface.CloudletType mCloudletType;
    private JSONArray rectJsonArray;
    private String mSubject = null;
    private int mTextSize = (int) (getResources().getDisplayMetrics().scaledDensity * 20);
    private Paint mPaint;
    private Paint mTextPaint;

    public enum ShapeType {
        RECT,
        OVAL
    }

    private ShapeType mShapeType = ShapeType.RECT;
    private Animation mAlphaAnim;

    public FaceBoxRenderer(Context context) {
        super(context);
    }

    public FaceBoxRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mTextPaint = new Paint();
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(Color.BLUE);

        mAlphaAnim = AnimationUtils.loadAnimation(context, R.anim.alpha);
        mAlphaAnim.setFillAfter(true);
        startAnimation(mAlphaAnim);
    }

    /**
     * Sets the color used to draw the face coordinates and the subject name.
     * @param color  The color to be used.
     */
    public void setColor(int color) {
        mPaint.setColor(color);
        mTextPaint.setColor(color);
    }

    /**
     * Sets the cloudlet type. Used to determine where to draw the subject name.
     * @param type  The cloudlet type.
     * @see com.mobiledgex.computervision.ImageServerInterface.CloudletType
     */
    public void setCloudletType(ImageServerInterface.CloudletType type) {
        mCloudletType = type;
        if (mCloudletType == ImageServerInterface.CloudletType.EDGE) {
            mTextPaint.setTextAlign(Paint.Align.RIGHT);
        } else if (mCloudletType == ImageServerInterface.CloudletType.CLOUD) {
            mTextPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    /**
     * Sets display parameters used to determine ratios and offsets to correctly draw the face coordinates.
     * @param imageRect  The coordinates of the TextureView that is showing the preview image.
     * @param serverToDisplayRatioX  The ratio of the width of the image sent to the server vs.
     *                               the width of the preview image being displayed.
     * @param serverToDisplayRatioY  The ratio of the height of the image sent to the server vs.
     *                               the height of the preview image being displayed.
     * @param mirrored  Whether the preview image is being mirrored.
     * @param multiFace  Whether multiple faces should be drawn if coordinates are received.
     */
    public void setDisplayParms(Rect imageRect, float serverToDisplayRatioX, float serverToDisplayRatioY,
                                boolean mirrored, boolean multiFace) {
        mWidth = imageRect.width();
        mHeight = imageRect.height();
        mWidthOff = imageRect.left;;
        mHeightOff = imageRect.top;
        mServerToDisplayRatioX = serverToDisplayRatioX;
        mServerToDisplayRatioY = serverToDisplayRatioY;
        mMirrored = mirrored;
        mMultiFace = multiFace;
    }

    /**
     * Reset the fade-out animation back to the beginning.
     */
    public void restartAnimation() {
        startAnimation(mAlphaAnim);
    }

    /**
     * Sets the shape type.
     * @param shapeType  The shape type. Can be either RECT or OVAL.
     */
    public void setShapeType(ShapeType shapeType) {
        this.mShapeType = shapeType;
    }

    /**
     * Sets the array of detected face coordinates.
     *
     * @param rectJsonArray  The array of detected face coordinates.
     * @param subject  The identified subject name. Use empty string to draw no text.
     */
    public void     setRectangles(JSONArray rectJsonArray, String subject) {
        this.rectJsonArray = rectJsonArray;
        this.mSubject = subject;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i(TAG, "onDraw() " + getWidth() + "," + getHeight() + " rectJsonArray=" + (rectJsonArray != null));
        super.onDraw(canvas);

        if (rectJsonArray == null) {
            return;
        }

        int i;
        int totalFaces;
        JSONArray jsonRect;
        if(mMultiFace) {
            totalFaces = rectJsonArray.length();
        } else {
            totalFaces = 1;
        }
        for(i = 0; i < totalFaces; i++) {
            try {
                jsonRect = rectJsonArray.getJSONArray(i);
                Rect rect = new Rect();
                rect.left = jsonRect.getInt(0);
                rect.top = jsonRect.getInt(1);
                rect.right = jsonRect.getInt(2);
                rect.bottom = jsonRect.getInt(3);
                Log.d(TAG, "received rect=" + rect.toShortString());
                if (rect.top == 0 && rect.left == 0 && rect.right == 0 && rect.bottom == 0) {
                    Log.d(TAG, "Discarding empty rectangle");
                    continue;
                }

                //In case we received the exact same coordinates from both Edge and Cloud,
                //offset only one of the rectangles so they will be distinct.
                if (mCloudletType == ImageServerInterface.CloudletType.EDGE) {
                    rect.left -= 1;
                    rect.right += 1;
                    rect.top -= 1;
                    rect.bottom += 1;
                }

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

                if(mShapeType == ShapeType.RECT) {
                    canvas.drawRect(rect, mPaint);
                } else {
                    canvas.drawOval(new RectF(rect), mPaint);
                }
                if (mSubject != null) {
                    int x = 0;
                    int y = 0;
                    if(mCloudletType == ImageServerInterface.CloudletType.EDGE) {
                        x = rect.right;
                        y = rect.bottom+ mTextSize;
                    } else if(mCloudletType == ImageServerInterface.CloudletType.CLOUD) {
                        x = rect.left;
                        y = rect.top- mTextSize /2;
                    }
                    canvas.drawText(mSubject, x, y, mTextPaint);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Set the stroke width for drawing the pose skeleton.
     *
     * @param strokeWidth
     */
    public void setStrokeWidth(int strokeWidth) {
        mStrokeWidth = strokeWidth;
    }
}
