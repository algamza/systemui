package com.humaxdigital.automotive.systemui.droplist.impl;

import android.os.RemoteException;
import android.content.Context;
import android.util.Log;

import com.humaxdigital.automotive.systemui.user.IUserAudio;
import com.humaxdigital.automotive.systemui.user.IUserAudioCallback;

public class MuteImpl extends BaseImplement<Boolean> {
    private final String TAG = "MuteImpl"; 
    private IUserAudio mUserAudio = null;

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

        sendMuteChangeEvent();
    }

    private final IUserAudioCallback.Stub mUserAudioCallback = 
        new IUserAudioCallback.Stub() {
        @Override
        public void onMasterMuteChanged(boolean enable) throws RemoteException {
            sendMuteChangeEvent();
        }
        @Override
        public void onBluetoothMicMuteChanged(boolean mute) throws RemoteException {
        }
        @Override
        public void onNavigationChanged(boolean mute) throws RemoteException {
        }
    }; 

    private void sendMuteChangeEvent() {
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
}
