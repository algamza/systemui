package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;

import android.hardware.automotive.vehicle.V2_0.VehicleArea;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaWindow;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaSeat;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaDoor;

import android.extension.car.CarUSMManager;
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
        AIR_CONDITIONER,
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

    public static final int WINDOW_FRONT = 
        VehicleAreaWindow.FRONT_WINDSHIELD; 
    public static final int WINDOW_REAR = 
        VehicleAreaWindow.REAR_WINDSHIELD; 

    public static final int HVAC_ALL = 
        HVAC_LEFT | HVAC_RIGHT; 

    private Context mContext; 
    private DataStore mDataStore;
    private boolean mIsInitialized = false;

    private CarUSMManager mCarUSMManager; 
    private CarHvacManagerEx mCarHvacManagerEx;
    
    private ClimateListener mListener; 

    private ClimateAirCirculationController mAirCirculation; 
    private ClimateAirConditionerController mAirConditioner; 
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
        public void onAirConditionerChanged(boolean isOn);
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

    public void fetch(CarHvacManagerEx hvacMgr, CarUSMManager usmMgr) {
        if ( hvacMgr == null || usmMgr == null ) return;
        mCarHvacManagerEx = hvacMgr; 
        mCarUSMManager = usmMgr; 
        try {
            mCarHvacManagerEx.registerCallback(mHvacCallback);
            mCarUSMManager.registerCallback(mUSMCallback); 
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "CarHvacManagerEx is fail to register callback!", e);
        }
        
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                mDRTemp.fetchUSMManager(mCarUSMManager); 
                mPSTemp.fetchUSMManager(mCarUSMManager); 
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
            case AIR_CONDITIONER: return mAirConditioner; 
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
        mAirConditioner = new ClimateAirConditionerController(mContext, mDataStore); 
        mControllers.add(mAirConditioner); 
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
                        handleFanPositionUpdate(getValue(val));
                        break;
                    case CarHvacManagerEx.ID_ZONED_FAN_SPEED_SETPOINT:
                        handleFanSpeedUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F:
                        handleTempUpdate(id, areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C:
                        handleTempUpdate(id, areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT_STATUS:
                        handleSeatWarmerUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.ID_ZONED_AIR_RECIRCULATION_ON:
                        handleAirCirculationUpdate(getValue(val));
                        break;
                    case CarHvacManagerEx.ID_ZONED_AC_ON:
                        handleAirConditionerUpdate(getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_DEFOG:
                        handleDefogUpdate(areaId, getValue(val));
                        break;
                    default: break; 
                }
            }

            @Override
            public void onErrorEvent(final int propertyId, final int zone) {
                Log.w(TAG, "onErrorEvent() :  propertyId = 0x" 
                    + Integer.toHexString(propertyId) + ", zone = 0x" + Integer.toHexString(zone));
            }
        };

    private final CarUSMManager.CarUSMEventCallback mUSMCallback = 
        new CarUSMManager.CarUSMEventCallback() {
        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            int zones = value.getAreaId();
            switch (value.getPropertyId()) {
                case CarUSMManager.VENDOR_CANRX_USM_TEMPRATURE_UNIT: {
                    handleTempModeUpdate(getValue(value)); 
                    break; 
                }
            }
        }

        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
            Log.w(TAG, "onErrorEvent() :  propertyId = 0x" 
                + Integer.toHexString(propertyId) + ", zone = 0x" + Integer.toHexString(zone));
        }
    }; 
    
    private void handleTempUpdate(int id, int zone, int temp) {
        if ( mDRTemp == null || mPSTemp == null ) return; 

        if (zone == SEAT_DRIVER) {
            if ( (id == CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F) && 
                (mDRTemp.getCurrentTemperatureMode() == ClimateDRTempController.MODE.FAHRENHEIT) ) {
                    if ( mDRTemp.update(temp) && mListener != null ) 
                        mListener.onDriverTemperatureChanged(ClimateUtils.temperatureHexToFahrenheit(mDRTemp.get()));
                }
            else if ( (id == CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C) && 
                (mDRTemp.getCurrentTemperatureMode() == ClimateDRTempController.MODE.CELSIUS) ) {
                    if ( mDRTemp.update(temp) && mListener != null ) 
                        mListener.onDriverTemperatureChanged(ClimateUtils.temperatureHexToCelsius(mDRTemp.get()));
                } 
        } else {
            if ( (id == CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F) && 
                (mPSTemp.getCurrentTemperatureMode() == ClimatePSTempController.MODE.FAHRENHEIT) ) {
                    if ( mPSTemp.update(temp) && mListener != null ) 
                        mListener.onPassengerTemperatureChanged(ClimateUtils.temperatureHexToFahrenheit(mPSTemp.get()));
                }
            else if ( (id == CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C) && 
                (mPSTemp.getCurrentTemperatureMode() == ClimatePSTempController.MODE.CELSIUS) ) {
                    if ( mPSTemp.update(temp) && mListener != null ) 
                        mListener.onPassengerTemperatureChanged(ClimateUtils.temperatureHexToCelsius(mPSTemp.get()));
                } 
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

    private void handleAirConditionerUpdate(boolean airConditionderState) {
        if ( mAirConditioner == null ) return; 

        if ( mAirConditioner.update(airConditionderState) )
            if ( mListener != null ) 
                mListener.onAirConditionerChanged(mAirConditioner.get());
    }

    private void handleTempModeUpdate(int mode) {
        if ( mDRTemp == null || mPSTemp == null ) return; 

        if ( mDRTemp.updateMode(mode) && mPSTemp.updateMode(mode) ) {
            if ( mListener == null ) return; 
            if ( mDRTemp.getCurrentTemperatureMode() == ClimateDRTempController.MODE.CELSIUS ) 
                mListener.onDriverTemperatureChanged(ClimateUtils.temperatureHexToCelsius(mDRTemp.get()));
            else 
                mListener.onDriverTemperatureChanged(ClimateUtils.temperatureHexToFahrenheit(mDRTemp.get()));
            
            if ( mPSTemp.getCurrentTemperatureMode() == ClimatePSTempController.MODE.CELSIUS ) 
                mListener.onPassengerTemperatureChanged(ClimateUtils.temperatureHexToCelsius(mPSTemp.get()));
            else 
                mListener.onPassengerTemperatureChanged(ClimateUtils.temperatureHexToFahrenheit(mPSTemp.get()));
        }
    }

    private void handleFanSpeedUpdate(int zone, int speed) {
        if ( mFanSpeed == null ) return; 

        if ( mFanSpeed.update(speed) )
            if ( mListener != null ) 
                mListener.onFanSpeedStatusChanged(mFanSpeed.get());
    }

    private void handleFanPositionUpdate(int position) {
        if ( mFanDirection == null ) return; 
        
        if ( mFanDirection.update(position) ) {
            if ( mListener != null )
                mListener.onFanDirectionChanged(mFanDirection.get());
        }
                
    }

    private void handleDefogUpdate(int zone, int val) {
        if ( mFanDirection == null ) return; 

        if ( mFanDirection.updateDefog(zone, val) )
            if ( mListener != null ) 
                mListener.onFanDirectionChanged(mFanDirection.get());
    }
}
