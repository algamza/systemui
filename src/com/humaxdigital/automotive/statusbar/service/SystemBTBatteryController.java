package com.humaxdigital.automotive.statusbar.service;

import android.os.RemoteException;
import android.content.Context;
import android.util.Log;

import com.humaxdigital.automotive.statusbar.user.IUserBluetooth;
import com.humaxdigital.automotive.statusbar.user.IUserBluetoothCallback;

public class SystemBTBatteryController extends BaseController<Integer> {
    private static final String TAG = "SystemBTBatteryController";
    private enum BTBatteryStatus { NONE, BT_BATTERY_0, BT_BATTERY_1, 
        BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }
    private IUserBluetooth mUserBluetooth = null;

    public SystemBTBatteryController(Context context, DataStore store) {
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
    }

    @Override
    public Integer get() {
        boolean is_connected = false; 
        int level = 0; 
        try {
            if ( mUserBluetooth != null ) {
                is_connected = mUserBluetooth.isHeadsetDeviceConnected();
                level = mUserBluetooth.getBatteryLevel(); 
            }
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 

        Log.d(TAG, "get:connected="+is_connected+", level="+level); 

        if ( is_connected ) 
            return convertToStatus(level).ordinal(); 
        else 
            return BTBatteryStatus.NONE.ordinal(); 
    }

    private BTBatteryStatus convertToStatus(int level) {
        BTBatteryStatus status = BTBatteryStatus.NONE; 
        // Battery Level : range (0~5)
        if ( level == 0 ) {
            status = BTBatteryStatus.BT_BATTERY_0; 
        } else if ( level == 1 ) {
            status = BTBatteryStatus.BT_BATTERY_1; 
        } else if ( level == 2 ) {
            status = BTBatteryStatus.BT_BATTERY_2; 
        } else if ( level == 3 ) {
            status = BTBatteryStatus.BT_BATTERY_3; 
        } else if ( level == 4 ) {
            status = BTBatteryStatus.BT_BATTERY_4; 
        } else if ( level == 5 ) {
            status = BTBatteryStatus.BT_BATTERY_5; 
        } 
        return status; 
    }

    private final IUserBluetoothCallback.Stub mUserBluetoothCallback = 
        new IUserBluetoothCallback.Stub() {

        @Override
        public void onBluetoothEnableChanged(int enable) throws RemoteException {
            Log.d(TAG, "onBluetoothEnableChanged:enable="+enable);
            if ( enable == 1 ) return; 
            for ( Listener listener : mListeners ) 
                listener.onEvent(BTBatteryStatus.NONE.ordinal());
        }
        @Override
        public void onHeadsetConnectionStateChanged(int state) throws RemoteException {
            int level = 0; 
            try {
                level = mUserBluetooth.getBatteryLevel(); 
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            } 
            boolean is_connected = state == 1 ? true:false;
            Log.d(TAG, "onConnectionStateChanged:level="+level+", connected="+is_connected);
            if ( is_connected ) {
                for ( Listener listener : mListeners ) 
                    listener.onEvent(convertToStatus(level).ordinal());
            } else {
                for ( Listener listener : mListeners ) 
                    listener.onEvent(BTBatteryStatus.NONE.ordinal());
            }
        }
        @Override
        public void onA2dpConnectionStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onBatteryStateChanged(int level) throws RemoteException {
            if ( mUserBluetooth == null ) return; 
            boolean is_connected = false; 
            try {
                is_connected = mUserBluetooth.isHeadsetDeviceConnected();
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            } 
            if ( !is_connected ) return;
            Log.d(TAG, "onBatteryStateChanged:level="+level);
            for ( Listener listener : mListeners ) 
                listener.onEvent(convertToStatus(level).ordinal());
        }
        @Override
        public void onAntennaStateChanged(int level) throws RemoteException {
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
