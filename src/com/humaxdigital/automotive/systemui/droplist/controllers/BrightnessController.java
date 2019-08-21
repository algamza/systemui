package com.humaxdigital.automotive.systemui.droplist.controllers;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Rect;

import android.content.Context;
import android.content.res.Resources;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.droplist.SystemControl;
import com.humaxdigital.automotive.systemui.droplist.ui.CustomSeekBar;

import com.humaxdigital.automotive.systemui.util.ProductConfig;

public class BrightnessController implements BaseController {
    private final String TAG = "BrightnessController"; 
    private enum TYPE {
        BRIGHTNESS,
        CLUSTER_BRIGHTNESS
    }

    private final int POPUP_VIEW_OFFSET = 560; 
    private final int POPUP_VIEW_Y = 28; 
    private Context mContext;
    private SystemControl mSystem;
    private View mView;
    private CustomSeekBar mSeekbar;
    private CheckBox mCheckbox;
    private View mPopupView;
    private TextView mText;
    private TextView mCheckboxText; 
    private ImageView mImgCenter; 

    private int mBrightnessMin;
    private int mBrightnessMax;
    private int mBrightnessProgress;
    private int mBrightnessRange;

    private int mClusterBrightnessMin;
    private int mClusterBrightnessMax;
    private int mClusterBrightnessProgress;
    private int mClusterBrightnessRange;
    private boolean mClusterChecked; 
    private boolean mIsUserSeek = false; 

    public BrightnessController(Context context) {
        mContext = context;
    }

    @Override
    public BaseController init(View view) {
        if ( view == null || mContext == null ) return this;
        mView = view;

        mSeekbar = mView.findViewById(R.id.seekbar);
        mSeekbar.setOnSeekBarChangeListener(mSeekListener);
        mSeekbar.setOnTouchListener(mTouchListener);

        mCheckbox = mView.findViewById(R.id.checkbox);
        mCheckbox.setOnCheckedChangeListener(mCheckListener);

        mBrightnessMin = mContext.getResources().getInteger(R.integer.seekbar_brightness_min);
        mBrightnessMax = mContext.getResources().getInteger(R.integer.seekbar_brightness_max);
        mClusterBrightnessMax = mContext.getResources().getInteger(R.integer.seekbar_cluster_brightness_max);
        mClusterBrightnessMin = mContext.getResources().getInteger(R.integer.seekbar_cluster_brightness_min);

        mPopupView = (View)mView.findViewById(R.id.view_popup);
        mText = (TextView)mPopupView.findViewById(R.id.text_progress);
        mImgCenter = (ImageView)mView.findViewById(R.id.img_center); 

        mCheckboxText = (TextView)mView.findViewById(R.id.text_checkbox); 

        return this;
    }

    @Override
    public void fetch(SystemControl system) {
        if ( system == null || mCheckbox == null || mSeekbar == null || mImgCenter == null ) return;
        mSystem = system;
        mSystem.registerCallback(mBrightnessListener);

        mBrightnessProgress = mSystem.getBrightness();
        mClusterChecked = mSystem.getClusterCheck();
        mClusterBrightnessProgress = mSystem.getClusterBrightness();
        
        mCheckbox.setChecked(mClusterChecked);

        if ( mCheckbox.isChecked() ) {
            updateType(TYPE.CLUSTER_BRIGHTNESS, mClusterBrightnessProgress);
            mImgCenter.setVisibility(View.GONE);
            mSeekbar.setHighlightType(CustomSeekBar.HIGHLIGHT_TYPE.HIGHLIGHT_RIGHT); 
        }   
        else {
            updateType(TYPE.BRIGHTNESS, mBrightnessProgress);
            mImgCenter.setVisibility(View.VISIBLE);
            mSeekbar.setHighlightType(CustomSeekBar.HIGHLIGHT_TYPE.HIGHLIGHT_CENTER); 
        }
        Log.d(TAG, "fetch:brightness="+mBrightnessProgress+", cluster="+mClusterBrightnessProgress+", cluseter check="+mClusterChecked);
    }

    @Override
    public View getView() {
        if ( mView == null ) return null;
        return mView.findViewById(R.id.seekbar_control);
    }
    
    @Override
    public BaseController setListener(Listener listener) { 
        return this; 
    }

    @Override
    public void refresh(Context context) {
        if ( context == null || mCheckboxText == null ) return;
        Resources res = context.getResources();
        mCheckboxText.setText(res.getString(R.string.STR_MESG_14967_ID));
    }

    public void clear() {
        if ( mPopupView != null ) mPopupView.setVisibility(View.GONE);
    }

    private void updateType(TYPE type, int progress) {
        Log.d(TAG, "updateType:type="+type+", progress="+progress);
        switch(type) {
            case BRIGHTNESS:
            {
                mSeekbar.setMax(mBrightnessMax);
                mSeekbar.setMin(mBrightnessMin);
                mSeekbar.setProgress(convertBrightnessToLevel(progress, mBrightnessMin, mBrightnessMax));
                break;
            }
            case CLUSTER_BRIGHTNESS:
            {
                mSeekbar.setMax(mClusterBrightnessMax);
                mSeekbar.setMin(mClusterBrightnessMin);
                mSeekbar.setProgress(convertValidClusterBrightness(progress));
                break;
            }
            default: break;
        }
    }

