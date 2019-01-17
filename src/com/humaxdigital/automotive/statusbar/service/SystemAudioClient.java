package com.humaxdigital.automotive.statusbar.service;

import android.os.Bundle; 
import android.os.Binder;
import android.os.IBinder;
import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.app.Service;

import android.extension.car.CarEx;
import android.extension.car.CarAudioManagerEx;
import android.media.AudioManager; 
import android.car.CarNotConnectedException;
import android.car.media.ICarVolumeCallback;

import java.util.ArrayList;
import java.util.HashMap; 
import java.util.Map; 
import java.util.List;
import android.util.Log;

public class SystemAudioClient {
    private static final String TAG = "SystemAudioClient";
    public enum AudioType {
        AUDIO, 
        NAVIGATION,
        BLUETOOTH_MIC
    }

    private CarEx mCarEx;
    private CarAudioManagerEx mCarAudioManagerEx;
    private AudioManager mAudioManager; 
    private Map<AudioType, Boolean> mAudioMuteState = new HashMap<>(); 

    public interface SystemAudioCallback {
        void onMuteChanged(AudioType type, boolean mute); 
    }

    private Context mContext; 
    private List<SystemAudioCallback> mListeners = new ArrayList<>(); 

    public SystemAudioClient(Context context) {
        if ( context == null ) return; 
        Log.d(TAG, "SystemAudioClient"); 
        mContext = context; 
    }

    public void connect() {
        if ( mContext == null ) return; 
        IntentFilter filter = new IntentFilter(); 
        filter.addAction(AudioManager.ACTION_MICROPHONE_MUTE_CHANGED); 
        mContext.registerReceiverAsUser(mAudioReceiver, UserHandle.ALL, filter, null, null);
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE); 
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            mCarEx = CarEx.createCar(mContext, mCarServiceConnection);
            mCarEx.connect();
        }
    }

    private void fetch() {
        if ( mCarAudioManagerEx == null || mAudioManager == null ) return; 
        mAudioMuteState.put(AudioType.AUDIO, mAudioManager.isMasterMute()); 
        mAudioMuteState.put(AudioType.BLUETOOTH_MIC, mAudioManager.isMicrophoneMute()); 
        mAudioMuteState.put(AudioType.NAVIGATION, false); 
    }

    public void disconnect() {
        Log.d(TAG, "disconnect"); 
        cleanupAudioManager(); 
    }

    public void registerCallback(SystemAudioCallback callback) {
        if ( callback == null ) return; 
        mListeners.add(callback); 
    }

    public void unregisterCallback(SystemAudioCallback callback) {
        if ( callback == null ) return; 
        mListeners.remove(callback);
    }

    public boolean isMute(AudioType type) {
        if ( mAudioManager == null || mCarAudioManagerEx == null ) return false; 
        boolean mute = false; 
        switch(type) {
            case BLUETOOTH_MIC: mute = mAudioManager.isMicrophoneMute(); break;
            case AUDIO: mute = mAudioManager.isMasterMute(); break; 
            case NAVIGATION: break;
            default: break;
        }
        Log.d(TAG, "isMute:type="+type+", mute="+mute); 
        return mute;
    }

    private void cleanupAudioManager() {
        Log.d(TAG, "cleanupAudioManager");
        if ( mContext != null ) mContext.unregisterReceiver(mAudioReceiver); 
        
        try {
            if (mCarAudioManagerEx != null)
                mCarAudioManagerEx.unregisterVolumeCallback(mVolumeChangeCallback.asBinder());
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        mCarAudioManagerEx = null;
        if (mCarEx != null)
            mCarEx.disconnect();
    }

    private void broadcastEvent(AudioType type, boolean mute) {
        for ( SystemAudioCallback callback : mListeners ) {
            callback.onMuteChanged(type, mute);
        }
    }

    private final ICarVolumeCallback mVolumeChangeCallback = new ICarVolumeCallback.Stub() {
        @Override
        public void onGroupVolumeChanged(int groupId, int flags) {
            Log.d(TAG, "onGroupVolumeChanged");
        }

        @Override
        public void onMasterMuteChanged(int flags) {
            Log.d(TAG, "onMasterMuteChanged");
            broadcastEvent(AudioType.AUDIO, isMute(AudioType.AUDIO)); 
        }
    };

    private final BroadcastReceiver mAudioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case AudioManager.ACTION_MICROPHONE_MUTE_CHANGED: {
                    if ( mAudioManager == null ) break;
                    Log.d(TAG, "ACTION_MICROPHONE_MUTE_CHANGED");
                    broadcastEvent(AudioType.BLUETOOTH_MIC, isMute(AudioType.BLUETOOTH_MIC)); 
                    break; 
                }
                default: break;
            }
        }
    };

    private final ServiceConnection mCarServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            try {
                mCarAudioManagerEx = (CarAudioManagerEx) mCarEx.getCarManager(android.car.Car.AUDIO_SERVICE);
                if (mCarAudioManagerEx == null)
                    return;
                mCarAudioManagerEx.registerVolumeCallback(mVolumeChangeCallback.asBinder());
                fetch(); 
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceConnected");
            cleanupAudioManager();
        }
    };
}
