package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.RemoteException;
import android.content.Context;
import android.util.Log;

import com.humaxdigital.automotive.systemui.statusbar.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.statusbar.user.IUserBluetoothCallback;

public class SystemAntennaController extends BaseController<Integer> {
    private static final String TAG = "SystemAntennaController";
    private IUserBluetooth mUserBluetooth = null; 
    private TMSClient mTMSClient = null;
    private int mBTCurrentLevel = 0; 
    private int mTMSCurrentLevel = 0; 
    private AntennaStatus mCurrentAntennaStatus = AntennaStatus.NONE; 

    private enum AntennaStatus { 
        NONE, BT_ANTENNA_NO, BT_ANTENNA_1, BT_ANTENNA_2, 
        BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5, TMS_ANTENNA_NO, 
        TMS_ANTENNA_0, TMS_ANTENNA_1, TMS_ANTENNA_2, TMS_ANTENNA_3, 
        TMS_ANTENNA_4, TMS_ANTENNA_5 }

    private enum Type {
        BT,
        TMS
    }

    public SystemAntennaController(Context context, DataStore store) {
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
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        if ( mTMSClient != null ) 
             mTMSClient.unregisterCallback(mTMSCallback);
        mTMSClient = null;
    }

    @Override
    public void fetch() {
    }

    public void fetchTMSClient(TMSClient tms) {
        if ( tms == null ) return; 
        Log.d(TAG, "fetchTMSClient"); 
        mTMSClient = tms; 
        mTMSClient.registerCallback(mTMSCallback);
        mCurrentAntennaStatus = getCurrentStatus();
    }

    public void fetchUserBluetooth(IUserBluetooth bt) {
        if ( bt == null ) {
            try {
                if ( mUserBluetooth != null ) 
                    mUserBluetooth.unregistCallback(mUserBluetoothCallback);
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
        mCurrentAntennaStatus = getCurrentStatus();
    }

    @Override
    public Integer get() {
        mCurrentAntennaStatus = getCurrentStatus();
        Log.d(TAG, "get="+mCurrentAntennaStatus); 
        return mCurrentAntennaStatus.ordinal(); 
    }

    private AntennaStatus getCurrentStatus() {
        AntennaStatus status = AntennaStatus.NONE; 
        if ( mTMSClient == null || mUserBluetooth == null ) return status;
        try {
            boolean tms_connected = mTMSClient.getConnectionStatus() == TMSClient.ConnectionStatus.CONNECTED ? true:false;
            if ( tms_connected ) {
                int tms_level = mTMSClient.getSignalLevel(); 
                status = convertToAntennaLevel(Type.TMS, tms_level); 
                Log.d(TAG, "get type="+Type.TMS+", level="+tms_level+", status="+status); 
                return status;
            }
            boolean bt_connected = mUserBluetooth.isHeadsetDeviceConnected();
            if ( bt_connected ) {
                int bt_level = mUserBluetooth.getAntennaLevel();
                status = convertToAntennaLevel(Type.BT, bt_level); 
                Log.d(TAG, "get type="+Type.BT+", level="+bt_level+", status="+status); 
                return status;
            }
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        return status;
    }

    private AntennaStatus convertToAntennaLevel(Type type, int level) {
        AntennaStatus status = AntennaStatus.NONE; 
        switch(type) {
            case BT: {
                switch(level) {
                    case 0: status = AntennaStatus.BT_ANTENNA_NO; break; 
                    case 1: status = AntennaStatus.BT_ANTENNA_1; break; 
                    case 2: status = AntennaStatus.BT_ANTENNA_2; break; 
                    case 3: status = AntennaStatus.BT_ANTENNA_3; break; 
                    case 4: status = AntennaStatus.BT_ANTENNA_4; break; 
                    case 5: status = AntennaStatus.BT_ANTENNA_5; break; 
                }
                break;
            }
            case TMS: {
                switch(level) {
                    case 0: status = AntennaStatus.TMS_ANTENNA_NO; break; 
                    case 1: status = AntennaStatus.TMS_ANTENNA_0; break;
                    case 2: status = AntennaStatus.TMS_ANTENNA_1; break; 
                    case 3: status = AntennaStatus.TMS_ANTENNA_2; break; 
                    case 4: status = AntennaStatus.TMS_ANTENNA_3; break; 
                    case 5: status = AntennaStatus.TMS_ANTENNA_4; break; 
                    case 6: status = AntennaStatus.TMS_ANTENNA_5; break; 
                    case 7: status = AntennaStatus.TMS_ANTENNA_5; break; 
                }
            }
        }
        return status; 
    }

    private void broadcastChangeEvent() {
        AntennaStatus status = getCurrentStatus();
        if ( mCurrentAntennaStatus == status ) return;
        mCurrentAntennaStatus = status;
        for ( Listener listener : mListeners ) 
            listener.onEvent(mCurrentAntennaStatus.ordinal());
    }

    private final TMSClient.TMSCallback mTMSCallback = new TMSClient.TMSCallback() {
        @Override
        public void onConnectionChanged(TMSClient.ConnectionStatus connection) {
            Log.d(TAG, "onConnectionChanged="+connection);
            broadcastChangeEvent();
        }

        @Override
        public void onSignalLevelChanged(int level) {
            Log.d(TAG, "onSignalLevelChanged="+level);
            broadcastChangeEvent();
        }
    }; 

    private final IUserBluetoothCallback.Stub mUserBluetoothCallback = 
        new IUserBluetoothCallback.Stub() {
        
        @Override
        public void onBluetoothEnableChanged(int enable) throws RemoteException {
            Log.d(TAG, "onBluetoothEnableChanged="+enable);
            if ( enable == 0 ) broadcastChangeEvent();
        }
        @Override
        public void onHeadsetConnectionStateChanged(int state) throws RemoteException {
            Log.d(TAG, "onConnectionStateChanged="+state);
            broadcastChangeEvent();
        }
        @Override
        public void onA2dpConnectionStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onBatteryStateChanged(int level) throws RemoteException {
        }
        @Override
        public void onAntennaStateChanged(int level) throws RemoteException {
            Log.d(TAG, "onAntennaStateChanged="+level);
            mBTCurrentLevel = level;
            broadcastChangeEvent();
        }
        @Override
        public void onBluetoothCallingStateChanged(int state) throws RemoteException {
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
}