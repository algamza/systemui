package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.HashMap;

public class ClimateTemperatureView extends TextView {
    private String mTemperture;

    public ClimateTemperatureView(Context context) {
        super(context);
    }

    public ClimateTemperatureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClimateTemperatureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ClimateTemperatureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void update(String text) {
        mTemperture = text;
        setText(text);
    }
}
