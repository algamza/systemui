package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateFanDirectionController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateFanDirectionController";
    private enum FanDirectionStatus { 
        FACE(0) { int signal() { return 0x1; } }, 
        FLOOR_FACE(1) { int signal() { return 0x2; } }, 
        FLOOR(2) { int signal() { return 0x3; } },  
        FLOOR_DEFROST(3) { int signal() { return 0x4; } };
        private final int state; 
        FanDirectionStatus(int state) { this.state = state; }
        public int state() { return state; } 
        abstract int signal();
        static FanDirectionStatus getStateFromSignal(int signal) { 
            FanDirectionStatus status = FanDirectionStatus.FLOOR;
            switch(signal) {
                case 0x3: status = FLOOR; break;
                case 0x1: status = FACE; break;
                case 0x2: status = FLOOR_FACE; break; 
                case 0x4: status = FLOOR_DEFROST; break;
                default: break; 
            }
            return status; 
        }; 
        static boolean isValidFromSignal(int signal) {
            if ( signal >= 0x1 && signal <= 0x4 ) return true; 
            return false; 
        }
    }
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
            if ( FanDirectionStatus.isValidFromSignal(val) ) mDataStore.setFanDirection(val);
            Log.d(TAG, "fetch:mode="+val);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanDirection");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e);
        if ( !FanDirectionStatus.isValidFromSignal(e) ) return false; 
        if ( !mDataStore.shouldPropagateFanDirectionUpdate(e) ) 
            return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        FanDirectionStatus state = FanDirectionStatus.getStateFromSignal(mDataStore.getFanDirection()); 
        Log.d(TAG, "get="+state);
        return state.state(); 
    }

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;
        FanDirectionStatus status = FanDirectionStatus.values()[e]; 
        int val = status.signal(); 
        //if ( !mDataStore.shouldPropagateFanDirectionUpdate(val) ) return;
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
}
