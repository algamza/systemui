package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.extension.car.CarRemainderManager;

import android.util.Log;

public class SystemWirelessChargeController extends BaseController<Integer> {
    private static final String TAG = "SystemWirelessChargeController";
    private enum WirelessChargeStatus { NONE, CHARGED, CHARGING, ERROR }
    private CarRemainderManager mManager;

    public SystemWirelessChargeController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
    }

    @Override
    public void disconnect() {

    }

    public void fetch(CarRemainderManager manager) {
        if ( mDataStore == null || manager == null ) return;
        mManager = manager; 
        int value = 0; 
        try {
            mManager.registerCallback(mRemainderCallback);
            value = mManager.getIntProperty(CarRemainderManager.VENDOR_CANRX_WPC_STATUS, 0); 
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }
        if ( checkValid(value) ) {
            WirelessChargeStatus state = convertToStatus(value); 
            Log.d(TAG, "fetch="+state); 
            mDataStore.setWirelessChargeState(state.ordinal());
        }        
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        int value = mDataStore.getWirelessChargeState(); 
        Log.d(TAG, "get="+value); 
        return convertToStatus(value).ordinal(); 
    }

    private Boolean checkValid(int value) {
        if ( value == 0x2 || value == 0x3 || value == 0x5 || value == 0x0 ) return true; 
        return false; 
    }

    private WirelessChargeStatus convertToStatus(int mode) {
        WirelessChargeStatus status = WirelessChargeStatus.NONE; 
        // todo : check status 
        switch(mode) {
            case 0x0: status = WirelessChargeStatus.NONE; break;
            case 0x3: status = WirelessChargeStatus.CHARGED; break;
            case 0x2: status = WirelessChargeStatus.CHARGING; break;
            case 0x5: status = WirelessChargeStatus.ERROR; break;
            default: break; 
        }
        return status; 
    }

    private final CarRemainderManager.CarRemainderEventCallback mRemainderCallback =
        new CarRemainderManager.CarRemainderEventCallback () {
        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            if ( mDataStore == null ) return; 
            switch(value.getPropertyId()) {
                case CarRemainderManager.VENDOR_CANRX_WPC_STATUS: {
                    if ( !checkValid((int)value.getValue()) ) break; 
                    if ( !mDataStore.shouldPropagateWirelessChargeStatusUpdate((int)value.getValue()) ) break; 
                    for ( Listener listener : mListeners ) {
                        listener.onEvent(convertToStatus(mDataStore.getWirelessChargeState()).ordinal());
                    }
                    break;
                } 
                default: break; 
            }
        }

        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
            Log.w(TAG, "onErrorEvent():propertyId="+propertyId+", zone="+zone);
        }
    };
}
