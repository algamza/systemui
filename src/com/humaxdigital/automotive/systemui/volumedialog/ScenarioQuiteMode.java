package com.humaxdigital.automotive.systemui.volumedialog;


import android.os.Handler;
import android.os.UserHandle;

import android.content.Context;
import android.content.ContentResolver;

import android.provider.Settings;
import android.media.AudioManager; 
import android.database.ContentObserver;
import android.net.Uri;


import android.extension.car.settings.CarExtraSettings;
import android.extension.car.CarAudioManagerEx;
import android.car.CarNotConnectedException;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;


public class ScenarioQuiteMode {
    private static final String TAG = "ScenarioQuiteMode";

    private Context mContext = null;
    private ContentResolver mContentResolver = null;
    private ContentObserver mModeObserver = null;
    private final int QUITE_MODE_MAX = 20;
    private final int QUITE_MODE_VOLUME = 7;
    private ArrayList<VolumeUtil.Type> mQuiteModeAudioTypeList = new ArrayList<>(); 
    private HashMap<VolumeUtil.Type,Integer> mQuiteModeLastVolume = new HashMap<>();
    private HashMap<VolumeUtil.Type,Boolean> mQuiteModeAudioChange = new HashMap<>();
    private CarAudioManagerEx mCarAudioManagerEx = null;
    private boolean mIsQuiteModeApplying = false; 

    public ScenarioQuiteMode(Context context) {
        if ( context == null ) return;
        mContext = context; 
        mContentResolver = mContext.getContentResolver();
    }

    private ContentObserver createObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                quitetModeChanged();
            }
        };
        return observer; 
    }
    
    public ScenarioQuiteMode init() {
        mQuiteModeAudioTypeList.add(VolumeUtil.Type.RADIO_AM);
        mQuiteModeAudioTypeList.add(VolumeUtil.Type.RADIO_FM);
        mQuiteModeAudioTypeList.add(VolumeUtil.Type.USB);
        mQuiteModeAudioTypeList.add(VolumeUtil.Type.ONLINE_MUSIC);
        mQuiteModeAudioTypeList.add(VolumeUtil.Type.CARLIFE_MEDIA); 
        mQuiteModeAudioTypeList.add(VolumeUtil.Type.BT_AUDIO);
        mModeObserver = createObserver(); 
        if ( mContentResolver != null ) mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.SOUND_QUIET_MODE_ON), 
            false, mModeObserver, UserHandle.USER_CURRENT); 
        if ( isQuiteMode() ) applyQuiteMode();
        return this; 
    }

    public void deinit() {
        mQuiteModeAudioTypeList.clear();
        mQuiteModeLastVolume.clear();
        mQuiteModeAudioChange.clear();
        if ( mContentResolver != null ) 
            mContentResolver.unregisterContentObserver(mModeObserver); 
        mContentResolver = null;
        mCarAudioManagerEx = null;
        mModeObserver = null;
        mContext = null;
    }
    
    public void fetchCarAudioManagerEx(CarAudioManagerEx mgr) {
        mCarAudioManagerEx = mgr; 
    }

    private void quitetModeChanged() {
        if ( isQuiteMode() ) applyQuiteMode();
        else {
            if ( mQuiteModeLastVolume.isEmpty() || mQuiteModeAudioChange.isEmpty() ) return;
            for ( VolumeUtil.Type type : mQuiteModeAudioTypeList ) {
                boolean changed = mQuiteModeAudioChange.get(type); 
                if ( !changed ) {
                    int volume = mQuiteModeLastVolume.get(type);
                    setAudioVolume(VolumeUtil.convertToMode(type), volume);
                    Log.d(TAG, "onChange:type="+type+", volume="+volume);
                }
            }
        }
    }

    private boolean setAudioVolume(int mode, int volume) {
        if ( mCarAudioManagerEx == null ) return false; 
        try {
            int _mode = mode; 
            int _volume = volume; 
            if ( _mode < 0 ) _mode = 0; 
            if ( _volume < 0 ) _volume = 0; 
            Log.d(TAG, "setAudioVolume:mode="+mode+", volume="+volume);
            mCarAudioManagerEx.setVolumeForLas(_mode, _volume); 
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Failed to set current volume", e);
        }
        return true; 
    }

    private boolean isQuiteMode() {
        if ( mContext == null ) return false;
        int on = Settings.System.getIntForUser(mContext.getContentResolver(), 
                CarExtraSettings.System.SOUND_QUIET_MODE_ON,
                CarExtraSettings.System.SOUND_QUIET_MODE_ON_DEFAULT,
                        UserHandle.USER_CURRENT);
        Log.d(TAG, "isQuiteMode="+on);
        return on==0?false:true;
    }

    private void applyQuiteMode() {
        if ( mCarAudioManagerEx == null ) return;
        try {
            for ( VolumeUtil.Type type : mQuiteModeAudioTypeList ) {
                int mode = VolumeUtil.convertToMode(type);
                int volume = mCarAudioManagerEx.getVolumeForLas(mode);
                mQuiteModeLastVolume.put(type, volume);
                mQuiteModeAudioChange.put(type, false);
                Log.d(TAG, "applyQuiteMode:type="+type+", mode="+mode+", volume="+volume);
            }
            mIsQuiteModeApplying = true;
            for ( VolumeUtil.Type type : mQuiteModeAudioTypeList ) {
                int mode = VolumeUtil.convertToMode(type);
                int volume = mCarAudioManagerEx.getVolumeForLas(mode);
                
                if ( volume > QUITE_MODE_VOLUME ) {
                    setAudioVolume(mode, QUITE_MODE_VOLUME); 
                }
            }
            mIsQuiteModeApplying = false;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Failed to get current volume", e);
        }
    }

    public boolean checkQuietMode(int mode, int volume) {
        if ( !isQuiteMode() ) return false; 
        
        for ( VolumeUtil.Type type : mQuiteModeAudioTypeList ) {
            if ( mode != VolumeUtil.convertToMode(type) ) continue; 

            if ( !mIsQuiteModeApplying && volume != QUITE_MODE_VOLUME ) {
                mQuiteModeAudioChange.put(type, true);
                if ( type == VolumeUtil.Type.RADIO_AM || type == VolumeUtil.Type.RADIO_FM 
                || type == VolumeUtil.Type.USB || type == VolumeUtil.Type.ONLINE_MUSIC 
                || type == VolumeUtil.Type.CARLIFE_MEDIA ) {
                    mQuiteModeAudioChange.put(VolumeUtil.Type.RADIO_AM, true);
                    mQuiteModeAudioChange.put(VolumeUtil.Type.RADIO_FM, true);
                    mQuiteModeAudioChange.put(VolumeUtil.Type.USB, true);
                    mQuiteModeAudioChange.put(VolumeUtil.Type.ONLINE_MUSIC, true);
                    mQuiteModeAudioChange.put(VolumeUtil.Type.CARLIFE_MEDIA, true);
                }
            }
            
            if ( volume > QUITE_MODE_MAX ) {
                setAudioVolume(mode, QUITE_MODE_MAX); 
                Log.d(TAG, "checkQuietMode:type="+type+", las volume="+volume);
                return true; 
            }
        }

        return false;
    }

    public void userRefresh() {
        if ( mContentResolver == null ) return; 
        if ( mModeObserver != null )  {
            mContentResolver.unregisterContentObserver(mModeObserver); 
        }
        Log.d(TAG, "userRefresh");
        mModeObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.SOUND_QUIET_MODE_ON), 
            false, mModeObserver, UserHandle.USER_CURRENT); 
        if ( isQuiteMode() ) applyQuiteMode();
    }
}