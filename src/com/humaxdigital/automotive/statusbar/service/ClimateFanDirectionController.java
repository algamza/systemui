package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateFanDirectionController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateFanDirectionController";
    private enum FanDirectionStatus { FACE, FLOOR, FLOOR_FACE, FLOOR_DEFROST }
    private final int mZone = 0; 

    public ClimateFanDirectionController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try {
            int val = mManager.getIntProperty(
                CarHvacManagerEx.VENDOR_CANRX_HVAC_MODE_DISPLAY, mZone);
            Log.d(TAG, "fetch="+val);
            mDataStore.setFanDirection(val);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanDirection");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e);
        if ( !mDataStore.shouldPropagateFanDirectionUpdate(e) ) 
            return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        int direction = mDataStore.getFanDirection(); 
        Log.d(TAG, "get="+direction);
        return convertToStatus(direction).ordinal(); 
    }

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;
        if ( !mDataStore.shouldPropagateFanDirectionUpdate(e) ) return;  
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... unused) {
                try {
                    Log.d(TAG, "set="+e); 
                    // todo : current only du2 
                    mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_HVAC_MODE, 0, 0x01);
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        }; 
        task.execute();
    }

    private FanDirectionStatus convertToStatus(int index) {
        FanDirectionStatus status = FanDirectionStatus.FLOOR;
        switch(index) {
            case 0x3: status = FanDirectionStatus.FLOOR; break;
            case 0x1: status = FanDirectionStatus.FACE; break;
            case 0x2: status = FanDirectionStatus.FLOOR_FACE; break; 
            case 0x8: status = FanDirectionStatus.FLOOR_DEFROST; break;
            default: break; 
        }
        return status; 
    }

}
