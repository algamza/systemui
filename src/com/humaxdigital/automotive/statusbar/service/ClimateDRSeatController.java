package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;

import android.support.car.CarNotConnectedException;

public class ClimateDRSeatController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateDRSeatController";
    enum SeatStatus { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
    final int mZone = ClimateControllerManager.SEAT_DRIVER; 

    public ClimateDRSeatController(Context context, 
        DataStore store, CarHvacManagerEx manager) {
        super(context, store, manager);
    }
    
    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return;
        try {
            int level = mManager.getIntProperty(CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT_STATUS, mZone);
            Log.d(TAG, "fetch="+level); 
            mDataStore.setSeatWarmerLevel(mZone, level);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchSeatWarmer");
        }
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        Log.d(TAG, "update="+e); 
        if ( !mDataStore.shouldPropagateSeatWarmerLevelUpdate(mZone, e) ) 
            return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        int val = mDataStore.getSeatWarmerLevel(mZone); 
        Log.d(TAG, "get="+val); 
        return convertToStatus(val).ordinal(); 
    }

    private SeatStatus convertToStatus(int level) {
        SeatStatus status = SeatStatus.NONE; 
        
        switch(level) {
            case 0x6: status = SeatStatus.HEATER1; break; 
            case 0x7: status = SeatStatus.HEATER2; break;
            case 0x8: status = SeatStatus.HEATER3; break;
            case 0x3: status = SeatStatus.COOLER1; break;
            case 0x4: status = SeatStatus.COOLER2; break; 
            case 0x5: status = SeatStatus.COOLER3; break;
            default: break; 
        }
        return status;
    }
}
