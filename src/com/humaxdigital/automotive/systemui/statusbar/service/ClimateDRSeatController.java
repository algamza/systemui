package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;
import android.extension.car.util.VehicleUtils;

import android.support.car.CarNotConnectedException;

public class ClimateDRSeatController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateDRSeatController";
    enum SeatStatus { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
    final int mZone = ClimateControllerManager.SEAT_DRIVER; 

    public ClimateDRSeatController(Context context, DataStore store) {
        super(context, store);
    }
    
    @Override
    public void fetch(CarHvacManagerEx manager) {
        super.fetch(manager); 
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

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;
        SeatStatus status = SeatStatus.values()[e]; 
        int val = convertToVal(status); 
        //if ( !mDataStore.shouldPropagateFanDirectionUpdate(val) ) return;
        final AsyncTask<Integer, Void, Void> task = new AsyncTask<Integer, Void, Void>() {
            protected Void doInBackground(Integer... integers) {
                try {
                    Log.d(TAG, "set="+integers[0]); 
                    mManager.setIntProperty(CarHvacManagerEx.VENDOR_CANTX_VR_SEAT_HEAT_VENT, VehicleUtils.SEAT_ROW_1_LEFT, integers[0]);
                } catch (android.car.CarNotConnectedException err) {
                    Log.e(TAG, "Car not connected in setAcState");
                }
                return null;
            }
        }; 
        task.execute(val);
    }

    private int convertToVal(SeatStatus status) {
        int val = 0; 
        switch(status) {
            case HEATER3: val = 0x8; break; 
            case HEATER2: val = 0x7; break; 
            case HEATER1: val = 0x6; break;
            case NONE: val = 0x2; break; 
            case COOLER1: val = 0x3; break; 
            case COOLER2: val = 0x4; break; 
            case COOLER3: val = 0x5; break; 
            default: break; 
        }
        return val; 
    }

    private SeatStatus convertToStatus(int level) {
        SeatStatus status = SeatStatus.NONE; 
        
        switch(level) {
            case 0x6: status = SeatStatus.HEATER1; break; 
            case 0x7: status = SeatStatus.HEATER2; break;
            case 0x8: status = SeatStatus.HEATER3; break;
            case 0x2: status = SeatStatus.NONE; break; 
            case 0x3: status = SeatStatus.COOLER1; break;
            case 0x4: status = SeatStatus.COOLER2; break; 
            case 0x5: status = SeatStatus.COOLER3; break;
            default: break; 
        }
        return status;
    }
}
