package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.extension.car.CarHvacManagerEx;
import android.extension.car.util.VehicleUtils;

import android.support.car.CarNotConnectedException;

public class ClimateDRSeatController extends ClimateBaseController<Integer> {
    private static final String TAG = "ClimateDRSeatController";
    enum SeatStatus { 
        HEATER3(0) { int signal() { return 0x8; } },  
        HEATER2(1) { int signal() { return 0x7; } },  
        HEATER1(2) { int signal() { return 0x6; } },  
        NONE(3) { int signal() { return 0x2; } },  
        COOLER1(4) { int signal() { return 0x3; } },  
        COOLER2(5) { int signal() { return 0x4; } },  
        COOLER3(6) { int signal() { return 0x5; } }; 
        private final int state; 
        SeatStatus(int state) { this.state = state; }
        public int state() { return state; }  
        abstract int signal();
        static SeatStatus getStateFromSignal(int signal) { 
            SeatStatus status = SeatStatus.NONE; 
            switch(signal) {
                case 0x6: status = HEATER1; break; 
                case 0x7: status = HEATER2; break;
                case 0x8: status = HEATER3; break;
                case 0x2: status = NONE; break; 
                case 0x3: status = COOLER1; break;
                case 0x4: status = COOLER2; break; 
                case 0x5: status = COOLER3; break;
                default: break; 
            }
            return status;
        }; 
    }
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
        return SeatStatus.getStateFromSignal(val).state(); 
    }

    @Override
    public void set(Integer e) {
        if ( mDataStore == null || mManager == null ) return;
        SeatStatus status = SeatStatus.values()[e]; 
        int val = status.signal(); 
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
}
