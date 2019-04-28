package com.mobiledgex.computervision;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a set of body pose "skeleton" coordinates in multiple colors.
 */
public class PoseRenderer extends View {
    private static final String TAG = "PoseRenderer";
    public static final int RADIUS = 18;
    public static final int STROKE_WIDTH = 15;
    private JSONArray mPoses;
    private Paint mPaint;
    private int mWidth;
    private int mHeight;
    private int mWidthOff;
    private int mHeightOff;
    private float mServerToDisplayRatioX;
    private float mServerToDisplayRatioY;
    private boolean mMirrored;

    /**
     * The pairs array is a 2D list of the body parts that should be connected together.
     * E.g., 1 for "Neck", 2 for "RShoulder", etc.
     * See https://github.com/CMU-Perceptual-Computing-Lab/openpose/blob/master/doc/output.md
     */
    private int[][] pairs = {
            {1,8},{1,2},{1,5},{2,3},{3,4},{5,6},{6,7},{8,9},{9,10},{10,11},{8,12},
            {12,13},{13,14},{1,0},{0,15},{15,17},{0,16},{16,18},{14,19},
            {19,20},{14,21},{11,22},{22,23},{11,24}
    };

    /**
     * Colors array corresponding to the "pairs" array above. For example, the first pair of
     * coordinates {1,8} will be drawn with the first color in this array, "#ff0055".
     */
    private String[] colors = {
            "#ff0055", "#ff0000", "#ff5500", "#ffaa00", "#ffff00", "#aaff00", "#55ff00", "#00ff00",
            "#ff0000", "#00ff55", "#00ffaa", "#00ffff", "#00aaff", "#0055ff", "#0000ff", "#ff00aa",
            "#aa00ff", "#ff00ff", "#5500ff", "#0000ff", "#0000ff", "#0000ff", "#00ffff", "#00ffff",
            "#00ffff"};

    private List<Paint> paints = new ArrayList<>();

    public PoseRenderer(Context context) {
        super(context);
        init(context);
    }

    public PoseRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        for(int i = 0; i < colors.length; i++) {
            mPaint = new Paint();
            mPaint.setColor(Color.parseColor(colors[i]));
            mPaint.setStrokeWidth(STROKE_WIDTH);
            mPaint.setStyle(Paint.Style.FILL);
            paints.add(mPaint);
        }
    }

    /**
     * Sets the array of detected pose skeleton coordinates.
     * @param posesJsonArray The array of detected pose skeleton coordinates.
     */
    public void setPoses(JSONArray posesJsonArray) {
        Log.d(TAG, "setPoses() "+getWidth()+","+getHeight()+" mPoses already exists = "+(mPoses !=null));
        mPoses = posesJsonArray;
    }

    /**
     * Sets display parameters used to determine ratios and offsets to correctly draw the pose skeletons.
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

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw() "+getWidth()+","+getHeight()+" mPoses="+(mPoses !=null));
        super.onDraw(canvas);

        if(mPoses == null) {
            return;
        }

        Log.d(TAG, "mPoses.length()="+ mPoses.length());

        int totalPoses = mPoses.length();
        JSONArray pose;
        try {
            for(int i = 0; i < totalPoses; i++) {
                pose = mPoses.getJSONArray(i);
                Log.d(TAG, i+" pose="+pose);
                for(int j = 0; j < pairs.length; j++) {
                    int[] pair = pairs[j];
                    int indexStart = pair[0];
                    int indexEnd = pair[1];
                    Log.d(TAG, "indexStart="+indexStart+" indexEnd="+indexEnd);

                    JSONArray keypoint1 = pose.getJSONArray(indexStart);
                    float x1 = (float) keypoint1.getDouble(0) * mServerToDisplayRatioX;
                    float y1 = (float) keypoint1.getDouble(1) * mServerToDisplayRatioY;
                    float score1 = (float) keypoint1.getDouble(2);

                    JSONArray keypoint2 = pose.getJSONArray(indexEnd);
                    float x2 = (float) keypoint2.getDouble(0) * mServerToDisplayRatioX;
                    float y2 = (float) keypoint2.getDouble(1) * mServerToDisplayRatioY;
                    float score2 = (float) keypoint2.getDouble(2);

                    if(score1 == 0 || score2 == 0) {
                        continue;
                    }

                    Log.d(TAG, "Drawing indexStart="+indexStart+" indexEnd="+indexEnd+" ("+x1+","+y1+","+x2+","+y2+")");
                    if(mMirrored) {
                        x1 = mWidth - x1;
                        x2 = mWidth - x2;
                    }

                    // Only add the offsets after everything else has been calculated.
                    x1 += mWidthOff;
                    x2 += mWidthOff;
                    y1 += mHeightOff;
                    y2 += mHeightOff;

                    canvas.drawLine(x1, y1, x2, y2, paints.get(j));
                    canvas.drawCircle(x1, y1, RADIUS, paints.get(j));
                    canvas.drawCircle(x2, y2, RADIUS, paints.get(j));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
