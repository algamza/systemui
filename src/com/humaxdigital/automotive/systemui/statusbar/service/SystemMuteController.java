package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.RemoteException;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.util.Log;

import android.car.CarNotConnectedException;
import android.car.media.ICarVolumeCallback;
import android.extension.car.CarAudioManagerEx;

import com.humaxdigital.automotive.systemui.user.IUserAudio;
import com.humaxdigital.automotive.systemui.user.IUserAudioCallback;

import android.extension.car.util.AudioTypes;

public class SystemMuteController extends BaseController<Integer> {
    private final String TAG = "SystemMuteController"; 
    private enum MuteStatus { NONE, AV_MUTE, NAV_MUTE, AV_NAV_MUTE }
    private IUserAudio mUserAudio = null;
    private MuteStatus mCurrentStatus = MuteStatus.NONE; 
    private CarAudioManagerEx mCarAudioEx = null;

    public SystemMuteController(Context context, DataStore store) {
        super(context, store);
        Log.d(TAG, "SystemMuteController"); 
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect"); 
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect"); 
        cleanupAudioManager(); 
        try {
            if ( mUserAudio != null ) 
                mUserAudio.unregistCallback(mUserAudioCallback);
            mUserAudio = null;
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
    }

    private void cleanupAudioManager() {
        Log.d(TAG, "cleanupAudioManager");
        try {
            if (mCarAudioEx != null)
                mCarAudioEx.unregisterVolumeCallback(mVolumeChangeCallback.asBinder());
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
        mCarAudioEx = null;
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
            Log.d(TAG, "fetchUserAudio="+mCurrentStatus); 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 

        broadcastChangeEvent();
    }

    public void fetchAudioEx(CarAudioManagerEx audio) {
        if ( audio == null ) {
            Log.d(TAG, "fetchAudioEx=null"); 
            cleanupAudioManager(); 
            return;
        }
        
        mCarAudioEx = audio;
        try {
            if ( mCarAudioEx != null ) {
                mCarAudioEx.registerVolumeCallback(mVolumeChangeCallback.asBinder());
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
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
        if ( mUserAudio == null || mCarAudioEx == null ) return state; 
        try {
            boolean audio_is_mute = false; 
            audio_is_mute = mCarAudioEx.getAudioMuteStatus(AudioTypes.AUDIO_MUTE_ID_USER);
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
        public void onMasterMuteChanged(boolean enable) throws RemoteException {
            //broadcastChangeEvent();
        }
        @Override
        public void onBluetoothMicMuteChanged(boolean mute) throws RemoteException {
        }
        @Override
        public void onNavigationChanged(boolean mute) throws RemoteException {
            //broadcastChangeEvent();
        }
    };

    private final ICarVolumeCallback mVolumeChangeCallback = new ICarVolumeCallback.Stub() {
        @Override
        public void onGroupVolumeChanged(int groupId, int flags) {
        }

        @Override
        public void onMasterMuteChanged(int flags) {
            broadcastChangeEvent();
        }
    };
}