    private int convertValidClusterBrightness(int progress) {
        if ( progress > mClusterBrightnessMax ) return mClusterBrightnessMax; 
        if ( progress < mClusterBrightnessMin ) return mClusterBrightnessMin; 
        return progress; 
    }

    private int convertBrightnessToLevel(int brightness, int levelMin, int levelMax) {
        int range = levelMax - levelMin;
        if ( range <= 0 ) return 0;
        float val = (range/10.0f)*brightness;
        int ret = (int)(val + levelMin);
        Log.d(TAG, "convertBrightnessToLevel="+ret);
        return ret;
    }

    private int convertLevelToBrightness(int level, int levelMin, int levelMax) {
        int range = levelMax - levelMin;
        if ( range <= 0 ) return 0;
        float val = (10.0f/range)*(level-levelMin);
        Log.d(TAG, "convertLevelToBrightness="+val);
        return (int)val;
    }

    private final SystemControl.SystemCallback mBrightnessListener =
            new SystemControl.SystemCallback() {
        @Override
        public void onBrightnessChanged(int brightness) {
            if ( !mCheckbox.isChecked() ) {
                mBrightnessProgress = brightness;
                int progress = convertBrightnessToLevel(brightness, mBrightnessMin, mBrightnessMax); 
                mSeekbar.setProgress(progress);
                Log.d(TAG, "onBrightnessChanged="+progress+", brightness="+brightness);
            } else {
                mBrightnessProgress = brightness;
            }
        }
        @Override
        public void onClusterBrightnessChanged(int brightness) {
            if ( mCheckbox.isChecked() ) {
                mClusterBrightnessProgress = convertValidClusterBrightness(brightness); 
                mSeekbar.setProgress(mClusterBrightnessProgress);
                Log.d(TAG, "onClusterBrightnessChanged="+mClusterBrightnessProgress);
            } else {
                mClusterBrightnessProgress = brightness;
            }
        }
        @Override
        public void onClusterChecked(boolean checked) {
            if ( mCheckbox == null || mImgCenter == null || mSeekbar == null ) return;
            mCheckbox.setChecked(checked); 
            mClusterChecked = checked; 
            if ( checked ) {
                mImgCenter.setVisibility(View.GONE);
                mSeekbar.setHighlightType(CustomSeekBar.HIGHLIGHT_TYPE.HIGHLIGHT_RIGHT); 
            }
            else {
                mImgCenter.setVisibility(View.VISIBLE);
                mSeekbar.setHighlightType(CustomSeekBar.HIGHLIGHT_TYPE.HIGHLIGHT_CENTER); 
            }
            
        }
    };

    private final CheckBox.OnCheckedChangeListener mCheckListener =
            new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if ( mImgCenter == null ) return;
            if ( isChecked ) {
                updateType(TYPE.CLUSTER_BRIGHTNESS, mClusterBrightnessProgress);
                mImgCenter.setVisibility(View.GONE);
                mSeekbar.setHighlightType(CustomSeekBar.HIGHLIGHT_TYPE.HIGHLIGHT_RIGHT); 
            }
            else {
                updateType(TYPE.BRIGHTNESS, mBrightnessProgress);
                mImgCenter.setVisibility(View.VISIBLE);
                mSeekbar.setHighlightType(CustomSeekBar.HIGHLIGHT_TYPE.HIGHLIGHT_CENTER); 
            }
            if ( mSystem != null ) mSystem.setClusterCheck(isChecked);
            
        }
    };

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if ( mCheckbox == null || mSystem == null ) return;

            Rect thumbRect = seekBar.getThumb().getBounds();
            if ( mPopupView != null ) {
                mPopupView.setX(thumbRect.left+POPUP_VIEW_OFFSET);
                mPopupView.setY(POPUP_VIEW_Y);
            }

            if ( !mIsUserSeek ) {
                if ( mPopupView != null && fromUser ) 
                    mPopupView.setVisibility(View.VISIBLE);
                mIsUserSeek = fromUser; 
            }

            Log.d(TAG, "onProgressChanged="+progress+", fromUser="+fromUser);
        
            if ( mCheckbox.isChecked() ) {
                mClusterBrightnessProgress = convertValidClusterBrightness(progress); 
                if ( mText != null ) 
                    mText.setText(Integer.toString(progress)); 
                if ( fromUser ) mSystem.setClusterBrightness(mClusterBrightnessProgress);
            } else {
                mBrightnessProgress = convertLevelToBrightness(progress, mBrightnessMin, mBrightnessMax);
                if ( mText != null ) 
                    mText.setText(Integer.toString(progress)); 
                if ( fromUser ) mSystem.setBrightness(mBrightnessProgress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // not implement
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // not implement
        }
    };

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if ( mPopupView != null && mIsUserSeek ) 
                        mPopupView.setVisibility(View.VISIBLE);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if ( mSystem != null ) mSystem.performClick();
                    if ( mPopupView != null ) mPopupView.setVisibility(View.GONE);
                    break;
                }
            }
            return false;
        }
    };
}
