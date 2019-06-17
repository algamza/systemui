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

public class ThemeController implements BaseController {
    public enum Theme {
        THEME1,
        THEME2,
        THEME3
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
        int mode = mSystem.getThemeMode(); 
        mView.updateState(convertToTheme(mode).ordinal()); 
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
        mView.updateStatusText(Theme.THEME1.ordinal(), res.getString(R.string.STR_THEME1_ID));
        mView.updateStatusText(Theme.THEME2.ordinal(), res.getString(R.string.STR_THEME2_ID));
        mView.updateStatusText(Theme.THEME3.ordinal(), res.getString(R.string.STR_THEME3_ID));
    }

    private Theme convertToTheme(int mode) {
        Theme theme = Theme.THEME1; 
        SystemControl.SystemTheme sysmode = SystemControl.SystemTheme.values()[mode]; 
        switch(sysmode) {
            case THEME1: theme = Theme.THEME1; break;
            case THEME2: theme = Theme.THEME2; break;
            case THEME3: theme = Theme.THEME3; break;
        }
        return theme; 
    }

    private SystemControl.SystemCallback mSystemCallback = new SystemControl.SystemCallback() {
        @Override
        public void onThemeChanged(SystemControl.SystemTheme theme) {
            if ( mView == null ) return;
            switch(theme) {
                case THEME1: 
                    mHandler.obtainMessage(UpdateHandler.H_THEME1, 0).sendToTarget(); 
                    break;
                case THEME2: 
                    mHandler.obtainMessage(UpdateHandler.H_THEME2, 0).sendToTarget(); 
                    break; 
                case THEME3: 
                    mHandler.obtainMessage(UpdateHandler.H_THEME3, 0).sendToTarget(); 
                    break; 
                default: break; 
            }
        }
    };

    private final class UpdateHandler extends Handler {
        private static final int H_THEME1 = 1;
        private static final int H_THEME2 = 2;
        private static final int H_THEME3 = 3;

        public UpdateHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if ( mView == null ) return; 
            switch(msg.what) {
                case H_THEME1: mView.updateState(Theme.THEME1.ordinal()); break;
                case H_THEME2: mView.updateState(Theme.THEME2.ordinal()); break;
                case H_THEME3: mView.updateState(Theme.THEME3.ordinal()); break;
                default: break;
            }
        }
    }

    private MenuLayout.MenuListener mMenuCallback = new MenuLayout.MenuListener() {
        @Override
        public boolean onClick() {
            if ( mSystem == null || mView == null ) return false;
   
            if ( mView.getStatus() == Theme.THEME1.ordinal() ) {
                mView.updateState(Theme.THEME2.ordinal());
                mSystem.setThemeMode(Theme.THEME2.ordinal());
            } else if ( mView.getStatus() == Theme.THEME2.ordinal() ) {
                mView.updateState(Theme.THEME3.ordinal());
                mSystem.setThemeMode(Theme.THEME3.ordinal());
            } else if ( mView.getStatus() == Theme.THEME3.ordinal() ) {
                mView.updateState(Theme.THEME1.ordinal());
                mSystem.setThemeMode(Theme.THEME1.ordinal());
            }

            return true; 
        }

        @Override
        public boolean onLongClick() {
            if ( mSystem != null ) mSystem.openThemeSetting();
            if ( mListener != null ) mListener.onClose();
            return true;
        }
    }; 
}
