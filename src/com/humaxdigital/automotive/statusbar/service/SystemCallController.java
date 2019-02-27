package com.humaxdigital.automotive.statusbar.service;

import android.os.RemoteException;
import android.content.Context;
import android.util.Log;

import com.humaxdigital.automotive.statusbar.user.IUserBluetooth;
import com.humaxdigital.automotive.statusbar.user.IUserBluetoothCallback;
import com.humaxdigital.automotive.statusbar.user.IUserAudio;
import com.humaxdigital.automotive.statusbar.user.IUserAudioCallback;

public class SystemCallController extends BaseController<Integer> {
    private final String TAG = "SystemCallController";
    private TMSClient mTMSClient = null;
    private IUserBluetooth mUserBluetooth = null; 
    private IUserAudio mUserAudio = null;
    private BTStatus mBTCurrentStatus = BTStatus.NONE; 
    private boolean mCurrentTMSCalling = false; 
    private boolean mCurrentBTMicMute = false;
    private CallStatus mCurrentStatus = CallStatus.NONE;

    public enum BTStatus {
        NONE, HANDS_FREE_CONNECTED, STREAMING_CONNECTED, 
        HF_FREE_STREAMING_CONNECTED, CALL_HISTORY_DOWNLOADING, 
        CONTACTS_HISTORY_DOWNLOADING, BT_CALLING
    }
    public enum CallStatus { 
        NONE, HANDS_FREE_CONNECTED, STREAMING_CONNECTED, 
        HF_FREE_STREAMING_CONNECTED, CALL_HISTORY_DOWNLOADING, 
        CONTACTS_HISTORY_DOWNLOADING, TMS_CALLING, BT_CALLING, 
        BT_PHONE_MIC_MUTE }; 

    public SystemCallController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
        try {
            if ( mUserBluetooth != null ) 
                mUserBluetooth.unregistCallback(mUserBluetoothCallback);
            mUserBluetooth = null;
            if ( mUserAudio != null ) 
                mUserAudio.unregistCallback(mUserAudioCallback);
            mUserAudio = null;
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        if ( mTMSClient != null ) 
            mTMSClient.unregisterCallback(mTMSCallback);
    }

