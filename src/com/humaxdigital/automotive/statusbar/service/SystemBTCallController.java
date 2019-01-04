package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

public class SystemBTCallController extends BaseController<Integer> {
    private static final String TAG = "SystemBTCallController";

    private SystemBluetoothClient mBluetoothClient; 

    public SystemBTCallController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
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
        int state = getCurrentState(); 
        Log.d(TAG, "get="+state); 
        return state; 
    }

    private int getCurrentState() {
        int current = SystemBluetoothClient.BluetoothState.NONE.ordinal(); 
        if ( mDataStore == null ) return current; 
        for ( SystemBluetoothClient.BluetoothState state 
            : SystemBluetoothClient.BluetoothState.values() ) {
            if ( mDataStore.getBTCallingState(state.ordinal()) == 1 ) {
                current = state.ordinal(); 
                if ( state == SystemBluetoothClient.BluetoothState.STREAMING_CONNECTED ) {
                    if ( mDataStore.getBTCallingState(
                        SystemBluetoothClient.BluetoothState.HANDSFREE_CONNECTED.ordinal()) ==1 ) {
                        current = SystemBluetoothClient.BluetoothState.HF_FREE_STREAMING_CONNECTED.ordinal(); 
                    }
                }
            }
        }
        return current; 
    }

    private SystemBluetoothClient.SystemBluetoothCallback mBTCallback = 
        new SystemBluetoothClient.SystemBluetoothCallback() {
        @Override
        public void onBatteryStateChanged(SystemBluetoothClient.Profiles profile) {
        }
        @Override
        public void onAntennaStateChanged(SystemBluetoothClient.Profiles profile) {
        }
        @Override
        public void onConnectionStateChanged(SystemBluetoothClient.Profiles profile) {
            for ( Listener listener : mListeners ) 
                listener.onEvent(getCurrentState());
        }
        @Override
        public void onBluetoothEnableChanged(Boolean enable) {
            Log.d(TAG, "onBluetoothEnableChanged:enable="+enable);
            if ( enable ) return; 
            for ( Listener listener : mListeners ) 
                listener.onEvent(SystemBluetoothClient.BluetoothState.NONE.ordinal());
        }
        @Override
        public void onCallingStateChanged(SystemBluetoothClient.BluetoothState state, int value) {
            for ( Listener listener : mListeners ) 
                listener.onEvent(getCurrentState());
        }
    }; 
}
