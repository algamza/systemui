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

public class DisplayController implements BaseController {
    private MenuLayout mView;
    private SystemControl mSystem;  
    private Listener mListener; 
    private UpdateHandler mHandler = new UpdateHandler();
    private boolean mIsCalling = false; 

    @Override
    public BaseController init(View view) {
        mView = (MenuLayout)view;
        mView.setListener(mMenuCallback);
        return this;
    }

    @Override
    public void fetch(SystemControl system) {
        if ( system == null ) return; 
        mSystem = system; 
        mSystem.registerCallback(mSystemCallback);
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
        mView.updateText(res.getString(R.string.STR_DISPLAY_OFF_01_ID));
    }

    private MenuLayout.MenuListener mMenuCallback = new MenuLayout.MenuListener() {
        @Override
        public boolean onClick() {
            if ( mIsCalling ) return false;
            if ( mSystem != null ) mSystem.displayOff(); 
            if ( mListener != null ) mListener.onClose();
            return true; 
        }
        
        @Override
        public boolean onLongClick() {
            return false; 
        }
    }; 

    private SystemControl.SystemCallback mSystemCallback = new SystemControl.SystemCallback() {
        @Override
        public void onCallingChanged(boolean on) {
            if ( mHandler == null ) return;
            /*
            mIsCalling = on; 
            if ( mIsCalling ) 
                mHandler.obtainMessage(UpdateHandler.MODE_DISABLE, 0).sendToTarget(); 
            else 
                mHandler.obtainMessage(UpdateHandler.MODE_ENABLE, 0).sendToTarget(); 
                */
        }
    };

    private final class UpdateHandler extends Handler {
        private static final int MODE_DISABLE = 1; 
        private static final int MODE_ENABLE = 2; 

        public UpdateHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if ( mView == null ) return; 
            switch(msg.what) {
                case MODE_DISABLE: {
                    mView.setListener(null); 
                    mView.updateEnable(false); 
                    mView.disableText(); 
                    break;
                }
                case MODE_ENABLE: {
                    mView.setListener(mMenuCallback);
                    mView.updateEnable(true);
                    mView.enableText(); 
                    break; 
                }
                default: break;
            }
        }
    }
}
