package com.humaxdigital.automotive.systemui.droplist.controllers;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.view.View;

import android.content.Context;
import android.content.res.Resources;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.droplist.SystemControl;
import com.humaxdigital.automotive.systemui.droplist.ui.MenuLayout;

public class QuietModeController implements BaseController {
    private MenuLayout mView;
    private SystemControl mSystem;  
    private Listener mListener; 
    private UpdateHandler mHandler = new UpdateHandler();
    private boolean mOn = false; 
    private boolean mIsCalling = false; 
    private boolean mIsAVOn = true;

    @Override
    public BaseController init(View view) {
        mView = (MenuLayout)view;
        mView.setListener(mMenuCallback);
        return this;
    }

    @Override
    public void fetch(SystemControl system) {
        if ( system == null || mView == null ) return; 
        mSystem = system; 
        mSystem.registerCallback(mSystemCallback);
        mOn = mSystem.getQuietModeOn();
        mView.updateEnable(mOn);
        mIsAVOn = mSystem.isAVOn(); 
        updateModeEnable(mIsAVOn); 
    }

    @Override
    public View getView() {
        return mView;
    }

    @Override
    public BaseController setListener(Listener listener) {
        mListener = listener; 
        return this; 
    }

    @Override
    public void refresh(Context context) {
        if ( context == null || mView == null ) return;
        Resources res = context.getResources();
        mView.updateText(res.getString(R.string.STR_QUIET_MODE_04_ID));
    }

    private void updateModeEnable(boolean enable) {
        if ( enable ) {
            mHandler.obtainMessage(UpdateHandler.MODE_ENABLE, 0).sendToTarget(); 
        } else {
            mHandler.obtainMessage(UpdateHandler.MODE_DISABLE, 0).sendToTarget(); 
        }
    }

    private SystemControl.SystemCallback mSystemCallback = new SystemControl.SystemCallback() {
        @Override
        public void onQuietModeOnChanged(boolean isOn) {
            if ( mView == null ) return;
            if ( isOn ) 
                mHandler.obtainMessage(UpdateHandler.MODE_ON, 0).sendToTarget(); 
            else 
                mHandler.obtainMessage(UpdateHandler.MODE_OFF, 0).sendToTarget(); 
        }
        
        @Override
        public void onCallingChanged(boolean on) {
            mIsCalling = on; 
            if ( mIsCalling ) 
                mHandler.obtainMessage(UpdateHandler.MODE_DISABLE, 0).sendToTarget(); 
            else 
                mHandler.obtainMessage(UpdateHandler.MODE_ENABLE, 0).sendToTarget(); 
        }

        @Override
        public void onAVOnChanged(boolean on) {
            mIsAVOn = on; 
            updateModeEnable(mIsAVOn); 
        }
    };

    private MenuLayout.MenuListener mMenuCallback = new MenuLayout.MenuListener() {
        @Override
        public boolean onClick() {
            if ( mSystem == null || mView == null ) return false;
            if ( mIsCalling || !mIsAVOn ) return false; 
            if ( mView.isEnable() ) {
                mOn = false;
                mView.updateEnable(false);
                mSystem.setQuietModeOn(false);
            } else {
                mOn = true; 
                mView.updateEnable(true);
                mSystem.setQuietModeOn(true);
            }
            return true;
        }

        @Override
        public boolean onLongClick() {
            if ( mIsCalling || !mIsAVOn ) return false; 
            if ( mSystem != null ) mSystem.openQuietModeSetting();
            if ( mListener != null ) mListener.onClose();
            return true;
        }
    }; 

    private final class UpdateHandler extends Handler {
        private static final int MODE_ON = 1;
        private static final int MODE_OFF = 2;
        private static final int MODE_DISABLE = 3; 
        private static final int MODE_ENABLE = 4; 

        public UpdateHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if ( mView == null ) return; 
            switch(msg.what) {
                case MODE_ON: {
                    mView.updateEnable(true); 
                    mOn = true; 
                    break;
                }
                case MODE_OFF: {
                    mView.updateEnable(false); 
                    mOn = false; 
                    break;
                }
                case MODE_DISABLE: {
                    mView.updateEnable(false); 
                    mView.disableText(); 
                    break;
                }
                case MODE_ENABLE: {
                    if ( mOn ) mView.updateEnable(true);
                    else  mView.updateEnable(false); 
                    mView.enableText(); 
                    break; 
                }
                default: break;
            }
        }
    }
}
