package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

public class SystemBTBatteryController extends BaseController<Integer> {
    private static final String TAG = "SystemBTBatteryController";
    private enum BTBatteryStatus { NONE, BT_BATTERY_0, BT_BATTERY_1, 
        BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }
    private SystemBluetoothClient mBluetoothClient; 

    public SystemBTBatteryController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
        if ( mBluetoothClient != null ) 
            mBluetoothClient.registerCallback(mBTCallback);
    }

    @Override
    public void fetch() {
    }

    public void fetch(SystemBluetoothClient client) {
        if ( client == null ) return; 
        mBluetoothClient = client; 
        mBluetoothClient.registerCallback(mBTCallback);
    }

    @Override
    public Integer get() {
        boolean is_connected = mBluetoothClient.isDeviceConnected(SystemBluetoothClient.Profiles.HEADSET); 
        int level = mBluetoothClient.getBatteryLevel(SystemBluetoothClient.Profiles.HEADSET); 
        Log.d(TAG, "get:connected="+is_connected+", level="+level); 
        if ( is_connected ) 
            return convertToStatus(level).ordinal(); 
        else 
            return BTBatteryStatus.NONE.ordinal(); 
    }

    private BTBatteryStatus convertToStatus(int level) {
        BTBatteryStatus status = BTBatteryStatus.NONE; 
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

    private SystemBluetoothClient.SystemBluetoothCallback mBTCallback = 
        new SystemBluetoothClient.SystemBluetoothCallback() {
        @Override
        public void onBatteryStateChanged(SystemBluetoothClient.Profiles profile) {
            if ( profile != SystemBluetoothClient.Profiles.HEADSET ) return; 
            int level = mBluetoothClient.getBatteryLevel(profile); 
            boolean is_connected = mBluetoothClient.isDeviceConnected(profile); 
            Log.d(TAG, "onBatteryStateChanged:level="+level+", connected="+is_connected);
            if ( is_connected ) {
                for ( Listener listener : mListeners ) 
                    listener.onEvent(convertToStatus(level).ordinal());
            }
        }
        @Override
        public void onAntennaStateChanged(SystemBluetoothClient.Profiles profile) {
        }
        @Override
        public void onConnectionStateChanged(SystemBluetoothClient.Profiles profile) {
            if ( profile != SystemBluetoothClient.Profiles.HEADSET ) return; 
            int level = mBluetoothClient.getBatteryLevel(profile); 
            boolean is_connected = mBluetoothClient.isDeviceConnected(profile); 
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
        public void onBluetoothEnableChanged(Boolean enable) {
            Log.d(TAG, "onBluetoothEnableChanged:enable="+enable);
            if ( enable ) return; 
            for ( Listener listener : mListeners ) 
                listener.onEvent(BTBatteryStatus.NONE.ordinal());
        }
        @Override
        public void onCallingStateChanged(SystemBluetoothClient.BluetoothState state, int value) {
        }
    }; 
}
