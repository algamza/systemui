package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.provider.Settings;
import android.extension.car.settings.CarExtraSettings;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;

import android.extension.car.CarAudioManagerEx;
import android.extension.car.util.AudioTypes;

import android.util.Log;

import com.humaxdigital.automotive.systemui.common.car.CarExClient;

public class QuietModeImpl extends BaseImplement<Boolean> {
    private static final String TAG = "QuietModeImpl"; 
    private ContentResolver mContentResolver;
    private ContentObserver mQuiteModeObserver;
    private CarExClient mCarClient = null;
    private CarAudioManagerEx mAudioMgrEx = null;

    public QuietModeImpl(Context context) {
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
    public Boolean get() {
        boolean on = isQuiteMode();
        Log.d(TAG, "get="+on);
        return on;
    }

    @Override
    public void set(Boolean e) {
        Log.d(TAG, "set="+e); 
        setQuiteMode(e);
    }

    public void fetchEx(CarExClient client) {
        Log.d(TAG, "fetchEx="+client);
        mCarClient = client; 

        if ( client == null ) {
            mAudioMgrEx = null;
            return;
        }
        mAudioMgrEx = mCarClient.getAudioManager(); 

    }

    public void refresh() {
        if ( mContentResolver == null ) return; 
        if ( mQuiteModeObserver != null )  {
            mContentResolver.unregisterContentObserver(mQuiteModeObserver); 
        }
        Log.d(TAG, "refresh");
        mQuiteModeObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.SOUND_QUIET_MODE_ON), 
            false, mQuiteModeObserver, UserHandle.USER_CURRENT); 

        if ( mListener != null ) 
            mListener.onChange(isQuiteMode()); 
    }

    private void cleanup() {
        if ( mContentResolver != null ) 
            mContentResolver.unregisterContentObserver(mQuiteModeObserver);
        mQuiteModeObserver = null;
        mContentResolver = null;
        mCarClient = null;
        mAudioMgrEx = null;
    }

    private void init() {
        if ( mContext == null ) return;
        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        mQuiteModeObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.SOUND_QUIET_MODE_ON), 
            false, mQuiteModeObserver, UserHandle.USER_CURRENT); 
    }

    private void setQuiteMode(boolean on) {
        if ( mContext == null ) return;
        Log.d(TAG, "setQuiteMode="+on);
        sendQuietModeCommand(on);
        Settings.System.putIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.SOUND_QUIET_MODE_ON,
            on?1:0, UserHandle.USER_CURRENT); 
    }

    private void sendQuietModeCommand(boolean on) {
        if ( mAudioMgrEx == null ) return;
        byte[] audData = new byte[1];
        audData[0] = (byte) ((on ? AudioTypes.AUDIO_QUIETMODE_ON : AudioTypes.AUDIO_QUIETMODE_OFF) & 0xFF);
        mAudioMgrEx.onAudioExCmdRequest(CarAudioManagerEx.APP_AUDIO_REQ_QUIETMODE, audData.length, audData);
    }

    private boolean isQuiteMode() {
        if ( mContext == null ) return false;
        int on = CarExtraSettings.System.SOUND_QUIET_MODE_ON_DEFAULT; 
        try {
            on = Settings.System.getIntForUser(mContext.getContentResolver(), 
                CarExtraSettings.System.SOUND_QUIET_MODE_ON,
                        UserHandle.USER_CURRENT);
        } catch(Settings.SettingNotFoundException e) {
            Log.e(TAG, "error : " + e ); 
        }
        Log.d(TAG, "isQuiteMode="+on);
        return on==0?false:true;
    }

    private ContentObserver createObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                boolean isQuiet = isQuiteMode();
                Log.d(TAG, "onChange="+isQuiet+", user="+userId);
                if ( mListener != null ) 
                    mListener.onChange(isQuiet); 
            }
        };
        return observer; 
    }
}
