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
import android.net.Uri;

import android.media.AudioManager; 

import android.app.Service;

import android.extension.car.CarEx;
import android.extension.car.CarAudioManagerEx;
import android.car.CarNotConnectedException;
import android.car.media.ICarVolumeCallback;
import android.extension.car.util.AudioTypes;
import android.extension.car.settings.CarExtraSettings;

import android.telephony.TelephonyManager;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; 

import com.humaxdigital.automotive.systemui.common.car.CarExClient;
import com.humaxdigital.automotive.systemui.common.logger.VCRMLogger;
import com.humaxdigital.automotive.systemui.common.CONSTANTS;

public class VolumeControlService extends Service {
    private static final String TAG = "VolumeControlService";

    public interface VolumeCallback {
        public void onVolumeChanged(VolumeUtil.Type type, int max, int val); 
        public void onMuteChanged(VolumeUtil.Type type, int max, int val, boolean mute); 
        default public void onShowUI(boolean show) {};
        default public void onUserChanged() {}; 
    }

    public class LocalBinder extends Binder {
        VolumeControlService getService() {
            return VolumeControlService.this;
        }
    }

    private final Binder mBinder = new LocalBinder();

    private ArrayList<VolumeCallback> mCallbacks = new ArrayList<>();
    private Object mServiceReady = new Object();
    private CarAudioManagerEx mCarAudioManagerEx;
    private AudioManager mAudioManager; 
    private TelephonyManager mTelephonyManager;
    private ContentObserver mUserSwitchingObserver; 

    private ScenarioQuiteMode mQuiteMode = null;
    private ScenarioBackupWran mBackupWran = null;
    private boolean mIsSettingsActivity = false;
    private boolean mIsSettingsDefault = false; 
    private boolean mIsShow = false; 

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        CarExClient.INSTANCE.connect(this, mCarClientListener); 

        mAudioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE); 
        mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        registUserSwicher();

        mQuiteMode = new ScenarioQuiteMode(this).init();
        mQuiteMode.registListener(mQuiteModeListener);
        mBackupWran = new ScenarioBackupWran(this).init();

        createObserver(); 
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

        CarExClient.INSTANCE.disconnect(mCarClientListener); 
        
        unregistUserSwicher();
        cleanupAudioManager();
        
        if ( mQuiteMode != null ) {
            mQuiteMode.unregistListener(mQuiteModeListener);
            mQuiteMode.deinit();
        }
        if ( mBackupWran != null ) mBackupWran.deinit();
        mQuiteMode = null;
        mBackupWran = null;

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
        filter.addAction(CONSTANTS.ACTION_VOLUME_SETTINGS_STARTED);
        filter.addAction(CONSTANTS.ACTION_VOLUME_SETTINGS_STOPPED);
        filter.addAction(CONSTANTS.ACTION_DEFAULT_SOUND_SETTINGS_STARTED); 
        filter.addAction(CONSTANTS.ACTION_DEFAULT_SOUND_SETTINGS_STOPPED); 
        registerReceiverAsUser(mApplicationActionReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistReceiver() {
        Log.d(TAG, "unregistReceiver");
        unregisterReceiver(mApplicationActionReceiver);
    }

    private void createObserver() {
        mUserSwitchingObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if ( isUserSwitching() ) return; 
                synchronized (mCallbacks) {
                    for (VolumeCallback callback : mCallbacks) {
                        callback.onUserChanged();
                    }
                }
            }
        };
        
        this.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(CarExtraSettings.Global.USERPROFILE_USER_SWITCHING_START_FINISH), 
            false, mUserSwitchingObserver, UserHandle.USER_CURRENT); 
    }

    private final BroadcastReceiver mApplicationActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( action == null ) return;
            Log.d(TAG, "mApplicationActionReceiver="+action);
            switch(action) {
                case CONSTANTS.ACTION_VOLUME_SETTINGS_STARTED: {
                    mIsSettingsActivity = true;
                    if ( mQuiteMode != null ) mQuiteMode.setSettingsActivityState(mIsSettingsActivity);
                    if ( mBackupWran != null ) mBackupWran.setSettingsActivityState(mIsSettingsActivity);  
                    break;
                }
                case CONSTANTS.ACTION_VOLUME_SETTINGS_STOPPED: {
                    mIsSettingsActivity = false;
                    if ( mQuiteMode != null ) mQuiteMode.setSettingsActivityState(mIsSettingsActivity);
                    if ( mBackupWran != null ) mBackupWran.setSettingsActivityState(mIsSettingsActivity);  
                    break;
                }
                case CONSTANTS.ACTION_DEFAULT_SOUND_SETTINGS_STARTED: {
                    mIsSettingsDefault = true;
                    if ( mQuiteMode != null ) mQuiteMode.setSettingsDefaultState(mIsSettingsDefault);
                    if ( mBackupWran != null ) mBackupWran.setSettingsDefaultState(mIsSettingsDefault);  
                    break;
                }
                case CONSTANTS.ACTION_DEFAULT_SOUND_SETTINGS_STOPPED: {
                    mIsSettingsDefault = false;
                    if ( mQuiteMode != null ) mQuiteMode.setSettingsDefaultState(mIsSettingsDefault);
                    if ( mBackupWran != null ) mBackupWran.setSettingsDefaultState(mIsSettingsDefault);  
                    break;
                }
            }
        }
    };


    public void registerCallback(VolumeCallback callback) {
        Log.d(TAG, "registerCallback");
        synchronized (mCallbacks) {
            mCallbacks.add(Objects.requireNonNull(callback));
        }
    }

    public void unregisterCallback(VolumeCallback callback) {
        Log.d(TAG, "unregisterCallback");
        synchronized (mCallbacks) {
            mCallbacks.remove(Objects.requireNonNull(callback));
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
        if ( mCarAudioManagerEx == null ) return false;
        boolean mute = false;
        int call_state = mTelephonyManager.getCallState(); 
        if( call_state == mTelephonyManager.CALL_STATE_RINGING 
            || call_state == mTelephonyManager.CALL_STATE_OFFHOOK ) {
			mute = mCarAudioManagerEx.getAudioMuteStatus(AudioTypes.AUDIO_MUTE_ID_FORCED_MEDIA);
		} else {
			mute = mCarAudioManagerEx.getAudioMuteStatus(AudioTypes.AUDIO_MUTE_ID_USER);
		}
        Log.d(TAG, "getCurrentMute="+mute+", call state="+call_state);
        return mute;
    }

    public void setMasterMute(boolean mute) {
        if ( mCarAudioManagerEx == null ) return;
        int flags = AudioManager.FLAG_FROM_KEY | AudioManager.FLAG_SHOW_UI; 
        Log.d(TAG, "set="+mute+", flags="+flags);
        mCarAudioManagerEx.setAudioMute(AudioTypes.AUDIO_MUTE_ID_USER, 
            ((mute==true) ? AudioTypes.AUDIO_MUTE_ON : AudioTypes.AUDIO_MUTE_OFF), 
            AudioTypes.AUDIO_MUTE_SHOW_ICON);
    }

    public void setMasterMuteShowUI(boolean mute) {
        if ( mCarAudioManagerEx == null ) return;
        int flags = AudioManager.FLAG_FROM_KEY | AudioManager.FLAG_SHOW_UI; 
        Log.d(TAG, "set="+mute+", flags="+flags);
        mCarAudioManagerEx.setAudioMute(AudioTypes.AUDIO_MUTE_ID_USER, 
            ((mute==true) ? AudioTypes.AUDIO_MUTE_ON : AudioTypes.AUDIO_MUTE_OFF), 
            AudioTypes.AUDIO_MUTE_SHOW_UI);
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

    private void onCarClientConnected() {
        mCarAudioManagerEx = CarExClient.INSTANCE.getAudioManager(); 
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
            mBackupWran.fetchCarSensorManagerEx(CarExClient.INSTANCE.getSensorManager()); 
        }
    }

    private final CarExClient.CarExClientListener mCarClientListener = 
        new CarExClient.CarExClientListener() {
        @Override
        public void onConnected() {
            onCarClientConnected();
        }
        @Override
        public void onDisconnected() {
            cleanupAudioManager();
        }
    }; 

    private VCRMLogger.VolumeType convertToVCRMVolumeType(int mode) {
        VCRMLogger.VolumeType type = VCRMLogger.VolumeType.UNKNOWN;
        switch (mode) {
            case 0: type = VCRMLogger.VolumeType.UNKNOWN; break;
            case 1: type = VCRMLogger.VolumeType.RADIO_FM; break; 
            case 2: type = VCRMLogger.VolumeType.RADIO_AM; break; 
            case 3: type = VCRMLogger.VolumeType.USB; break; 
            case 4: type = VCRMLogger.VolumeType.ONLINE_MUSIC; break; 
            case 5: type = VCRMLogger.VolumeType.BT_AUDIO; break; 
            case 6: type = VCRMLogger.VolumeType.BT_PHONE_RING; break; 
            case 7: type = VCRMLogger.VolumeType.BT_PHONE_CALL; break; 
            case 8: type = VCRMLogger.VolumeType.CARLIFE_MEDIA; break; 
            case 9: type = VCRMLogger.VolumeType.CARLIFE_NAVI; break; 
            case 10: type = VCRMLogger.VolumeType.CARLIFE_TTS; break; 
            case 11: type = VCRMLogger.VolumeType.BAIDU_MEDIA; break; 
            case 12: type = VCRMLogger.VolumeType.BAIDU_ALERT; break; 
            case 13: type = VCRMLogger.VolumeType.BAIDU_VR_TTS; break; 
            case 14: type = VCRMLogger.VolumeType.BAIDU_NAVI; break; 
            case 15: type = VCRMLogger.VolumeType.EMERGENCY_CALL; break; 
            case 16: type = VCRMLogger.VolumeType.ADVISOR_CALL; break; 
            case 17: type = VCRMLogger.VolumeType.BEEP; break; 
            case 18: type = VCRMLogger.VolumeType.WELCOME_SOUND; break; 
            case 19: type = VCRMLogger.VolumeType.SETUP_GUIDE; break; 
            default: break;
        }
        return type;
    }

    private final ICarVolumeCallback mVolumeChangeCallback = new ICarVolumeCallback.Stub() {
        @Override
        public void onGroupVolumeChanged(int groupId, int flags) {
            int mode = getCarAudioCurrentMode(); 
            int max = getCarAudioVolumeMax(mode); 
            int volume = getCarAudioVolume(mode);

            Log.d(TAG, "onGroupVolumeChanged:mode="+mode+", max="+max+", volume="+volume + "flags="+flags);

            if ( (mQuiteMode != null) && mQuiteMode.checkQuietMode(mode, volume) ) return;
            if ( (mBackupWran != null) && mBackupWran.checkBackupWarn(mode, volume) ) return;

            VCRMLogger.changedVolume(convertToVCRMVolumeType(mode), volume);

            if ( mIsSettingsActivity && mode != AudioTypes.LAS_BAIDU_VR_TTS ) return;
            if ( isExceptionVolume(VolumeUtil.convertToType(mode)) ) return;

            if ((flags & AudioManager.FLAG_SHOW_UI) == 0){
                Log.d(TAG, "SKIP broadcastEventVolumeChange : mIsShow ="+mIsShow);
                if ( !mIsShow ) {
                    return;
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
                broadcastEventShowUI(false); 
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

    private void broadcastEventShowUI(boolean show) {
        Log.d(TAG, "broadcastEventShowUI:show=" + show);
        synchronized (mCallbacks) {
            for (VolumeCallback callback : mCallbacks) {
                callback.onShowUI(show);
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

    private final ScenarioQuiteMode.ScenarioQuiteModeListener mQuiteModeListener = 
        new ScenarioQuiteMode.ScenarioQuiteModeListener() {
        @Override
        public void onShowUI(boolean show) {
            Log.d(TAG, "ScenarioQuiteModeListener:"+show);
            broadcastEventShowUI(show);
        }
    }; 
}