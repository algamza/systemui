package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;
import android.support.car.CarNotConnectedException;

public class ClimateAirCirculationController extends ClimateBaseController<Boolean> {
    private static final String TAG = "ClimateAirCirculationController";
    
    public ClimateAirCirculationController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try { 
            boolean val = mManager.getBooleanProperty(
                CarHvacManagerEx.ID_ZONED_AIR_RECIRCULATION_ON,
                ClimateControllerManager.HVAC_ALL); 
            Log.d(TAG, "fetch="+val);
            mDataStore.setAirCirculationState(val);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchAirCirculationState");
        }
    }

    @Override
    public Boolean update(Boolean e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !mDataStore.shouldPropagateAirCirculationUpdate(e) ) return false;
        return true;
    }

    @Override
    public Boolean get() {
        if ( mDataStore == null ) return false;
        boolean val = mDataStore.getAirCirculationState(); 
        Log.d(TAG, "get="+val); 
        return val;
    }

    @Override
    public void set(Boolean e) {
        if ( mDataStore == null || mManager == null ) return;
        mDataStore.setAirCirculationState(e);
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... unused) {
                try {
                    Log.d(TAG, "set="+e); 
                    mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_HVAC_RECIRC, 0, e?0x1:0x0);
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        }; 
        task.execute();
    }
}
