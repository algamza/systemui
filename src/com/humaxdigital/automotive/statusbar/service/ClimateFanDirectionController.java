package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateFanDirectionController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateFanDirectionController";
    private enum FanDirectionStatus { FACE, FLOOR, FLOOR_FACE, FLOOR_DEFROST, DEFROST }
    private final int mZone = 0; 
    private final int mZoneFrontDef = ClimateControllerManager.WINDOW_FRONT; 
    private final int mZoneRearDef = ClimateControllerManager.WINDOW_REAR; 

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
            int front_def = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_DEFOG, mZoneFrontDef);
            int rear_def = mManager.getIntProperty(
                    CarHvacManagerEx.VENDOR_CANRX_HVAC_DEFOG, mZoneRearDef);
            if ( checkInvalid(val) ) mDataStore.setFanDirection(val);
            mDataStore.setDefrosterState(mZoneFrontDef, front_def==0x1?true:false);
            mDataStore.setDefrosterState(mZoneRearDef, rear_def==0x1?true:false);
            Log.d(TAG, "fetch:mode="+val+", front="+front_def+", rear="+rear_def);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanDirection");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e);
        if ( !checkInvalid(e) || !mDataStore.shouldPropagateFanDirectionUpdate(e) ) 
            return false;
        return true;
    }

    public Boolean updateDefog(int zone, int val) {
        if ( mDataStore == null ) return false; 
        if ( !mDataStore.shouldPropagateDefrosterUpdate(zone, val==0x1?true:false) ) 
            return false; 
        if ( !mDataStore.getDefrosterState(mZoneFrontDef) ) {
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
            task.execute(mDataStore.getFanDirection());
        }
        return true;  
    }

    @Override
    public Integer get() {
        int val = 0; 
        if ( mDataStore == null ) return val;
        if ( mDataStore.getDefrosterState(mZoneFrontDef) ) 
            val = FanDirectionStatus.DEFROST.ordinal(); 
        else 
            val = convertToStatus(mDataStore.getFanDirection()).ordinal(); 
        Log.d(TAG, "get="+val);
        return val; 
    }

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;
        FanDirectionStatus status = FanDirectionStatus.values()[e]; 
        if ( !mDataStore.shouldPropagateFanDirectionUpdate(convertToValue(status)) ) return;

        if ( mDataStore.getDefrosterState(mZoneFrontDef) ) {
            final AsyncTask<Void, Void, Void> defogtask = new AsyncTask<Void, Void, Void>() {
                protected Void doInBackground(Void... voids) {
                    try {
                        Log.d(TAG, "set defog="+0); 
                        mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_HVAC_DEFOG, mZoneFrontDef, 0x0);
                    } catch (android.car.CarNotConnectedException err) {
                        Log.e(TAG, "Car not connected in setAcState");
                    }
                    return null;
                }
            }; 
            defogtask.execute();
        }
        
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
        task.execute(convertToValue(status));
    }

    public Boolean isFrontDefogOn() {
        return mDataStore.getDefrosterState(mZoneFrontDef); 
    }

    private Boolean checkInvalid(int val) {
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
