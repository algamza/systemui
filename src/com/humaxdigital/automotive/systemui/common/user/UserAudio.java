package com.humaxdigital.automotive.systemui.common.user;

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

import com.humaxdigital.automotive.systemui.common.CONSTANTS;

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
        filter.addAction(CONSTANTS.ACTION_CHANGE_MIC_MUTE); 
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

    @Override
    public boolean isMasterMute() throws RemoteException {
        if ( mManager == null ) return false;
        boolean mute = mManager.isMasterMute();
        Log.d(TAG, "isMasterMute="+mute);
        return mute;
    }

    @Override
    public void setMasterMute(boolean mute) throws RemoteException {
        if ( mManager == null ) return;
        int flags = AudioManager.FLAG_FROM_KEY | AudioManager.FLAG_SHOW_UI; 
        Log.d(TAG, "set="+mute+", flags="+flags);
        mManager.adjustSuggestedStreamVolume(
            mute?AudioManager.ADJUST_MUTE:AudioManager.ADJUST_UNMUTE,
            AudioManager.STREAM_MUSIC, flags);
    }

    @Override
    public void performClick() throws RemoteException {
        if ( mManager == null ) return;
        mManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mManager == null ) return;
            switch(intent.getAction()) {
                case AudioManager.MASTER_MUTE_CHANGED_ACTION: {
                    try {
                        boolean mute = mManager.isMasterMute();
                        Log.d(TAG, "MASTER_MUTE_CHANGED_ACTION="+mute);
                        for ( IUserAudioCallback callback : mListeners ) {
                            callback.onMasterMuteChanged(mute);
                        }
                    } catch( RemoteException e ) {
                        Log.e(TAG, "error:"+e);
                    }
                    break;
                }
                case AudioManager.ACTION_MICROPHONE_MUTE_CHANGED: {
                    try {
                        boolean mute = mManager.isMicrophoneMute();
                        Log.d(TAG, "ACTION_MICROPHONE_MUTE_CHANGED="+mute);
                        for ( IUserAudioCallback callback : mListeners ) {
                            callback.onBluetoothMicMuteChanged(mute);
                        }
                    } catch( RemoteException e ) {
                        Log.e(TAG, "error:"+e);
                    }
                    break;
                }
                case CONSTANTS.ACTION_CHANGE_MIC_MUTE: {
                    try {
                        boolean mute = mManager.isMicrophoneMute();
                        Log.d(TAG, "CONSTANTS.ACTION_CHANGE_MIC_MUTE="+mute);
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