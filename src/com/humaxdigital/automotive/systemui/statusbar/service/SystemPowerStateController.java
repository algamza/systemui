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

public class SystemPowerStateController extends BaseController<Integer> {
    private final String TAG = "SystemPowerStateController"; 
    public enum State {
        NORMAL,
        AV_OFF, 
        POWER_OFF
    }
    private CarSystemManager mSystemManager;
    private State mPowerState = State.NORMAL; 
   
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
    public Integer get() {
        return mPowerState.ordinal(); 
    }

    public void fetch(CarSystemManager manager) {
        if ( manager == null ) return; 
        mSystemManager = manager; 
        int value = 0; 
        try {
            mSystemManager.registerCallback(mSystemCallback);
            int state = mSystemManager.getCurrentPowerState(); 
            if ( state == PowerState.POWER_STATE_POWER_OFF ) 
                mPowerState = State.POWER_OFF; 
            else if ( state == PowerState.POWER_STATE_AV_OFF ) 
                mPowerState = State.AV_OFF; 
            else 
                mPowerState = State.NORMAL; 
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }

        for ( Listener listener : mListeners ) 
            listener.onEvent(mPowerState.ordinal());
    }

    private final CarSystemManager.CarSystemEventCallback mSystemCallback = 
        new CarSystemManager.CarSystemEventCallback () {
        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            switch(value.getPropertyId()) {
                case CarSystemManager.VENDOR_SYSTEM_LASTPOWERSTATE: {
                    Log.d(TAG, "VENDOR_SYSTEM_LASTPOWERSTATE="+value.getValue()); 
                    if( value.getValue().equals(PowerState.POWER_STATE_POWER_OFF) ) {
                        if ( mPowerState == State.POWER_OFF ) break;
                        mPowerState = State.POWER_OFF; 
                    } else if( value.getValue().equals(PowerState.POWER_STATE_AV_OFF) ) {
                        if ( mPowerState == State.AV_OFF ) break; 
                        mPowerState = State.AV_OFF; 
                    } else {
                        if ( mPowerState == State.NORMAL ) break; 
                        mPowerState = State.NORMAL; 
                    }

                    for ( Listener listener : mListeners ) 
                        listener.onEvent(mPowerState.ordinal());

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
