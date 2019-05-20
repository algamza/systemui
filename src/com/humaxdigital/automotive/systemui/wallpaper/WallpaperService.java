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

import android.app.Service;
import android.app.WallpaperManager;
import android.graphics.BitmapFactory; 
import android.graphics.Bitmap; 

import android.provider.Settings;
import android.database.ContentObserver;
import android.net.Uri;

import android.util.Log;
import java.io.IOException;

import android.extension.car.settings.CarExtraSettings;

import com.humaxdigital.automotive.systemui.R; 

public class WallpaperService extends Service {
    private static final String TAG = "WallpaperService";

    private ContentResolver mContentResolver;
    private ContentObserver mThemeObserver;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate"); 
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
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind"); 
        return null;
    }

    private void initThemeObserver() {
        mContentResolver = getContentResolver();
        if ( mContentResolver == null ) return;
        setWallPaper(getCurrentTheme()); 
        mThemeObserver = createThemeObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.ADVANCED_THEME_STYLE), 
            false, mThemeObserver, UserHandle.USER_CURRENT); 
    }

    private void setWallPaper(int id) {
        WallpaperManager wallpaper = WallpaperManager.getInstance(this);
        Log.d(TAG, "setWallPaper="+id); 
        int resid = R.drawable.ho_bg_theme_1; 
        switch(id) {
            case 1: resid = R.drawable.ho_bg_theme_1; 
            case 2: resid = R.drawable.ho_bg_theme_2; 
            case 3: resid = R.drawable.ho_bg_theme_3; 
        }

        Bitmap paper = BitmapFactory.decodeResource(getResources(), resid);
        if ( paper == null ) return;

        try {
            //wallpaper.setResource(resid);
            wallpaper.setBitmap(paper); 
        } catch (IOException e) {
        }
        
    }

    private int getCurrentTheme() {
        int theme = Settings.System.getIntForUser(getContentResolver(), 
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
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistUserSwicher() {
        unregisterReceiver(mUserChangeReceiver);
    }

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh(); 
        }
    };
}
