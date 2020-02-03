package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateFanSpeedController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateFanSpeedController";
    private enum FanSpeedStatus { 
        STEP_OFF(0) { int signal() { return 0x0; } },   
        STEP_0(1) { int signal() { return 0x1; } },  
        STEP_1(2) { int signal() { return 0x2; } },  
        STEP_2(3) { int signal() { return 0x3; } },  
        STEP_3(4) { int signal() { return 0x4; } },  
        STEP_4(5) { int signal() { return 0x5; } },  
        STEP_5(6) { int signal() { return 0x6; } },  
        STEP_6(7) { int signal() { return 0x7; } },  
        STEP_7(8) { int signal() { return 0x8; } },  
        STEP_8(9) { int signal() { return 0x9; } }; 
        private final int state; 
        FanSpeedStatus(int state) { this.state = state;}
        public int state() { return state; } 
        abstract int signal();
        static FanSpeedStatus getStateFromSignal(int signal) { 
            FanSpeedStatus status = FanSpeedStatus.STEP_OFF; 
            switch(signal) {
                case 0x0: status = STEP_OFF; break;
                case 0x1: status = STEP_0; break;
                case 0x2: status = STEP_1; break;
                case 0x3: status = STEP_2; break;
                case 0x4: status = STEP_3; break;
                case 0x5: status = STEP_4; break;
                case 0x6: status = STEP_5; break;
                case 0x7: status = STEP_6; break;
                case 0x8: status = STEP_7; break;
                case 0x9: status = STEP_8; break;
                default: break;
            }
            return status;
        }; 
        static boolean isValidFromSignal(int signal) {
            if ( signal >= 0x0 && signal <= 0x9 ) return true; 
            else return false; 
        }
    }
    private final int mZone = ClimateControllerManager.HVAC_ALL; 

    public ClimateFanSpeedController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null || mDataStore == null ) return;
        try {
            int speed = mManager.getIntProperty(
                CarHvacManagerEx.ID_ZONED_FAN_SPEED_SETPOINT, mZone); 
            Log.d(TAG, "fetch="+speed); 
            if ( FanSpeedStatus.isValidFromSignal(speed) ) 
                mDataStore.setFanSpeed(speed);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanSpeed");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !FanSpeedStatus.isValidFromSignal(e) || 
            !mDataStore.shouldPropagateFanSpeedUpdate(mZone, e) ) return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        int speed = mDataStore.getFanSpeed(); 
        Log.d(TAG, "get="+speed); 
        return FanSpeedStatus.getStateFromSignal(speed).state(); 
    }

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;
        FanSpeedStatus status = FanSpeedStatus.values()[e];
        int val = status.signal();
        final AsyncTask<Integer, Void, Void> task = new AsyncTask<Integer, Void, Void>() {
            protected Void doInBackground(Integer... Integers) {
                try {
                    mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_HVAC_MAIN_BLOWER, 0, Integers[0]);
                    Log.d(TAG, "set="+Integers[0]);
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        };
        task.execute(val);
    }
}
