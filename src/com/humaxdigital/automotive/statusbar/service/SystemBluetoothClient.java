package com.humaxdigital.automotive.statusbar.service;

import android.os.Bundle; 

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile; 
import android.bluetooth.BluetoothHeadsetClient;

import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothDevice; 

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class SystemBluetoothClient {
    private static final String TAG = "SystemBluetoothClient";
    public enum BluetoothState {
        NONE,
        HANDSFREE_CONNECTED,
        STREAMING_CONNECTED,
        HF_FREE_STREAMING_CONNECTED,
        CONTACTS_DOWNLOADING,
        CALL_HISTORY_DOWNLOADING,
        BLUETOOTH_CALLING,
    }

    public interface SystemBluetoothCallback {
        void onBatteryStateChanged(SystemBluetoothClient.Profiles profile); 
        void onAntennaStateChanged(SystemBluetoothClient.Profiles profile);
        void onConnectionStateChanged(SystemBluetoothClient.Profiles profile); 
        void onBluetoothEnableChanged(Boolean enable); 
        void onCallingStateChanged(BluetoothState state, int value); 
    }

    public enum Profiles { HEADSET, A2DP_SINK }

    private Context mContext; 
    private BluetoothAdapter mBluetoothAdapter;
    private DataStore mDataStore; 
    private List<SystemBluetoothCallback> mListeners = new ArrayList<>(); 
    private int mContactsDownloadingState = 0; 
    private int mCallHistoryDownloadingState = 0; 
    
    private int[] mBTDeviceProfiles = { 
            BluetoothProfile.HEADSET_CLIENT, 
            BluetoothProfile.A2DP_SINK }; 

    public SystemBluetoothClient(Context context, DataStore store) {
        if ( context == null || store == null ) return; 
        Log.d(TAG, "SystemBluetoothClient"); 
        mContext = context; 
        mDataStore = store; 
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect"); 
        IntentFilter filter = new IntentFilter(); 
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); 
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED); 
        filter.addAction(BluetoothHeadsetClient.ACTION_AG_EVENT); 
        filter.addAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED); 
        filter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED); 
        // FIXME: it is bluetooth extension action. ( action_pbap_state ) 
        // please check. packages\apps\Bluetooth\src\com\android\bluetooth\pbapclient\PbapClientStateMachine.java 
        // intent.getIntExtra("state", 0);
        // PBAP_STATE_IDLE = 0;
        // PBAP_STATE_CONNECTING = 1;
        // PBAP_STATE_CONNECTED = 2;
        // PBAP_STATE_DOWNLOADING = 3;
        filter.addAction("action_pbap_state"); 
        mContext.registerReceiver(mBTReceiver, filter);
        checkAllProfileConnection(); 
    }

    public void disconnect() {
        if ( mContext == null ) return; 
        Log.d(TAG, "disconnect"); 
        mContext.unregisterReceiver(mBTReceiver); 
    }

    public void registerCallback(SystemBluetoothCallback callback) {
        if ( callback == null ) return; 
        mListeners.add(callback); 
    }

    public void unregisterCallback(SystemBluetoothCallback callback) {
        if ( callback == null ) return; 
        mListeners.remove(callback);
    }

    public Boolean isDeviceConnected(Profiles profile) {
        Boolean connection = false; 
        if ( mDataStore == null ) return connection; 
        connection = mDataStore.getBTDeviceConnectionState(profile.ordinal()) == 1 ? true : false; 
        Log.d(TAG, "isDeviceConnected="+connection); 
        return connection; 
    }

    public BluetoothState getCurrentState() {
        BluetoothState bt_state = BluetoothState.NONE; 
        for ( BluetoothState state : BluetoothState.values() ) {
            boolean on  = mDataStore.getBTCallingState(state.ordinal()) == 1 ? true:false; 
            if ( on ) bt_state = state;  
        }
        return bt_state; 
    }

    public int getBatteryLevel(Profiles profile) {
        int level = 0; 
        if ( mDataStore == null ) return level; 
        level = mDataStore.getBTDeviceBatteryState(profile.ordinal()); 
        Log.d(TAG, "getBatteryLevel="+level); 
        return level; 
    }

    public int getAntennaLevel(Profiles profile) {
        int level = 0; 
        if ( mDataStore == null ) return level; 
        level = mDataStore.getBTDeviceAntennaLevel(profile.ordinal()); 
        Log.d(TAG, "getAntennaLevel="+level); 
        return level; 
    }

    private int convertToProfile(Profiles profile) {
        int device_profile = 0; 
        switch(profile) {
            case HEADSET: device_profile = mBTDeviceProfiles[0]; break; 
            case A2DP_SINK: device_profile = mBTDeviceProfiles[1]; break; 
            default: break; 
        }
        return device_profile; 
    }

    private void updateAllProfileConnection(int state) {
        if ( mDataStore == null ) return; 
        Log.d(TAG, "updateAllProfileConnection="+state); 
        for ( Profiles profile : Profiles.values() ) 
            mDataStore.setBTDeviceConnectionState(profile.ordinal(), state); 
    }

    private void checkAllProfileConnection() {
        for ( Profiles profile : Profiles.values() ) {
            checkProfileConnection(profile); 
        }
    }

    private void checkProfileConnection(Profiles profile) {
        mBluetoothAdapter.getProfileProxy(mContext, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                switch(profile) {
                    case BluetoothProfile.HEADSET_CLIENT: {
                        if ( proxy.getConnectedDevices().size() > 0 ) {
                            BluetoothHeadsetClient client = (BluetoothHeadsetClient)proxy;
                            Bundle features = client.getCurrentAgEvents(proxy.getConnectedDevices().get(0));  
                            int battery_level = features.getInt(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL);
                            int antenna_level = features.getInt(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH);
                            mDataStore.setBTDeviceBatteryState(Profiles.HEADSET.ordinal(), battery_level);
                            mDataStore.setBTDeviceAntennaLevel(Profiles.HEADSET.ordinal(), antenna_level);
                            updateConectionState(Profiles.HEADSET, 1); 
                        }
                        else {
                            updateConectionState(Profiles.HEADSET, 0); 
                        }
                        break; 
                    }
                    case BluetoothProfile.A2DP_SINK: {
                        if ( proxy.getConnectedDevices().size() > 0 ) {
                            updateConectionState(Profiles.A2DP_SINK, 1); 
                        }
                        else {
                            updateConectionState(Profiles.A2DP_SINK, 0); 
                        } 
                        break; 
                    }
                    default: break; 
                }
            }
            @Override
            public void onServiceDisconnected(int profile) {
                switch(profile) {
                    case BluetoothProfile.HEADSET_CLIENT: 
                        updateConectionState(Profiles.HEADSET, 0); 
                        break; 
                    case BluetoothProfile.A2DP_SINK: 
                        updateConectionState(Profiles.A2DP_SINK, 0); 
                        break; 
                    default: break; 
                }
            }
        }, convertToProfile(profile));
    }

    private void updateConectionState(Profiles profile, int state) {
        if ( mDataStore == null ) return; 
        if ( mDataStore.shouldPropagateBTDeviceConnectionStateUpdate(profile.ordinal(), state) ) {
            if ( profile == Profiles.HEADSET ) 
                mDataStore.setBTCallingState(BluetoothState.HANDSFREE_CONNECTED.ordinal(), state);
            else if ( profile == Profiles.A2DP_SINK )
                mDataStore.setBTCallingState(BluetoothState.STREAMING_CONNECTED.ordinal(), state);

            for ( SystemBluetoothCallback callback : mListeners ) 
                callback.onConnectionStateChanged(profile);
        }
    }

    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mBluetoothAdapter == null || intent == null ) return;
            switch(intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    Log.d(TAG, "ACTION_STATE_CHANGED"); 
                    int state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF); 
                    if ( state == BluetoothAdapter.STATE_OFF ) 
                        updateAllProfileConnection(0); 
                    for ( SystemBluetoothCallback callback : mListeners ) {
                        if ( state == BluetoothAdapter.STATE_OFF ) 
                            callback.onBluetoothEnableChanged(false);
                        else if ( state == BluetoothAdapter.STATE_ON ) 
                            callback.onBluetoothEnableChanged(true);
                    }
                    break; 
                }
                // TODO : Because of timing issue ... 
                //case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED: 
                case BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED: {
                    Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED"); 
                    // TODO: check the profile extra state 
                    /*
                    int STATE_DISCONNECTED = 0;
                    int STATE_CONNECTING = 1;
                    int STATE_CONNECTED = 2;
                    int STATE_DISCONNECTING = 3;
                    */
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1); 
                    if ( state == 0 || state == 2 ) 
                        checkProfileConnection(Profiles.HEADSET); 
                    break;
                }
                case BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1); 
                    if ( state == 0 || state == 2 ) 
                        checkProfileConnection(Profiles.A2DP_SINK); 
                    break; 
                }
                case BluetoothHeadsetClient.ACTION_AG_EVENT: {
                    Log.d(TAG, "ACTION_AG_EVENT"); 
                    Profiles profile = Profiles.HEADSET; 
                    int battery_level = intent.getIntExtra(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL, -1);
                    if ( battery_level != -1 ) {
                        if ( mDataStore.shouldPropagateBTDeviceBatteryStateUpdate(profile.ordinal(), battery_level) ) {
                            for ( SystemBluetoothCallback callback : mListeners ) 
                                callback.onBatteryStateChanged(profile);
                        }
                    }
                    int antenna_level = intent.getIntExtra(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH, -1);
                    if ( antenna_level != -1 ) {
                        if ( mDataStore.shouldPropagateBTDeviceAntennaLevelUpdate(profile.ordinal(), antenna_level) ) {
                            for ( SystemBluetoothCallback callback : mListeners ) 
                                callback.onAntennaStateChanged(profile);
                        }
                    }
                    break; 
                }
                case "action_pbap_state": {
                    int state = intent.getIntExtra("state", 0);
                    if ( mContactsDownloadingState == state ) break; 
                    if ( state == 3 ) {
                        Log.d(TAG, "action_pbap_state:CONTACTS_DOWNLOADING"); 
                        if ( mDataStore.shouldPropagateBTCallingStateUpdate(BluetoothState.CONTACTS_DOWNLOADING.ordinal(), 1) ) {
                            for ( SystemBluetoothCallback callback : mListeners ) 
                                callback.onCallingStateChanged(BluetoothState.CONTACTS_DOWNLOADING, 1); 
                        }
                        mContactsDownloadingState = state; 
                    } else if ( state == 2 ) {
                        Log.d(TAG, "action_pbap_state:COMPLETE"); 
                        if ( mDataStore.shouldPropagateBTCallingStateUpdate(BluetoothState.CONTACTS_DOWNLOADING.ordinal(), 0) ) {
                            for ( SystemBluetoothCallback callback : mListeners ) 
                                callback.onCallingStateChanged(BluetoothState.CONTACTS_DOWNLOADING, 0); 
                        }
                    }
                    break; 
                }
                default: break;
            }
        }
    };
    
}
