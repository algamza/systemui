package com.humaxdigital.automotive.systemui.statusbar.ui;

import android.os.Handler;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.humaxdigital.automotive.systemui.R;

import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Objects; 

import android.util.Log; 

public class SystemView extends FrameLayout {
    private class SystemViewAnimation extends TimerTask {
        private ArrayList<Drawable> mIcons;
        private ImageView mParent;
        private int mStatus = 0;
        private Handler mHandler; 
        public SystemViewAnimation(ArrayList<Drawable> icons, Handler handler) {
            this.mIcons = icons;
            mHandler = handler; 
        }

        public void setParentView(ImageView view) {
            this.mParent = view;
        }

        @Override
        public void run() {
            if ( this.mParent == null || this.mIcons.size() == 0 ) return;
            if ( ++this.mStatus >= this.mIcons.size() ) this.mStatus = 0;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateImage(); 
                }
            }); 
        }

        private void updateImage() {
            this.mParent.setImageDrawable(this.mIcons.get(this.mStatus));
        }
    }

    private Context mContext;
    private ImageView mView;
    private int mStatus = 0;
    private WeakHashMap<Integer,Drawable> mIcons = new WeakHashMap<>();
    private WeakHashMap<Integer, ArrayList<Drawable>> mAnimationIcons = new WeakHashMap<>();
    private SystemViewAnimation mTask = null; 
    private Timer mTimer = new Timer();
    private Handler mHandler; 
    

    public SystemView(Context context) {
        super(context);
        mContext = Objects.requireNonNull(context);
        mHandler = new Handler(mContext.getMainLooper());
    }

    public SystemView inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.system_menu, this, true);
        mView = (ImageView)this.findViewById(R.id.system_menu);
        if ( mView != null && mIcons.size() > 0 ) mView.setImageDrawable(mIcons.get(mStatus));
        return this;
    }

    public SystemView addIcon(int status, Drawable icon) {
        mIcons.put(status, icon);
        return this;
    }

    public SystemView addIconAnimation(int status, ArrayList<Drawable> icons) {
        mAnimationIcons.put(status, icons);
        return this;
    }

    public void update(int status) {
        ArrayList<Drawable> icons = mAnimationIcons.get(mStatus);
        if ( icons != null && icons.size() != 0 ) {
            if ( mTask != null ) {
                mTask.cancel(); 
                mTimer.purge(); 
                mTask = null; 
            }
        }

        Drawable drawable = mIcons.get(status);
        if ( drawable != null ) {
            mView.setImageDrawable(drawable);
        }
        else {
            ArrayList<Drawable> _icons = mAnimationIcons.get(status);
            if ( _icons != null && _icons.size() != 0 && mTask == null ) {
                mTask = new SystemViewAnimation(_icons, mHandler);
                mTask.setParentView(mView);
                mTimer.schedule(mTask, 0, 1000);
            }
        }
        mStatus = status;
        
        if ( mStatus == 0 ) {
            ((View)this.getParent()).setVisibility(View.GONE); 
            //this.setVisibility(View.GONE); 
        }
        else {
            ((View)this.getParent()).setVisibility(View.VISIBLE); 
        }

        return;
    }
}
