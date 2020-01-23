package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;
import android.support.car.CarNotConnectedException;

public class ClimateSyncController extends ClimateBaseController<Boolean> {
    private static final String TAG = "ClimateSyncController";
    private final int mZone = ClimateControllerManager.HVAC_ALL; 
    
    public ClimateSyncController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try { 
            int sync = mManager.getIntProperty(
                CarHvacManagerEx.VENDOR_CANRX_HVAC_DISPLAY_SYNC, mZone); 
            if ( checkValid(sync) ) mDataStore.setSyncState(checkOn(sync));
            Log.d(TAG, "fetch="+sync);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in sync fetch");
        }
    }

    @Override
    public Boolean update(Boolean e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !mDataStore.shouldPropagateSyncStateUpdate(e) ) return false;
        return true;
    }

    @Override
    public Boolean get() {
        if ( mDataStore == null ) return false;
        boolean val = mDataStore.getSyncState(); 
        Log.d(TAG, "get="+val); 
        return val;
    }

    @Override
    public void set(Boolean e) {
        if ( mDataStore == null || mManager == null ) return;
        final AsyncTask<Boolean, Void, Void> task = new AsyncTask<Boolean, Void, Void>() {
            protected Void doInBackground(Boolean... booleans) {
                try {
                    mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_HVAC_SYNC, 0, booleans[0]?0x2:0x1);
                    Log.d(TAG, "set="+booleans[0]); 
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        }; 
        task.execute(e);
    }

    public boolean checkOn(int val) {
        if ( val == 0x1 ) return true;
        else return false; 
    }

    public boolean checkValid(int val) {
        // 0x1:Sync On / 0x2:Sync OFF
        if ( val == 0x1 || val == 0x2 ) return true; 
        return false; 
    }
}
