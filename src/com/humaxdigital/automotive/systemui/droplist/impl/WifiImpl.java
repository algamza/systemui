package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.provider.Settings;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;
import android.os.RemoteException;

import android.util.Log;

import com.humaxdigital.automotive.systemui.common.user.IUserWifi;
import com.humaxdigital.automotive.systemui.common.user.IUserWifiCallback;

public class WifiImpl extends BaseImplement<Boolean> {
    private final String TAG = "WifiImpl"; 
    private IUserWifi mUserWifi = null;
    private ContentResolver mContentResolver;
    private ContentObserver mWifiObserver;

    public WifiImpl(Context context) {
        super(context);
    }

    @Override
    public void create() {
        init();
    }

    @Override
    public void destroy() {
        cleanup();
    }

    @Override
    public Boolean get() {
        boolean on = isWifiOn();
        Log.d(TAG, "get="+on);
        return on;
    }

    @Override
    public void set(Boolean e) {
        Log.d(TAG, "set="+e); 
        setWifiOn(e);
    }

    public void refresh() {
        if ( mContentResolver == null ) return; 
        if ( mWifiObserver != null )  {
            mContentResolver.unregisterContentObserver(mWifiObserver); 
        }
        Log.d(TAG, "refresh");
        mWifiObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.WIFI_ON), 
            false, mWifiObserver, UserHandle.USER_CURRENT); 

        if ( mListener != null ) mListener.onChange(isWifiOn()); 
    }

    public void fetch(IUserWifi wifi) {
        if ( wifi == null ) {
            Log.d(TAG, "fetch = null");
            try {
                if ( mUserWifi != null ) 
                    mUserWifi.unregistCallback(mUserWifiCallback);
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            } 
            mUserWifi = null;
            return;
        }
        Log.d(TAG, "fetch = user Audio");
        mUserWifi = wifi;
        try {
            if ( mUserWifi != null ) {
                mUserWifi.registCallback(mUserWifiCallback); 
            }
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        }
        
        if ( mListener != null ) mListener.onChange(isWifiOn()); 
    }

    private void cleanup() {
        if ( mContentResolver != null ) 
            mContentResolver.unregisterContentObserver(mWifiObserver); 
        mWifiObserver = null;
        mContentResolver = null;
    }

    private void init() {
        if ( mContext == null ) return;
        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        mWifiObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.WIFI_ON), 
            false, mWifiObserver, UserHandle.USER_CURRENT); 
    }

    private void setWifiOn(boolean on) {
        if ( mContext == null ) return;
        Log.d(TAG, "setWifiOn="+on);
        Settings.Global.putInt(mContext.getContentResolver(), 
            Settings.Global.WIFI_ON, on?1:0); 
        try {
            if ( mUserWifi != null ) 
                mUserWifi.setWifiEnable(on);
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        }
    }

    private boolean isWifiOn() {
        if ( mContext == null ) return false;
        int on = 0; 
        try {
            on = Settings.Global.getInt(mContext.getContentResolver(), 
                Settings.Global.WIFI_ON);
        } catch(Settings.SettingNotFoundException e) {
            Log.e(TAG, "error : " + e ); 
        }
        boolean isOn = false;
        try {
            if ( mUserWifi != null ) 
                isOn = mUserWifi.isEnabled();
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        }
        
        Log.d(TAG, "isWifiOn:setting value="+on+", device on="+isOn);

        return isOn;//on==0?false:true;
    }

    private ContentObserver createObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                Log.d(TAG, "onChange");
                if ( mListener != null ) 
                    mListener.onChange(isWifiOn()); 
            }
        };
        return observer; 
    }

    private final IUserWifiCallback.Stub mUserWifiCallback = 
        new IUserWifiCallback.Stub() {
        @Override
        public void onWifiEnableChanged(boolean enable) throws RemoteException {
            Log.d(TAG, "onWifiEnableChanged="+enable);
            if ( mListener != null ) mListener.onChange(isWifiOn()); 
        }
        @Override
        public void onWifiConnectionChanged(boolean connected) throws RemoteException {
        }
        @Override
        public void onWifiRssiChanged(int rssi) throws RemoteException {
        }
    }; 
}
