package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimatePSSeatOptionController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimatePSSeatOptionController";
    private enum SeatStatus { 
        HEAT_ONLY_2STEP(0), HEAT_ONLY_3STEP(1), VENT_ONLY_2STEP(2), 
        VENT_ONLY_3STEP(3), HEAT_VENT_2STEP(4), HEAT_VENT_3STEP(5), INVALID(6);
        private final int state; 
        SeatStatus(int state) { this.state = state;}
        public int state() { return state; }  
    }
    final int mZone = ClimateControllerManager.SEAT_PASSENGER; 
    private SeatStatus mStatus = SeatStatus.INVALID; 

    public ClimatePSSeatOptionController(Context context, DataStore store) {
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
        return mStatus.state(); 
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
