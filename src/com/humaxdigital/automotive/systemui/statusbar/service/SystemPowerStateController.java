package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.car.CarNotConnectedException;
import android.extension.car.util.PowerState;
import android.extension.car.CarSystemManager;
import android.car.hardware.CarPropertyValue;
import android.extension.car.value.CarEventValue;

import android.util.Log;

public class SystemPowerStateController extends BaseController<Boolean> {
    private final String TAG = "SystemPowerStateController"; 
    private CarSystemManager mSystemManager;
    private boolean mPowerOn = true; 
   
    public SystemPowerStateController(Context context, DataStore store) {
        super(context, store);
        Log.d(TAG, "SystemPowerStateController"); 
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect"); 
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect"); 
        if ( mSystemManager != null ) {
            try {
                mSystemManager.unregisterCallback(mSystemCallback);
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car is not connected!");
            }
        }
        mSystemManager = null;
    }

    @Override
    public Boolean get() {
        return mPowerOn; 
    }

    public void fetch(CarSystemManager manager) {
        if ( manager == null ) return; 
        mSystemManager = manager; 
        int value = 0; 
        try {
            mSystemManager.registerCallback(mSystemCallback);
            mPowerOn = mSystemManager.getCurrentPowerState() 
                == PowerState.POWER_STATE_POWER_OFF ? false:true;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }

        for ( Listener listener : mListeners ) 
            listener.onEvent(mPowerOn);
    }

    private final CarSystemManager.CarSystemEventCallback mSystemCallback = 
        new CarSystemManager.CarSystemEventCallback () {
        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            switch(value.getPropertyId()) {
                case CarSystemManager.VENDOR_SYSTEM_LASTPOWERSTATE: {
                    Log.d(TAG, "VENDOR_SYSTEM_LASTPOWERSTATE="+value.getValue()); 
                    if( value.getValue().equals(PowerState.POWER_STATE_POWER_OFF) ) {
                        if ( !mPowerOn ) return;
                        mPowerOn = false;
                    } else {
                        if ( mPowerOn ) return;
                        mPowerOn = true;
                    }
                    for ( Listener listener : mListeners ) 
                        listener.onEvent(mPowerOn);
                    break;
                }
            }
        }
        @Override
        public void onChangeEvent(final CarEventValue value) {
        }
        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
        }
    };
}
