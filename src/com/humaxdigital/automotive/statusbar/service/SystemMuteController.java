package com.humaxdigital.automotive.statusbar.service;

import android.os.RemoteException;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.util.Log;

import com.humaxdigital.automotive.statusbar.user.IUserAudio;
import com.humaxdigital.automotive.statusbar.user.IUserAudioCallback;

public class SystemMuteController extends BaseController<Integer> {
    private final String TAG = "SystemMuteController"; 
    private enum MuteStatus { NONE, AV_MUTE, NAV_MUTE, AV_NAV_MUTE }
    private IUserAudio mUserAudio = null;
    private MuteStatus mCurrentStatus = MuteStatus.NONE; 

    public SystemMuteController(Context context, DataStore store) {
        super(context, store);
        Log.d(TAG, "BluetoothClient"); 
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect"); 
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect"); 
        try {
            if ( mUserAudio != null ) 
                mUserAudio.unregistCallback(mUserAudioCallback);
            mUserAudio = null;
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
    }

    public void fetchUserAudio(IUserAudio audio) {
        try {
            if ( audio == null ) {
                if ( mUserAudio != null ) 
                    mUserAudio.unregistCallback(mUserAudioCallback); 
                mUserAudio = null;
                return;
            }
            mUserAudio = audio; 
            mUserAudio.registCallback(mUserAudioCallback); 
            mCurrentStatus = getCurrentState();
            Log.d(TAG, "fetchAudioClient="+mCurrentStatus); 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
    }

    @Override
    public Integer get() {
        mCurrentStatus = getCurrentState(); 
        Log.d(TAG, "get="+mCurrentStatus); 
        return mCurrentStatus.ordinal(); 
    }

    private MuteStatus getCurrentState() {
        MuteStatus state = MuteStatus.NONE; 
        if ( mUserAudio == null ) return state; 
        try {
            boolean audio_is_mute = mUserAudio.isAudioMute(); 
            boolean naviation_is_mute = mUserAudio.isNavigationMute(); 
            if ( audio_is_mute && naviation_is_mute ) state = MuteStatus.AV_NAV_MUTE; 
            else if ( audio_is_mute ) state = MuteStatus.AV_MUTE; 
            else if ( naviation_is_mute ) state = MuteStatus.NAV_MUTE; 
            else state = MuteStatus.NONE; 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        Log.d(TAG, "getCurrentState="+state); 
        return state; 
    }

    private void broadcastChangeEvent() {
        MuteStatus status = getCurrentState();
        if ( mCurrentStatus == status ) return;
        mCurrentStatus = status;
        for ( Listener listener : mListeners ) 
            listener.onEvent(mCurrentStatus.ordinal());
    }

    private final IUserAudioCallback.Stub mUserAudioCallback = 
        new IUserAudioCallback.Stub() {
        @Override
        public void onAudioMuteChanged(boolean mute) throws RemoteException { 
            broadcastChangeEvent();
        }
        @Override
        public void onBluetoothMicMuteChanged(boolean mute) throws RemoteException {
        }
        @Override
        public void onNavigationChanged(boolean mute) throws RemoteException {
            broadcastChangeEvent();
        }
    };
}
