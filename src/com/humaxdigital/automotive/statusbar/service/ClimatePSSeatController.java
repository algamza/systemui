package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

import android.car.hardware.hvac.CarHvacManager;
import android.support.car.CarNotConnectedException;

public class ClimatePSSeatController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimatePSSeatController";
    enum SeatStatus { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
    final int mZone = ClimateControllerManager.PASSENGER_ZONE_ID; 

    public ClimatePSSeatController(Context context, 
        DataStore store, CarHvacManager manager) {
        super(context, store, manager);
    }
    
    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return;
        //try {
            // todo : crash ( ALM #2009952 ) 
            int level = 0;//mManager.getIntProperty(CarHvacManager.ID_ZONED_SEAT_TEMP, mZone);
            mDataStore.setSeatWarmerLevel(mZone, level);
        //} catch (android.car.CarNotConnectedException e) {
        //    Log.e(TAG, "Car not connected in fetchSeatWarmer");
        //}
    }

    @Override
    public Boolean update(Integer e) {
        if ( mDataStore == null ) return false;
        if ( !mDataStore.shouldPropagateSeatWarmerLevelUpdate(mZone, e) ) 
            return false;
        return true;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0;
        return convertToStatus(mDataStore.getSeatWarmerLevel(mZone)).ordinal(); 
    }

    private SeatStatus convertToStatus(int level) {
        SeatStatus status = SeatStatus.NONE; 
        // todo : match enum 
        switch(level) {
            case 0: status = SeatStatus.NONE; break;
            case 1: status = SeatStatus.HEATER1; break; 
            case 2: status = SeatStatus.HEATER2; break;
            case 3: status = SeatStatus.HEATER3; break;
            case 4: status = SeatStatus.COOLER1; break;
            case 5: status = SeatStatus.COOLER2; break; 
            case 6: status = SeatStatus.COOLER3; break;
            default: break; 
        }
        return status;
    }
}
