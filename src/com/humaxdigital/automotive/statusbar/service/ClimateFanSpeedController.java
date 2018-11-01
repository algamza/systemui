package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.car.hardware.hvac.CarHvacManager;
import android.support.car.CarNotConnectedException;

public class ClimateFanSpeedController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateFanSpeedController";
    private enum FanSpeedStatus { STEP_1, STEP_2, STEP_3, STEP_4, STEP_5, STEP_6, STEP_7, STEP_8 }
    private final int mZone = ClimateControllerManager.SEAT_ALL; 

    public ClimateFanSpeedController(Context context, 
        DataStore store, CarHvacManager manager) {
        super(context, store, manager);
    }
    
    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return;
        try {
            mDataStore.setFanSpeed(mManager.getIntProperty(
                    CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT, mZone));
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchFanSpeed");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        if ( !mDataStore.shouldPropagateFanSpeedUpdate(mZone, e) ) return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        return convertToStatus(mDataStore.getFanSpeed()).ordinal(); 
    }

    private FanSpeedStatus convertToStatus(int speed) {
        FanSpeedStatus status = FanSpeedStatus.STEP_1; 
        // todo : change speed to status
        switch(speed) {
            case 1: status = FanSpeedStatus.STEP_1; break;
            case 2: status = FanSpeedStatus.STEP_2; break;
            case 3: status = FanSpeedStatus.STEP_3; break;
            case 4: status = FanSpeedStatus.STEP_4; break;
            case 5: status = FanSpeedStatus.STEP_5; break;
            case 6: status = FanSpeedStatus.STEP_6; break;
            case 7: status = FanSpeedStatus.STEP_7; break;
            case 8: status = FanSpeedStatus.STEP_8; break;
            default: break;
        }
        return status;
    }
}
