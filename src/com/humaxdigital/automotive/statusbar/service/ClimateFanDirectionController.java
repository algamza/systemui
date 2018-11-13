package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.hardware.automotive.vehicle.V2_0.VehicleProperty;
import android.extension.car.CarHvacManagerEx;

import android.car.hardware.hvac.CarHvacManager;
import android.support.car.CarNotConnectedException;

public class ClimateFanDirectionController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateFanDirectionController";
    enum FanDirectionStatus { FLOOR, FACE, FLOOR_FACE, FLOOR_DEFROST }
    private final int[] AIRFLOW_STATES = new int[]{
        CarHvacManager.FAN_DIRECTION_FLOOR,
        CarHvacManager.FAN_DIRECTION_FACE,
        (CarHvacManager.FAN_DIRECTION_FACE | CarHvacManager.FAN_DIRECTION_FLOOR),
        (CarHvacManager.FAN_DIRECTION_FLOOR | CarHvacManager.FAN_DIRECTION_DEFROST)
    };
    private final int mZone = 0; 

    public ClimateFanDirectionController(Context context, 
        DataStore store, CarHvacManagerEx manager) {
        super(context, store, manager);
    }
    
    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return;
        //try {
            int val = 0; //mManager.getIntProperty(
                //VehicleProperty.VENDOR_CANRX_HVAC_MODE_DISPLAY, mZone);
            mDataStore.setAirflow(mZone, fanPositionToAirflowIndex(val));
        //} catch (android.car.CarNotConnectedException e) {
        //    Log.e(TAG, "Car not connected in fetchFanDirection");
        //}
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        int index = fanPositionToAirflowIndex(e);
        if ( !mDataStore.shouldPropagateFanPositionUpdate(mZone, index) ) 
            return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        return convertToStatus(mDataStore.getAirflow(mZone)).ordinal(); 
    }

    private int fanPositionToAirflowIndex(int fanPosition) {
        for (int i = 0; i < AIRFLOW_STATES.length; i++) {
            if (fanPosition == AIRFLOW_STATES[i]) {
                return i;
            }
        }
        Log.e(TAG, "Unknown fan position " + fanPosition + ". Returning default.");
        return 0;
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
