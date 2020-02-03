package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;
import android.extension.car.util.VehicleUtils;
import android.support.car.CarNotConnectedException;

public class ClimateAirCleaningController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateAirCleaningController";
    private enum Status { 
        ON(0), OFF(1);
        private final int state; 
        Status(int state) { this.state = state; }
        public int state() { return state; }
    }

    public ClimateAirCleaningController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try { 
            int val = mManager.getIntProperty(
                CarHvacManagerEx.VENDOR_CANRX_HVAC_AIR_CLEANING_STATUS, 
                VehicleUtils.VEHICLE_AREA_TYPE_GLOBAL); 
            Log.d(TAG, "fetch="+val);
            if ( !isValid(val) ) return; 
            mDataStore.setAirCleaningState(val);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchAirConditionerState");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !isValid(e) ) return false;  
        if ( !mDataStore.shouldPropagateAirCleaningStateUpdate(e) ) return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        Integer val = mDataStore.getAirCleaningState(); 
        Log.d(TAG, "get="+val); 
        return convertToStatus(val).state();
    }

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;

        //mDataStore.setAirCleaningState(convertToVal(Status.values()[e]));
        final AsyncTask<Integer, Void, Void> task = new AsyncTask<Integer, Void, Void>() {
            protected Void doInBackground(Integer... Integers) {
                try {
                    mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_HVAC_AIR_CLEANING, 
                        VehicleUtils.VEHICLE_AREA_TYPE_GLOBAL, Integers[0]);
                    Log.d(TAG, "set="+Integers[0]); 
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        }; 
        task.execute(convertToVal(Status.values()[e]));
    }

    private boolean isValid(int val) {
        if ( val == 0x0 || val == 0x1 ) return true; 
        return false; 
    }

    private Status convertToStatus(int val) {
        Status status = Status.OFF; 
        switch(val) {
            case 0x0: status = Status.OFF; break; 
            case 0x1: status = Status.ON; break;
            default: break; 
        }
        return status; 
    }

    private int convertToVal(Status status) {
        int val = 0; 
        switch(status) {
            case ON: val = 0x1; break; 
            case OFF: val = 0x0; break; 
            default: break; 
        }
        return val; 
    }
}
