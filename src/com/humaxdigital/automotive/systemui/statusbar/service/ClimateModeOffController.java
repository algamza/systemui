package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateModeOffController extends ClimateBaseController<Boolean> {
    private static final String TAG = "ClimateModeOffController";
    private boolean mModeOff = false; 
    private int MODE_OFF = 0x0; 
    private final int mZone = 0; 

    public ClimateModeOffController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null ) return;
        try {
            mModeOff = mManager.getIntProperty(
                CarHvacManagerEx.VENDOR_CANRX_HVAC_MODE_DISPLAY, 
                mZone) == MODE_OFF ? true:false; 
            Log.d(TAG, "fetch:mode off="+mModeOff);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanDirection");
        }
    }

    @Override
    public Boolean update(Boolean e) {
        Log.d(TAG, "update="+e); 
        if ( mModeOff == e ) return false;
        mModeOff = e; 
        return true;
    }

    public Boolean convertUpdateValue(int val) {
        Log.d(TAG, "convertUpdateValue="+val); 
        return val == MODE_OFF ? true:false; 
    }

    @Override
    public Boolean get() {
        Log.d(TAG, "get="+mModeOff);
        return mModeOff; 
    }
}
