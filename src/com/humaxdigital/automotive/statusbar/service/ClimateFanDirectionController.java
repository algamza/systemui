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
            if ( checkValid(val) ) mDataStore.setFanDirection(val);
            Log.d(TAG, "fetch:mode="+val);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanDirection");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e);
        if ( !checkValid(e) ) return false; 
        if ( !mDataStore.shouldPropagateFanDirectionUpdate(e) ) 
            return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        FanDirectionStatus state = convertToStatus(mDataStore.getFanDirection()); 
        Log.d(TAG, "get="+state);
        return state.ordinal(); 
    }

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;
        FanDirectionStatus status = FanDirectionStatus.values()[e]; 
        int val = convertToValue(status); 
        if ( !mDataStore.shouldPropagateFanDirectionUpdate(val) ) return;
        final AsyncTask<Integer, Void, Void> task = new AsyncTask<Integer, Void, Void>() {
            protected Void doInBackground(Integer... integers) {
                try {
                    Log.d(TAG, "set="+integers[0]); 
                    mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_HVAC_MODE, 0, integers[0]);
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        }; 
        task.execute(val);
    }

    private Boolean checkValid(int val) {
        if ( val >= 0x1 && val <= 0x4 ) return true; 
        return false; 
    }

    private int convertToValue(FanDirectionStatus status) {
        int value = 0x1;
        switch(status) {
            case FLOOR: value = 0x3; break;
            case FACE: value = 0x1; break; 
            case FLOOR_FACE: value = 0x2; break; 
            case FLOOR_DEFROST: value = 0x4; break; 
            default: break; 
        }
        return value; 
    }

    private FanDirectionStatus convertToStatus(int index) {
        FanDirectionStatus status = FanDirectionStatus.FLOOR;
        switch(index) {
            case 0x3: status = FanDirectionStatus.FLOOR; break;
            case 0x1: status = FanDirectionStatus.FACE; break;
            case 0x2: status = FanDirectionStatus.FLOOR_FACE; break; 
            case 0x4: status = FanDirectionStatus.FLOOR_DEFROST; break;
            default: break; 
        }
        return status; 
    }
}
