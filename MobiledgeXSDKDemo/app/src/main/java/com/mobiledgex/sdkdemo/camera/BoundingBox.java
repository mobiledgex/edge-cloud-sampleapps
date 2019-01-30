package com.mobiledgex.sdkdemo.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.mobiledgex.sdkdemo.R;

public class BoundingBox extends View
{
    String subject = null;
    Camera2BasicFragment.CloudLetType cloudLetType;
    int textSize = (int) (getResources().getDisplayMetrics().scaledDensity*20);
    Paint paint;
    Paint textPaint;
    enum ShapeType {
        RECT,
        OVAL
    }
    public ShapeType shapeType = ShapeType.RECT;
    public Rect rect;
    public Animation alphaAnim;

    public BoundingBox(Context context)
    {
        super(context);
        init(context);
    }

    public BoundingBox(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    private void init(Context context)
    {
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
        textPaint = new Paint();
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.BLUE);
        rect = new Rect(0, 0, 0, 0);

        alphaAnim = AnimationUtils.loadAnimation(context, R.anim.alpha);
        alphaAnim.setFillAfter(true);
        startAnimation(alphaAnim);
    }

    public void setColor(int color) {
        paint.setColor(color);
        textPaint.setColor(color);
    }

    public void setCloudletType(Camera2BasicFragment.CloudLetType type) {
        cloudLetType = type;
        if(cloudLetType == Camera2BasicFragment.CloudLetType.EDGE) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
        } else if(cloudLetType == Camera2BasicFragment.CloudLetType.CLOUD) {
            textPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(shapeType == ShapeType.RECT) {
            canvas.drawRect(rect, paint);
        } else {
            canvas.drawOval(new RectF(rect), paint);
        }
        if (subject != null) {
            int x = 0;
            int y = 0;
            if(cloudLetType == Camera2BasicFragment.CloudLetType.EDGE) {
                x = rect.right;
                y = rect.bottom+textSize;
            } else if(cloudLetType == Camera2BasicFragment.CloudLetType.CLOUD) {
                x = rect.left;
                y = rect.top-textSize/2;
            }
            canvas.drawText(subject, x, y, textPaint);
        }
    }

    public void restartAnimation() {
        startAnimation(alphaAnim);
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
