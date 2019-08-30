package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateDRSeatOptionController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateDRSeatOptionController";
    enum SeatStatus { HEAT_ONLY_2STEP, HEAT_ONLY_3STEP, VENT_ONLY_2STEP, VENT_ONLY_3STEP, HEAT_VENT_2STEP, HEAT_VENT_3STEP, INVALID }
    final int mZone = ClimateControllerManager.SEAT_DRIVER; 
    private SeatStatus mStatus = SeatStatus.INVALID; 

    public ClimateDRSeatOptionController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
        if ( mManager == null ) return;
        try {
            int option = mManager.getIntProperty(CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT, mZone);
            mStatus = convertToStatus(option); 
            Log.d(TAG, "fetch:option="+option+", status="+mStatus); 
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchSeatWarmer");
        }
    }

    @Override
    public Boolean update(Integer e) {
        mStatus = convertToStatus(e); 
        Log.d(TAG, "update:option="+e+", status="+mStatus); 
        return true;
    }

    @Override
    public Integer get() {
        Log.d(TAG, "get="+mStatus); 
        return mStatus.ordinal(); 
    }

    private SeatStatus convertToStatus(int option) {
        SeatStatus status = SeatStatus.INVALID; 
        switch(option) {
            case 0x1: status = SeatStatus.HEAT_ONLY_2STEP; break; 
            case 0x2: status = SeatStatus.HEAT_ONLY_3STEP; break;
            case 0x3: status = SeatStatus.VENT_ONLY_2STEP; break;
            case 0x4: status = SeatStatus.VENT_ONLY_3STEP; break;
            case 0x5: status = SeatStatus.HEAT_VENT_2STEP; break; 
            case 0x6: status = SeatStatus.HEAT_VENT_3STEP; break;
            case 0x7: status = SeatStatus.INVALID; break; 
            default: break; 
        }
        return status;
    }
}
