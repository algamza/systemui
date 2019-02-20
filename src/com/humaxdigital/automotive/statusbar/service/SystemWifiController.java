package com.humaxdigital.automotive.statusbar.service;

import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.NetworkInfo;

import android.util.Log;

public class SystemWifiController extends BaseController<Integer> {
    private final String TAG = "SystemWifiController"; 
    private enum WifiStatus { NONE, WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
    private WifiManager mManager; 
    private boolean mConnected = false;

    public SystemWifiController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect");
        mManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiverAsUser(mWifiReceiver, UserHandle.ALL, filter, null, null);
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect");
        if ( mContext != null ) mContext.unregisterReceiver(mWifiReceiver);
    }

    @Override
    public void fetch() {
    }

    @Override
    public Integer get() {
        WifiStatus status = WifiStatus.NONE; 
        if ( mManager == null ) return status.ordinal(); 
        if ( !mManager.isWifiEnabled() ) status.ordinal();  
        if ( !mConnected ) status.ordinal(); 
        status = convertToStatus(getWifiLevel()); 
        Log.d(TAG, "get="+status);
        return status.ordinal(); 
    }

    private final BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mManager == null ) return;
            switch(intent.getAction()) {
                case WifiManager.RSSI_CHANGED_ACTION: {
                    int level = getWifiLevel(); 
                    Log.d(TAG, "RSSI_CHANGED_ACTION="+level);
                    for ( Listener<Integer> listener : mListeners ) 
                        listener.onEvent(convertToStatus(level).ordinal());
                    break;
                }
                case WifiManager.WIFI_STATE_CHANGED_ACTION: {
                    int level = getWifiLevel(); 
                    boolean enable = mManager.isWifiEnabled();
                    Log.d(TAG, "WIFI_STATE_CHANGED_ACTION:level="+level+", enable="+enable);
                    if ( enable ) {
                        for ( Listener<Integer> listener : mListeners ) 
                            listener.onEvent(convertToStatus(level).ordinal());
                    } else {
                        for ( Listener<Integer> listener : mListeners ) 
                            listener.onEvent(WifiStatus.NONE.ordinal());
                    }
                    break;
                }
                case WifiManager.NETWORK_STATE_CHANGED_ACTION: {
                    int level = getWifiLevel(); 
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO); 
                    if ( info == null ) break;
                    if ( info.getState() == NetworkInfo.State.CONNECTED ) mConnected = true;
                    else mConnected = false;
                    Log.d(TAG, "NETWORK_STATE_CHANGED_ACTION:level="+level+", connected="+mConnected);
                    if ( mConnected ) {
                        for ( Listener<Integer> listener : mListeners ) 
                            listener.onEvent(convertToStatus(level).ordinal());
                    } else {
                        for ( Listener<Integer> listener : mListeners ) 
                            listener.onEvent(WifiStatus.NONE.ordinal());
                    }
                    break;
                }
            }
        }
    };

    private int getWifiLevel() {
        int numberOfLevels = WifiStatus.values().length - 1;
        WifiInfo wifiinfo = mManager.getConnectionInfo(); 
        // min == -100;
        // max == -50;
        int real_level = wifiinfo.getRssi();
        Log.d(TAG, "getWifiLevel:real_level="+real_level+", numberof="+numberOfLevels);
        int level = WifiManager.calculateSignalLevel(real_level, numberOfLevels);
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
