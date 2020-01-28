package com.humaxdigital.automotive.systemui.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.humaxdigital.automotive.systemui.R;

import java.util.HashMap;
import java.util.Objects; 

public class ClimateMenuImg extends LinearLayout {
    private Context mContext;
    private int mStatus = 0;
    private ImageView mView;
    private HashMap<Integer,Drawable> mIcons= new HashMap<>();
    private HashMap<Integer,Drawable> mDisableIcons= new HashMap<>();
    private Drawable mDisableIcon; 
    private Boolean mDisable = false; 

    public ClimateMenuImg(Context context) {
        super(context);
        mContext = Objects.requireNonNull(context);
    }

    public ClimateMenuImg inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.climate_menu_img, this, true);
        mView = (ImageView)this.findViewById(R.id.img);
        if ( mView != null && mIcons.size() > 0 ) mView.setImageDrawable(mIcons.get(mStatus));
        return this;
    }

    public ClimateMenuImg addIcon(int status, Drawable icon) {
        mIcons.put(status, icon);
        return this;
    }

    public ClimateMenuImg addDisableIcon(int status, Drawable icon) {
        mDisableIcons.put(status, icon);
        return this;
    }

    public ClimateMenuImg addDisableIcon(Drawable icon) {
        mDisableIcon = icon; 
        return this;
    }

    public ClimateMenuImg update(int status) {
        if ( mView == null || mIcons.size() <= 0 ) return this;
        if ( !mDisable ) mView.setImageDrawable(mIcons.get(status));
        mStatus = status; 
        return this;
    }

    public ClimateMenuImg updateDisable(boolean disable) { 
        mDisable = disable; 
        if ( mView == null ) return this;
        if ( disable ) {
            if ( mDisableIcon != null ) mView.setImageDrawable(mDisableIcon);
            else mView.setImageDrawable(mDisableIcons.get(mStatus));
        } else {
            mView.setImageDrawable(mIcons.get(mStatus));
        }
        return this;
    }
}