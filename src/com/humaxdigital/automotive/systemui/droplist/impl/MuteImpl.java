package com.humaxdigital.automotive.systemui.droplist.impl;

import android.os.RemoteException;
import android.content.Context;
import android.util.Log;

import android.extension.car.CarAudioManagerEx;
import android.car.CarNotConnectedException;
import android.car.media.ICarVolumeCallback;

import com.humaxdigital.automotive.systemui.user.IUserAudio;
import com.humaxdigital.automotive.systemui.user.IUserAudioCallback;

public class MuteImpl extends BaseImplement<Boolean> {
    private final String TAG = "MuteImpl"; 
    private IUserAudio mUserAudio = null;
    private CarExtensionClient mCarClient = null;
    private CarAudioManagerEx mAudioMgrEx = null;

    public MuteImpl(Context context) {
        super(context);
    }

    @Override
    public void destroy() {
        try {
            if ( mUserAudio != null ) 
                mUserAudio.unregistCallback(mUserAudioCallback);
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        mUserAudio = null;
        mListener = null;
    }

    @Override
    public Boolean get() {
        if ( mUserAudio == null ) return false;
        boolean enable = false; 
        try {
            enable = mUserAudio.isMasterMute(); 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        }
        Log.d(TAG, "get="+enable);
        return enable;
    }

    @Override
    public void set(Boolean e) {
        if ( mUserAudio == null ) return;
        Log.d(TAG, "set="+e);
        try {
            mUserAudio.setMasterMute(e); 
        } catch( RemoteException err ) {
            Log.e(TAG, "error:"+err);
        }
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
    }

    public void fetchEx(CarExtensionClient client) {
        mCarClient = client; 
        try {
            if ( client == null ) {
                if ( mAudioMgrEx != null ) {
                    mAudioMgrEx.unregisterVolumeCallback(mVolumeChangeCallback.asBinder()); 
                    mAudioMgrEx = null;
                }
                return;
            }
            mAudioMgrEx = mCarClient.getAudioManager(); 
            if ( mAudioMgrEx != null ) {
                mAudioMgrEx.registerVolumeCallback(mVolumeChangeCallback.asBinder());
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
    }

    private final IUserAudioCallback.Stub mUserAudioCallback = 
        new IUserAudioCallback.Stub() {
        @Override
        public void onMasterMuteChanged(boolean enable) throws RemoteException {
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
            Log.d(TAG, "onGroupVolumeChanged");
        }

        @Override
        public void onMasterMuteChanged(int flags) {
            if ( mListener != null && mUserAudio != null )  {
                try {
                    boolean mute = mUserAudio.isMasterMute(); 
                    Log.d(TAG, "onMasterMuteChanged="+mute);
                    mListener.onChange(mute);
                } catch( RemoteException e ) {
                    Log.e(TAG, "error:"+e);
                }
            }
        }
    };
}
