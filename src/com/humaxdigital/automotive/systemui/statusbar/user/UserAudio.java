package com.humaxdigital.automotive.systemui.statusbar.user;

import android.os.UserHandle;
import android.os.RemoteException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager; 

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class UserAudio extends IUserAudio.Stub {
    private final String TAG = "UserAudio";
    private final PerUserService mService; 
    private AudioManager mManager;
    private Context mContext; 

    private List<IUserAudioCallback> mListeners = new ArrayList<>(); 

    public UserAudio(PerUserService service) {
        Log.d(TAG, "UserAudio");
        mService = service; 
        if ( service == null ) return;
        mContext = mService.getApplicationContext(); 
        if ( mContext == null ) return;
        mManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE); 
        IntentFilter filter = new IntentFilter(); 
        filter.addAction(AudioManager.ACTION_MICROPHONE_MUTE_CHANGED); 
        filter.addAction(AudioManager.MASTER_MUTE_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver,filter, null, null);
    }

    public void destroy() {
        if ( mContext != null ) mContext.unregisterReceiver(mReceiver); 
        mListeners.clear();
        mManager = null;
        mContext = null;
    }

    @Override
    public void registCallback(IUserAudioCallback callback) throws RemoteException {
        Log.d(TAG, "registCallback");
        mListeners.add(callback);
    }

    @Override
    public void unregistCallback(IUserAudioCallback callback) throws RemoteException {
        Log.d(TAG, "unregistCallback");
        mListeners.remove(callback);
    }

    @Override
    public boolean isAudioMute() throws RemoteException {
        if ( mManager == null ) return false;
        boolean mute = mManager.isMasterMute();
        Log.d(TAG, "isAudioMute="+mute);
        return mute;
    }

    @Override
    public boolean isBluetoothMicMute() throws RemoteException {
        if ( mManager == null ) return false;
        boolean mute = mManager.isMicrophoneMute();
        Log.d(TAG, "isBluetoothMicMute="+mute);
        return mute;
    }

    @Override
    public boolean isNavigationMute() throws RemoteException {
        return false;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mManager == null ) return;
            switch(intent.getAction()) {
                case AudioManager.MASTER_MUTE_CHANGED_ACTION: {
                    Log.d(TAG, "MASTER_MUTE_CHANGED_ACTION");
                    try {
                        boolean mute = mManager.isMasterMute();
                        for ( IUserAudioCallback callback : mListeners ) {
                            callback.onAudioMuteChanged(mute);
                        }
                    } catch( RemoteException e ) {
                        Log.e(TAG, "error:"+e);
                    }
                    break;
                }
                case AudioManager.ACTION_MICROPHONE_MUTE_CHANGED: {
                    Log.d(TAG, "ACTION_MICROPHONE_MUTE_CHANGED");
                    try {
                        boolean mute = mManager.isMicrophoneMute();
                        for ( IUserAudioCallback callback : mListeners ) {
                            callback.onBluetoothMicMuteChanged(mute);
                        }
                    } catch( RemoteException e ) {
                        Log.e(TAG, "error:"+e);
                    }
                    break;
                }
            }
        }
    };
}