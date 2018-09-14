package com.mobiledgex.sdkdemo.com.mobiledgex.sdkdemo.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BoundingBox extends View
{
    Paint paint;
    public Rect rect;

    public BoundingBox(Context context)
    {
        super(context);
        init();
    }

    public BoundingBox(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    private void init()
    {
        paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
        rect = new Rect(0, 0, 0, 0);
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
}
