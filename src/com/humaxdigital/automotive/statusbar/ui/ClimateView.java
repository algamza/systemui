package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import java.util.HashMap;

public class ClimateView extends ImageView {
    public interface ClickListener {
        void onClicked(int state);
    }

    private ClickListener mListener;
    private int mCurrentState;
    private HashMap<Integer,Drawable> mIcons = new HashMap<>();

    public ClimateView(Context context) {
        super(context);
    }

    public ClimateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ClimateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setListener(ClickListener listener) {
        if ( mListener != null ) return;

        mListener = listener;

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( mListener != null ) mListener.onClicked(mCurrentState);
            }
        });
    }

    public void addIcon(int state, Drawable icon) {
        mIcons.put(state, icon);
    }

    public int getCurrentKey() {
        return mCurrentState;
    }

    public boolean update(int state) {
        Drawable icon = mIcons.get(state);
        if ( icon == null ) return false;
        mCurrentState = state;
        setImageDrawable(icon);
        return true;
    }
}
