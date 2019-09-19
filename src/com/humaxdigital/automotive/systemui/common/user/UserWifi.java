package com.humaxdigital.automotive.systemui.common.user;

import android.os.RemoteException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.NetworkInfo;
import android.net.ConnectivityManager; 

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class UserWifi extends IUserWifi.Stub {
    private final String TAG = "UserWifi";
    private final PerUserService mService; 
    private WifiManager mManager;
    private Context mContext; 
    private ConnectivityManager mConnectivityMgr; 
    private boolean mConnected = false;

    private List<IUserWifiCallback> mListeners = new ArrayList<>(); 

    public UserWifi(PerUserService service) {
        Log.d(TAG, "UserWifi");
        mService = service; 
        if ( service == null ) return;
        mContext = mService.getApplicationContext(); 
        if ( mContext == null ) return;
        mManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        mConnectivityMgr = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE); 
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION); 
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, filter);

        updateNetworkInfo(mConnectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI));
    }

    public void destroy() {
        Log.d(TAG, "destroy");
        if ( mContext != null ) mContext.unregisterReceiver(mReceiver);
        mListeners.clear(); 
        mManager = null;
        mContext = null;
    }

    @Override
    public void registCallback(IUserWifiCallback callback) throws RemoteException {
        Log.d(TAG, "registCallback");
        mListeners.add(callback);
    }

    @Override
    public void unregistCallback(IUserWifiCallback callback) throws RemoteException {
        Log.d(TAG, "unregistCallback");
        mListeners.remove(callback);
    }

    @Override
    public boolean isEnabled() throws RemoteException {
        if ( mManager == null ) return false;
        boolean is_enable = mManager.isWifiEnabled();
        Log.d(TAG, "isEnabled="+is_enable);
        return is_enable;
    }

    @Override
    public void setWifiEnable(boolean enable) throws RemoteException {
        if ( mManager == null ) return;
        mManager.setWifiEnabled(enable);
    }

    @Override
    public boolean isConnected() throws RemoteException {
        //updateNetworkInfo(mConnectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI));
        Log.d(TAG, "isConnected="+mConnected); 
        return mConnected;
    }

    @Override
    public int getRssi() throws RemoteException { 
        if ( mManager == null ) return 0;
        WifiInfo wifiinfo = mManager.getConnectionInfo(); 
        int rssi = wifiinfo.getRssi();
        Log.d(TAG, "getRssi="+rssi);
        return rssi;
    }

    private void updateNetworkInfo(NetworkInfo info) { 
        if ( info == null ) return;
        if ( info.getState() == NetworkInfo.State.CONNECTED ) mConnected = true;
        else mConnected = false;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mManager == null ) return;
            switch(intent.getAction()) {
                case WifiManager.WIFI_STATE_CHANGED_ACTION: {
                    boolean enable = mManager.isWifiEnabled();
                    Log.d(TAG, "WIFI_STATE_CHANGED_ACTION="+enable);
                    try {
                        for ( IUserWifiCallback callback : mListeners ) 
                            callback.onWifiEnableChanged(enable);
                    } catch( RemoteException e ) {
                        Log.e(TAG, "error:"+e);
                    }
                    break;
                }
                case WifiManager.RSSI_CHANGED_ACTION: {
                    try {
                        WifiInfo wifiinfo = mManager.getConnectionInfo(); 
                        int rssi = wifiinfo.getRssi();
                        Log.d(TAG, "RSSI_CHANGED_ACTION="+rssi);
                        for ( IUserWifiCallback callback : mListeners ) 
                            callback.onWifiRssiChanged(rssi);
                    } catch( RemoteException e ) {
                        Log.e(TAG, "error:"+e);
                    }
                    break;
                }
                case WifiManager.NETWORK_STATE_CHANGED_ACTION: {
                    try {
                        boolean is_connected = false;
                        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO); 
                        if ( info == null ) break;
                        if ( info.getState() == NetworkInfo.State.CONNECTED ) is_connected = true;
                        else is_connected = false;
                        if ( is_connected == mConnected ) break;
                        updateNetworkInfo(info);
                        Log.d(TAG, "NETWORK_STATE_CHANGED_ACTION:connected="+mConnected);
                        if ( mConnected ) {
                            for ( IUserWifiCallback callback : mListeners ) 
                                callback.onWifiConnectionChanged(true);
                        } else {
                            for ( IUserWifiCallback callback : mListeners ) 
                                callback.onWifiConnectionChanged(false);
                        }                                                                   
                    } catch( RemoteException e ) {
                        Log.e(TAG, "error:"+e);
                    }
                    break;
                }
            }
        }
    };
}