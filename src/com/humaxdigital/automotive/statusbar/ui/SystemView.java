package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.humaxdigital.automotive.statusbar.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class SystemView extends FrameLayout {
    private class SystemViewAnimation extends TimerTask {
        private ArrayList<Drawable> mIcons;
        private ImageView mParent;
        private int mStatus = 0;

        public SystemViewAnimation(ArrayList<Drawable> icons) {
            this.mIcons = icons;
        }

        public void setParentView(ImageView view) {
            this.mParent = view;
        }

        @Override
        public void run() {
            if ( this.mParent == null || this.mIcons.size() == 0 ) return;
            if ( ++this.mStatus >= this.mIcons.size() ) this.mStatus = 0;
            this.mParent.setImageDrawable(this.mIcons.get(this.mStatus));
        }
    }

    private Context mContext;
    private ImageView mView;
    private int mStatus = 0;
    private HashMap<Integer,Drawable> mIcons = new HashMap<>();
    private HashMap<Integer, SystemViewAnimation> mAnimationIcons = new HashMap<>();
    private Timer mTimer = new Timer();

    public SystemView(Context context) {
        super(context);
        mContext = context;
    }

    public SystemView inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.system_menu, this, true);
        mView = (ImageView)this.findViewById(R.id.system_menu);
        if ( mView != null ) mView.setImageDrawable(mIcons.get(mStatus));
        for ( int key : mAnimationIcons.keySet() ) mAnimationIcons.get(key).setParentView(mView);
        return this;
    }

    public SystemView addIcon(int status, Drawable icon) {
        mIcons.put(status, icon);
        return this;
    }

    public SystemView addIconAnimation(int status, ArrayList<Drawable> icons) {
        SystemViewAnimation task = new SystemViewAnimation(icons);
        mAnimationIcons.put(status, task);
        return this;
    }

    public SystemView update(int status) {
        SystemViewAnimation task =  mAnimationIcons.get(mStatus);
        if ( task != null ) task.cancel();

        Drawable drawable = mIcons.get(status);
        if ( drawable != null ) mView.setImageDrawable(mIcons.get(status));
        else {
            SystemViewAnimation ani = mAnimationIcons.get(status);
            if ( ani != null ) mTimer.schedule(ani, 0, 1000);
        }
        mStatus = status;
        return this;
    }
}
