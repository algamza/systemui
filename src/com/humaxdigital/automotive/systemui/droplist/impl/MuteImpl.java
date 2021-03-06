package com.humaxdigital.automotive.systemui.droplist.impl;

import android.os.RemoteException;
import android.content.Context;
import android.util.Log;

import android.car.CarNotConnectedException;
import android.car.media.ICarVolumeCallback;
import android.extension.car.CarAudioManagerEx;

import com.humaxdigital.automotive.systemui.common.user.IUserAudio;
import com.humaxdigital.automotive.systemui.common.user.IUserAudioCallback;

import android.extension.car.util.AudioTypes;

import com.humaxdigital.automotive.systemui.common.car.CarExClient;

public class MuteImpl extends BaseImplement<Boolean> {
    private final String TAG = "MuteImpl"; 
    private IUserAudio mUserAudio = null;
    private CarExClient mCarClient = null;
    private CarAudioManagerEx mCarAudioEx = null;

    public MuteImpl(Context context) {
        super(context);
    }

    @Override
    public void destroy() {
        cleanupAudioManager(); 
        try {
            if ( mUserAudio != null ) 
                mUserAudio.unregistCallback(mUserAudioCallback);
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        mUserAudio = null;
        mListener = null;
        mCarClient = null;
        mCarAudioEx = null;
    }

    @Override
    public Boolean get() {
        if ( mCarAudioEx == null ) return false;
        boolean enable = false; 
        enable = mCarAudioEx.getAudioMuteStatus(AudioTypes.AUDIO_MUTE_ID_USER);
        Log.d(TAG, "get="+enable);
        return enable;
    }

    @Override
    public void set(Boolean e) {
        if ( mCarAudioEx == null ) return;
        Log.d(TAG, "set="+e);
        mCarAudioEx.setAudioMute(AudioTypes.AUDIO_MUTE_ID_USER, 
            ((e==true) ? AudioTypes.AUDIO_MUTE_ON : AudioTypes.AUDIO_MUTE_OFF), 
            AudioTypes.AUDIO_MUTE_SHOW_ICON);
    }

    public void fetch(IUserAudio audio) {
        if ( audio == null ) {
            Log.d(TAG, "fetch = null");
            try {
                if ( mUserAudio != null ) 
                mUserAudio.unregistCallback(mUserAudioCallback);
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            } 
            mUserAudio = null;
            return;
        }
        Log.d(TAG, "fetch = user Audio");
        mUserAudio = audio;
        try {
            if ( mUserAudio != null ) {
                mUserAudio.registCallback(mUserAudioCallback); 
            }
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        }

        sendMuteChangeEvent();
    }

    public void fetchEx(CarExClient client) {
        Log.d(TAG, "fetchEx="+client);
        if ( client == null ) {
            cleanupAudioManager(); 
            return;
        }

        mCarClient = client; 

        if ( client == null ) {
            mCarAudioEx = null;
            return;
        }
        mCarAudioEx = mCarClient.getAudioManager(); 
        try {
            if ( mCarAudioEx != null ) {
                mCarAudioEx.registerVolumeCallback(mVolumeChangeCallback.asBinder());
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
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

    private final IUserAudioCallback.Stub mUserAudioCallback = 
        new IUserAudioCallback.Stub() {
        @Override
        public void onMasterMuteChanged(boolean enable) throws RemoteException {
            //sendMuteChangeEvent();
        }
        @Override
        public void onBluetoothMicMuteChanged(boolean mute) throws RemoteException {
        }
        @Override
        public void onNavigationChanged(boolean mute) throws RemoteException {
        }
    }; 

    private final ICarVolumeCallback mVolumeChangeCallback = new ICarVolumeCallback.Stub() {
        @Override
        public void onGroupVolumeChanged(int groupId, int flags) {
        }

        @Override
        public void onMasterMuteChanged(int flags) {
            sendMuteChangeEvent();
        }
    };

    private void sendMuteChangeEvent() {
        if ( mListener != null && mCarAudioEx != null )  {
            boolean mute = false; 
            mute = mCarAudioEx.getAudioMuteStatus(AudioTypes.AUDIO_MUTE_ID_USER);
            Log.d(TAG, "onMasterMuteChanged="+mute);
            mListener.onChange(mute);
        }
    }
}
