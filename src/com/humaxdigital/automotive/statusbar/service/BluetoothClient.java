package com.humaxdigital.automotive.statusbar.service;

import android.os.Bundle; 
import android.os.UserHandle;

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
import java.util.HashMap; 
import java.util.Map; 
import java.util.List;
import android.util.Log;

public class BluetoothClient {
    private static final String TAG = "BluetoothClient";
    public enum BluetoothState {
        NONE,
        HANDSFREE_CONNECTED,
        STREAMING_CONNECTED,
        HF_FREE_STREAMING_CONNECTED,
        CONTACTS_DOWNLOADING,
        CALL_HISTORY_DOWNLOADING,
        BLUETOOTH_CALLING,
    }

    public static abstract class BluetoothCallback {
        void onBatteryStateChanged(BluetoothClient.Profiles profile) {}
        void onAntennaStateChanged(BluetoothClient.Profiles profile) {}
        void onConnectionStateChanged(BluetoothClient.Profiles profile) {} 
        void onBluetoothEnableChanged(Boolean enable) {}
        void onCallingStateChanged(BluetoothState state, int value) {} 
    }

    public enum Profiles { HEADSET, A2DP_SINK }

    private Context mContext; 
    private BluetoothAdapter mBluetoothAdapter;
    private DataStore mDataStore; 
    private List<BluetoothCallback> mListeners = new ArrayList<>(); 
    private int mContactsDownloadingState = 0; 
    private int mCallHistoryDownloadingState = 0; 
    private Map<Integer, BluetoothProfile> mCurrentProxy = new HashMap<>(); 
    
    private int[] mBTDeviceProfiles = { 
            BluetoothProfile.HEADSET_CLIENT, 
            BluetoothProfile.A2DP_SINK }; 

    public BluetoothClient(Context context, DataStore store) {
        if ( context == null || store == null ) return; 
        Log.d(TAG, "BluetoothClient"); 
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
        mContext.registerReceiverAsUser(mBTReceiver, UserHandle.ALL, filter, null, null);
        checkAllProfileConnection(); 
    }

    public void disconnect() {
        if ( mContext == null ) return; 
        Log.d(TAG, "disconnect"); 
        mListeners.clear();
        for ( int key : mCurrentProxy.keySet() ) {
            if ( mBluetoothAdapter == null ) break; 
            mBluetoothAdapter.closeProfileProxy(key, mCurrentProxy.get(key)); 
        }
        mContext.unregisterReceiver(mBTReceiver); 
    }

    public void refresh() {
        Log.d(TAG, "refresh");
        //mContext.unregisterReceiver(mBTReceiver); 
        //mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //connect(); 
        for ( int key : mCurrentProxy.keySet() ) {
            if ( mBluetoothAdapter == null ) break; 
            mBluetoothAdapter.closeProfileProxy(key, mCurrentProxy.get(key)); 
        }
        mCurrentProxy.clear();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkAllProfileConnection(); 
    }

    public void registerCallback(BluetoothCallback callback) {
        if ( callback == null ) return; 
        mListeners.add(callback); 
    }

    public void unregisterCallback(BluetoothCallback callback) {
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
        if ( mDataStore == null ) return bt_state;
        for ( BluetoothState state : BluetoothState.values() ) {
            boolean on  = mDataStore.getBTCallingState(state.ordinal()) == 1 ? true:false; 
            if ( on ) bt_state = state;  
        }
        if ( bt_state == BluetoothState.HANDSFREE_CONNECTED 
            || bt_state == BluetoothState.STREAMING_CONNECTED ) {
            if ( mDataStore.getBTCallingState(BluetoothState.HANDSFREE_CONNECTED.ordinal()) == 1 
                && mDataStore.getBTCallingState(BluetoothState.STREAMING_CONNECTED.ordinal()) == 1 ) {
                mDataStore.setBTCallingState(BluetoothState.HF_FREE_STREAMING_CONNECTED.ordinal(), 1);
                bt_state = BluetoothState.HF_FREE_STREAMING_CONNECTED;
            }
        } else if ( bt_state == BluetoothState.HF_FREE_STREAMING_CONNECTED ) {
            if ( mDataStore.getBTCallingState(BluetoothState.HANDSFREE_CONNECTED.ordinal()) != 1 
                || mDataStore.getBTCallingState(BluetoothState.STREAMING_CONNECTED.ordinal()) != 1 ) {
                mDataStore.setBTCallingState(BluetoothState.HF_FREE_STREAMING_CONNECTED.ordinal(), 0);
                if ( mDataStore.getBTCallingState(BluetoothState.HANDSFREE_CONNECTED.ordinal()) == 1 )
                    bt_state = BluetoothState.HANDSFREE_CONNECTED;
                else if ( mDataStore.getBTCallingState(BluetoothState.STREAMING_CONNECTED.ordinal()) == 1 )
                    bt_state = BluetoothState.STREAMING_CONNECTED;
                else 
                    bt_state = BluetoothState.NONE; 
            }
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
        if ( mBluetoothAdapter == null ) return; 

        int _profile = convertToProfile(profile); 
        if ( !mCurrentProxy.isEmpty() ) {
            BluetoothProfile _proxy = mCurrentProxy.get(_profile);
            if ( _proxy != null ) 
                mBluetoothAdapter.closeProfileProxy(_profile, _proxy); 
        }

        mBluetoothAdapter.getProfileProxy(mContext, mBluetoothProfileListener, _profile); 
    }

    BluetoothProfile.ServiceListener mBluetoothProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mCurrentProxy.put(profile, proxy); 

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
    };

    private void updateConectionState(Profiles profile, int state) {
        if ( mDataStore == null ) return; 
        Log.d(TAG, "updateConectionState : profile="+profile+", state="+state); 
        if ( mDataStore.shouldPropagateBTDeviceConnectionStateUpdate(profile.ordinal(), state) ) {
            if ( profile == Profiles.HEADSET ) 
                mDataStore.setBTCallingState(BluetoothState.HANDSFREE_CONNECTED.ordinal(), state);
            else if ( profile == Profiles.A2DP_SINK )
                mDataStore.setBTCallingState(BluetoothState.STREAMING_CONNECTED.ordinal(), state);

            for ( BluetoothCallback callback : mListeners ) 
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
                    for ( BluetoothCallback callback : mListeners ) {
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
                    Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED:Headset"); 
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
                    Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED:A2DP"); 
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
                            for ( BluetoothCallback callback : mListeners ) 
                                callback.onBatteryStateChanged(profile);
                        }
                    }
                    int antenna_level = intent.getIntExtra(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH, -1);
                    if ( antenna_level != -1 ) {
                        if ( mDataStore.shouldPropagateBTDeviceAntennaLevelUpdate(profile.ordinal(), antenna_level) ) {
                            for ( BluetoothCallback callback : mListeners ) 
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
                            for ( BluetoothCallback callback : mListeners ) 
                                callback.onCallingStateChanged(BluetoothState.CONTACTS_DOWNLOADING, 1); 
                        }
                        mContactsDownloadingState = state; 
                    } else if ( state == 2 ) {
                        Log.d(TAG, "action_pbap_state:COMPLETE"); 
                        if ( mDataStore.shouldPropagateBTCallingStateUpdate(BluetoothState.CONTACTS_DOWNLOADING.ordinal(), 0) ) {
                            for ( BluetoothCallback callback : mListeners ) 
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
