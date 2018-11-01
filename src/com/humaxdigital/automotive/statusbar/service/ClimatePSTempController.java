package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.car.hardware.hvac.CarHvacManager;
import android.support.car.CarNotConnectedException;

public class ClimatePSTempController extends ClimateBaseController<Float> {
    private static final String TAG = "ClimatePSTempController";

    final int mZone = ClimateControllerManager.PASSENGER_ZONE_ID; 

    public ClimatePSTempController(Context context, 
        DataStore store, CarHvacManager manager) {
        super(context, store, manager);
    }
    
    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return;
        try {
            float value = mManager.getFloatProperty(
                CarHvacManager.ID_ZONED_TEMP_SETPOINT, mZone);
            mDataStore.setTemperature(mZone, value);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchTemperature");
        }
    }

    @Override
    public Boolean update(Float e) {
        if ( mDataStore == null ) return false;
        if ( !mDataStore.shouldPropagateTempUpdate(mZone, e) ) 
            return false;
        return true;
    }

    @Override
    public Float get() {
        if ( mDataStore == null ) return 0.0f;
        return celsiusToFahrenheit(mDataStore.getTemperature(mZone)); 
    }

    private float celsiusToFahrenheit(float c) {
        return c * 9 / 5 + 32;
    }
/*
    private float fahrenheitToCelsius(float f) {
        return (f - 32) * 5 / 9;
    }
    */
}
