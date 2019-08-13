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
    private ContentObserver mUserObserver = null; 
    private final int VOLUME_MAX = 45;
    private final int QUITE_MODE_MAX = 20;
    private final int QUITE_MODE_VOLUME = 7;

    private CarAudioManagerEx mCarAudioManagerEx = null;
    private boolean mIsQuiteModeApplying = false; 
    private boolean mNeedToShowUI = false; 

    public class VolumeValue {
        public int mLastVolume = 0; 
        public boolean mIsVolumeChanged = false; 
    }

    public class VolumeDB {
        public VolumeValue mVolumeValue = new VolumeValue();
    }

    public class UserVolume {
        public HashMap<VolumeUtil.Type, VolumeDB> mVolumeDB = new HashMap<>(); 
    }

    private HashMap<Integer, UserVolume> mUserVolume = new HashMap<>(); 
    private ArrayList<VolumeUtil.Type> mAudioTypeList = new ArrayList<>(); 


    public ScenarioQuiteMode(Context context) {
        if ( context == null ) return;
        mContext = context; 
        mContentResolver = mContext.getContentResolver();
    }

    private int getCurrentUser() {
        int user = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_LAST_DRIVER, 
            CarExtraSettings.Global.USERPROFILE_LAST_DRIVER_DEFAULT);
        Log.d(TAG, "getCurrentUser="+user);
        return user; 
    }

    private boolean isUserSwiching() {
        int isUserSwitching = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_USER_SWITCHING_START_FINISH, 
            CarExtraSettings.Global.FALSE);
        Log.d(TAG, "isUserSwiching="+isUserSwitching);
        return (isUserSwitching == CarExtraSettings.Global.FALSE) ? false:true; 
    }

    private ContentObserver createModeObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                Log.d(TAG, "mode changed"); 
                quitetModeChanged();
            }
        };
        return observer; 
    }

    private ContentObserver createUserObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                Log.d(TAG, "user changed"); 
                quitetModeChanged();
            }
        };
        return observer; 
    }
    
    public ScenarioQuiteMode init() {
        mAudioTypeList.add(VolumeUtil.Type.RADIO_AM);
        mAudioTypeList.add(VolumeUtil.Type.RADIO_FM);
        mAudioTypeList.add(VolumeUtil.Type.USB);
        mAudioTypeList.add(VolumeUtil.Type.ONLINE_MUSIC);
        mAudioTypeList.add(VolumeUtil.Type.CARLIFE_MEDIA); 
        mAudioTypeList.add(VolumeUtil.Type.BT_AUDIO);
        mModeObserver = createModeObserver(); 
        mUserObserver = createUserObserver();
        if ( mContentResolver != null ) {
            mContentResolver.registerContentObserver(
                Settings.System.getUriFor(CarExtraSettings.System.SOUND_QUIET_MODE_ON), 
                false, mModeObserver, UserHandle.USER_CURRENT); 
            mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(CarExtraSettings.Global.USERPROFILE_LAST_DRIVER), 
                false, mUserObserver, UserHandle.USER_CURRENT); 
        }
        
        if ( isQuiteMode() ) applyQuiteMode();
        return this; 
    }

    public void deinit() {
        mAudioTypeList.clear();
        if ( mContentResolver != null ) {
            mContentResolver.unregisterContentObserver(mModeObserver); 
            mContentResolver.unregisterContentObserver(mUserObserver); 
        }
            
        mContentResolver = null;
        mCarAudioManagerEx = null;
        mModeObserver = null;
        mUserObserver = null;
        mContext = null;
    }
    
    public void fetchCarAudioManagerEx(CarAudioManagerEx mgr) {
        mCarAudioManagerEx = mgr; 
    }

    private int getQuiteModeLastVolume(VolumeUtil.Type type) {
        int volume = 0; 
        int user = getCurrentUser(); 
        UserVolume uv = mUserVolume.get(user);
        if ( uv == null ) return volume; 
        VolumeDB vd = uv.mVolumeDB.get(type);
        if ( vd == null ) return volume; 
        volume = vd.mVolumeValue.mLastVolume; 
        Log.d(TAG, "getQuiteModeLastVolume : type="+type+", volume="+volume); 
        return volume; 
    }

    private boolean isQuiteModeLastVolumeChanged(VolumeUtil.Type type) {
        boolean changed = false; 
        int user = getCurrentUser(); 
        UserVolume uv = mUserVolume.get(user);
        if ( uv == null ) return changed; 
        VolumeDB vd = uv.mVolumeDB.get(type);
        if ( vd == null ) return changed; 
        changed = vd.mVolumeValue.mIsVolumeChanged; 
        Log.d(TAG, "isQuiteModeLastVolumeChanged : type="+type+", changed="+changed); 
        return changed;  
    }

    private void updateQuiteModeLastVolume(VolumeUtil.Type type, int volume, boolean changed) {
        int user = getCurrentUser(); 
        UserVolume uv = mUserVolume.get(user);
        if ( uv == null ) {
            uv = new UserVolume(); 
            mUserVolume.put(user, uv); 
        }
        VolumeDB vd = uv.mVolumeDB.get(type);
        if ( vd == null ) {
            vd = new VolumeDB();
            uv.mVolumeDB.put(type, vd); 
        } 
        vd.mVolumeValue.mLastVolume = volume; 
        vd.mVolumeValue.mIsVolumeChanged = changed;
        Log.d(TAG, "updateQuiteModeLastVolume : type="+type+", volume="+volume); 
    }

    private void updateQuiteModeLastVolume(VolumeUtil.Type type, int volume) {
        updateQuiteModeLastVolume(type, volume, false); 
    }
    
    private void updateQuiteModeLastVolumeChanged(VolumeUtil.Type type, boolean changed) {
        int user = getCurrentUser(); 
        UserVolume uv = mUserVolume.get(user);
        if ( uv == null ) {
            uv = new UserVolume(); 
            mUserVolume.put(user, uv); 
        }
        VolumeDB vd = uv.mVolumeDB.get(type);
        if ( vd == null ) {
            vd = new VolumeDB();
            uv.mVolumeDB.put(type, vd); 
        } 
        vd.mVolumeValue.mIsVolumeChanged = changed; 
        Log.d(TAG, "updateQuiteModeLastVolumeChanged : type="+type+", changed="+changed);
    }

    private void quitetModeChanged() {
        Log.d(TAG, "quitetModeChanged"); 
        if ( isUserSwiching() ) return; 
        if ( isQuiteMode() ) applyQuiteMode();
        else {
            for ( VolumeUtil.Type type : mAudioTypeList ) {
                boolean changed = isQuiteModeLastVolumeChanged(type); 
                if ( !changed ) {
                    int volume = getQuiteModeLastVolume(type);
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

    public boolean isQuiteMode() {
        if ( mContext == null ) return false;
        int on = Settings.System.getIntForUser(mContext.getContentResolver(), 
                CarExtraSettings.System.SOUND_QUIET_MODE_ON,
                CarExtraSettings.System.SOUND_QUIET_MODE_ON_DEFAULT,
                        UserHandle.USER_CURRENT);
        Log.d(TAG, "isQuiteMode="+on);
        return on==0?false:true;
    }

    public int getQuiteModeMax(int mode) {
        int max = VOLUME_MAX; 
        for ( VolumeUtil.Type type : mAudioTypeList ) {
            if ( mode != VolumeUtil.convertToMode(type) ) continue; 
            max = QUITE_MODE_MAX; 
        }
        return max; 
    }

    private void applyQuiteMode() {
        if ( mCarAudioManagerEx == null ) return;
        try {
            for ( VolumeUtil.Type type : mAudioTypeList ) {
                int mode = VolumeUtil.convertToMode(type);
                int volume = mCarAudioManagerEx.getVolumeForLas(mode);
                updateQuiteModeLastVolume(type, volume); 
                Log.d(TAG, "applyQuiteMode:type="+type+", mode="+mode+", volume="+volume);
            }
            mIsQuiteModeApplying = true;
            for ( VolumeUtil.Type type : mAudioTypeList ) {
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

        mNeedToShowUI = false;

        for ( VolumeUtil.Type type : mAudioTypeList ) {
            if ( mode != VolumeUtil.convertToMode(type) ) continue; 

            if ( !mIsQuiteModeApplying && volume != QUITE_MODE_VOLUME ) {

                updateQuiteModeLastVolumeChanged(type, true);
                if ( type == VolumeUtil.Type.RADIO_AM || type == VolumeUtil.Type.RADIO_FM 
                || type == VolumeUtil.Type.USB || type == VolumeUtil.Type.ONLINE_MUSIC 
                || type == VolumeUtil.Type.CARLIFE_MEDIA ) {
                    updateQuiteModeLastVolumeChanged(VolumeUtil.Type.RADIO_AM, true);
                    updateQuiteModeLastVolumeChanged(VolumeUtil.Type.RADIO_FM, true);
                    updateQuiteModeLastVolumeChanged(VolumeUtil.Type.USB, true);
                    updateQuiteModeLastVolumeChanged(VolumeUtil.Type.ONLINE_MUSIC, true);
                    updateQuiteModeLastVolumeChanged(VolumeUtil.Type.CARLIFE_MEDIA, true);
                }
            }
            
            if ( volume == QUITE_MODE_MAX ) mNeedToShowUI = true;
            
            if ( volume > QUITE_MODE_MAX ) {
                setAudioVolume(mode, QUITE_MODE_MAX); 
                Log.d(TAG, "checkQuietMode:type="+type+", las volume="+volume);
                return true; 
            }
        }

        return false;
    }

    public boolean isNeedToShowUI() {
        return mNeedToShowUI; 
    }

    public void userRefresh() {
        if ( mContentResolver == null ) return; 
        if ( mModeObserver != null )  {
            mContentResolver.unregisterContentObserver(mModeObserver); 
        }
        Log.d(TAG, "userRefresh");
        mModeObserver = createModeObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.SOUND_QUIET_MODE_ON), 
            false, mModeObserver, UserHandle.USER_CURRENT); 
        if ( isQuiteMode() ) applyQuiteMode();
    }
}