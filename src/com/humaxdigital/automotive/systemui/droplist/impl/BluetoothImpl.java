package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.provider.Settings;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;

import android.util.Log;

public class BluetoothImpl extends BaseImplement<Boolean> {
    private final String TAG = "BluetoothImpl"; 
    private ContentResolver mContentResolver;
    private ContentObserver mBluetoothObserver;
    private final String BT_SYSTEM = "android.extension.car.BT_SYSTEM";

    public BluetoothImpl(Context context) {
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
        boolean bluetoothon = isBluetoothOn();
        Log.d(TAG, "get="+bluetoothon);
        return bluetoothon;
    }

    @Override
    public void set(Boolean e) {
        Log.d(TAG, "set="+e); 
        setBluetoothOn(e);
    }

    public void refresh() {
        if ( mContentResolver == null ) return; 
        if ( mBluetoothObserver != null )  {
            mContentResolver.unregisterContentObserver(mBluetoothObserver); 
        }
        Log.d(TAG, "refresh");
        mBluetoothObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(BT_SYSTEM), 
            false, mBluetoothObserver, UserHandle.USER_CURRENT); 

        if ( mListener != null ) mListener.onChange(isBluetoothOn()); 
    }

    private void cleanup() {
        if ( mContentResolver != null ) 
            mContentResolver.unregisterContentObserver(mBluetoothObserver); 
        mBluetoothObserver = null;
        mContentResolver = null;
    }

    private void init() {
        if ( mContext == null ) return;
        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        mBluetoothObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(BT_SYSTEM), 
            false, mBluetoothObserver, UserHandle.USER_CURRENT); 
    }

    private void setBluetoothOn(boolean on) {
        if ( mContext == null ) return;
        Log.d(TAG, "setBluetoothOn="+on);
        Settings.Global.putInt(mContext.getContentResolver(), 
            BT_SYSTEM,
            on?1:0); 
    }

    private boolean isBluetoothOn() {
        if ( mContext == null ) return false;
        int on = Settings.Global.getInt(mContext.getContentResolver(), BT_SYSTEM, 1);
        Log.d(TAG, "isBluetoothOn="+on);
        return on==0?false:true;
    }

    private ContentObserver createObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                Log.d(TAG, "onChange");
                if ( mListener != null ) 
                    mListener.onChange(isBluetoothOn()); 
            }
        };
        return observer; 
    }
}
