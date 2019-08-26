package com.humaxdigital.automotive.systemui.wallpaper; 

import android.os.Bundle;
import android.os.Binder;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.Handler;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Configuration;

import android.app.Service;
import android.app.WallpaperManager;
import android.graphics.BitmapFactory; 
import android.graphics.Bitmap; 
import android.graphics.Point;

import android.provider.Settings;
import android.database.ContentObserver;
import android.net.Uri;

import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.IOException;

import android.extension.car.settings.CarExtraSettings;

import com.humaxdigital.automotive.systemui.SystemUIBase;
import com.humaxdigital.automotive.systemui.R; 

public class WallpaperService implements SystemUIBase {
    private static final String TAG = "WallpaperService";

    private ContentResolver mContentResolver;
    private ContentObserver mThemeObserver;
    private Display mDefaultDisplay;
    private Context mContext = null; 

    @Override
    public void onCreate(Context context) {
        Log.d(TAG, "onCreate"); 
        mContext = context; 
        if ( mContext == null ) return;
        registUserSwicher();
        initThemeObserver();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy"); 
        cleanupThemeObserver();
        unregistUserSwicher();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    private void initThemeObserver() {
        if ( mContext == null ) return;
        mContentResolver =  mContext.getContentResolver();
        if ( mContentResolver == null ) return;
        setWallPaper(getCurrentTheme()); 
        mThemeObserver = createThemeObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.ADVANCED_THEME_STYLE), 
            false, mThemeObserver, UserHandle.USER_CURRENT); 
    }

    private void setWallPaper(int id) {
        if ( mContext == null ) return;
        WallpaperManager wallpaper = WallpaperManager.getInstance(mContext);
        Log.d(TAG, "setWallPaper="+id); 
        int resid = R.drawable.ho_bg_theme_1; 
        switch(id) {
            case 1: resid = R.drawable.ho_bg_theme_1; break;
            case 2: resid = R.drawable.ho_bg_theme_2; break;
            case 3: resid = R.drawable.ho_bg_theme_3; break;
        }

        try {
            Point displaySize = getDefaultDisplaySize();
            wallpaper.suggestDesiredDimensions(displaySize.x, displaySize.y);
            wallpaper.setResource(resid);
        } catch (IOException e) {
        }
    }

    private int getCurrentTheme() {
        if ( mContext == null ) return 0;
        int theme = Settings.System.getIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.ADVANCED_THEME_STYLE,
            CarExtraSettings.System.ADVANCED_THEME_STYLE_DEFAULT,
            UserHandle.USER_CURRENT);
        Log.d(TAG, "getCurrentTheme="+theme); 
        return theme; 
    }

    private ContentObserver createThemeObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                setWallPaper(getCurrentTheme()); 
            }
        };
        return observer; 
    }

    private void cleanupThemeObserver() {
        if ( mContentResolver != null ) {
            mContentResolver.unregisterContentObserver(mThemeObserver); 
        }
        mThemeObserver = null;
        mContentResolver = null;
    }

    private void refresh() {
        Log.d(TAG, "refresh"); 
        if ( mContentResolver == null ) return; 
        if ( mThemeObserver != null )  {
            mContentResolver.unregisterContentObserver(mThemeObserver); 
        }
        mThemeObserver = createThemeObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.ADVANCED_THEME_STYLE), 
            false, mThemeObserver, UserHandle.USER_CURRENT); 
        setWallPaper(getCurrentTheme()); 
    }

    private void registUserSwicher() {
        Log.d(TAG, "registUserSwicher");
        if ( mContext == null ) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistUserSwicher() {
        if ( mContext == null ) return;
        mContext.unregisterReceiver(mUserChangeReceiver);
    }

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh(); 
        }
    };

    private Point getDefaultDisplaySize() {
        Point p = new Point();
        if ( mContext == null ) return p;
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        d.getRealSize(p);
        return p;
    }
}
