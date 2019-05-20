package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateDefogController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateDefogController";
    private enum DefogState { ON, OFF }
    private final int mZone = 0; 
    private final int FRONT_DEFOG_VALUE = 0x5; 

    public ClimateDefogController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try {
            int front_def = mManager.getIntProperty(
                CarHvacManagerEx.VENDOR_CANRX_HVAC_MODE_DISPLAY, mZone);
            if ( checkValid(front_def) )
                mDataStore.setDefrosterState(mZone, front_def==FRONT_DEFOG_VALUE?true:false);
            Log.d(TAG, "fetch:front="+front_def);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanDirection");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        if ( !checkValid(e) ) return false;  
        Log.d(TAG, "update="+e);
        if ( !mDataStore.shouldPropagateDefrosterUpdate(mZone, e==FRONT_DEFOG_VALUE?true:false) ) 
            return false; 
        return true; 
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        DefogState state = convertToStatus(mDataStore.getDefrosterState(mZone)?FRONT_DEFOG_VALUE:0x0); 
        Log.d(TAG, "get="+state);
        return state.ordinal(); 
    }

    private Boolean checkValid(int val) {
        if ( val >= 0x1 && val <= 0x5 ) return true; 
        return false; 
    }

    private DefogState convertToStatus(int index) {
        DefogState status = DefogState.OFF;
        switch(index) {
            case 0x0: status = DefogState.OFF; break;
            case FRONT_DEFOG_VALUE: status = DefogState.ON; break;
            default: break; 
        }
        return status; 
    }
}
