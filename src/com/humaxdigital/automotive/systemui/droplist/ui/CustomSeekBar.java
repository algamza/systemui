package com.humaxdigital.automotive.systemui.droplist.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

public class CustomSeekBar extends AppCompatSeekBar {
    final private int COLOR_BACKGROUND = 0xff293342; 
    final private int COLOR_PROGRESS = 0xffffffff; 
    final private int SEEKBAR_HEIGHT = 10;
    final private int SEEKBAR_RADIUS = 8; 

    public enum HIGHLIGHT_TYPE {
        HIGHLIGHT_CENTER,
        HIGHLIGHT_RIGHT,
        HIGHLIGHT_LEFT
    }

    private Rect rect;
    private Paint paint;
    private HIGHLIGHT_TYPE mHighlightType = HIGHLIGHT_TYPE.HIGHLIGHT_CENTER; 

    public CustomSeekBar(Context context) {
        super(context);

    }

    public CustomSeekBar(Context context, AttributeSet attrs) {

        super(context, attrs);
        rect = new Rect();
        paint = new Paint();
    }

    public CustomSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setHighlightType(HIGHLIGHT_TYPE type) {
        mHighlightType = type; 
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        int max = this.getMax(); 
        int min = this.getMin(); 
        int range = max - min; 
        int center = (max+min)/2; 

        rect.set(0 + getThumbOffset(),
                (getHeight() / 2) - (SEEKBAR_HEIGHT/2),
                getWidth()- getThumbOffset(),
                (getHeight() / 2) + (SEEKBAR_HEIGHT/2));
        paint.setColor(COLOR_BACKGROUND);
        RectF rectf = new RectF(rect);
        canvas.drawRoundRect(rectf, SEEKBAR_RADIUS, SEEKBAR_RADIUS, paint);

        if ( mHighlightType == HIGHLIGHT_TYPE.HIGHLIGHT_RIGHT ) {
            rect.set(0,
                (getHeight() / 2) - (SEEKBAR_HEIGHT/2),
                getWidth() / 2 + (getWidth() / (range)) * (getProgress() - center),
                getHeight() / 2 + (SEEKBAR_HEIGHT/2));
                paint.setColor(COLOR_PROGRESS);
                canvas.drawRect(rect, paint);
        } else {
            if (this.getProgress() > center) {
                rect.set(getWidth() / 2,
                        (getHeight() / 2) - (SEEKBAR_HEIGHT/2),
                        getWidth() / 2 + (getWidth() / range) * (getProgress() - center),
                        getHeight() / 2 + (SEEKBAR_HEIGHT/2));
                paint.setColor(COLOR_PROGRESS);
                canvas.drawRect(rect, paint);
            }
    
            if (this.getProgress() < center) {
                rect.set(getWidth() / 2 - ((getWidth() / range) * (center - getProgress())),
                        (getHeight() / 2) - (SEEKBAR_HEIGHT/2),
                        getWidth() / 2,
                        getHeight() / 2 + (SEEKBAR_HEIGHT/2));
                paint.setColor(COLOR_PROGRESS);
                canvas.drawRect(rect, paint);
            }
        }
        super.onDraw(canvas);
    }
}