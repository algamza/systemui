package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.UserHandle;
import android.os.RemoteException;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.net.wifi.WifiManager;

import android.util.Log;

import com.humaxdigital.automotive.systemui.common.user.IUserWifi;
import com.humaxdigital.automotive.systemui.common.user.IUserWifiCallback;

public class SystemWifiController extends BaseController<Integer> {
    private final String TAG = "SystemWifiController"; 
    private enum WifiStatus { NONE, WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
    private IUserWifi mUserWifi = null;
    private WifiStatus mCurrentStatus = WifiStatus.NONE; 

    public SystemWifiController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        Log.d(TAG, "connect");
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect");
        try {
            if ( mUserWifi != null ) 
                mUserWifi.unregistCallback(mUserWifiCallback);
            mUserWifi = null;
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
    }

    @Override
    public Integer get() {
        mCurrentStatus = getCurrentState(); 
        Log.d(TAG, "get="+mCurrentStatus); 
        return mCurrentStatus.ordinal(); 
    }

    public void fetchUserWifi(IUserWifi wifi) {
        try {
            if ( wifi == null ) {
                if ( mUserWifi != null ) 
                    mUserWifi.unregistCallback(mUserWifiCallback); 
                mUserWifi = null;
                return;
            }
            mUserWifi = wifi; 
            mUserWifi.registCallback(mUserWifiCallback); 
            mCurrentStatus = getCurrentState();
            Log.d(TAG, "fetchUserWifi="+mCurrentStatus); 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
    }

    private WifiStatus getCurrentState() {
        WifiStatus state = WifiStatus.NONE; 
        if ( mUserWifi == null ) return state; 
        try {
            boolean enable = mUserWifi.isEnabled(); 
            if ( !enable ) return state; 
            boolean connected = mUserWifi.isConnected(); 
            if ( !connected ) return state;
            int level = getWifiLevel();
            state = convertToStatus(level);
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
        Log.d(TAG, "getCurrentState="+state); 
        return state; 
    }

    private void broadcastChangeEvent() {
        WifiStatus status = getCurrentState();
        if ( mCurrentStatus == status ) return;
        mCurrentStatus = status;
        for ( Listener listener : mListeners ) 
            listener.onEvent(mCurrentStatus.ordinal());
    }

    private final IUserWifiCallback.Stub mUserWifiCallback = 
        new IUserWifiCallback.Stub() {
        @Override
        public void onWifiEnableChanged(boolean enable) throws RemoteException { 
            Log.d(TAG, "onWifiEnableChanged="+enable); 
            if ( !enable ) broadcastChangeEvent();
        }
        @Override
        public void onWifiConnectionChanged(boolean connected) throws RemoteException {
            Log.d(TAG, "onWifiConnectionChanged="+connected); 
            broadcastChangeEvent();
        }
        @Override
        public void onWifiRssiChanged(int rssi) throws RemoteException {
            Log.d(TAG, "onWifiRssiChanged="+rssi); 
            broadcastChangeEvent();
        }
    };

    private int getWifiLevel() {
        if ( mUserWifi == null ) return 0;
        int numberOfLevels = WifiStatus.values().length - 1;
        // min == -100;
        // max == -50;
        int real_level = 0;
        try {
            real_level = mUserWifi.getRssi(); 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
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
