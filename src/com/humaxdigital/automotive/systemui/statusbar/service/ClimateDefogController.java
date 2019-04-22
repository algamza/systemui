package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateDefogController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateDefogController";
    private enum DefogState { ON, OFF }
    private final int mZoneFrontDef = ClimateControllerManager.WINDOW_FRONT; 
    private final int mZoneRearDef = ClimateControllerManager.WINDOW_REAR; 

    public ClimateDefogController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try {
            int front_def = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_DEFOG, mZoneFrontDef);
            if ( checkValid(front_def) )
                mDataStore.setDefrosterState(mZoneFrontDef, front_def==0x1?true:false);
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
        if ( !mDataStore.shouldPropagateDefrosterUpdate(mZoneFrontDef, e==0x1?true:false) ) 
            return false; 
        return true; 
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        DefogState state = convertToStatus(mDataStore.getDefrosterState(mZoneFrontDef)?0x1:0x0); 
        Log.d(TAG, "get="+state);
        return state.ordinal(); 
    }

    private Boolean checkValid(int val) {
        if ( val == 0x0 || val == 0x1 ) return true; 
        return false; 
    }

    private DefogState convertToStatus(int index) {
        DefogState status = DefogState.OFF;
        switch(index) {
            case 0x0: status = DefogState.OFF; break;
            case 0x1: status = DefogState.ON; break;
            default: break; 
        }
        return status; 
    }
}
