package com.humaxdigital.automotive.statusbar.droplist.user;

import android.os.UserHandle;
import android.os.RemoteException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager; 

import android.util.Log;

public class UserAudio extends IUserAudio.Stub {
    private final String TAG = "UserAudio";
    private final UserDroplistService mService; 
    private AudioManager mManager;
    private Context mContext; 

    private IUserAudioCallback mUserAudioCallback = null;

    public UserAudio(UserDroplistService service) {
        Log.d(TAG, "UserAudio");
        mService = service; 
        if ( service == null ) return;
        mContext = mService.getApplicationContext(); 
        if ( mContext == null ) return;
        mManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE); 
        mContext.registerReceiver(mReceiver, 
            new IntentFilter(AudioManager.MASTER_MUTE_CHANGED_ACTION), null, null);
    }

    public void destroy() {
        if ( mContext != null ) mContext.unregisterReceiver(mReceiver); 
        mManager = null;
        mContext = null;
    }

    @Override
    public void registCallback(IUserAudioCallback callback) throws RemoteException {
        Log.d(TAG, "registCallback");
        mUserAudioCallback = callback;
    }

    @Override
    public void unregistCallback(IUserAudioCallback callback) throws RemoteException {
        Log.d(TAG, "unregistCallback");
        mUserAudioCallback = null;
    }

    @Override
    public boolean isMasterMute() throws RemoteException {
        if ( mManager == null ) return false;
        boolean mute = mManager.isMasterMute();
        Log.d(TAG, "get="+mute);
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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mManager == null || mUserAudioCallback == null ) return;
            if ( !intent.getAction().equals(AudioManager.MASTER_MUTE_CHANGED_ACTION) ) return;
            Log.d(TAG, "MASTER_MUTE_CHANGED_ACTION");
            try {
                if ( mManager.isMasterMute() ) 
                    mUserAudioCallback.onMasterMuteChanged(true);
                else 
                    mUserAudioCallback.onMasterMuteChanged(false);
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            }
        }
    };
}