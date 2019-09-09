package com.humaxdigital.automotive.systemui.volumedialog;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import android.provider.Settings;
import android.database.ContentObserver;

import android.media.AudioManager; 

import android.app.Service;

import android.extension.car.CarEx;
import android.extension.car.CarAudioManagerEx;
import android.car.CarNotConnectedException;
import android.car.media.ICarVolumeCallback;
import android.extension.car.util.AudioTypes;
import android.extension.car.settings.CarExtraSettings;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class VolumeControlService extends Service {
    private static final String TAG = "VolumeControlService";

    private static final String ACTION_VOLUME_SETTINGS_STARTED = "com.humaxdigital.setup.ACTION_VOLUME_SETTINGS_STARTED";
    private static final String ACTION_VOLUME_SETTINGS_STOPPED = "com.humaxdigital.setup.ACTION_VOLUME_SETTINGS_STOPPED";

    public static abstract class VolumeCallback {
        public void onVolumeChanged(VolumeUtil.Type type, int max, int val) {}
        public void onMuteChanged(VolumeUtil.Type type, int max, int val, boolean mute) {}
    }

    public class LocalBinder extends Binder {
        VolumeControlService getService() {
            return VolumeControlService.this;
        }
    }

    private final Binder mBinder = new LocalBinder();

    private ArrayList<VolumeCallback> mCallbacks = new ArrayList<>();
    private Object mServiceReady = new Object();
    private CarExtensionClient mCarClient; 
    private CarAudioManagerEx mCarAudioManagerEx;
    private AudioManager mAudioManager; 

    private ScenarioQuiteMode mQuiteMode = null;
    private ScenarioBackupWran mBackupWran = null;
    private ScenarioVCRMLog mVCRMLog = null; 
    private boolean mIsSettingsActivity = false;
    private boolean mIsShow = false; 

    private ContentObserver mUserSwitchingObserver; 

    private final int VOLUME_EVENT_BLOCKING_MS_TIME = 4000; 
/*    private boolean mIsEventBlocking = true;*/

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mCarClient = new CarExtensionClient(this);
        if ( mCarClient != null ) 
            mCarClient.registerListener(mCarClientListener).connect();
        mAudioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE); 
        
        registUserSwicher();

        mQuiteMode = new ScenarioQuiteMode(this).init();
        mBackupWran = new ScenarioBackupWran(this).init();
        mVCRMLog = new ScenarioVCRMLog(); 

        registReceiver();
 
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregistReceiver();

        if ( mCarClient != null ) 
            mCarClient.unregisterListener().disconnect();
        unregistUserSwicher();
        cleanupAudioManager();
        
        if ( mQuiteMode != null ) mQuiteMode.deinit();
        if ( mBackupWran != null ) mBackupWran.deinit();
        mQuiteMode = null;
        mBackupWran = null;

        if ( mVCRMLog != null ) mVCRMLog.destroy();
        mVCRMLog = null;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    private void registReceiver() {
        Log.d(TAG, "registReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_VOLUME_SETTINGS_STARTED);
        filter.addAction(ACTION_VOLUME_SETTINGS_STOPPED);
        registerReceiverAsUser(mApplicationActionReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistReceiver() {
        Log.d(TAG, "unregistReceiver");
        unregisterReceiver(mApplicationActionReceiver);
    }

    private final BroadcastReceiver mApplicationActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if ( action == null ) return;
            Log.d(TAG, "mApplicationActionReceiver="+action);
            switch(action) {
                case ACTION_VOLUME_SETTINGS_STARTED: {
                    mIsSettingsActivity = true;
                    break;
                }
                
                case ACTION_VOLUME_SETTINGS_STOPPED: {
                    mIsSettingsActivity = false;
                    break;
                }
            }
        }
    };


    public void registerCallback(VolumeCallback callback) {
        if (callback == null)
            return;
        Log.d(TAG, "registerCallback");
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(VolumeCallback callback) {
        if (callback == null)
            return;
        Log.d(TAG, "unregisterCallback");
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    public void requestRefresh(final Runnable r, final Handler h) {
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                Log.d(TAG, "doInBackground");
                synchronized (mServiceReady) {
                    while (mCarAudioManagerEx == null) {
                        try {
                            Log.d(TAG, "system ready"); 
                            mServiceReady.wait();
                        } catch (InterruptedException e) {
                            return null;
                        }
                        return null;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                Log.d(TAG, "onPostExecute");
                h.post(r);
            }
        };
        task.execute();
    }

    public VolumeUtil.Type getCurrentVolumeType() {
        VolumeUtil.Type type = VolumeUtil.Type.UNKNOWN;
        type = VolumeUtil.convertToType(getCarAudioCurrentMode()); 
        Log.d(TAG, "getCurrentVolumeType=" + type);
        return type;
    }

    public int getVolume(VolumeUtil.Type type) {
        int volume = 0;
        volume = getCarAudioVolume(VolumeUtil.convertToMode(type)); 
        Log.d(TAG, "getVolume type=" + type + ", volume=" + volume);
        return volume;
    }

    public int getVolumeMax(VolumeUtil.Type type) {
        int max = 0;
        max = getCarAudioVolumeMax(VolumeUtil.convertToMode(type)); 
        Log.d(TAG, "getVolumeMax type=" + type + ", max=" + max);
        return max;
    }

    public boolean getCurrentMute() {
        if ( mAudioManager == null ) return false;
        // boolean mute = mAudioManager.isMasterMute(); 
        boolean mute = false;
        mute = mCarAudioManagerEx.getAudioMuteStatus(AudioTypes.AUDIO_MUTE_ID_USER);
        //  TODO: (Audio) mute = mCarAudioManagerEx.getAudioMute(); 
        Log.d(TAG, "getCurrentMute="+mute);
        return mute;
    }

    private void setMasterMute(boolean mute) {
        if ( mAudioManager == null ) return;
        int flags = AudioManager.FLAG_FROM_KEY | AudioManager.FLAG_SHOW_UI; 
        Log.d(TAG, "set="+mute+", flags="+flags);
        //mAudioManager.adjustSuggestedStreamVolume(
        //    mute?AudioManager.ADJUST_MUTE:AudioManager.ADJUST_UNMUTE,
        //    AudioManager.STREAM_MUSIC, flags);
        mCarAudioManagerEx.setAudioMute(AudioTypes.AUDIO_MUTE_ID_USER, 
            ((mute==true) ? AudioTypes.AUDIO_MUTE_ON : AudioTypes.AUDIO_MUTE_OFF), 
            AudioTypes.AUDIO_MUTE_SHOW_ICON);
        //  TODO: (Audio) mCarAudioManagerEx.setAudioMute(mute); 
    }

    public boolean setVolume(VolumeUtil.Type type, int volume) {

        if ( (mQuiteMode != null) && mQuiteMode.checkQuietMode(VolumeUtil.convertToMode(type), volume) ) return false;
        if ( (mBackupWran != null) && mBackupWran.checkBackupWarn(VolumeUtil.convertToMode(type), volume) ) return false;

        if ( mAudioManager != null && getCurrentMute() ) setMasterMute(false); 

        setAudioVolume(VolumeUtil.convertToMode(type), volume); 
        Log.d(TAG, "setVolume type=" + type + ", volume=" + volume);
        return true;
    }

    public void onShow(boolean show) {
        mIsShow = show; 
    }

    private void cleanupAudioManager() {
        Log.d(TAG, "cleanupAudioManager");
        try {
            if (mCarAudioManagerEx != null)
                mCarAudioManagerEx.unregisterVolumeCallback(mVolumeChangeCallback.asBinder());
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        mCarAudioManagerEx = null;
    }

    private final CarExtensionClient.CarExClientListener mCarClientListener = 
        new CarExtensionClient.CarExClientListener() {
        @Override
        public void onConnected() {
            if ( mCarClient == null ) return;
            mCarAudioManagerEx = mCarClient.getAudioManagerEx(); 
            synchronized (mServiceReady) {
                mServiceReady.notify(); 
            }
            try {
                if ( mCarAudioManagerEx != null ) {
                    mCarAudioManagerEx.registerCallbackForStartupVolume();
                    mCarAudioManagerEx.registerVolumeCallback(mVolumeChangeCallback.asBinder());
                }
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!", e);
            } 

            if ( mQuiteMode != null ) mQuiteMode.fetchCarAudioManagerEx(mCarAudioManagerEx);
            if ( mBackupWran != null ) {
                mBackupWran.fetchCarAudioManagerEx(mCarAudioManagerEx);
                mBackupWran.fetchCarSensorManagerEx(mCarClient.getSensorManagerEx()); 
            }
            if ( mVCRMLog != null ) mVCRMLog.fetch(mCarAudioManagerEx).updateLogAll();
        }
        @Override
        public void onDisconnected() {
            cleanupAudioManager();
        }
    }; 

    private final ICarVolumeCallback mVolumeChangeCallback = new ICarVolumeCallback.Stub() {
        @Override
        public void onGroupVolumeChanged(int groupId, int flags) {
            int mode = getCarAudioCurrentMode(); 
            int max = getCarAudioVolumeMax(mode); 
            int volume = getCarAudioVolume(mode);

            Log.d(TAG, "onGroupVolumeChanged:mode="+mode+", max="+max+", volume="+volume + "flags="+flags);

            if ( (mQuiteMode != null) && mQuiteMode.checkQuietMode(mode, volume) ) return;
            if ( (mBackupWran != null) && mBackupWran.checkBackupWarn(mode, volume) ) return;
            if ( mVCRMLog != null ) mVCRMLog.updateLog(mode, volume);

            if ( mIsSettingsActivity ) return;
            if ( isExceptionVolume(VolumeUtil.convertToType(mode)) ) return;

            if ((flags & AudioManager.FLAG_SHOW_UI) == 0){
                boolean isNeedToShowUI = mQuiteMode.isNeedToShowUI(); 
                Log.d(TAG, "isNeedToShowUI="+isNeedToShowUI); 
                if ( !isNeedToShowUI ) {
                    Log.d(TAG, "SKIP broadcastEventVolumeChange : mIsShow ="+mIsShow);
                    if ( !mIsShow ) {
                        return;
                    } 
                }
            }

            if ( isUserSwitching() ) return; 

            broadcastEventVolumeChange(mode, max, volume);
        }

        @Override
        public void onMasterMuteChanged(int flags) {
            Log.d(TAG, "onMasterMuteChanged");
            int mode = getCarAudioCurrentMode(); 
            int max = getCarAudioVolumeMax(mode); 
            int volume = getCarAudioVolume(mode);
            boolean mute = getCurrentMute();

            if ( (mQuiteMode != null) && mQuiteMode.isQuiteMode() ) {
                int quite_max = mQuiteMode.getQuiteModeMax(mode); 
                if ( quite_max < volume ) volume = quite_max; 
            }

            if ( mIsSettingsActivity ) return;

            if ((flags & AudioManager.FLAG_SHOW_UI) == 0){
                Log.d(TAG, "SKIP onMasterMuteChanged");
                return;
            }
            
            if ( isUserSwitching() ) return; 

            broadcastEventMuteChange(mode, max, volume, mute);
        }
    };

    private boolean isUserSwitching() {
        boolean ret = false; 
        int isUserSwitching = Settings.Global.getInt(this.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_USER_SWITCHING_START_FINISH, 
            CarExtraSettings.Global.FALSE);
        if ( isUserSwitching == CarExtraSettings.Global.TRUE ) ret = true;
        else ret = false; 
        Log.d(TAG, "isUserSwitching="+ret); 
        return ret; 
    }

    private boolean isExceptionVolume(VolumeUtil.Type type) {
        boolean isException = false; 
        switch(type) {
/*            case BAIDU_NAVI: */
            case BEEP: {
                isException = true;
                break;
            }
        }
        Log.d(TAG, "isExceptionVolume="+isException+", type="+type);
        return isException; 
    }

    private void broadcastEventVolumeChange(int mode, int max, int value) {
        Log.d(TAG, "broadcastEventVolumeChange:mode=" + mode + ", max=" + max + ", value=" + value);
        synchronized (mCallbacks) {
            for (VolumeCallback callback : mCallbacks) {
                callback.onVolumeChanged(VolumeUtil.convertToType(mode), max, value);
            }
        }
    }

    private void broadcastEventMuteChange(int mode, int max, int volume, boolean mute) {
        Log.d(TAG, "broadcastEventMuteChange:mode=" + mode + ", max=" + max + ", volume="+volume+", mute=" + mute);
        synchronized (mCallbacks) {
            for (VolumeCallback callback : mCallbacks) {
                callback.onMuteChanged(VolumeUtil.convertToType(mode), max, volume, mute);
            }
        }
    }

    private int getCarAudioCurrentMode() {
        int mode = 0; 

        if ( mCarAudioManagerEx == null ) return mode; 
        
        //try {
            mode = mCarAudioManagerEx.getCurrentAudioMode(); 
        //} catch (CarNotConnectedException e) {
        //    Log.e(TAG, "Failed to get current volume mode", e);
        //}

        return mode; 
    }

    private int getCarAudioVolume(int mode) {
        int volume = 0; 

        if ( mCarAudioManagerEx == null ) return volume; 

        try {
            volume = mCarAudioManagerEx.getVolumeForLas(mode); 
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Failed to get current volume", e);
        }

        return volume; 
    }

    private int getCarAudioVolumeMax(int mode) {
        int max = 45; 

        if ( mCarAudioManagerEx == null ) return max; 

        //try {
            // todo : get max 
            //max = mCarAudioManagerEx.getVolumeForLas(mode); 
        //} catch (CarNotConnectedException e) {
        //    Log.e(TAG, "Failed to get current volume", e);
        //}

        return max; 
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
	
    private void registUserSwicher() {
        Log.d(TAG, "registUserSwicher");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        this.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistUserSwicher() {
        this.unregisterReceiver(mUserChangeReceiver);
    }

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mQuiteMode != null ) mQuiteMode.userRefresh();
            if ( mBackupWran != null ) mBackupWran.userRefresh();
        }
    };
}