    @Override
    public void fetch() {
    }
    public void fetchUserBluetooth(IUserBluetooth bt) {
        if ( bt == null ) {
            try {
                if ( mUserBluetooth != null ) 
                    mUserBluetooth.unregistCallback(mUserBluetoothCallback);
                mUserBluetooth = null;
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            } 
            return;
        }
        mUserBluetooth = bt;
        try {
            mUserBluetooth.registCallback(mUserBluetoothCallback);
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 

        mCurrentStatus = getCurrentCallStatus();
        Log.d(TAG, "fetchUserBluetooth="+mCurrentStatus); 
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
            mCurrentStatus = getCurrentCallStatus();
            Log.d(TAG, "fetchAudioClient="+mCurrentStatus); 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
    }

    public void fetchTMSClient(TMSClient tms) {
        mTMSClient = tms; 
        if ( mTMSClient != null ) 
            mTMSClient.registerCallback(mTMSCallback); 
        mCurrentStatus = getCurrentCallStatus();
        Log.d(TAG, "fetchTMSClient="+mCurrentStatus); 
    }

    @Override
    public Integer get() {
        mCurrentStatus = getCurrentCallStatus(); 
        Log.d(TAG, "get="+mCurrentStatus); 
        return mCurrentStatus.ordinal(); 
    }

    private CallStatus getCurrentCallStatus() {
        CallStatus status = CallStatus.NONE; 
        if ( mUserAudio == null || mUserBluetooth == null || mTMSClient == null ) 
            return status;
        
        boolean is_bt_mic_mute = false; 
        try{
            is_bt_mic_mute = mUserAudio.isBluetoothMicMute();
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        if ( is_bt_mic_mute ) return CallStatus.BT_PHONE_MIC_MUTE; 
        boolean is_tms_calling = mTMSClient.getCallingStatus() == 
            TMSClient.CallingStatus.CALL_CONNECTED ? true:false;
        if ( is_tms_calling ) return CallStatus.TMS_CALLING; 
        status = convertToCallStatus(getBluetoothState());
        return status;
    }

    private BTStatus getBluetoothState() {
        BTStatus state = BTStatus.NONE; 
        if ( mUserBluetooth == null ) return state;
        try {
            if ( mUserBluetooth.isHeadsetDeviceConnected() )
                state = BTStatus.HANDS_FREE_CONNECTED;
            if ( mUserBluetooth.isA2dpDeviceConnected() ) {
                if ( state == BTStatus.HANDS_FREE_CONNECTED ) 
                    state = BTStatus.HF_FREE_STREAMING_CONNECTED; 
                else state = BTStatus.STREAMING_CONNECTED;
            }
            if ( mUserBluetooth.getContactsDownloadState() == 1 ) 
                state = BTStatus.CONTACTS_HISTORY_DOWNLOADING;
            if ( mUserBluetooth.getCallHistoryDownloadState() == 1 ) 
                state = BTStatus.CALL_HISTORY_DOWNLOADING;
            if ( mUserBluetooth.getBluetoothCallingState() == 1 ) 
                state = BTStatus.BT_CALLING;
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        return state;         
    }

    private BTStatus convertToBTStatus(BluetoothClient.BluetoothState status) {
        BTStatus bts = BTStatus.NONE; 
        switch(status) {
            case HANDSFREE_CONNECTED: bts = BTStatus.HANDS_FREE_CONNECTED; break;
            case STREAMING_CONNECTED: bts = BTStatus.STREAMING_CONNECTED; break;
            case HF_FREE_STREAMING_CONNECTED: bts = BTStatus.HF_FREE_STREAMING_CONNECTED; break;
            case CONTACTS_DOWNLOADING: bts = BTStatus.CONTACTS_HISTORY_DOWNLOADING; break;
            case CALL_HISTORY_DOWNLOADING: bts = BTStatus.CALL_HISTORY_DOWNLOADING; break;
            case BLUETOOTH_CALLING: bts = BTStatus.BT_CALLING; break;
        }
        return bts;
    }

    private CallStatus convertToCallStatus(BTStatus bts) {
        CallStatus status = CallStatus.NONE; 
        switch(bts) {
            case HANDS_FREE_CONNECTED: status = CallStatus.HANDS_FREE_CONNECTED; break;
            case STREAMING_CONNECTED: status = CallStatus.STREAMING_CONNECTED; break;  
            case HF_FREE_STREAMING_CONNECTED: status = CallStatus.HF_FREE_STREAMING_CONNECTED; break;
            case CALL_HISTORY_DOWNLOADING: status = CallStatus.CALL_HISTORY_DOWNLOADING; break;
            case CONTACTS_HISTORY_DOWNLOADING: status = CallStatus.CONTACTS_HISTORY_DOWNLOADING; break;
            case BT_CALLING: status = CallStatus.BT_CALLING; break;
        }
        return status;
    }

    private void broadcastChangeEvent() {
        CallStatus status = getCurrentCallStatus();
        if ( mCurrentStatus == status ) return;
        mCurrentStatus = status;
        for ( Listener listener : mListeners ) 
            listener.onEvent(mCurrentStatus.ordinal());
    }

    private final TMSClient.TMSCallback mTMSCallback = new TMSClient.TMSCallback() {
        @Override
        public void onConnectionChanged(TMSClient.ConnectionStatus connection) {
            Log.d(TAG, "onConnectionChanged="+connection); 
            broadcastChangeEvent();
        }

        @Override
        public void onCallingStatusChanged(TMSClient.CallingStatus status) {
            Log.d(TAG, "onCallingStatusChanged="+status); 
            broadcastChangeEvent();
        }
    }; 

    private final IUserBluetoothCallback.Stub mUserBluetoothCallback = 
        new IUserBluetoothCallback.Stub() {
        
        @Override
        public void onBluetoothEnableChanged(int enable) throws RemoteException {
            Log.d(TAG, "onBluetoothEnableChanged:enable="+enable);
            if ( enable == 0 ) broadcastChangeEvent();
        }
        @Override
        public void onHeadsetConnectionStateChanged(int state) throws RemoteException {
            broadcastChangeEvent();
        }
        @Override
        public void onA2dpConnectionStateChanged(int state) throws RemoteException {
            broadcastChangeEvent();
        }
        @Override
        public void onBatteryStateChanged(int level) throws RemoteException {
        }
        @Override
        public void onAntennaStateChanged(int level) throws RemoteException {
        }
        @Override
        public void onBluetoothCallingStateChanged(int state) throws RemoteException {
            broadcastChangeEvent();
        }
        @Override
        public void onBluetoothMicMuteStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onContactsDownloadStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onCallHistoryDownloadStateChanged(int state) throws RemoteException {
        }
    };

    private final IUserAudioCallback.Stub mUserAudioCallback = 
        new IUserAudioCallback.Stub() {
        @Override
        public void onAudioMuteChanged(boolean mute) throws RemoteException { 
        }
        @Override
        public void onBluetoothMicMuteChanged(boolean mute) throws RemoteException {
            broadcastChangeEvent();
        }
        @Override
        public void onNavigationChanged(boolean mute) throws RemoteException {
        }
    };
}
