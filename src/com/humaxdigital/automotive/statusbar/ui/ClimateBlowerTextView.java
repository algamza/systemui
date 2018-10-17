package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;


public class ClimateBlowerTextView extends AppCompatTextView {
    private String mCount;

    public ClimateBlowerTextView(Context context) {
        super(context);
    }

    public ClimateBlowerTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClimateBlowerTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void update(String text) {
        mCount = text;
        setText(text);
    }
}
