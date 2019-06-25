package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.provider.Settings;
import android.os.RemoteException;

import android.util.Log;

import com.humaxdigital.automotive.systemui.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.user.IUserBluetoothCallback;

public class BluetoothImpl extends BaseImplement<Boolean> {
    private final String TAG = "BluetoothImpl"; 
    private final String BT_SYSTEM = "android.extension.car.BT_SYSTEM";
    private IUserBluetooth mUserBluetooth = null;

    public BluetoothImpl(Context context) {
        super(context);
    }

    @Override
    public void create() {
    }

    @Override
    public void destroy() {
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

    public void fetch(IUserBluetooth bt) {
        if ( bt == null ) {
            try {
                if ( mUserBluetooth != null ) 
                    mUserBluetooth.unregistCallback(mUserBluetoothCallback);
                mUserBluetooth = null;
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            } 
            return;
        }
        mUserBluetooth = bt; 
        try {
            mUserBluetooth.registCallback(mUserBluetoothCallback);
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
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

    private final IUserBluetoothCallback.Stub mUserBluetoothCallback = 
        new IUserBluetoothCallback.Stub() {

        @Override
        public void onBluetoothEnableChanged(int enable) throws RemoteException {
            Log.d(TAG, "onBluetoothEnableChanged:enable="+enable);
            if ( mListener != null ) 
                mListener.onChange(enable==1?true:false); 
        }
        @Override
        public void onHeadsetConnectionStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onA2dpConnectionStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onBatteryStateChanged(int level) throws RemoteException {
        }
        @Override
        public void onAntennaStateChanged(int level) throws RemoteException {
        }
        @Override
        public void onBluetoothMicMuteStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onContactsDownloadStateChanged(int state) throws RemoteException {
        }
        @Override
        public void onCallHistoryDownloadStateChanged(int state) throws RemoteException {
        }
    };  
}
