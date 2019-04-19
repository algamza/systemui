package com.humaxdigital.automotive.statusbar.droplist.controllers;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.view.View;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.droplist.SystemControl;
import com.humaxdigital.automotive.statusbar.droplist.ui.MenuLayout;

public class MuteController implements BaseController {
    private final String TAG = "MuteController";
    private MenuLayout mView;
    private SystemControl mSystem;  
    private UpdateHandler mHandler = new UpdateHandler();
    private boolean mIsVolumeSettingsActivated = false;
    
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
        mView.updateEnable(mSystem.getMuteOn());

        mIsVolumeSettingsActivated = mSystem.isVolumeSettingsActivated();
    }

    @Override
    public View getView() {
        return mView;
    }

    @Override
    public BaseController setListener(Listener listener) { 
        return this; 
    }

    @Override
    public void refresh(Context context) {
        if ( context == null || mView == null ) return;
        Resources res = context.getResources();
        mView.updateText(res.getString(R.string.STR_MUTE_06_ID));
    }

    private SystemControl.SystemCallback mSystemCallback = new SystemControl.SystemCallback() {
        @Override
        public void onMuteOnChanged(boolean isOn) {
            if ( mView == null ) return;
            if ( isOn ) 
                mHandler.obtainMessage(UpdateHandler.MODE_ON, 0).sendToTarget(); 
            else 
                mHandler.obtainMessage(UpdateHandler.MODE_OFF, 0).sendToTarget(); 
        }

        @Override
        public void onVolumeSettingsActivated(boolean on) {
            mIsVolumeSettingsActivated = on;
        }
    };

    private MenuLayout.MenuListener mMenuCallback = new MenuLayout.MenuListener() {
        @Override
        public boolean onClick() {
            if ( mSystem == null || mView == null ) return false;
            if ( mIsVolumeSettingsActivated ) {
                Log.d(TAG, "VolumeSettingsActivated. is not working !!");
                return false;  
            }
            if ( mView.isEnable() ) {
                //mView.updateEnable(false);
                mSystem.setMuteOn(false);
            } else {
                //mView.updateEnable(true);
                mSystem.setMuteOn(true);
            }
            return true; 
        }

        @Override
        public boolean onLongClick() {
            return false; 
        }
    }; 

    private final class UpdateHandler extends Handler {
        private static final int MODE_ON = 1;
        private static final int MODE_OFF = 2;

        public UpdateHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if ( mView == null ) return; 
            switch(msg.what) {
                case MODE_ON: mView.updateEnable(true); break;
                case MODE_OFF: mView.updateEnable(false); break;
                default: break;
            }
        }
    }
}
