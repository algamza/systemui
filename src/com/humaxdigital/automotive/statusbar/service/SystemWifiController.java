package com.humaxdigital.automotive.statusbar.service;

import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class SystemWifiController extends BaseController<Integer> {
    private enum WifiStatus { NONE, WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
    private WifiManager mManager; 

    public SystemWifiController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        mManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mContext.registerReceiverAsUser(mWifiReceiver, UserHandle.ALL, filter, null, null);
    }

    @Override
    public void disconnect() {
        if ( mContext != null ) mContext.unregisterReceiver(mWifiReceiver);
    }

    @Override
    public void fetch() {
        if ( mDataStore == null ) return;
        mDataStore.setWifiLevel(getWifiLevel());
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        return convertToStatus(mDataStore.getWifiLevel()).ordinal(); 
    }

    private final BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = getWifiLevel(); 
            boolean shouldPropagate = mDataStore.shouldPropagateStatusUserIdUpdate(level);
            if ( shouldPropagate ) {
                for ( Listener<Integer> listener : mListeners ) 
                    listener.onEvent(convertToStatus(level).ordinal());
            }
        }
    };

    private int getWifiLevel() {
        if ( mManager == null ) return -1; 
        if ( !mManager.isWifiEnabled() ) return -1; 
        int numberOfLevels = WifiStatus.values().length - 1;
        WifiInfo wifiinfo = mManager.getConnectionInfo(); 
        int level = WifiManager.calculateSignalLevel(wifiinfo.getRssi(), numberOfLevels);
        return level; 
    }

    private WifiStatus convertToStatus(int level) {
        WifiStatus status = WifiStatus.NONE; 
        // todo : check wifi level
        switch(level) {
            case 0: status = WifiStatus.WIFI_1; break;
            case 1: status = WifiStatus.WIFI_2; break;
            case 2: status = WifiStatus.WIFI_3; break;
            case 3: status = WifiStatus.WIFI_4; break; 
            default: break; 
        }
        return status; 
    }
}
