package com.humaxdigital.automotive.systemui.volumedialog;

import android.os.Handler;
import android.os.UserHandle;

import android.content.Context;
import android.content.ComponentName;
import android.content.ContentResolver;


import android.provider.Settings;
import android.media.AudioManager; 
import android.database.ContentObserver;
import android.net.Uri;

import android.app.Service;

import android.car.hardware.CarSensorEvent;
import android.car.hardware.CarSensorEvent.GearData;
import android.extension.car.value.CarSensorEventEx;
import android.extension.car.settings.CarExtraSettings;
import android.extension.car.CarAudioManagerEx;
import android.extension.car.CarSensorManagerEx;
import android.car.CarNotConnectedException;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ScenarioBackupWran {
    private static final String TAG = "ScenarioBackupWran";

    private Context mContext = null;
    private ContentResolver mContentResolver = null;
    private ContentObserver mModeObserver = null; 
    private final int BACKUP_WRAN_VOLUME = 5; 
    private final int BACKUP_WRAN_BT_MAX = 15; 
    private final int BACKUP_WRAN_BT_MIN = 5; 
    private final float BACKUP_WRAN_BT_DWON_RATE = 0.6f; 
    private ArrayList<VolumeUtil.Type> mBackupWarnAudioTypeList = new ArrayList<>(); 
    private HashMap<VolumeUtil.Type,Integer> mBackupWarnLastVolume = new HashMap<>();
    private HashMap<VolumeUtil.Type,Boolean> mBackupWarnAudioChange = new HashMap<>();
    private boolean mIsRGearDetected = false; 
    private boolean mIsIGNOff = false; 
    private int mBTAudioChangeVolume = 0;
    private CarAudioManagerEx mCarAudioManagerEx = null;
    private CarSensorManagerEx mCarSensorManagerEx = null;
    private boolean mIsBackupWranApplying = false;

    private Timer mTimer = new Timer();
    private TimerTask mChatteringTask = null;
    private final int CHATTERING_TIME = 600; 

    public ScenarioBackupWran(Context context) {
        mContext = context; 
    }

    private ContentObserver createBackupWranObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                backupWarnChanged();
            }
        };
        return observer; 
    }

    public ScenarioBackupWran init() {
        mBackupWarnAudioTypeList.add(VolumeUtil.Type.RADIO_AM);
        mBackupWarnAudioTypeList.add(VolumeUtil.Type.RADIO_FM);
        mBackupWarnAudioTypeList.add(VolumeUtil.Type.USB);
        mBackupWarnAudioTypeList.add(VolumeUtil.Type.ONLINE_MUSIC);
        mBackupWarnAudioTypeList.add(VolumeUtil.Type.CARLIFE_MEDIA);
        mBackupWarnAudioTypeList.add(VolumeUtil.Type.BT_AUDIO);

        mModeObserver = createBackupWranObserver(); 
        if ( mContentResolver != null ) mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.SOUND_PRIORITY_BACKUP_WARNING_ON), 
            false, mModeObserver, UserHandle.USER_CURRENT); 
    
        updateRGearDetectState();

        if ( isBackupWarn() ) applyBackupWarn(); 
        return this;
    }

    public void deinit() {
        mBackupWarnAudioChange.clear();
        mBackupWarnLastVolume.clear();
        mBackupWarnAudioTypeList.clear();
        if ( mContentResolver != null ) 
            mContentResolver.unregisterContentObserver(mModeObserver); 
        if ( mCarSensorManagerEx != null ) 
            mCarSensorManagerEx.unregisterListener(mSensorChangeListener);
        mContentResolver = null;
        mModeObserver = null;
        mContext = null;
        mCarAudioManagerEx = null;
        mCarSensorManagerEx = null;
    }

    public void fetchCarAudioManagerEx(CarAudioManagerEx mgr) {
        mCarAudioManagerEx = mgr; 
    }

    public void fetchCarSensorManagerEx(CarSensorManagerEx mgr) {
        if ( mgr == null ) return;
        mCarSensorManagerEx = mgr; 
        try {
            mCarSensorManagerEx.registerListener(
                mSensorChangeListener, 
                CarSensorManagerEx.SENSOR_TYPE_GEAR, 
                CarSensorManagerEx.SENSOR_RATE_NORMAL);
            mCarSensorManagerEx.registerListener(
                mSensorChangeListener, 
                CarSensorManagerEx.SENSOR_TYPE_IGNITION_STATE, 
                CarSensorManagerEx.SENSOR_RATE_NORMAL);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        updateRGearDetectState();
        if ( isBackupWarn() ) applyBackupWarn(); 
    }

    private void updateRGearDetectState() {
        if ( mCarSensorManagerEx == null ) return;
        try {
            CarSensorEvent event = mCarSensorManagerEx.getLatestSensorEvent(CarSensorManagerEx.SENSOR_TYPE_GEAR);
            GearData gear = event.getGearData(null);
            if(gear.gear == CarSensorEventEx.GEAR_R) mIsRGearDetected = true;
            CarSensorEvent ign_event = mCarSensorManagerEx.getLatestSensorEvent(CarSensorManagerEx.SENSOR_TYPE_IGNITION_STATE);
            int state = ign_event.intValues[0]; 
            Log.d(TAG, "updateRGearDetectState:gear=" + gear.gear+", ign="+state);
            if ( state == CarSensorEvent.IGNITION_STATE_LOCK 
                || state == CarSensorEvent.IGNITION_STATE_OFF
                || state == CarSensorEvent.IGNITION_STATE_ACC ) {
                mIsIGNOff = true;
            } else if ( state == CarSensorEvent.IGNITION_STATE_ON
                || state == CarSensorEvent.IGNITION_STATE_START ) {
                mIsIGNOff = false;
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
    }

    private synchronized void gearScenario(boolean r) {
        Log.d(TAG, "gearScenario="+r);
        if ( mIsRGearDetected == r ) return; 
        mIsRGearDetected = r; 
        if ( mIsRGearDetected ) {
            if ( isBackupWarn() ) applyBackupWarn();
        } else {
            backupWarnChanged();
        }
    }

    private void backupWarnChanged() {
        if ( isBackupWarn() ) applyBackupWarn(); 
        else {
            if ( mIsIGNOff ) return;
            if ( mBackupWarnLastVolume.isEmpty() || mBackupWarnAudioChange.isEmpty() ) return;
            for ( VolumeUtil.Type type : mBackupWarnAudioTypeList ) {
                boolean changed = mBackupWarnAudioChange.get(type); 
                if ( !changed ) {
                    int volume = mBackupWarnLastVolume.get(type);
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

    private boolean isBackupWarn() {
        boolean ret = false; 
        int on = Settings.System.getIntForUser(mContext.getContentResolver(), 
                CarExtraSettings.System.SOUND_PRIORITY_BACKUP_WARNING_ON,
                CarExtraSettings.System.SOUND_PRIORITY_BACKUP_WARNING_ON_DEFAULT,
                        UserHandle.USER_CURRENT);
        if ( on != 0 && mIsRGearDetected && !mIsIGNOff ) ret = true; 
        else ret = false; 

        Log.d(TAG, "isBackupWarn="+ret+", settings value="+on+", gearR="+mIsRGearDetected+", ignoff="+mIsIGNOff);
        
        return ret;
    }

    private void applyBackupWarn() {
        if ( mCarAudioManagerEx == null ) return;
        try {
            for ( VolumeUtil.Type type : mBackupWarnAudioTypeList ) {
                int mode = VolumeUtil.convertToMode(type);
                int volume = mCarAudioManagerEx.getVolumeForLas(mode);
                mBackupWarnLastVolume.put(type, volume);
                mBackupWarnAudioChange.put(type, false);
                Log.d(TAG, "applyBackupWarn:type="+type+", mode="+mode+", volume="+volume);
            }
            mIsBackupWranApplying = true;
            for ( VolumeUtil.Type type : mBackupWarnAudioTypeList ) {
                int mode = VolumeUtil.convertToMode(type);
                int volume = mCarAudioManagerEx.getVolumeForLas(mode);
                if ( type == VolumeUtil.Type.BT_AUDIO ) {
                    if ( volume > BACKUP_WRAN_BT_MIN ) {
                        float down_vol = volume * BACKUP_WRAN_BT_DWON_RATE; 
                        mBTAudioChangeVolume = (int)Math.floor(down_vol);
                        if ( mBTAudioChangeVolume > BACKUP_WRAN_BT_MAX ) mBTAudioChangeVolume = BACKUP_WRAN_BT_MAX;
                        else if ( mBTAudioChangeVolume < BACKUP_WRAN_BT_MIN ) mBTAudioChangeVolume = BACKUP_WRAN_BT_MIN;
                        setAudioVolume(mode, mBTAudioChangeVolume);
                    }
                } else {
                    if ( volume > BACKUP_WRAN_VOLUME ) 
                        setAudioVolume(mode, BACKUP_WRAN_VOLUME); 
                }
            }
            mIsBackupWranApplying = false;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Failed to get current volume", e);
        }
    }

    public boolean checkBackupWarn(int mode, int volume) {
        if ( !isBackupWarn() ) return false; 

        for ( VolumeUtil.Type type : mBackupWarnAudioTypeList ) {
            if ( mode != VolumeUtil.convertToMode(type) ) continue; 
            if ( type == VolumeUtil.Type.BT_AUDIO ) {
                if ( !mIsBackupWranApplying && volume != mBTAudioChangeVolume ) 
                    mBackupWarnAudioChange.put(type, true);
            } else {
                if ( !mIsBackupWranApplying && volume != BACKUP_WRAN_VOLUME ) {
                    mBackupWarnAudioChange.put(type, true);
                    if ( type == VolumeUtil.Type.RADIO_AM || type == VolumeUtil.Type.RADIO_FM 
                    || type == VolumeUtil.Type.USB || type == VolumeUtil.Type.ONLINE_MUSIC 
                    || type == VolumeUtil.Type.CARLIFE_MEDIA) {
                        mBackupWarnAudioChange.put(VolumeUtil.Type.RADIO_AM, true);
                        mBackupWarnAudioChange.put(VolumeUtil.Type.RADIO_FM, true);
                        mBackupWarnAudioChange.put(VolumeUtil.Type.USB, true);
                        mBackupWarnAudioChange.put(VolumeUtil.Type.ONLINE_MUSIC, true);
                        mBackupWarnAudioChange.put(VolumeUtil.Type.CARLIFE_MEDIA, true);
                    }
                }
            }
            Log.d(TAG, "checkBackupWarn:type="+type+", las volume="+volume);
        }

        return false; 
    }

    private void cancelChattering() {
        if ( mChatteringTask == null ) return;
        if ( mChatteringTask.scheduledExecutionTime() > 0 ) {
            Log.d(TAG, "cancelChattering");
            mChatteringTask.cancel();
            mTimer.purge();
            mChatteringTask =  null;
        }
    }
    private final CarSensorManagerEx.OnSensorChangedListenerEx mSensorChangeListener =
        new CarSensorManagerEx.OnSensorChangedListenerEx () {
        public void onSensorChanged(final CarSensorEvent event) {
            switch (event.sensorType) {
                case CarSensorManagerEx.SENSOR_TYPE_GEAR: {
                    GearData gear = event.getGearData(null);
                    Log.d(TAG, "onSensorChanged:SENSOR_TYPE_GEAR:gear=" + gear.gear);
                    if(gear.gear == CarSensorEventEx.GEAR_R) {
                        cancelChattering(); 
                        mChatteringTask = new TimerTask() {
                            @Override
                            public void run() {
                                Log.d(TAG, "gearScenario start in thread");
                                gearScenario(true);
                            }
                        };
                        mTimer.schedule(mChatteringTask, CHATTERING_TIME);
                    } else {
                        cancelChattering(); 
                        gearScenario(false);
                    }
                    break;
                }
                case CarSensorManagerEx.SENSOR_TYPE_IGNITION_STATE: {
                    int state = event.intValues[0];
                    Log.d(TAG, "onSensorChanged:SENSOR_TYPE_IGNITION_STATE="+state);
                    if ( state == CarSensorEvent.IGNITION_STATE_LOCK 
                        || state == CarSensorEvent.IGNITION_STATE_OFF
                        || state == CarSensorEvent.IGNITION_STATE_ACC ) {
                        if ( !mIsIGNOff ) mIsIGNOff = true;
                    } else if ( state == CarSensorEvent.IGNITION_STATE_ON
                        || state == CarSensorEvent.IGNITION_STATE_START ) {
                        if ( mIsIGNOff ) mIsIGNOff = false;
                    }
                    break; 
                }
                default: break;
            }
        }

        public void onSensorChanged(final CarSensorEventEx event) {
        }
    };

    public void userRefresh() {
        if ( mContentResolver == null ) return; 
        if ( mModeObserver != null )  {
            mContentResolver.unregisterContentObserver(mModeObserver); 
        }
        Log.d(TAG, "userRefresh");
        mModeObserver = createBackupWranObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.SOUND_PRIORITY_BACKUP_WARNING_ON), 
            false, mModeObserver, UserHandle.USER_CURRENT); 

        if ( isBackupWarn() ) applyBackupWarn();
    }
}