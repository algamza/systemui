package com.humaxdigital.automotive.systemui.droplist.user;

import android.os.RemoteException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import android.util.Log;

public class UserWifi extends IUserWifi.Stub {
    private final String TAG = "UserWifi";
    private final UserDroplistService mService; 
    private WifiManager mManager;
    private Context mContext; 

    private IUserWifiCallback mUserWifiCallback = null;

    public UserWifi(UserDroplistService service) {
        Log.d(TAG, "UserWifi");
        mService = service; 
        if ( service == null ) return;
        mContext = mService.getApplicationContext(); 
        if ( mContext == null ) return;
        mManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        mContext.registerReceiver(mReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
    }

    public void destroy() {
        if ( mContext != null ) mContext.unregisterReceiver(mReceiver); 
        mManager = null;
        mContext = null;
    }

    @Override
    public void registCallback(IUserWifiCallback callback) throws RemoteException {
        Log.d(TAG, "registCallback");
        mUserWifiCallback = callback;
    }

    @Override
    public void unregistCallback(IUserWifiCallback callback) throws RemoteException {
        Log.d(TAG, "unregistCallback");
        mUserWifiCallback = null;
    }

    @Override
    public boolean isEnabled() throws RemoteException {
        if ( mManager == null ) return false;
        return mManager.isWifiEnabled();
    }

    @Override
    public void setWifiEnable(boolean enable) throws RemoteException {
        if ( mManager == null ) return;
        mManager.setWifiEnabled(enable);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mManager == null || mUserWifiCallback == null ) return;
            if ( !intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION) ) return;
            Log.d(TAG, "WIFI_STATE_CHANGED_ACTION");
            try {
                if ( mManager.isWifiEnabled() ) 
                    mUserWifiCallback.onWifiEnableChanged(true);
                else 
                    mUserWifiCallback.onWifiEnableChanged(false);
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            }
        }
    };
}