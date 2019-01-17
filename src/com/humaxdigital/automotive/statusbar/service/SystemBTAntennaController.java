package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

public class SystemBTAntennaController extends BaseController<Integer> {
    private static final String TAG = "SystemBTAntennaController";

    private enum AntennaStatus { NONE, BT_ANTENNA_NO, BT_ANTENNA_1, BT_ANTENNA_2, 
        BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5 }

    private BluetoothClient mBluetoothClient; 

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

    public void fetch(BluetoothClient client) {
        if ( client == null ) return; 
        Log.d(TAG, "fetch"); 
        mBluetoothClient = client; 
        mBluetoothClient.registerCallback(mBTCallback);
    }

    @Override
    public Integer get() {
        boolean is_connected = mBluetoothClient.isDeviceConnected(BluetoothClient.Profiles.HEADSET); 
        int level = mBluetoothClient.getAntennaLevel(BluetoothClient.Profiles.HEADSET); 
        Log.d(TAG, "get:connected="+is_connected+", level="+level); 
        if ( is_connected ) 
            return convertToBTAntennaLevel(level).ordinal(); 
        else 
            return AntennaStatus.NONE.ordinal(); 
    }

    private AntennaStatus convertToBTAntennaLevel(int level) {
        AntennaStatus status = AntennaStatus.NONE; 
        // Signal Level : range (0~5)
        switch(level) {
            case 0: status = AntennaStatus.BT_ANTENNA_NO; break; 
            case 1: status = AntennaStatus.BT_ANTENNA_1; break; 
            case 2: status = AntennaStatus.BT_ANTENNA_2; break; 
            case 3: status = AntennaStatus.BT_ANTENNA_3; break; 
            case 4: status = AntennaStatus.BT_ANTENNA_4; break; 
            case 5: status = AntennaStatus.BT_ANTENNA_5; break; 
        }
        return status; 
    }

    private BluetoothClient.SystemBluetoothCallback mBTCallback = 
        new BluetoothClient.SystemBluetoothCallback() {
        @Override
        public void onBatteryStateChanged(BluetoothClient.Profiles profile) {
        }
        @Override
        public void onAntennaStateChanged(BluetoothClient.Profiles profile) {
            if ( profile != BluetoothClient.Profiles.HEADSET ) return; 
            int level = mBluetoothClient.getAntennaLevel(profile); 
            boolean is_connected = mBluetoothClient.isDeviceConnected(profile); 
            Log.d(TAG, "onAntennaStateChanged:level="+level+", connected="+is_connected);
            if ( is_connected ) {
                for ( Listener listener : mListeners ) 
                    listener.onEvent(convertToBTAntennaLevel(level).ordinal());
            }  
        }
        @Override
        public void onConnectionStateChanged(BluetoothClient.Profiles profile) {
            if ( profile != BluetoothClient.Profiles.HEADSET ) return; 
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
        @Override
        public void onCallingStateChanged(BluetoothClient.BluetoothState state, int value) {
        }
    }; 
}
