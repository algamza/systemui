package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ClimateBlowerView extends ImageView {
    private Drawable mIcon;
    private String mPan;

    public ClimateBlowerView(Context context) {
        super(context);
    }

    public ClimateBlowerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClimateBlowerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setIcon(Drawable drawable) {
        mIcon = drawable;
        setImageDrawable(mIcon);
    }

    public void update(String text) {
        // text;
        mPan = text;
    }
}
