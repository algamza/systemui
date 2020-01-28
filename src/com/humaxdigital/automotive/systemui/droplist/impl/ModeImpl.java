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
import android.car.CarNotConnectedException;
import android.extension.car.CarSystemManager;

import java.util.Timer;
import java.util.TimerTask;

import com.humaxdigital.automotive.systemui.common.car.CarExClient;

public class ModeImpl extends BaseImplement<Integer> {
    public enum Mode {
        AUTOMATIC,
        DAYLIGHT,
        NIGHT
    };
    private final String TAG = "ModeImpl";
    private ContentResolver mContentResolver;
    private ContentObserver mModeTypeObserver; 
    private Mode mCurrentMode; 
    private CarExClient mCarClient = null;
    private CarSystemManager mCarSystem = null; 
    private Timer mTimer = new Timer(); 
    private TimerTask mStoreTask = null; 

    public ModeImpl(Context context) {
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
        Log.d(TAG, "get="+mCurrentMode); 
        return mCurrentMode.ordinal(); 
    }

    @Override
    public void set(Integer e) {
        Mode mode = Mode.values()[e]; 
        Log.d(TAG, "set="+mode+", user="+UserHandle.USER_CURRENT); 
        // (Value = 0: Auto, 1: Day, 2: Night)
        
        switch(mode) {
            case AUTOMATIC: {
                Settings.System.putIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.DISPLAY_MODE_TYPE, 
                    CarExtraSettings.System.DISPLAY_MODE_TYPE_AUTO, 
                    UserHandle.USER_CURRENT); 
                break; 
            }
            case DAYLIGHT: {
                Settings.System.putIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.DISPLAY_MODE_TYPE, 
                    CarExtraSettings.System.DISPLAY_MODE_TYPE_DAYLIGHT, 
                    UserHandle.USER_CURRENT); 
                break; 
            }
            case NIGHT: {
                Settings.System.putIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.DISPLAY_MODE_TYPE, 
                    CarExtraSettings.System.DISPLAY_MODE_TYPE_NIGHT, 
                    UserHandle.USER_CURRENT); 
                break; 
            }
        }

        sendStoreCommand();
    }

    public void fetchEx(CarExClient client) {
        Log.d(TAG, "fetchEx");
        mCarClient = client; 
        mCarSystem = mCarClient.getSystemManager(); 
    }

    private void cleanup() {
        if ( mContentResolver != null ) {
            mContentResolver.unregisterContentObserver(mModeTypeObserver);
        }
        mModeTypeObserver = null;
        mContentResolver = null;
    }

    private void init() {
        Log.d(TAG, "init"); 
        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        int type = CarExtraSettings.System.DISPLAY_MODE_TYPE_DEFAULT; 
        try {
            type = Settings.System.getIntForUser(mContext.getContentResolver(), 
                CarExtraSettings.System.DISPLAY_MODE_TYPE,
                UserHandle.USER_CURRENT);
        } catch(Settings.SettingNotFoundException e) {
            Log.e(TAG, "error : " + e ); 
        }
        mCurrentMode = convertToMode(type); 

        mModeTypeObserver = createModeTypeObserver(); 

        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.DISPLAY_MODE_TYPE), 
            false, mModeTypeObserver, UserHandle.USER_CURRENT); 
    }

    private void sendStoreCommand() {
        if ( mStoreTask != null ) {
            mStoreTask.cancel(); 
            mTimer.purge(); 
            mStoreTask = null; 
        }

        mStoreTask = new TimerTask(){
            @Override
            public void run() {
                try {
                    Log.d(TAG, "setLCDSettingStored"); 
                    if ( mCarSystem != null ) mCarSystem.setLCDSettingStored(); 
                } catch (CarNotConnectedException err) {
                    Log.e(TAG, "CarNotConnectedException : "+ err);
                }
            }
        };

        mTimer.schedule(mStoreTask, 1000);
    }

    private Mode convertToMode(int type) {
        Log.d(TAG, "convertToMode:type="+type); 
        Mode mode = Mode.AUTOMATIC; 
        switch(type) {
            case CarExtraSettings.System.DISPLAY_MODE_TYPE_DAYLIGHT: 
                mode = Mode.DAYLIGHT; 
                break;
            case CarExtraSettings.System.DISPLAY_MODE_TYPE_NIGHT: 
                mode = Mode.NIGHT; 
                break; 
            case CarExtraSettings.System.DISPLAY_MODE_TYPE_AUTO: 
                mode = Mode.AUTOMATIC; 
                break; 
        }
        return mode; 
    }

    private ContentObserver createModeTypeObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                int mode = getCurrentMode(); 
                
                if ( mListener != null ) {
                    Mode _mode = convertToMode(mode); 
                    Log.d(TAG, "Mode type onChange:mode="+_mode+", type="+mode); 
                    if ( _mode == mCurrentMode ) return;
                    mCurrentMode = _mode; 
                    mListener.onChange(mCurrentMode.ordinal()); 
                }
            }
        };
        return observer; 
    }

    private int getCurrentMode() {
        int mode = 0; 
        try {
            mode = Settings.System.getIntForUser(mContext.getContentResolver(), 
                CarExtraSettings.System.DISPLAY_MODE_TYPE,
                UserHandle.USER_CURRENT);
        } catch(Settings.SettingNotFoundException e) {
            Log.e(TAG, "error : " + e ); 
        }
        return mode; 
    }

    public void refresh() {
        Log.d(TAG, "refresh"); 
        if ( mContentResolver == null ) return; 
        if ( mModeTypeObserver != null )  {
            mContentResolver.unregisterContentObserver(mModeTypeObserver); 
        }

        mModeTypeObserver = createModeTypeObserver(); 

        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.DISPLAY_MODE_TYPE), 
            false, mModeTypeObserver, UserHandle.USER_CURRENT); 

        if ( mListener != null ) {
            mCurrentMode = convertToMode(getCurrentMode()); 
            mListener.onChange(mCurrentMode.ordinal()); 
        }
    }
}
