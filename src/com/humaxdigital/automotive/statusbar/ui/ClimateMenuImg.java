package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.humaxdigital.automotive.statusbar.R;

import java.util.HashMap;

public class ClimateMenuImg extends LinearLayout {
    private Context mContext;
    private int mStatus = 0;
    private ImageView mView;
    private HashMap<Integer,Drawable> mIcons= new HashMap<>();

    public ClimateMenuImg(Context context) {
        super(context);
        mContext = context;
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

    public ClimateMenuImg update(int status) {
        if ( mView == null || mIcons.size() <= 0 ) return this;
        mView.setImageDrawable(mIcons.get(status));
        return this;
    }
}