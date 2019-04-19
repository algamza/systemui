package com.humaxdigital.automotive.statusbar.droplist.controllers;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.view.View;

import android.content.Context;
import android.content.res.Resources;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.droplist.SystemControl;
import com.humaxdigital.automotive.statusbar.droplist.ui.MenuLayout;

public class WifiController implements BaseController {
    private MenuLayout mView;
    private SystemControl mSystem;  
    private Listener mListener; 
    private UpdateHandler mHandler = new UpdateHandler();

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
        mView.updateEnable(mSystem.getWifiOn());
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
        mView.updateText(res.getString(R.string.STR_WI_FI_08_ID));
    }

    private SystemControl.SystemCallback mSystemCallback = new SystemControl.SystemCallback() {
        @Override
        public void onWifiOnChanged(boolean isOn) {
            if ( mView == null || mHandler == null ) return;
            if ( isOn ) 
                mHandler.obtainMessage(UpdateHandler.MODE_ON, 0).sendToTarget(); 
            else 
                mHandler.obtainMessage(UpdateHandler.MODE_OFF, 0).sendToTarget(); 
        }
    };

    private MenuLayout.MenuListener mMenuCallback = new MenuLayout.MenuListener() {
        @Override
        public boolean onClick() {
            if ( mSystem == null || mView == null ) return false;
            if ( mView.isEnable() ) {
                mView.updateEnable(false);
                mSystem.setWifiOn(false);
            } else {
                mView.updateEnable(true);
                mSystem.setWifiOn(true);
            }
            return true;
        }

        @Override
        public boolean onLongClick() {
            if ( mSystem != null ) mSystem.openWifiSetting();
            return true;
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
