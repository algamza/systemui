package com.humaxdigital.automotive.systemui.droplist.user;

import android.os.RemoteException;

import android.os.UserHandle;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;

public class UserBluetooth extends IUserBluetooth.Stub {
    private final String TAG = "UserBluetooth";
    private final UserDroplistService mService; 
    private BluetoothAdapter mBluetoothAdapter;
    private Context mContext; 

    private IUserBluetoothCallback mUserBluetoothCallback = null;

    public UserBluetooth(UserDroplistService service) {
        Log.d(TAG, "UserBluetooth");
        mService = service; 
        if ( service == null ) return;
        mContext = mService.getApplicationContext(); 
        if ( mContext == null ) return;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void destroy() {
        if ( mContext != null ) mContext.unregisterReceiver(mReceiver); 
        mBluetoothAdapter = null;
        mContext = null;
    }
    
    @Override
    public void registCallback(IUserBluetoothCallback callback) throws RemoteException {
        Log.d(TAG, "registCallback");
        mUserBluetoothCallback = callback;
    }

    @Override
    public void unregistCallback(IUserBluetoothCallback callback) throws RemoteException {
        Log.d(TAG, "unregistCallback");
        mUserBluetoothCallback = null; 
    }

    @Override
    public boolean isEnabled() throws RemoteException {
        
        if ( mBluetoothAdapter == null ) return false;
        boolean enable = mBluetoothAdapter.isEnabled();
        Log.d(TAG, "isEnabled="+enable);
        return enable;
    }

    @Override
    public void setBluetoothEnable(boolean enable) throws RemoteException {
        if ( mBluetoothAdapter == null ) return;
        Log.d(TAG, "setBluetoothEnable="+enable);
        if ( enable ) mBluetoothAdapter.enable();
        else mBluetoothAdapter.disable();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mBluetoothAdapter == null || mUserBluetoothCallback == null ) return;
            if ( intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED) ) {
                Log.d(TAG, "ACTION_STATE_CHANGED="+mBluetoothAdapter.getState());
                try {
                    if ( mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF ) 
                        mUserBluetoothCallback.onBluetoothEnableChanged(false);
                    else if ( mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON ) 
                        mUserBluetoothCallback.onBluetoothEnableChanged(true);
                } catch( RemoteException e ) {
                    Log.e(TAG, "error:"+e);
                }
            }
        }
    };
}