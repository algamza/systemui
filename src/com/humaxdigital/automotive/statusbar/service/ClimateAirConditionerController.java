package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;
import android.support.car.CarNotConnectedException;

public class ClimateAirConditionerController extends ClimateBaseController<Boolean> {
    private static final String TAG = "ClimateAirConditionerController";
    
    public ClimateAirConditionerController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try { 
            boolean val = mManager.getBooleanProperty(
                CarHvacManagerEx.ID_ZONED_AC_ON, 
                ClimateControllerManager.HVAC_ALL); 
            Log.d(TAG, "fetch="+val);
            mDataStore.setAirConditionerState(val);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchAirConditionerState");
        }
    }

    @Override
    public Boolean update(Boolean e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !mDataStore.shouldPropagateAirConditionerUpdate(e) ) return false;
        return true;
    }

    @Override
    public Boolean get() {
        if ( mDataStore == null ) return false;
        boolean val = mDataStore.getAirConditionerState(); 
        Log.d(TAG, "get="+val); 
        return val;
    }

    @Override
    public void set(Boolean e) {
        if ( mDataStore == null || mManager == null ) return;
        mDataStore.setAirConditionerState(e);
        final AsyncTask<Boolean, Void, Void> task = new AsyncTask<Boolean, Void, Void>() {
            protected Void doInBackground(Boolean... booleans) {
                try {
                    mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_HVAC_AC, 0, booleans[0]?0x1:0x0);
                    Log.d(TAG, "set="+booleans[0]); 
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        }; 
        task.execute(e);
    }
}
