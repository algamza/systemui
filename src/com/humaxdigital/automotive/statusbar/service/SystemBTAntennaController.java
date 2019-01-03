package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

public class SystemBTAntennaController extends BaseController<Integer> {
    private static final String TAG = "SystemBTAntennaController";

    private enum AntennaStatus { NONE, BT_ANTENNA_NO, BT_ANTENNA_0, 
        BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5 }

    private SystemBluetoothClient mBluetoothClient; 

    public SystemBTAntennaController(Context context, DataStore store) {
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
        Log.d(TAG, "fetch"); 
        mBluetoothClient = client; 
        mBluetoothClient.registerCallback(mBTCallback);
    }

    @Override
    public Integer get() {
        boolean is_connected = mBluetoothClient.isDeviceConnected(SystemBluetoothClient.Profiles.HEADSET); 
        int level = mBluetoothClient.getAntennaLevel(SystemBluetoothClient.Profiles.HEADSET); 
        Log.d(TAG, "get:connected="+is_connected+", level="+level); 
        if ( is_connected ) 
            return convertToBTAntennaLevel(level).ordinal(); 
        else 
            return AntennaStatus.NONE.ordinal(); 
    }

    private AntennaStatus convertToBTAntennaLevel(int level) {
        AntennaStatus status = AntennaStatus.BT_ANTENNA_NO; 
        if ( level == 0 ) status = AntennaStatus.BT_ANTENNA_0; 
        else if ( level > 0 && level <= 1 ) status = AntennaStatus.BT_ANTENNA_1; 
        else if ( level > 1 && level <= 2 ) status = AntennaStatus.BT_ANTENNA_3; 
        else if ( level > 2 && level <= 3 ) status = AntennaStatus.BT_ANTENNA_4; 
        else if ( level > 3 && level <= 4 ) status = AntennaStatus.BT_ANTENNA_5; 
        else if ( level > 4 ) status = AntennaStatus.BT_ANTENNA_5; 
        return status; 
    }

    private SystemBluetoothClient.SystemBluetoothCallback mBTCallback = 
        new SystemBluetoothClient.SystemBluetoothCallback() {
        @Override
        public void onBatteryStateChanged(SystemBluetoothClient.Profiles profile) {
        }
        @Override
        public void onAntennaStateChanged(SystemBluetoothClient.Profiles profile) {
            if ( profile != SystemBluetoothClient.Profiles.HEADSET ) return; 
            int level = mBluetoothClient.getAntennaLevel(profile); 
            boolean is_connected = mBluetoothClient.isDeviceConnected(profile); 
            Log.d(TAG, "onAntennaStateChanged:level="+level+", connected="+is_connected);
            if ( is_connected ) {
                for ( Listener listener : mListeners ) 
                    listener.onEvent(convertToBTAntennaLevel(level).ordinal());
            }  
        }
        @Override
        public void onConnectionStateChanged(SystemBluetoothClient.Profiles profile) {
            if ( profile != SystemBluetoothClient.Profiles.HEADSET ) return; 
            int level = mBluetoothClient.getAntennaLevel(profile); 
            boolean is_connected = mBluetoothClient.isDeviceConnected(profile); 
            Log.d(TAG, "onConnectionStateChanged:level="+level+", connected="+is_connected);
            if ( is_connected ) {
                for ( Listener listener : mListeners ) 
                    listener.onEvent(convertToBTAntennaLevel(level).ordinal());
            } else {
                for ( Listener listener : mListeners ) 
                    listener.onEvent(AntennaStatus.NONE.ordinal());
            }
        }
        @Override
        public void onBluetoothEnableChanged(Boolean enable) {
            Log.d(TAG, "onBluetoothEnableChanged:enable="+enable);
            if ( enable ) return; 
            for ( Listener listener : mListeners ) 
                listener.onEvent(AntennaStatus.NONE.ordinal());
        }
    }; 
}
