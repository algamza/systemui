package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateFanSpeedController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateFanSpeedController";
    private enum FanSpeedStatus { STEP_OFF, STEP_0, STEP_1, STEP_2, STEP_3, STEP_4, STEP_5, STEP_6, STEP_7, STEP_8 }
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
            mDataStore.setFanSpeed(speed);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanSpeed");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !mDataStore.shouldPropagateFanSpeedUpdate(mZone, e) ) return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        int speed = mDataStore.getFanSpeed(); 
        Log.d(TAG, "get="+speed); 
        return convertToStatus(speed).ordinal(); 
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
}
