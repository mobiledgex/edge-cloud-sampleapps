package com.mobiledgex.sdkdemo.cv;

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

import com.mobiledgex.sdkdemo.R;

import org.json.JSONArray;
import org.json.JSONException;

public class FaceBoxRenderer extends View {
    private static final String TAG = "FaceBoxRender";
    private static final int MAX_FACES = 8;
    public static final int STROKE_WIDTH = 10;
    private boolean mMirrored;
    private boolean mMultiFace;
    private int mWidth;
    private int mHeight;
    private int mWidthOff;
    private int mHeightOff;
    private float mServerToDisplayRatioX;
    private float mServerToDisplayRatioY;

    ImageProcessorFragment.CloudletType mCloudletType;
    JSONArray rectJsonArray;
    String mSubject = null;
    int mTextSize = (int) (getResources().getDisplayMetrics().scaledDensity * 20);
    Paint mPaint;
    Paint mTextPaint;

    enum ShapeType {
        RECT,
        OVAL
    }

    public ShapeType shapeType = ShapeType.RECT;
    public Animation alphaAnim;

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
        mPaint.setStrokeWidth(STROKE_WIDTH);
        mPaint.setStyle(Paint.Style.STROKE);
        mTextPaint = new Paint();
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(Color.BLUE);

        alphaAnim = AnimationUtils.loadAnimation(context, R.anim.alpha);
        alphaAnim.setFillAfter(true);
        startAnimation(alphaAnim);
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        mTextPaint.setColor(color);
    }

    public void setCloudletType(ImageProcessorFragment.CloudletType type) {
        mCloudletType = type;
        if (mCloudletType == ImageProcessorFragment.CloudletType.EDGE) {
            mTextPaint.setTextAlign(Paint.Align.RIGHT);
        } else if (mCloudletType == ImageProcessorFragment.CloudletType.CLOUD) {
            mTextPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

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

    public void restartAnimation() {
        startAnimation(alphaAnim);
    }

    public void setRectangles(JSONArray rectJsonArray, String subject) {
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
            if(totalFaces > MAX_FACES) {
                totalFaces = MAX_FACES;
                Log.w(TAG, "MAX_FACES ("+MAX_FACES+") exceeded. Ignoring additional");
            }
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
                if (mCloudletType == ImageProcessorFragment.CloudletType.EDGE) {
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

                if(shapeType == ShapeType.RECT) {
                    canvas.drawRect(rect, mPaint);
                } else {
                    canvas.drawOval(new RectF(rect), mPaint);
                }
                if (mSubject != null) {
                    int x = 0;
                    int y = 0;
                    if(mCloudletType == ImageProcessorFragment.CloudletType.EDGE) {
                        x = rect.right;
                        y = rect.bottom+ mTextSize;
                    } else if(mCloudletType == ImageProcessorFragment.CloudletType.CLOUD) {
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

}
