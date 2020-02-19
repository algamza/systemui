package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;
import android.extension.car.CarUSMManager;

import android.support.car.CarNotConnectedException;

public class ClimatePSTempController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimatePSTempController";
    public enum MODE {
        CELSIUS(0), FAHRENHEIT(1);
        private final int state; 
        MODE(int state) { this.state = state; }
        public int state() { return state; }  
        static MODE getStateFromSignal(int signal) { 
            MODE ret = MODE.CELSIUS; 
            switch(signal) {
                case 0x1: ret = CELSIUS; break; 
                case 0x2: ret = FAHRENHEIT; break; 
                default: break; 
            }
            return ret; 
        }; 
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
        Log.d(TAG, "fetch"); 
        update();
    }

    @Override
    public Boolean update() {
        if ( mManager == null || mDataStore == null ) return false;
        try {
            int value = 0; 
            if ( mMode == MODE.CELSIUS ) {
                value = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C, mZone);
            } else {
                value = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F, mZone);
            }
            Log.d(TAG, "update="+value); 
            mDataStore.setTemperature(mZone, value);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchTemperature");
            return false; 
        }
        return true; 
    }

    public void fetchUSMManager(CarUSMManager manager) { 
        if ( manager == null ) return; 
        mUSMMgr = manager; 
        try {
            int value = mUSMMgr.getIntProperty(
                CarUSMManager.VENDOR_CANRX_USM_TEMPRATURE_UNIT, 0);
            mMode = MODE.getStateFromSignal(value); 
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
        MODE _mode = MODE.getStateFromSignal(mode);
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
}
