package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class ClimateText extends AppCompatTextView {
    private String mText;

    public ClimateText(Context context) {
        super(context);
    }

    public ClimateText(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClimateText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void update(String text) {
        mText = text;
        setText(text);
    }
}
