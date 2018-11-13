package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.hardware.automotive.vehicle.V2_0.VehicleProperty;
import android.extension.car.CarHvacManagerEx;

import android.car.hardware.hvac.CarHvacManager;
import android.support.car.CarNotConnectedException;

public class ClimatePSTempController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimatePSTempController";

    final int mZone = ClimateControllerManager.SEAT_PASSENGER; 

    public ClimatePSTempController(Context context, 
        DataStore store, CarHvacManagerEx manager) {
        super(context, store, manager);
    }
    
    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return;
        //try {
            int value = 0x20; //mManager.getIntProperty(
                //VehicleProperty.VENDOR_CANRX_HVAC_TEMPERATURE_F, mZone);
           mDataStore.setTemperature(mZone, value);
        //} catch (android.car.CarNotConnectedException e) {
        //    Log.e(TAG, "Car not connected in fetchTemperature");
        //}
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        if ( !mDataStore.shouldPropagateTempUpdate(mZone, e) ) 
            return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        return mDataStore.getTemperature(mZone); 
    }
}
