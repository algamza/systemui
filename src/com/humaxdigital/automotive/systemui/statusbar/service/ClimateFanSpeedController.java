package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateFanSpeedController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateFanSpeedController";
    private enum FanSpeedStatus { 
        STEP_OFF(0), STEP_0(1), STEP_1(2), STEP_2(3), STEP_3(4), 
        STEP_4(5), STEP_5(6), STEP_6(7), STEP_7(8), STEP_8(9);
        private final int state; 
        FanSpeedStatus(int state) { this.state = state;}
        public int state() { return state; } 
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
            if ( checkInvalid(speed) ) mDataStore.setFanSpeed(speed);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanSpeed");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !checkInvalid(e) || 
            !mDataStore.shouldPropagateFanSpeedUpdate(mZone, e) ) return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        int speed = mDataStore.getFanSpeed(); 
        Log.d(TAG, "get="+speed); 
        return convertToStatus(speed).state(); 
    }

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;
        FanSpeedStatus status = FanSpeedStatus.values()[e];
        int val = convertToValue(status);
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

    private Boolean checkInvalid(int val) {
        if ( val >= 0x0 && val <= 0x9 ) return true; 
        else return false; 
    }

    private FanSpeedStatus convertToStatus(int speed) {
        FanSpeedStatus status = FanSpeedStatus.STEP_OFF; 
        switch(speed) {
            case 0x0: status = FanSpeedStatus.STEP_OFF; break;
            case 0x1: status = FanSpeedStatus.STEP_0; break;
            case 0x2: status = FanSpeedStatus.STEP_1; break;
            case 0x3: status = FanSpeedStatus.STEP_2; break;
            case 0x4: status = FanSpeedStatus.STEP_3; break;
            case 0x5: status = FanSpeedStatus.STEP_4; break;
            case 0x6: status = FanSpeedStatus.STEP_5; break;
            case 0x7: status = FanSpeedStatus.STEP_6; break;
            case 0x8: status = FanSpeedStatus.STEP_7; break;
            case 0x9: status = FanSpeedStatus.STEP_8; break;
            default: break;
        }
        return status;
    }

    private int convertToValue(FanSpeedStatus status) {
        int val = 0x0;
        switch(status) {
            case STEP_OFF: val = 0x0; break;
            case STEP_0  : val = 0x1; break;
            case STEP_1  : val = 0x2; break;
            case STEP_2  : val = 0x3; break;
            case STEP_3  : val = 0x4; break;
            case STEP_4  : val = 0x5; break;
            case STEP_5  : val = 0x6; break;
            case STEP_6  : val = 0x7; break;
            case STEP_7  : val = 0x8; break;
            case STEP_8  : val = 0x9; break;
            default: break;
        }
        return val;
    }
}
