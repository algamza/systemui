package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.HashMap;

public class ClimateAirView extends ImageView {
    private int mCurrentKey;
    private HashMap<Integer,Drawable> mIcons = new HashMap<>();

    public ClimateAirView(Context context) {
        super(context);
    }

    public ClimateAirView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClimateAirView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ClimateAirView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setIcon(int key, Drawable icon) {
        mIcons.put(key, icon);
    }

    public int getCurrentKey() {
        return mCurrentKey;
    }

    public void update(int key) {
        Drawable icon = mIcons.get(key);
        if ( icon == null ) return;
        mCurrentKey = key;
        setImageDrawable(icon);
    }
}
