package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;
import android.extension.car.CarUSMManager;

import android.support.car.CarNotConnectedException;

public class ClimatePSTempController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimatePSTempController";
    public enum MODE {
        CELSIUS,
        FAHRENHEIT
    }
    final int mZone = ClimateControllerManager.SEAT_PASSENGER; 
    MODE mMode = MODE.CELSIUS; 
    private CarUSMManager mUSMMgr; 

    public ClimatePSTempController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try {
            int value = 0; 
            if ( mMode == MODE.CELSIUS ) {
                value = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C, mZone);
            } else {
                value = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F, mZone);
            }
            Log.d(TAG, "fetch="+value); 
            mDataStore.setTemperature(mZone, value);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchTemperature");
        }
    }

    public void fetchUSMManager(CarUSMManager manager) { 
        if ( manager == null ) return; 
        mUSMMgr = manager; 
        try {
            int value = mUSMMgr.getIntProperty(
                CarUSMManager.VENDOR_CANRX_USM_TEMPRATURE_UNIT, 0);
            mMode = convertToMode(value); 
            Log.d(TAG, "fetchUSMManager="+value+", mode="+mMode); 
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchTemperature");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !mDataStore.shouldPropagateTempUpdate(mZone, e) ) 
            return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        Integer val = mDataStore.getTemperature(mZone); 
        return val; 
    }

    public MODE getCurrentTemperatureMode() {
        return mMode; 
    }

    public Boolean updateMode(int mode) {
        if ( mManager == null || mDataStore == null ) return false;
        MODE _mode = convertToMode(mode);
        if ( _mode == mMode ) return false; 
        mMode = _mode; 
        try {
            int value = 0; 
            if ( mMode == MODE.CELSIUS ) {
                value = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C, mZone);
            } else {
                value = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F, mZone);
            }
            Log.d(TAG, "fetch="+value+", mode="+mMode); 
            mDataStore.setTemperature(mZone, value);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchTemperature");
        }
        return true; 
    }

    private MODE convertToMode(int mode) {
        MODE ret = MODE.CELSIUS; 
        switch(mode) {
            case 0x1: ret = MODE.CELSIUS; break; 
            case 0x2: ret = MODE.FAHRENHEIT; break; 
            default: break; 
        }
        return ret; 
    }
}
