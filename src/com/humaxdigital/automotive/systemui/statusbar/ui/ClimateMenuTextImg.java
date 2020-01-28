package com.humaxdigital.automotive.systemui.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.util.Log; 

import java.util.HashMap;
import java.util.Objects; 

import com.humaxdigital.automotive.systemui.R;

public class ClimateMenuTextImg extends LinearLayout {
    private Context mContext;
    private ImageView mImageView;
    private TextView mTextView;
    private int mStatus = 0; 
    private String mText = "0";
    private HashMap<Integer,Drawable> mIcons= new HashMap<>();
    private Drawable mDisableIcon; 
    private Boolean mDisable = false; 

    public ClimateMenuTextImg(Context context) {
        super(context);
        mContext = Objects.requireNonNull(context);
    }

    public ClimateMenuTextImg inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.climate_menu_text_img, this, true);
        mImageView = (ImageView)this.findViewById(R.id.img);
        mTextView = (TextView)this.findViewById(R.id.text);
        refresh();
        return this;
    }

    public ClimateMenuTextImg addIcon(int status, Drawable icon) {
        mIcons.put(status, icon);
        return this;
    }

    public ClimateMenuTextImg addDisableIcon(Drawable icon) {
        mDisableIcon = icon; 
        return this;
    }

    public ClimateMenuTextImg update(int status, String text) {
        mText = text;
        mStatus = status; 
        refresh();
        return this;
    }

    public ClimateMenuTextImg updateDisable(boolean disable) {
        mDisable = disable; 

        if ( mTextView == null || mImageView == null ) return this; 
        if ( mDisable ) {
            if ( mDisableIcon != null ) mImageView.setImageDrawable(mDisableIcon);
            mTextView.setTextColor(mContext.getResources().getColor(R.color.climateTextDis)); 
        } else {
            mImageView.setImageDrawable(mIcons.get(mStatus));
            mTextView.setTextColor(mContext.getResources().getColor(R.color.ClimateTextNor)); 
        }
        refresh(); 
        return this;
    }

    private void refresh() {
        if ( mImageView != null && mIcons.size() > 0 ) {
            if ( mDisable ) {
                if ( mDisableIcon != null ) {
                    mImageView.setImageDrawable(mDisableIcon);
                }
            }
            else {
                mImageView.setImageDrawable(mIcons.get(mStatus));
            }
        }
        
        if ( mTextView == null ) return; 
        mTextView.setText(mText);
        if ( mDisable ) mTextView.setTextColor(mContext.getResources().getColor(R.color.climateTextDis)); 
        else mTextView.setAlpha(1.0f); 
    }
}