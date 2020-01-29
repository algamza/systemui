package com.humaxdigital.automotive.systemui.statusbar.ui;

import android.content.Context;
import android.os.Handler;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.util.Log; 

import com.humaxdigital.automotive.systemui.R;

import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.Objects; 

public class ClimateMenuImgTimeout extends LinearLayout {
    private Context mContext;
    private int mStatus = 0;
    private ImageView mView;
    private WeakHashMap<Integer,DrawableTimeout> mIcons= new WeakHashMap<>();
    private ArrayList<ClimateDrawableTimout> mListeners = new ArrayList<>();
    private Runnable mRunnable;
    private Handler mHandler;
    private Drawable mDisableIcon; 
    private Boolean mDisable = false;

    public interface ClimateDrawableTimout {
        public void onDrawableTimout(int status);
    }

    private class DrawableTimeout {
        public int mTimeout;
        public Drawable mImg;
        DrawableTimeout(int timeout_ms, Drawable img) {
            mTimeout = timeout_ms;
            mImg = img;
        }
    }

    public ClimateMenuImgTimeout(Context context) {
        super(context);
        mContext =  Objects.requireNonNull(context);
        mHandler = new Handler();
    }

    public ClimateMenuImgTimeout inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.climate_menu_img, this, true);
        mView = (ImageView)this.findViewById(R.id.img);
        startTimeout(); 
        return this;
    }

    public ClimateMenuImgTimeout registTimeoutListener(ClimateDrawableTimout listener) {
        mListeners.add(Objects.requireNonNull(listener));
        return this; 
    }

    public ClimateMenuImgTimeout addIcon(int status, Drawable icon, int timeout_ms) {
        mIcons.put(status, new DrawableTimeout(timeout_ms, icon));
        return this;
    }

    public ClimateMenuImgTimeout addDisableIcon(Drawable icon) {
        mDisableIcon = icon; 
        return this;
    }

    public ClimateMenuImgTimeout update(int status) {
        mStatus = status; 
        if ( !mDisable ) updateTimeout(); 
        return this;
    }

    public ClimateMenuImgTimeout updateDisable(boolean disable) { 
        mDisable = disable;  
        if ( mView == null ) return this;
        if ( disable ) {
            if ( mDisableIcon != null ) mView.setImageDrawable(mDisableIcon);
        } else {
            updateTimeout(); 
        }
        return this;
    }
    
    private void updateTimeout() {
        if ( mRunnable != null ) mHandler.removeCallbacks(mRunnable);
        startTimeout(); 
    }

    private void startTimeout() {
        if ( mView == null || mIcons.size() <= 0 ) return;
        DrawableTimeout timeout = mIcons.get(mStatus);
        if ( timeout == null ) return; 
        mView.setImageDrawable(timeout.mImg);
        if ( timeout.mTimeout == 0 ) return;
        mRunnable = new Runnable() {
            @Override
            public void run() {
                for ( ClimateDrawableTimout listener : mListeners )
                    listener.onDrawableTimout(mStatus);
            }
        };
        mHandler.postDelayed(mRunnable, timeout.mTimeout);
    }
}
