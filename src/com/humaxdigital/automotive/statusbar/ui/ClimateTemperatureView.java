package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class ClimateTemperatureView extends AppCompatTextView {
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

    public void update(String text) {
        mTemperture = text;
        setText(text);
    }
}
