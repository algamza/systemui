package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

public class SystemBTCallController extends BaseController<Integer> {
    private static final String TAG = "SystemBTCallController";

    private BluetoothClient mBluetoothClient; 

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

    public void fetch(BluetoothClient client) {
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
        int current = BluetoothClient.BluetoothState.NONE.ordinal(); 
        if ( mDataStore == null ) return current; 
        for ( BluetoothClient.BluetoothState state 
            : BluetoothClient.BluetoothState.values() ) {
            if ( mDataStore.getBTCallingState(state.ordinal()) == 1 ) {
                current = state.ordinal(); 
                if ( state == BluetoothClient.BluetoothState.STREAMING_CONNECTED ) {
                    if ( mDataStore.getBTCallingState(
                        BluetoothClient.BluetoothState.HANDSFREE_CONNECTED.ordinal()) ==1 ) {
                        current = BluetoothClient.BluetoothState.HF_FREE_STREAMING_CONNECTED.ordinal(); 
                    }
                }
            }
        }
        return current; 
    }

    private BluetoothClient.SystemBluetoothCallback mBTCallback = 
        new BluetoothClient.SystemBluetoothCallback() {
        @Override
        public void onBatteryStateChanged(BluetoothClient.Profiles profile) {
        }
        @Override
        public void onAntennaStateChanged(BluetoothClient.Profiles profile) {
        }
        @Override
        public void onConnectionStateChanged(BluetoothClient.Profiles profile) {
            Log.d(TAG, "onConnectionStateChanged:profile="+profile); 
            for ( Listener listener : mListeners ) 
                listener.onEvent(getCurrentState());
        }
        @Override
        public void onBluetoothEnableChanged(Boolean enable) {
            Log.d(TAG, "onBluetoothEnableChanged:enable="+enable);
            if ( enable ) return; 
            for ( Listener listener : mListeners ) 
                listener.onEvent(BluetoothClient.BluetoothState.NONE.ordinal());
        }
        @Override
        public void onCallingStateChanged(BluetoothClient.BluetoothState state, int value) {
            Log.d(TAG, "onCallingStateChanged:state="+state+", value="+value); 
            for ( Listener listener : mListeners ) 
                listener.onEvent(getCurrentState());
        }
    }; 
}
