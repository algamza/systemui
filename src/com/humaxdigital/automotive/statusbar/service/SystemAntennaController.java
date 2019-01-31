package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

public class SystemAntennaController extends BaseController<Integer> {
    private static final String TAG = "SystemAntennaController";
    private BluetoothClient mBluetoothClient = null;
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
        if ( mBluetoothClient != null ) 
            mBluetoothClient.unregisterCallback(mBTCallback);
        if ( mTMSClient != null ) 
             mTMSClient.unregisterCallback(mTMSCallback);
    }

    @Override
    public void fetch() {
    }

    public void fetch(BluetoothClient bt, TMSClient tms) {
        if ( bt == null || tms == null ) return; 
        Log.d(TAG, "fetch"); 
        
        mBluetoothClient = bt; 
        mTMSClient = tms; 

        mBluetoothClient.registerCallback(mBTCallback);
        mTMSClient.registerCallback(mTMSCallback);

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
        if ( mTMSClient == null || mBluetoothClient == null ) return status;
        boolean tms_connected = mTMSClient.getConnectionStatus() == TMSClient.ConnectionStatus.CONNECTED ? true:false;
        if ( tms_connected ) {
            int tms_level = mTMSClient.getSignalLevel(); 
            status = convertToAntennaLevel(Type.TMS, tms_level); 
            Log.d(TAG, "get type="+Type.TMS+", level="+tms_level+", status="+status); 
            return status;
        }
        boolean bt_connected = mBluetoothClient.isDeviceConnected(BluetoothClient.Profiles.HEADSET); 
        if ( bt_connected ) {
            int bt_level = mBluetoothClient.getAntennaLevel(BluetoothClient.Profiles.HEADSET); 
            status = convertToAntennaLevel(Type.BT, bt_level); 
            Log.d(TAG, "get type="+Type.BT+", level="+bt_level+", status="+status); 
            return status;
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

    private final BluetoothClient.BluetoothCallback mBTCallback = 
        new BluetoothClient.BluetoothCallback() {
        @Override
        public void onAntennaStateChanged(BluetoothClient.Profiles profile) {
            Log.d(TAG, "onAntennaStateChanged="+profile);
            if ( profile == BluetoothClient.Profiles.HEADSET ) broadcastChangeEvent();
        }
        @Override
        public void onConnectionStateChanged(BluetoothClient.Profiles profile) {
            Log.d(TAG, "onConnectionStateChanged="+profile);
            if ( profile == BluetoothClient.Profiles.HEADSET ) broadcastChangeEvent();
        }
        @Override
        public void onBluetoothEnableChanged(Boolean enable) {
            Log.d(TAG, "onBluetoothEnableChanged="+enable);
            if ( !enable ) broadcastChangeEvent();
        }
    }; 
}
