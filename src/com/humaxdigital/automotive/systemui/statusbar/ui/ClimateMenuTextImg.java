package com.humaxdigital.automotive.systemui.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.util.Log; 

import java.util.HashMap;

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
    private Boolean mStateDisable = false; 

    public ClimateMenuTextImg(Context context) {
        super(context);
        mContext = context;
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

    public ClimateMenuTextImg update(int status, Boolean disable, String text) {
        mText = text;
        mStatus = status; 
        mStateDisable = disable; 
        refresh();
        return this;
    }

    public ClimateMenuTextImg updateDisable(boolean disable) {
        mDisable = disable; 

        if ( mTextView == null || mImageView == null ) return this; 
        if ( mDisable ) {
            if ( mDisableIcon != null ) mImageView.setImageDrawable(mDisableIcon);
            mTextView.setAlpha(0.4f); 
        } else {
            mImageView.setImageDrawable(mIcons.get(mStatus));
            mTextView.setAlpha(1.0f); 
        }
        refresh(); 
        return this;
    }

    private void refresh() {
        if ( mDisable ) return; 

        Log.d("ClimateMenu", "disable="+mStateDisable+", mStatus="+mStatus); 
        if ( mImageView != null && mIcons.size() > 0 ) {
            if ( mStateDisable ) {
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
        if ( mStateDisable ) mTextView.setAlpha(0.4f); 
        else mTextView.setAlpha(1.0f); 
    }

    public Boolean isDisable() {
        return mDisable; 
    }
}