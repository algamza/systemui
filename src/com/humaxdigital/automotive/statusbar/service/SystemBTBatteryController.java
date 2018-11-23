package com.humaxdigital.automotive.statusbar.service;

import android.os.Bundle; 

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile; 
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothDevice; 

import android.util.Log;

public class SystemBTBatteryController extends BaseController<Integer> {
    private static final String TAG = "SystemBTBatteryController";
    private enum BTBatteryStatus { NONE, BT_BATTERY_0, BT_BATTERY_1, 
        BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }
    private enum Action { FETCH, UPDATE }
    private enum BTDevice { NONE, HEADSET, A2DP_SINK }

    private BluetoothAdapter mBluetoothAdapter;
    
    private int[] mBTDeviceProfiles = 
        { BluetoothProfile.HEADSET_CLIENT, BluetoothProfile.A2DP_SINK }; 

    public SystemBTBatteryController(Context context, DataStore store) {
        super(context, store);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        IntentFilter filter = new IntentFilter(); 
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); 
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED); 
        filter.addAction(BluetoothHeadsetClient.ACTION_AG_EVENT); 
        mContext.registerReceiver(mBTReceiver, filter);
    }

    @Override
    public void disconnect() {
        if ( mContext != null ) mContext.unregisterReceiver(mBTReceiver); 
    }

    @Override
    public void fetch() {
        if ( mDataStore == null || mBluetoothAdapter == null ) return;
        if ( mBluetoothAdapter.isEnabled() )
            for ( int profile : mBTDeviceProfiles ) 
                checkDevice(Action.FETCH, profile); 
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        return 0; 
    }

    private BTDevice convertToType(int devtype) {
        BTDevice type = BTDevice.NONE; 
        switch(devtype) {
            case BluetoothProfile.HEADSET_CLIENT: type = BTDevice.HEADSET; break; 
            case BluetoothProfile.A2DP_SINK: type = BTDevice.A2DP_SINK; break; 
        }
        return type; 
    }

    private void checkDevice(Action action, int _profile) {
        mBluetoothAdapter.getProfileProxy(mContext, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if ( proxy.getConnectedDevices().size() > 0 ) {
                    BluetoothHeadsetClient client = (BluetoothHeadsetClient)proxy;
                    Bundle features = client.getCurrentAgEvents(proxy.getConnectedDevices().get(0));  
                    int level = features.getInt(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL);
                    BTBatteryStatus status = convertToStatus(level); 
                    if ( action == Action.FETCH ) {
                        Log.d(TAG, "fetch="+status.ordinal()); 
                        mDataStore.setBTBatterylevel(convertToType(_profile).ordinal(), status.ordinal()); 
                    } else if ( action == Action.UPDATE ) {
                        if ( mDataStore.shouldPropagateBTBatteryLevelUpdate(
                            convertToType(_profile).ordinal(), status.ordinal()) ) {
                            Log.d(TAG, "update="+status.ordinal()); 
                            for ( Listener listener : mListeners ) listener.onEvent(status.ordinal());
                        }
                    }
                }
            }
            @Override
            public void onServiceDisconnected(int profile) {
                if ( action != Action.UPDATE ) return; 
                Log.d(TAG, "onServiceDisconnected"); 
                // todo : check disconnected device 
            }
        }, _profile);
    }

    private BTBatteryStatus convertToStatus(int level) {
        BTBatteryStatus status = BTBatteryStatus.NONE; 
        if ( (0 <= level) && (level < 17) ) {
            status = BTBatteryStatus.BT_BATTERY_0; 
        } else if ( (17 <= level) && (level < 34) ) {
            status = BTBatteryStatus.BT_BATTERY_1; 
        } else if ( (34 <= level) && (level < 51) ) {
            status = BTBatteryStatus.BT_BATTERY_2; 
        } else if ( (51 <= level) && (level < 68) ) {
            status = BTBatteryStatus.BT_BATTERY_3; 
        } else if ( (68 <= level) && (level < 85) ) {
            status = BTBatteryStatus.BT_BATTERY_4; 
        } else if ( (85 <= level) && (level <= 100) ) {
            status = BTBatteryStatus.BT_BATTERY_5; 
        } 
        return status; 
    }

    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mBluetoothAdapter == null ) return;
            switch(intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    Log.d(TAG, "ACTION_STATE_CHANGED"); 
                    if ( intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 
                        BluetoothAdapter.STATE_OFF) != BluetoothAdapter.STATE_OFF ) break; 
                    BTBatteryStatus status = BTBatteryStatus.NONE; 
                    if ( !mDataStore.shouldPropagateBTBatteryLevelUpdate(
                        BTDevice.NONE.ordinal(), status.ordinal()) ) break; 
                    for ( Listener listener : mListeners ) listener.onEvent(status.ordinal());
                    break; 
                }
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED: 
                    Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED"); 
                    for ( int profile : mBTDeviceProfiles ) checkDevice(Action.UPDATE, profile); 
                    break;
                case BluetoothHeadsetClient.ACTION_AG_EVENT: {
                    Log.d(TAG, "ACTION_AG_EVENT"); 
                    int level = intent.getIntExtra(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL, -1);
                    if ( level == -1 ) break; 
                    //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BTDevice device = BTDevice.HEADSET; 
                    if ( mDataStore.shouldPropagateBTBatteryLevelUpdate(
                        device.ordinal(), convertToStatus(level).ordinal()) ) {
                        for ( Listener listener : mListeners ) listener.onEvent(convertToStatus(level).ordinal());
                    }
                    break; 
                }
                default: break;
            }
        }
    };
}
