package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;

import android.hardware.automotive.vehicle.V2_0.VehicleArea;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaWindow;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaSeat;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaDoor;

import android.car.hardware.CarVendorExtensionManager;
import android.car.hardware.CarPropertyValue;

import android.car.CarNotConnectedException;
import android.car.hardware.hvac.CarHvacManager;

import android.extension.car.CarHvacManagerEx;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ClimateControllerManager {
    private static final String TAG = "ClimateControllerManager";

    public enum ControllerType {
        AIR_CIRCULATION, 
        DRIVER_SEAT,
        DRIVER_TEMPERATURE,
        PASSENGER_SEAT,
        PASSENGER_TEMPERATURE,
        FAN_SPEED,
        FAN_DIRECTION
    }

    public static final int SEAT_DRIVER = 
        VehicleAreaSeat.ROW_1_LEFT;

    public static final int SEAT_PASSENGER = 
        VehicleAreaSeat.ROW_1_RIGHT;
    
    public static final int HVAC_LEFT = 
        VehicleAreaSeat.ROW_1_LEFT |
        VehicleAreaSeat.ROW_2_LEFT |
        VehicleAreaSeat.ROW_2_CENTER;

    public static final int HVAC_RIGHT = 
        VehicleAreaSeat.ROW_1_RIGHT |
        VehicleAreaSeat.ROW_2_RIGHT; 

    public static final int HVAC_ALL = 
        HVAC_LEFT | HVAC_RIGHT; 

    private Context mContext; 
    private DataStore mDataStore;
    private boolean mIsInitialized = false;

    //private CarEx mCarApi;
    private CarHvacManagerEx mCarHvacManagerEx;
    
    private ClimateListener mListener; 

    private ClimateAirCirculationController mAirCirculation; 
    private ClimateDRSeatController mDRSeat; 
    private ClimateDRTempController mDRTemp; 
    private ClimateFanDirectionController mFanDirection; 
    private ClimateFanSpeedController mFanSpeed; 
    private ClimatePSSeatController mPSSeat; 
    private ClimatePSTempController mPSTemp; 

    private List<ClimateBaseController> mControllers = new ArrayList<>(); 

    public interface ClimateListener {
        public void onInitialized();
        public void onDriverTemperatureChanged(float temp);
        public void onDriverSeatStatusChanged(int status);
        public void onAirCirculationChanged(boolean isOn);
        public void onFanDirectionChanged(int direction);
        public void onFanSpeedStatusChanged(int status);
        public void onPassengerSeatStatusChanged(int status);
        public void onPassengerTemperatureChanged(float temp);
    }
    
    @SuppressWarnings("unchecked")
    public static <E> E getValue(CarPropertyValue propertyValue) {
        return (E) propertyValue.getValue();
    }

    public ClimateControllerManager(Context context, DataStore store) {
        if ( context == null || store == null ) return;
        mContext = context; 
        mDataStore = store; 

        createControllers(); 
    }

    public ClimateControllerManager registerListener(ClimateListener listener) {
        mListener = listener; 
        return this; 
    }

    public void unRegisterListener() {
        mListener = null; 
    }

    public void fetch(CarHvacManagerEx manager) {
        if ( manager == null ) return;
        mCarHvacManagerEx = manager; 
        try {
            mCarHvacManagerEx.registerCallback(mHvacCallback);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "CarHvacManagerEx is fail to register callback!", e);
        }
        
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                for ( ClimateBaseController controller : mControllers ) 
                    controller.fetch(mCarHvacManagerEx); 
                return null; 
            }

            @Override
            protected void onPostExecute(Void unused) {
                if ( mListener != null ) mListener.onInitialized();
                mIsInitialized = true;
            }
        };
        task.execute(); 
    }

    public ClimateBaseController getController(ControllerType type) {
        switch(type) {
            case AIR_CIRCULATION: return mAirCirculation;  
            case DRIVER_SEAT: return mDRSeat; 
            case DRIVER_TEMPERATURE: return mDRTemp; 
            case PASSENGER_SEAT: return mPSSeat; 
            case PASSENGER_TEMPERATURE: return mPSTemp; 
            case FAN_SPEED: return mFanSpeed; 
            case FAN_DIRECTION: return mFanDirection; 
        }
        return null; 
    }

    public boolean isInitialized() {
        return mIsInitialized; 
    }

    private void createControllers() {
        mAirCirculation = new ClimateAirCirculationController(mContext, mDataStore); 
        mControllers.add(mAirCirculation); 
        mDRSeat = new ClimateDRSeatController(mContext, mDataStore); 
        mControllers.add(mDRSeat); 
        mDRTemp = new ClimateDRTempController(mContext, mDataStore); 
        mControllers.add(mDRTemp); 
        mFanDirection = new ClimateFanDirectionController(mContext, mDataStore); 
        mControllers.add(mFanDirection); 
        mFanSpeed = new ClimateFanSpeedController(mContext, mDataStore); 
        mControllers.add(mFanSpeed); 
        mPSSeat = new ClimatePSSeatController(mContext, mDataStore); 
        mControllers.add(mPSSeat); 
        mPSTemp = new ClimatePSTempController(mContext, mDataStore); 
        mControllers.add(mPSTemp); 
    }

    private final CarHvacManager.CarHvacEventCallback mHvacCallback =
        new CarHvacManager.CarHvacEventCallback () {
            @Override
            public void onChangeEvent(final CarPropertyValue val) {
                int id = val.getPropertyId(); 
                int areaId = val.getAreaId();
                Log.d(TAG, "HVAC event, id: " + id + ", area: " + areaId);
                switch (id) {
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_MODE_DISPLAY:
                        handleFanPositionUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.ID_ZONED_FAN_SPEED_SETPOINT:
                        handleFanSpeedUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F:
                        handleTempUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT_STATUS:
                        handleSeatWarmerUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.ID_ZONED_AIR_RECIRCULATION_ON:
                        handleAirCirculationUpdate(getValue(val));
                        break;
                    default:
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Unhandled HVAC event, id: " + id);
                        }
                }
            }

            @Override
            public void onErrorEvent(final int propertyId, final int zone) {
                Log.w(TAG, "onErrorEvent() :  propertyId = 0x" 
                    + Integer.toHexString(propertyId) + ", zone = 0x" + Integer.toHexString(zone));
            }
        };
    
    private void handleTempUpdate(int zone, int temp) {
        if ( mDRTemp == null || mPSTemp == null ) return; 

        if (zone == SEAT_DRIVER) {
            if ( mDRTemp.update(tempHexToPhy(temp)) ) 
                if ( mListener != null ) 
                    mListener.onDriverTemperatureChanged(mDRTemp.get());
        } else {
            if ( mPSTemp.update(tempHexToPhy(temp)) ) 
                if ( mListener != null ) 
                    mListener.onPassengerTemperatureChanged(mPSTemp.get());
        }
    }

    private void handleSeatWarmerUpdate(int zone, int level) {
        if ( mDRSeat == null || mPSSeat == null ) return; 
        
        if (zone == SEAT_DRIVER) {
            if ( mDRSeat.update(level) ) 
                if ( mListener != null ) 
                    mListener.onDriverSeatStatusChanged(mDRSeat.get());
        } else {
            if ( mPSSeat.update(level) ) 
                if ( mListener != null ) 
                    mListener.onPassengerSeatStatusChanged(mPSSeat.get());
        }
    }

    private void handleAirCirculationUpdate(boolean airCirculationState) {
        if ( mAirCirculation == null ) return; 

        if ( mAirCirculation.update(airCirculationState) )
            if ( mListener != null ) 
                mListener.onAirCirculationChanged(mAirCirculation.get());
    }

    private void handleFanSpeedUpdate(int zone, int speed) {
        if ( mFanSpeed == null ) return; 

        if ( mFanSpeed.update(speed) )
            if ( mListener != null ) 
                mListener.onFanSpeedStatusChanged(mFanSpeed.get());
    }

    private void handleFanPositionUpdate(int zone, int position) {
        if ( mFanDirection == null ) return; 

        if ( mFanDirection.update(position) )
            if ( mListener != null ) 
                mListener.onFanDirectionChanged(mFanDirection.get());
    }

    static public float tempHexToPhy(int hex) {
        if ( hex < 0x01 || 
            hex == 0xfe || 
            hex == 0xff || 
            hex > 0x24 ) return 0.0f; 
        float phy = hex*0.5f + 14; 
        return phy; 
    }
}
