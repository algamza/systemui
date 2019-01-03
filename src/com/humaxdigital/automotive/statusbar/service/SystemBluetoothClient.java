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

    public interface SystemBluetoothCallback {
        void onBatteryStateChanged(SystemBluetoothClient.Profiles profile); 
        void onAntennaStateChanged(SystemBluetoothClient.Profiles profile);
        void onConnectionStateChanged(SystemBluetoothClient.Profiles profile); 
        void onBluetoothEnableChanged(Boolean enable); 
    }

    public enum Profiles { HEADSET, A2DP_SINK }

    private Context mContext; 
    private BluetoothAdapter mBluetoothAdapter;
    private DataStore mDataStore; 
    private List<SystemBluetoothCallback> mListeners = new ArrayList<>(); 
    
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
            mBluetoothAdapter.getProfileProxy(mContext, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    switch(profile) {
                        case BluetoothProfile.HEADSET_CLIENT: {
                            if ( proxy.getConnectedDevices().size() > 0 ) {
                                Log.d(TAG, "onServiceConnected:HEADSET_CLIENT=1"); 
                                BluetoothHeadsetClient client = (BluetoothHeadsetClient)proxy;
                                Bundle features = client.getCurrentAgEvents(proxy.getConnectedDevices().get(0));  
                                int battery_level = features.getInt(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL);
                                int antenna_level = features.getInt(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH);
                                mDataStore.setBTDeviceBatteryState(Profiles.HEADSET.ordinal(), battery_level);
                                mDataStore.setBTDeviceAntennaLevel(Profiles.HEADSET.ordinal(), antenna_level);
                                updateConectionState(Profiles.HEADSET, 1); 
                            }
                            else {
                                Log.d(TAG, "onServiceConnected:HEADSET_CLIENT=0"); 
                                updateConectionState(Profiles.HEADSET, 0); 
                            }
                            break; 
                        }
                        case BluetoothProfile.A2DP_SINK: {
                            if ( proxy.getConnectedDevices().size() > 0 ) {
                                Log.d(TAG, "onServiceConnected:A2DP_SINK=1"); 
                                updateConectionState(Profiles.A2DP_SINK, 1); 
                            }
                            else {
                                Log.d(TAG, "onServiceConnected:A2DP_SINK=0"); 
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
                            Log.d(TAG, "onServiceDisconnected:HEADSET_CLIENT"); 
                            updateConectionState(Profiles.HEADSET, 0); 
                            break; 
                        case BluetoothProfile.A2DP_SINK: 
                            Log.d(TAG, "onServiceDisconnected:A2DP_SINK"); 
                            updateConectionState(Profiles.A2DP_SINK, 0); 
                            break; 
                        default: break; 
                    }
                }
            }, convertToProfile(profile));
        }
    }

    private void updateConectionState(Profiles profile, int state) {
        if ( mDataStore == null ) return; 
        if ( mDataStore.shouldPropagateBTDeviceConnectionStateUpdate(profile.ordinal(), state) ) {
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
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED: 
                    Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED"); 
                    checkAllProfileConnection(); 
                    break;
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
                }
                default: break;
            }
        }
    };
    
}
