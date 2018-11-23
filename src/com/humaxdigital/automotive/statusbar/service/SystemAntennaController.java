package com.humaxdigital.automotive.statusbar.service;

import android.os.Bundle; 

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile; 
import android.bluetooth.BluetoothHeadsetClient; 

import android.util.Log;

public class SystemAntennaController extends BaseController<Integer> {
    private static final String TAG = "SystemAntennaController";

    private enum AntennaStatus { NONE, BT_ANTENNA_NO, BT_ANTENNA_0, 
        BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, 
        BT_ANTENNA_5, TMU_ANTENNA_NO, TMU_ANTENNA_0, TMU_ANTENNA_1, 
        TMU_ANTENNA_2, TMU_ANTENNA_3, TMU_ANTENNA_4, TMU_ANTENNA_5 }

    private BluetoothAdapter mBluetoothAdapter;
    // todo : check tmu antenna 

    public SystemAntennaController(Context context, DataStore store) {
        super(context, store);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        IntentFilter filter = new IntentFilter(); 
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); 
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED); 
        mContext.registerReceiver(mBTReceiver, filter);
    }

    @Override
    public void disconnect() {
        if ( mContext != null ) mContext.unregisterReceiver(mBTReceiver); 
    }

    @Override
    public void fetch() {
        if ( mDataStore == null || mBluetoothAdapter == null ) return;
        AntennaStatus status = AntennaStatus.NONE; 

        if ( mBluetoothAdapter.isEnabled() ) {
            fetchBTAntennaClient(); 
        } 
        
        if ( true ) { 
            // todo : check tmu antenna 
        }

        Log.d(TAG, "fetch="+status.ordinal()); 
        mDataStore.setAntennaState(status.ordinal()); 
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        int val = mDataStore.getAntennaState(); 
        Log.d(TAG, "get="+val); 
        return 0; 
    }

    private AntennaStatus convertToTMUAntennaLevel(int level) {
        AntennaStatus status = AntennaStatus.TMU_ANTENNA_NO; 
        // todo : check status 
        if ( level < 0 ) status = AntennaStatus.TMU_ANTENNA_0; 
        else if ( level > 0 && level <= 1 ) status = AntennaStatus.TMU_ANTENNA_1; 
        else if ( level > 1 && level <= 2 ) status = AntennaStatus.TMU_ANTENNA_2; 
        else if ( level > 2 && level <= 3 ) status = AntennaStatus.TMU_ANTENNA_3; 
        else if ( level > 3 && level <= 4 ) status = AntennaStatus.TMU_ANTENNA_4; 
        else if ( level > 4 ) status = AntennaStatus.TMU_ANTENNA_5; 

        return status; 
    }

    private AntennaStatus convertToBTAntennaLevel(int level) {
        AntennaStatus status = AntennaStatus.BT_ANTENNA_NO; 
        // todo : check status 
        if ( level < 0 ) status = AntennaStatus.BT_ANTENNA_0; 
        else if ( level > 0 && level <= 1 ) status = AntennaStatus.BT_ANTENNA_1; 
        else if ( level > 1 && level <= 2 ) status = AntennaStatus.BT_ANTENNA_3; 
        else if ( level > 2 && level <= 3 ) status = AntennaStatus.BT_ANTENNA_4; 
        else if ( level > 3 && level <= 4 ) status = AntennaStatus.BT_ANTENNA_5; 
        else if ( level > 4 ) status = AntennaStatus.BT_ANTENNA_5; 
        return status; 
    }

    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mBluetoothAdapter == null ) return;

            if ( intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED) ) {
                Log.d(TAG, "onReceive=ACTION_STATE_CHANGED"); 
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                int current = mDataStore.getAntennaState(); 
                if ( state == BluetoothAdapter.STATE_OFF ) {
                    if ( current >= AntennaStatus.BT_ANTENNA_NO.ordinal() &&
                        current <= AntennaStatus.BT_ANTENNA_5.ordinal() ) {
                            AntennaStatus antennastate = AntennaStatus.NONE; 
                            if ( mDataStore.shouldPropagateAntennaUpdate(antennastate.ordinal()) ) {
                                for ( Listener listener : mListeners ) listener.onEvent(antennastate.ordinal());
                            }
                        }
                }
            }

            if ( intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED) ) {
                Log.d(TAG, "onReceive=ACTION_CONNECTION_STATE_CHANGED"); 
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 
                    BluetoothAdapter.STATE_CONNECTED); 
                if ( state == BluetoothAdapter.STATE_CONNECTED || state == BluetoothAdapter.STATE_DISCONNECTED ) {
                    updateBTAntennaClient(); 
                }
            }
        }
    };

    private void fetchBTAntennaClient() {
        mBluetoothAdapter.getProfileProxy(mContext, 
            new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                AntennaStatus status = AntennaStatus.NONE; 
                int current = mDataStore.getAntennaState(); 
                if ( profile == BluetoothProfile.HEADSET_CLIENT && 
                    proxy.getConnectedDevices().size() > 0 ) {
                    BluetoothHeadsetClient client = (BluetoothHeadsetClient)proxy;
                    Bundle features = client.getCurrentAgEvents(proxy.getConnectedDevices().get(0));  
                    int level = features.getInt(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH);
                    status = convertToBTAntennaLevel(level); 
                    Log.d(TAG, "fetch="+status.ordinal()); 
                    mDataStore.setAntennaState(status.ordinal()); 
                } 
            }

            @Override
            public void onServiceDisconnected(int profile) {
            }
        }, BluetoothProfile.HEADSET_CLIENT); 
    } 

    private void updateBTAntennaClient() {
        mBluetoothAdapter.getProfileProxy(mContext, 
            new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                AntennaStatus status = AntennaStatus.NONE; 
                int current = mDataStore.getAntennaState(); 
                if ( profile == BluetoothProfile.HEADSET_CLIENT && 
                    proxy.getConnectedDevices().size() > 0 ) {
                    BluetoothHeadsetClient client = (BluetoothHeadsetClient)proxy;
                    Bundle features = client.getCurrentAgEvents(proxy.getConnectedDevices().get(0));  
                    int level = features.getInt(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH);
                    status = convertToBTAntennaLevel(level); 
                    Log.d(TAG, "update="+status.ordinal()); 
                    if ( mDataStore.shouldPropagateAntennaUpdate(status.ordinal()) ) {
                        for ( Listener listener : mListeners ) listener.onEvent(status.ordinal());
                    }
                } else if ( current >= AntennaStatus.BT_ANTENNA_NO.ordinal() && 
                        current <= AntennaStatus.BT_ANTENNA_5.ordinal() ) {
                    Log.d(TAG, "update="+status.ordinal()); 
                    if ( mDataStore.shouldPropagateAntennaUpdate(status.ordinal()) ) {
                        for ( Listener listener : mListeners ) listener.onEvent(status.ordinal());
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                AntennaStatus status = AntennaStatus.NONE; 
                int current = mDataStore.getAntennaState(); 
                if ( current >= AntennaStatus.BT_ANTENNA_NO.ordinal() &&
                    current <= AntennaStatus.BT_ANTENNA_5.ordinal() ) {
                    Log.d(TAG, "update="+status.ordinal()); 
                    if ( mDataStore.shouldPropagateAntennaUpdate(status.ordinal()) ) {
                        for ( Listener listener : mListeners ) listener.onEvent(status.ordinal());
                    }
                }
            }
        }, BluetoothProfile.HEADSET_CLIENT); 
    } 
}
