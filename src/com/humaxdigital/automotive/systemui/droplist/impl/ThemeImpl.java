package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.provider.Settings;
import android.extension.car.settings.CarExtraSettings;
import android.util.Log;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;

public class ThemeImpl extends BaseImplement<Integer> {
    public enum Theme {
        THEME1,
        THEME2,
        THEME3
    };

    private final String TAG = "ThemeImpl";
    private ContentResolver mContentResolver;
    private ContentObserver mThemeObserver;
    private Theme mCurrentTheme; 

    public ThemeImpl(Context context) {
        super(context);
    }

    @Override
    public void create() {
        init();
    }

    @Override
    public void destroy() {
        cleanup();
    }

    @Override
    public Integer get() {
        Log.d(TAG, "get="+mCurrentTheme); 
        return mCurrentTheme.ordinal(); 
    }

    @Override
    public void set(Integer e) {
        if ( mContext == null ) return;
        Theme theme = Theme.values()[e]; 
        Log.d(TAG, "set="+theme); 
        switch(theme) {
            case THEME1: {
                Settings.System.putIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.ADVANCED_THEME_STYLE,
                    CarExtraSettings.System.ADVANCED_THEME_STYLE_1,
                    UserHandle.USER_CURRENT); 
                break; 
            }
            case THEME2: {
                Settings.System.putIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.ADVANCED_THEME_STYLE,
                    CarExtraSettings.System.ADVANCED_THEME_STYLE_2,
                    UserHandle.USER_CURRENT); 
                break; 
            }
            case THEME3: {
                Settings.System.putIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.ADVANCED_THEME_STYLE,
                    CarExtraSettings.System.ADVANCED_THEME_STYLE_3,
                    UserHandle.USER_CURRENT); 
                break; 
            }
        }
    }

    private void cleanup() {
        if ( mContentResolver != null ) {
            mContentResolver.unregisterContentObserver(mThemeObserver); 
        }
        mThemeObserver = null;
        mContentResolver = null;
    }

    private void init() {
        if ( mContext == null ) return;
        Log.d(TAG, "init"); 
        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        int theme = CarExtraSettings.System.ADVANCED_THEME_STYLE_DEFAULT; 
        try {
            theme = Settings.System.getIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.ADVANCED_THEME_STYLE,
                    UserHandle.USER_CURRENT);
        } catch(Settings.SettingNotFoundException e) {
            Log.e(TAG, "error : " + e ); 
        }

        mCurrentTheme = convertToTheme(theme); 
        mThemeObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.ADVANCED_THEME_STYLE), 
            false, mThemeObserver, UserHandle.USER_CURRENT); 
    }

    private Theme convertToTheme(int type) {
        Theme theme = Theme.THEME1; 
        switch(type) {
            case CarExtraSettings.System.ADVANCED_THEME_STYLE_1: 
                theme = Theme.THEME1; 
                break;
            case CarExtraSettings.System.ADVANCED_THEME_STYLE_2: 
                theme = Theme.THEME2; 
                break; 
            case CarExtraSettings.System.ADVANCED_THEME_STYLE_3: 
                theme = Theme.THEME3; 
                break; 
        }

        return theme; 
    }

    private ContentObserver createObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if ( mContext == null ) return;
                Log.d(TAG, "onChange:userId="+userId+", current="+UserHandle.USER_CURRENT); 
                Theme current = convertToTheme(getCurrentTheme()); 
                Log.d(TAG, "onChange : current = "+current+", old = "+mCurrentTheme); 
                if ( mCurrentTheme == current ) return; 
                mCurrentTheme = current; 
                if ( mListener != null ) mListener.onChange(mCurrentTheme.ordinal()); 
            }
        };
        return observer; 
    }

    private int getCurrentTheme() {
        int theme = 0; 
        if ( mContext == null ) return theme; 
        try {
            theme = Settings.System.getIntForUser(mContext.getContentResolver(), 
                CarExtraSettings.System.ADVANCED_THEME_STYLE, UserHandle.USER_CURRENT);
        } catch(Settings.SettingNotFoundException e) {
            Log.e(TAG, "error : " + e ); 
        }
        return theme; 
    }

    public void refresh() {
        Log.d(TAG, "refresh"); 
        if ( mContentResolver == null ) return; 
        if ( mThemeObserver != null )  {
            mContentResolver.unregisterContentObserver(mThemeObserver); 
        }
        mThemeObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.ADVANCED_THEME_STYLE), 
            false, mThemeObserver, UserHandle.USER_CURRENT); 

        mCurrentTheme = convertToTheme(getCurrentTheme()); 
        if ( mListener != null ) mListener.onChange(mCurrentTheme.ordinal()); 
    }
}
