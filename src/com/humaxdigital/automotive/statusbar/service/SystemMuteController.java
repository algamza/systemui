package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.util.Log;

public class SystemMuteController extends BaseController<Integer> {
    private final String TAG = "SystemMuteController"; 
    private enum MuteStatus { NONE, AV_MUTE, NAV_MUTE, AV_NAV_MUTE }
    private SystemAudioClient mAudioClient; 

    public SystemMuteController(Context context, DataStore store) {
        super(context, store);
        Log.d(TAG, "SystemBluetoothClient"); 
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect"); 
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect"); 
        if ( mAudioClient != null ) mAudioClient.unregisterCallback(mAudioCallback); 
    }

    public void fetch(SystemAudioClient client) {
        Log.d(TAG, "fetch"); 
        if ( client == null ) return;
        mAudioClient = client; 
        mAudioClient.registerCallback(mAudioCallback); 
        for ( SystemAudioClient.AudioType type : SystemAudioClient.AudioType.values() ) {
            mDataStore.setAudioMuteState(type.ordinal(), mAudioClient.isMute(type) ? 1:0);
        }
    }

    @Override
    public Integer get() {
        MuteStatus state = getCurrentState(); 
        Log.d(TAG, "get="+state); 
        return state.ordinal(); 
    }

    private MuteStatus getCurrentState() {
        MuteStatus state = MuteStatus.NONE; 
        if ( mDataStore == null ) return state; 
        boolean audio_is_mute = false; 
        boolean naviation_is_mute = false; 
        for ( SystemAudioClient.AudioType type : SystemAudioClient.AudioType.values() ) {
            int current = mDataStore.getAudioMuteState(type.ordinal()); 
            if ( current == 1 ) {
                if ( SystemAudioClient.AudioType.AUDIO == type ) audio_is_mute = true;    
                else if ( SystemAudioClient.AudioType.NAVIGATION == type ) naviation_is_mute = true;
            } 
        }
        if ( audio_is_mute && naviation_is_mute ) state = MuteStatus.AV_NAV_MUTE; 
        else if ( audio_is_mute ) state = MuteStatus.AV_MUTE; 
        else if ( naviation_is_mute ) state = MuteStatus.NAV_MUTE; 
        Log.d(TAG, "getCurrentState="+state); 
        return state; 
    }

    private SystemAudioClient.SystemAudioCallback mAudioCallback = 
        new SystemAudioClient.SystemAudioCallback() {
        @Override
        public void onMuteChanged(SystemAudioClient.AudioType type, boolean mute) {
            if ( mAudioClient == null ) return; 
            if ( mDataStore.shouldPropagateAudioMuteStateUpdate(type.ordinal(), mute?1:0) ) {
                for ( Listener listener : mListeners ) {
                    listener.onEvent(getCurrentState().ordinal());
                }
            }
        }
    }; 
}
