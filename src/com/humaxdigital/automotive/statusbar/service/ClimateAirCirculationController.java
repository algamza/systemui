package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.car.hardware.hvac.CarHvacManager;
import android.support.car.CarNotConnectedException;

public class ClimateAirCirculationController extends ClimateBaseController<Boolean> {
    private static final String TAG = "ClimateAirCirculationController";
    
    public ClimateAirCirculationController(Context context, DataStore store, CarHvacManager manager) {
        super(context, store, manager);
    }
    
    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return;
        try {
            mDataStore.setAirCirculationState(
                mManager.getBooleanProperty(
                    CarHvacManager.ID_ZONED_AIR_RECIRCULATION_ON,
                    ClimateControllerManager.SEAT_ALL));
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchAirCirculationState");
        }
    }

    @Override
    public Boolean update(Boolean e) {
        if ( mDataStore == null ) return false;
        if ( !mDataStore.shouldPropagateAirCirculationUpdate(e) ) return false;
        return true;
    }

    @Override
    public Boolean get() {
        if ( mDataStore == null ) return false;
        return mDataStore.getAirCirculationState();
    }

    @Override
    public void set(Boolean e) {
        if ( mDataStore == null || mManager == null ) return;
        mDataStore.setAirCirculationState(e);
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... unused) {
                try {
                    mManager.setBooleanProperty(
                            CarHvacManager.ID_ZONED_AIR_RECIRCULATION_ON,
                            ClimateControllerManager.SEAT_ALL, e);
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        }; 
        task.execute();
    }
}
