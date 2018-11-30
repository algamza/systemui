package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;

import com.humaxdigital.automotive.statusbar.R;

public class ClimateMenuTextImg extends LinearLayout {
    private Context mContext;
    private ImageView mImageView;
    private TextView mTextView;
    private int mStatus = 0; 
    private String mText = "";
    private HashMap<Integer,Drawable> mIcons= new HashMap<>();

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

    public ClimateMenuTextImg update(int status, String text) {
        mText = text;
        mStatus = status; 
        refresh();
        return this;
    }

    public ClimateMenuTextImg setTextColor(int color) {
        if ( mTextView == null ) return this;
        mTextView.setTextColor(color);
        return this;
    }

    private void refresh() {
        if ( mImageView != null && mIcons.size() > 0 ) 
            mImageView.setImageDrawable(mIcons.get(mStatus));
        if ( mTextView != null ) mTextView.setText(mText);
    }
}