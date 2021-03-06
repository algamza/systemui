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

public class ModeController implements BaseController, SystemControl.SystemCallback {
    public enum Mode {
        AUTOMATIC(0),
        DAYLIGHT(1),
        NIGHT(2); 
        private final int mode; 
        Mode(int mode) { this.mode = mode; }
        public int mode() { return mode; }
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
        mSystem.registerCallback(this);
        int mode = mSystem.getAutomaticMode(); 
        mView.updateState(convertToMode(mode).mode()); 
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
        mView.updateStatusText(Mode.AUTOMATIC.mode(), res.getString(R.string.STR_AUTOMATIC_04_ID));
        mView.updateStatusText(Mode.DAYLIGHT.mode(), res.getString(R.string.STR_DAYLIGHT_04_ID));
        mView.updateStatusText(Mode.NIGHT.mode(), res.getString(R.string.STR_NIGHT_04_ID));
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
    
    private MenuLayout.MenuListener mMenuCallback = new MenuLayout.MenuListener() {
        @Override
        public boolean onClick() {
            if ( mSystem == null || mView == null ) return false;

            if ( mView.getStatus() == Mode.AUTOMATIC.mode() ) {
                mView.updateState(Mode.DAYLIGHT.mode());
                mSystem.setAutomaticMode(Mode.DAYLIGHT.mode());
            } else if ( mView.getStatus() == Mode.DAYLIGHT.mode() ) {
                mView.updateState(Mode.NIGHT.mode());
                mSystem.setAutomaticMode(Mode.NIGHT.mode());
            } else if ( mView.getStatus() == Mode.NIGHT.mode() ) {
                mView.updateState(Mode.AUTOMATIC.mode());
                mSystem.setAutomaticMode(Mode.AUTOMATIC.mode());
            }

            return true; 
        }

        @Override
        public boolean onLongClick() {
            if ( mSystem != null ) mSystem.openAutomaticSetting();
            if ( mListener != null ) mListener.onClose();
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
                case MODE_AUTO: mView.updateState(Mode.AUTOMATIC.mode()); break;
                case MODE_DAYLIGHT: mView.updateState(Mode.DAYLIGHT.mode()); break;
                case MODE_NIGHT: mView.updateState(Mode.NIGHT.mode()); break;
                default: break;
            }
        }
    }
}
