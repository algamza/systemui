package com.humaxdigital.automotive.statusbar.service;

import com.humaxdigital.automotive.statusbar.service.AudioClient.AudioType;

import android.content.Context;
import android.util.Log;

public class SystemCallController extends BaseController<Integer> {
    private final String TAG = "SystemCallController";
    private BluetoothClient mBluetoothClient; 
    private TMSClient mTMSClient; 
    private AudioClient mAudioClient; 
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
        if ( mBluetoothClient != null ) 
            mBluetoothClient.unregisterCallback(mBTCallback);
        if ( mTMSClient != null ) 
            mTMSClient.unregisterCallback(mTMSCallback);
        if ( mAudioClient != null ) 
            mAudioClient.unregisterCallback(mAudioCallback);
    }

    @Override
    public void fetch() {
    }

    public void fetch(BluetoothClient bt, TMSClient tms, AudioClient audio) {
        mBluetoothClient = bt;
        mTMSClient = tms; 
        mAudioClient = audio;

        if ( mBluetoothClient != null ) mBluetoothClient.registerCallback(mBTCallback);
        if ( mTMSClient != null ) mTMSClient.registerCallback(mTMSCallback); 
        if ( mAudioClient != null ) mAudioClient.registerCallback(mAudioCallback);

        mCurrentStatus = getCurrentCallStatus();
        Log.d(TAG, "fetch="+mCurrentStatus); 
    }

    @Override
    public Integer get() {
        mCurrentStatus = getCurrentCallStatus(); 
        Log.d(TAG, "get="+mCurrentStatus); 
        return mCurrentStatus.ordinal(); 
    }

    private CallStatus getCurrentCallStatus() {
        CallStatus status = CallStatus.NONE; 
        if ( mAudioClient == null || mBluetoothClient == null || mTMSClient == null ) 
            return status;
        boolean is_bt_mic_mute = mAudioClient.isMute(AudioType.BLUETOOTH_MIC); 
        if ( is_bt_mic_mute ) return CallStatus.BT_PHONE_MIC_MUTE; 
        boolean is_tms_calling = mTMSClient.getCallingStatus() == 
            TMSClient.CallingStatus.CALL_CONNECTED ? true:false;
        if ( is_tms_calling ) return CallStatus.TMS_CALLING; 
        status = convertToCallStatus(convertToBTStatus(mBluetoothClient.getCurrentState()));
        return status;
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

    private final AudioClient.AudioCallback mAudioCallback = new AudioClient.AudioCallback() {
        @Override
        public void onMuteChanged(AudioClient.AudioType type, boolean mute) {
            Log.d(TAG, "onConnectionChanged:type="+type); 
            broadcastChangeEvent();
        } 
    }; 

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

    private final BluetoothClient.BluetoothCallback mBTCallback = 
        new BluetoothClient.BluetoothCallback() {
        @Override
        public void onConnectionStateChanged(BluetoothClient.Profiles profile) {
            Log.d(TAG, "onConnectionStateChanged="+profile); 
            broadcastChangeEvent();
        }
        @Override
        public void onBluetoothEnableChanged(Boolean enable) {
            Log.d(TAG, "onBluetoothEnableChanged:enable="+enable);
            if ( !enable ) broadcastChangeEvent();
        }
        @Override
        public void onCallingStateChanged(BluetoothClient.BluetoothState state, int value) {
            Log.d(TAG, "onCallingStateChanged:state="+state+", value="+value); 
            broadcastChangeEvent();
        }
    }; 
}
