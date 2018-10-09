package com.mobiledgex.sdkdemo.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.mobiledgex.sdkdemo.R;

public class BoundingBox extends View
{
    Paint paint;
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
        rect = new Rect(0, 0, 0, 0);

        alphaAnim = AnimationUtils.loadAnimation(context, R.anim.alpha);
        alphaAnim.setFillAfter(true);
        startAnimation(alphaAnim);
    }

    public void setColor(int color)
    {
        paint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        canvas.drawRect(rect, paint);
    }

    public void restartAnimation() {
        startAnimation(alphaAnim);
    }
}
