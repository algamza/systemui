package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import com.humaxdigital.automotive.systemui.common.logger.VCRMLogger;

import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.extension.car.CarSystemManager;
import android.extension.car.CarPropertyFilter;
import android.extension.car.value.CarEventValue;

import android.util.Log;

public class SystemWirelessChargeController extends BaseController<Integer> {
    private static final String TAG = "SystemWirelessChargeController";
    private enum WirelessChargeStatus { 
        NONE(0), CHARGED(1), CHARGING(2), ERROR(3);
        private final int state; 
        WirelessChargeStatus(int state) { this.state = state;}
        public int state() { return state; } 
    }
    private CarSystemManager mManager;

    @SuppressWarnings("unchecked")
    public static <E> E getValue(CarPropertyValue propertyValue) {
        return (E) propertyValue.getValue();
    }

    public SystemWirelessChargeController(Context context, DataStore store) {
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
            mManager.unregisterCallback(mSystemCallback);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }
    }

    public void fetch(CarSystemManager manager) {
        if ( mDataStore == null || manager == null ) return;
        mManager = manager; 
        int value = 0; 
        try {
            CarPropertyFilter filter = new CarPropertyFilter();
            filter.addId(CarSystemManager.VENDOR_CANRX_WPC_STATUS);
            mManager.registerCallback(mSystemCallback, filter);
            value = mManager.getWirelessChargeStatus();
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }
        Log.d(TAG, "fetch="+value); 
        if ( !checkValid(value) ) return;
        mDataStore.setWirelessChargeState(value);   
    }
    

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        int value = mDataStore.getWirelessChargeState(); 
        Log.d(TAG, "get="+value); 
        return convertToStatus(value).state(); 
    }

    private Boolean checkValid(int value) {
        if ( value == 0x2 || value == 0x3 || value == 0x5 || value == 0x0 ) return true; 
        return false; 
    }

    private WirelessChargeStatus convertToStatus(int mode) {
        WirelessChargeStatus status = WirelessChargeStatus.NONE; 
        /*
        0x0:Off
        0x1:Cellphone on the pad
        0x2:Charging
        0x3:Charging complete
        0x4:Cellphone reminder
        0x5:Charging error
        */
        switch(mode) {
            case 0x0: status = WirelessChargeStatus.NONE; break;
            case 0x3: status = WirelessChargeStatus.CHARGED; break;
            case 0x2: status = WirelessChargeStatus.CHARGING; break;
            case 0x5: status = WirelessChargeStatus.ERROR; break;
            default: break; 
        }
        return status; 
    }

    private void sendVcrmLog(int mode) {
        VCRMLogger.WirelessChargingState state = VCRMLogger.WirelessChargingState.OFF; 
        switch(mode) {
            case 0x0: state = VCRMLogger.WirelessChargingState.OFF; break; 
            case 0x1: state = VCRMLogger.WirelessChargingState.CELLPHONE_ON_PAD; break; 
            case 0x2: state = VCRMLogger.WirelessChargingState.CHARGING; break; 
            case 0x3: state = VCRMLogger.WirelessChargingState.CHARGING_COMPLETE; break; 
            case 0x4: state = VCRMLogger.WirelessChargingState.CELLPHONE_REMINDER; break; 
            case 0x5: state = VCRMLogger.WirelessChargingState.CHARGING_ERROR; break; 
            default: return;  
        }
        VCRMLogger.changedWirelessCharging(state);
    }

    private final CarSystemManager.CarSystemEventCallback mSystemCallback = 
        new CarSystemManager.CarSystemEventCallback () {
        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            int id = value.getPropertyId();
            Log.d(TAG, "onChangeEvent:CarPropertyValue="+id); 
            switch(id) {
                case CarSystemManager.VENDOR_CANRX_WPC_STATUS: {
                    int mode = getValue(value);
                    sendVcrmLog(mode); 
                    if ( !checkValid(mode) ) break;
                    if ( mDataStore.shouldPropagateWirelessChargeStatusUpdate(mode) ) {
                        for ( Listener listener : mListeners ) 
                            listener.onEvent(convertToStatus(mode).state());
                    }
                    break;
                }
            }
        }

        @Override
        public void onChangeEvent(final CarEventValue value) {
            Log.d(TAG, "onChangeEvent:CarEventValue"); 
        }

        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
            Log.w(TAG, "Error:id="+propertyId+", zone="+zone); 
        }
    };
}
