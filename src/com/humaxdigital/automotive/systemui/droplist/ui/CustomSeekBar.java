package com.humaxdigital.automotive.systemui.droplist.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

import android.util.Log;

public class CustomSeekBar extends AppCompatSeekBar {
    private static final String TAG = "CustomSeekBar"; 
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
        int top = 0; 
        int left =0; 
        int right = 0; 
        int bottom = 0; 
        int progress = getProgress(); 
        int height = getHeight(); 
        int width = getWidth(); 
        int offset = getThumbOffset(); 

        rect.set(0 + offset,
                (height / 2) - (SEEKBAR_HEIGHT/2),
                width- offset,
                (height / 2) + (SEEKBAR_HEIGHT/2));
        paint.setColor(COLOR_BACKGROUND);
        RectF rectf = new RectF(rect);
        canvas.drawRoundRect(rectf, SEEKBAR_RADIUS, SEEKBAR_RADIUS, paint);

        if ( mHighlightType == HIGHLIGHT_TYPE.HIGHLIGHT_RIGHT ) {
            top = 0; 
            left = (height / 2) - (SEEKBAR_HEIGHT/2); 
            right = width / 2 + (width / (range+1)) * (progress - center) - 7; 
            bottom = height / 2 + (SEEKBAR_HEIGHT/2); 
        } else if (progress > center) {
            top = width / 2; 
            left = (height / 2) - (SEEKBAR_HEIGHT/2); 
            right = width / 2 + (width / range) * (progress - center); 
            bottom = height / 2 + (SEEKBAR_HEIGHT/2); 

        } else {
            top = (width / 2) - ((width / range) * (center - progress)); 
            left = (height / 2) - (SEEKBAR_HEIGHT/2); 
            right = width / 2; 
            bottom = height / 2 + (SEEKBAR_HEIGHT/2); 
        }

        //Log.d(TAG, "type="+mHighlightType+", center="+center+", range="+range+", progress="+progress+", width="+width+", height="+height+", offset="+offset+", top="+top+", left="+left+", right="+right+", bottom="+bottom); 

        rect.set(top, left, right, bottom);
        paint.setColor(COLOR_PROGRESS);
        canvas.drawRect(rect, paint);

        super.onDraw(canvas);
    }
}