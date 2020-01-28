package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.RemoteException;
import android.os.UserHandle;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.telephony.TelephonyManager;

import android.provider.Settings;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.net.Uri;

import com.humaxdigital.automotive.systemui.common.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.common.user.IUserBluetoothCallback;
import com.humaxdigital.automotive.systemui.common.user.IUserAudio;
import com.humaxdigital.automotive.systemui.common.user.IUserAudioCallback;
import com.humaxdigital.automotive.systemui.common.CONSTANTS;

public class SystemCallController extends BaseController<Integer> {
    private final String TAG = "SystemCallController";
    private TMSClient mTMSClient = null;
    private IUserBluetooth mUserBluetooth = null; 
    private IUserAudio mUserAudio = null;
    private BTStatus mBTCurrentStatus = BTStatus.NONE; 
    private boolean mCurrentTMSCalling = false; 
    private boolean mCurrentBTMicMute = false;
    private CallStatus mCurrentStatus = CallStatus.NONE;
    private boolean mCarlifeConnected = false;

    private ContentResolver mContentResolver = null;
    private ContentObserver mObserver = null;
    private boolean mCurrentContactDownloading = false; 
    private boolean mCurrentCallHistoryDownloading = false; 
    private int mDownloadState = 0; 

    private TelephonyManager mTelephony = null;

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
        final IntentFilter filter = new IntentFilter();
        filter.addAction(CONSTANTS.ACTION_CARLIFE_STATE);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mContext.registerReceiverAsUser(mBroadcastReceiver, 
            UserHandle.ALL, filter, null, null);
        initObserver();
        mTelephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
        deinitObserver();
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

        broadcastChangeEvent();
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
            Log.d(TAG, "fetchUserAudio="+mCurrentStatus); 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 

        broadcastChangeEvent();
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
        if ( is_bt_mic_mute ) {
            if ( getCallState() == 1 ) 
                return CallStatus.BT_PHONE_MIC_MUTE;  
        }
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
            boolean isHeadsetConnected = mUserBluetooth.isHeadsetDeviceConnected(); 
            boolean isA2DPConnected = mUserBluetooth.isA2dpDeviceConnected();
            if ( isHeadsetConnected )
                state = BTStatus.HANDS_FREE_CONNECTED;
            if ( isA2DPConnected ) {
                if ( state == BTStatus.HANDS_FREE_CONNECTED ) {
                    if ( !mCarlifeConnected ) 
                        state = BTStatus.HF_FREE_STREAMING_CONNECTED; 
                }
                else {
                    if ( mCarlifeConnected ) state = BTStatus.NONE; 
                    else state = BTStatus.STREAMING_CONNECTED;
                }
            }
            if ( isHeadsetConnected ) {
                if ( mCurrentContactDownloading ) 
                    state = BTStatus.CONTACTS_HISTORY_DOWNLOADING;
                if ( mCurrentCallHistoryDownloading ) 
                    state = BTStatus.CALL_HISTORY_DOWNLOADING;
                if ( getCallState() == 1 ) 
                    state = BTStatus.BT_CALLING;
            } 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        return state;         
    }

    private int getCallState() {
        if ( mTelephony == null ) return 0; 
        int state = mTelephony.getCallState(); 
        switch(state) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
            case TelephonyManager.CALL_STATE_RINGING: {
                return 1; 
            }
            case TelephonyManager.CALL_STATE_IDLE: break; 
        }
        return 0; 
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

    private void initObserver() {
        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        mObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CONSTANTS.PBAP_STATE), 
            false, mObserver, UserHandle.USER_ALL); 
    }

    private void deinitObserver() {
        if ( mContentResolver != null && mObserver != null ) 
            mContentResolver.unregisterContentObserver(mObserver); 
        mObserver = null;
        mContentResolver = null;
    }

    private ContentObserver createObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if ( mContentResolver == null ) return;
                int state = Settings.Global.getInt(mContentResolver, CONSTANTS.PBAP_STATE, 0);
                Log.d(TAG, "CONSTANTS.PBAP_STATE="+state);
                /*  
                    PBAP_STATE_DOWNLOADING = 3;
                    PBAP_STATE_DOWNLOADING_PHONEBOOK = 4;
                    PBAP_STATE_DOWNLOADING_CALLLOG = 5
                */
                if ( mDownloadState == state ) return;
                mDownloadState = state; 
                if ( state == 3 ) {
                    mCurrentContactDownloading = true;
                    mCurrentCallHistoryDownloading = true;
                } else if ( state == 4 ) {   
                    mCurrentContactDownloading = true;
                } else if ( state == 5 ) {
                    mCurrentCallHistoryDownloading = true;
                } else if ( state == 2 ) {
                    mDownloadState = state; 
                    mCurrentContactDownloading = false;
                    mCurrentCallHistoryDownloading = false;
                } 
                broadcastChangeEvent();
            }
        };
        return observer; 
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
        public void onBluetoothMicMuteStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onContactsDownloadStateChanged(int state) throws RemoteException {
            broadcastChangeEvent();
        }
        @Override
        public void onCallHistoryDownloadStateChanged(int state) throws RemoteException {
            broadcastChangeEvent();
        }
    };

    private final IUserAudioCallback.Stub mUserAudioCallback 
        = new IUserAudioCallback.Stub() {
        
        @Override
        public void onBluetoothMicMuteChanged(boolean mute) throws RemoteException {
            broadcastChangeEvent();
        }
        @Override
        public void onNavigationChanged(boolean mute) throws RemoteException {
        }
        @Override
        public void onMasterMuteChanged(boolean mute) throws RemoteException {
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch(action) {
                case CONSTANTS.ACTION_CARLIFE_STATE: {
                    mCarlifeConnected = intent.getBooleanExtra("isConnected", false);
                    Log.d(TAG, "mBroadcastReceiver:CONSTANTS.ACTION_CARLIFE_STATE="+mCarlifeConnected);
                    broadcastChangeEvent();
                    break;
                }
                case TelephonyManager.ACTION_PHONE_STATE_CHANGED: {
                    String phone_state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
                    Log.d(TAG, "ACTION_PHONE_STATE_CHANGED="+phone_state);
                    if ( phone_state == null ) break;
                    /*
                    if ( phone_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) ) {
                    } else if ( phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING) ) {
                    } else if ( phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE) ) {
                    } else {
                    }
                    */
                    broadcastChangeEvent();
                    break;
                }
            }
        }
    };
}
