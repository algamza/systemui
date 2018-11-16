package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateDRTempController extends ClimateBaseController<Float> {
    private static final String TAG = "ClimateDRTempController";
    final int mZone = ClimateControllerManager.SEAT_DRIVER; 

    public ClimateDRTempController(Context context, 
        DataStore store, CarHvacManagerEx manager) {
        super(context, store, manager);
    }
    
    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return;
        try {
            int value = mManager.getIntProperty(
                CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F, mZone);
            float f = ClimateControllerManager.tempHexToPhy(value); 
            Log.d(TAG, "fetch="+f+", value="+value); 
            mDataStore.setTemperature(mZone, f);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchTemperature");
        }
    }

    @Override
    public Boolean update(Float e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !mDataStore.shouldPropagateTempUpdate(mZone, e) ) 
            return false;
        return true;
    }

    @Override
    public Float get() {
        if ( mDataStore == null ) return 0.0f;
        float val = mDataStore.getTemperature(mZone); 
        Log.d(TAG, "get="+val); 
        return val; 
    }
}
