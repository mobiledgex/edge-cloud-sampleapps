package com.mobiledgex.sdkdemo.cv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class PoseRenderer extends View {
    private static final String TAG = "PoseSkeleton";
    public static final int RADIUS = 18;
    public static final int STROKE_WIDTH = 15;
    JSONArray poses;
    Paint paint;
    private int width;
    private int height;
    private int widthOff;
    private int heightOff;
    private float serverToDisplayRatioX;
    private float serverToDisplayRatioY;
    private boolean mirrored;

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

    public PoseRenderer(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        for(int i = 0; i < colors.length; i++) {
            paint = new Paint();
            paint.setColor(Color.parseColor(colors[i]));
            paint.setStrokeWidth(STROKE_WIDTH);
            paint.setStyle(Paint.Style.FILL);
            paints.add(paint);
        }
    }

    public void setPoses(JSONArray posesJsonArray) {
        Log.i(TAG, "setPoses() "+getWidth()+","+getHeight()+" poses already exists = "+(poses!=null));
        poses = posesJsonArray;
    }

    public void setDisplayParms(int width, int height, int widthOff, int heightOff,
                                float serverToDisplayRatioX, float serverToDisplayRatioY, boolean mirrored) {
        this.width = width;
        this.height = height;
        this.widthOff = widthOff;
        this.heightOff = heightOff;
        this.serverToDisplayRatioX = serverToDisplayRatioX;
        this.serverToDisplayRatioY = serverToDisplayRatioY;
        this.mirrored = mirrored;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i(TAG, "onDraw() "+getWidth()+","+getHeight()+" poses="+(poses!=null));
        super.onDraw(canvas);

        if(poses == null) {
            return;
        }

        Log.i(TAG, "poses.length()="+poses.length());

        int totalPoses = poses.length();
        JSONArray pose;
        try {
            for(int i = 0; i < totalPoses; i++) {
                pose = poses.getJSONArray(i);
                Log.d(TAG, i+" pose="+pose);
                for(int j = 0; j < pairs.length; j++) {
                    int[] pair = pairs[j];
                    int indexStart = pair[0];
                    int indexEnd = pair[1];
                    Log.i(TAG, "indexStart="+indexStart+" indexEnd="+indexEnd);

                    JSONArray keypoint1 = pose.getJSONArray(indexStart);
                    float x1 = (float) keypoint1.getDouble(0) * serverToDisplayRatioX + widthOff;
                    float y1 = (float) keypoint1.getDouble(1) * serverToDisplayRatioY + heightOff;
                    float score1 = (float) keypoint1.getDouble(2);

                    JSONArray keypoint2 = pose.getJSONArray(indexEnd);
                    float x2 = (float) keypoint2.getDouble(0) * serverToDisplayRatioX + widthOff;
                    float y2 = (float) keypoint2.getDouble(1) * serverToDisplayRatioY + heightOff;
                    float score2 = (float) keypoint2.getDouble(2);

                    if(score1 == 0 || score2 == 0) {
                        continue;
                    }

                    Log.i(TAG, "Drawing indexStart="+indexStart+" indexEnd="+indexEnd+" ("+x1+","+y1+","+x2+","+y2+")");
                    if(mirrored) {
                        x1 = width - x1;
                        x2 = width - x2;
                    }
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
