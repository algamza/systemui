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

public class ModeController implements BaseController {
    public enum Mode {
        AUTOMATIC,
        DAYLIGHT,
        NIGHT
    }

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
        int mode = mSystem.getAutomaticMode(); 
        mView.updateState(convertToMode(mode).ordinal()); 
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
        mView.updateStatusText(Mode.AUTOMATIC.ordinal(), res.getString(R.string.STR_AUTOMATIC_04_ID));
        mView.updateStatusText(Mode.DAYLIGHT.ordinal(), res.getString(R.string.STR_DAYLIGHT_04_ID));
        mView.updateStatusText(Mode.NIGHT.ordinal(), res.getString(R.string.STR_NIGHT_04_ID));
    }

    private Mode convertToMode(int system_mode) {
        Mode mode = Mode.AUTOMATIC; 
        SystemControl.SystemAutoMode sysmode = SystemControl.SystemAutoMode.values()[system_mode]; 
        switch(sysmode) {
            case AUTOMATIC: mode = Mode.AUTOMATIC; break;
            case DAYLIGHT: mode = Mode.DAYLIGHT; break;
            case NIGHT: mode = Mode.NIGHT; break;
        }
        return mode; 
    }

    private SystemControl.SystemCallback mSystemCallback = new SystemControl.SystemCallback() {
        @Override
        public void onAutomaticModeChanged(SystemControl.SystemAutoMode mode) {
            if ( mView == null ) return;
            switch(mode) {
                case AUTOMATIC: 
                    mHandler.obtainMessage(UpdateHandler.MODE_AUTO, 0).sendToTarget(); 
                    break;
                case DAYLIGHT: 
                    mHandler.obtainMessage(UpdateHandler.MODE_DAYLIGHT, 0).sendToTarget(); 
                    break; 
                case NIGHT: 
                    mHandler.obtainMessage(UpdateHandler.MODE_NIGHT, 0).sendToTarget(); 
                    break; 
                default: break; 
            }
        }
    };
    
    private MenuLayout.MenuListener mMenuCallback = new MenuLayout.MenuListener() {
        @Override
        public boolean onClick() {
            if ( mSystem == null || mView == null ) return false;

            if ( mView.getStatus() == Mode.AUTOMATIC.ordinal() ) {
                mView.updateState(Mode.DAYLIGHT.ordinal());
                mSystem.setAutomaticMode(Mode.DAYLIGHT.ordinal());
            } else if ( mView.getStatus() == Mode.DAYLIGHT.ordinal() ) {
                mView.updateState(Mode.NIGHT.ordinal());
                mSystem.setAutomaticMode(Mode.NIGHT.ordinal());
            } else if ( mView.getStatus() == Mode.NIGHT.ordinal() ) {
                mView.updateState(Mode.AUTOMATIC.ordinal());
                mSystem.setAutomaticMode(Mode.AUTOMATIC.ordinal());
            }

            return true; 
        }

        @Override
        public boolean onLongClick() {
            if ( mSystem != null ) mSystem.openAutomaticSetting();
            return true; 
        }
    }; 

    private final class UpdateHandler extends Handler {
        private static final int MODE_AUTO = 1;
        private static final int MODE_DAYLIGHT = 2;
        private static final int MODE_NIGHT = 3;

        public UpdateHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if ( mView == null ) return; 
            switch(msg.what) {
                case MODE_AUTO: mView.updateState(Mode.AUTOMATIC.ordinal()); break;
                case MODE_DAYLIGHT: mView.updateState(Mode.DAYLIGHT.ordinal()); break;
                case MODE_NIGHT: mView.updateState(Mode.NIGHT.ordinal()); break;
                default: break;
            }
        }
    }
}
