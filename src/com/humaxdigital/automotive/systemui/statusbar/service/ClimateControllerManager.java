package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.AsyncTask;

import android.hardware.automotive.vehicle.V2_0.VehicleArea;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaWindow;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaSeat;
import android.hardware.automotive.vehicle.V2_0.VehicleAreaDoor;

import android.car.hardware.CarVendorExtensionManager;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.CarSensorEvent;

import android.car.CarNotConnectedException;
import android.car.hardware.hvac.CarHvacManager;

import android.extension.car.value.CarSensorEventEx;
import android.extension.car.CarHvacManagerEx;
import android.extension.car.CarUSMManager;
import android.extension.car.CarSensorManagerEx;
import android.extension.car.CarPropertyFilter;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; 

import java.util.Timer;
import java.util.TimerTask;

public class ClimateControllerManager {
    private static final String TAG = "ClimateControllerManager";

    public enum ControllerType {
        AIR_CIRCULATION, 
        AIR_CONDITIONER,
        AIR_CLEANING,
        SYNC,
        DRIVER_SEAT,
        DRIVER_SEAT_OPTION,
        DRIVER_TEMPERATURE,
        PASSENGER_SEAT,
        PASSENGER_SEAT_OPTION,
        PASSENGER_TEMPERATURE,
        FAN_SPEED,
        FAN_DIRECTION,
        DEFOG, 
        MODE_OFF
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
    private CarSensorManagerEx mCarSensorManagerEx;
    
    private ClimateListener mListener; 

    private ClimateAirCirculationController mAirCirculation; 
    private ClimateAirConditionerController mAirConditioner; 
    private ClimateAirCleaningController mAirCleaning; 
    private ClimateSyncController mSync; 
    private ClimateDRSeatController mDRSeat; 
    private ClimateDRSeatOptionController mDRSeatOption; 
    private ClimateDRTempController mDRTemp; 
    private ClimateFanDirectionController mFanDirection; 
    private ClimateModeOffController mModeOff; 
    private ClimateFanSpeedController mFanSpeed; 
    private ClimatePSSeatController mPSSeat; 
    private ClimatePSSeatOptionController mPSSeatOption; 
    private ClimatePSTempController mPSTemp; 
    private ClimateDefogController mDefog; 

    private List<ClimateBaseController> mControllers = new ArrayList<>(); 
    private boolean mIGNOn = true; 
    private boolean mOperateStateOn = false; 
    private Timer mTimer = new Timer(); 
    private TimerTask mChatteringTask = null; 
    private boolean mChatteringWrongSignal = false; 
    private final int CHATTERING_WRONG_SIGNAL = 1000; 

    public interface ClimateListener {
        public void onInitialized();
        public void onDriverTemperatureChanged(float temp);
        public void onDriverSeatStatusChanged(int status);
        public void onDriverSeatOptionChanged(int option);
        public void onAirCirculationChanged(boolean isOn);
        public void onAirConditionerChanged(boolean isOn);
        public void onSyncChanged(boolean sync);
        public void onAirCleaningChanged(int status);
        public void onFanDirectionChanged(int direction);
        public void onFanSpeedStatusChanged(int status);
        public void onPassengerSeatStatusChanged(int status);
        public void onPassengerSeatOptionChanged(int option);
        public void onPassengerTemperatureChanged(float temp);
        public void onFrontDefogStatusChanged(int status); 
        public void onModeOffChanged(boolean on);
        public void onIGNOnChanged(boolean on);
        public void onOperateOnChanged(boolean on);
    }
    
    @SuppressWarnings("unchecked")
    public static <E> E getValue(CarPropertyValue propertyValue) {
        return (E) propertyValue.getValue();
    }

    public ClimateControllerManager(Context context, DataStore store) {
        mContext = Objects.requireNonNull(context); 
        mDataStore = Objects.requireNonNull(store); 

        createControllers(); 
    }

    public ClimateControllerManager registerListener(ClimateListener listener) {
        mListener = Objects.requireNonNull(listener); 
        return this; 
    }

    public void unRegisterListener() {
        mListener = null; 
        try {
            if ( mCarUSMManager != null ) 
                mCarUSMManager.unregisterCallback(mUSMCallback);
            if ( mCarHvacManagerEx != null ) 
                mCarHvacManagerEx.unregisterCallback(mHvacCallback);
            if ( mCarSensorManagerEx != null ) 
                mCarSensorManagerEx.unregisterListener(mSensorChangeListener);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "unRegisterListener is fail !", e);
        }
    }

    public void fetch(CarHvacManagerEx hvacMgr, CarUSMManager usmMgr, CarSensorManagerEx sensorMgr) {
        if ( hvacMgr == null || usmMgr == null || sensorMgr == null ) return;
        mCarHvacManagerEx = hvacMgr; 
        mCarUSMManager = usmMgr; 
        mCarSensorManagerEx = sensorMgr;
        try {
            CarPropertyFilter hvac_filter = new CarPropertyFilter();
            hvac_filter.addId(CarHvacManagerEx.VENDOR_CANRX_HVAC_MODE_DISPLAY); 
            hvac_filter.addId(CarHvacManagerEx.ID_ZONED_FAN_SPEED_SETPOINT);
            hvac_filter.addId(CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F);
            hvac_filter.addId(CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C);
            hvac_filter.addId(CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT_STATUS);
            hvac_filter.addId(CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT);
            hvac_filter.addId(CarHvacManagerEx.ID_ZONED_AIR_RECIRCULATION_ON);
            hvac_filter.addId(CarHvacManagerEx.ID_ZONED_AC_ON);
            hvac_filter.addId(CarHvacManagerEx.VENDOR_CANRX_HVAC_DISPLAY_SYNC);
            hvac_filter.addId(CarHvacManagerEx.VENDOR_CANRX_HVAC_AIR_CLEANING_STATUS);
            hvac_filter.addId(CarHvacManagerEx.VENDOR_CANRX_HVAC_OPERATE_STATUS);
            mCarHvacManagerEx.registerCallback(mHvacCallback, hvac_filter);
            CarPropertyFilter usm_filter = new CarPropertyFilter();
            usm_filter.addId(CarUSMManager.VENDOR_CANRX_USM_TEMPRATURE_UNIT); 
            mCarUSMManager.registerCallback(mUSMCallback, usm_filter); 
            mCarSensorManagerEx.registerListener(
                mSensorChangeListener, 
                CarSensorManagerEx.SENSOR_TYPE_IGNITION_STATE, 
                CarSensorManagerEx.SENSOR_RATE_NORMAL);
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
                try {
                    if ( mCarSensorManagerEx != null ) {
                        CarSensorEvent event = mCarSensorManagerEx.getLatestSensorEvent(
                                CarSensorManagerEx.SENSOR_TYPE_IGNITION_STATE);
                        int state = event.intValues[0]; 
                        Log.d(TAG, "fetch ign="+state);
                        if ( state == CarSensorEvent.IGNITION_STATE_LOCK 
                            || state == CarSensorEvent.IGNITION_STATE_OFF
                            || state == CarSensorEvent.IGNITION_STATE_ACC ) {
                            updateIGNStatus(false);
                        } else if ( state == CarSensorEvent.IGNITION_STATE_ON
                            || state == CarSensorEvent.IGNITION_STATE_START ) {
                            updateIGNStatus(true);
                        }
                    }

                    if ( mCarHvacManagerEx != null ) {
                        int state = mCarHvacManagerEx.getIntProperty(
                            CarHvacManagerEx.VENDOR_CANRX_HVAC_OPERATE_STATUS, 0); 
                        Log.d(TAG, "fetch operate="+state);
                        if ( state == 0x3 ) 
                            mOperateStateOn = true;
                        else if ( state == 0x1 || state == 0x0 || state == 0x2 ) 
                            mOperateStateOn = false;
                    }
                } catch ( CarNotConnectedException e ) {
                    Log.e(TAG, "getLatestSensorEvent is fail : ", e);
                }

                if ( mListener != null ) mListener.onInitialized();
                mIsInitialized = true;

                return null; 
            }

            @Override
            protected void onPostExecute(Void unused) {
            }
        };
        task.execute(); 
    }

    private void chatteringWrongSignal() {
        Log.d(TAG, "wrong signal chattering start");
        if ( mChatteringTask != null ) {
            mChatteringWrongSignal = false; 
            mChatteringTask.cancel(); 
            mTimer.purge(); 
            mChatteringTask = null; 
        }
        mChatteringWrongSignal = true; 
        mChatteringTask = new TimerTask(){
            @Override
            public void run() {
                Log.d(TAG, "wrong signal chattering end"); 
                mChatteringWrongSignal = false; 
                updateControllers(); 
                if ( mListener != null ) 
                    mListener.onIGNOnChanged(mIGNOn);
            }
        };

        mTimer.schedule(mChatteringTask, CHATTERING_WRONG_SIGNAL);
    }

    private void updateControllers() {
        if ( mListener == null ) return; 
        if ( mAirCirculation != null ) {
            mAirCirculation.update(); 
            mListener.onAirCirculationChanged(mAirCirculation.get());
        }
        if ( mAirConditioner != null ) {
            mAirConditioner.update(); 
            mListener.onAirConditionerChanged(mAirConditioner.get());
        }
        if ( mFanSpeed != null ) {
            mFanSpeed.update(); 
            mListener.onFanSpeedStatusChanged(mFanSpeed.get());
        }
    }

    public int getIGNStatus() {
        return mIGNOn?1:0; 
    }

    private void updateIGNStatus(boolean on) {
        Log.d(TAG, "updateIGNStatus="+on);
        mIGNOn = on; 
        
        if ( on ) {
            chatteringWrongSignal(); 
        } else {
            if ( mListener != null ) 
                mListener.onIGNOnChanged(mIGNOn);
        }
    }

    public boolean isOperateOn() {
        Log.d(TAG, "isOperateOn = "+mOperateStateOn);
        return mOperateStateOn;
    }

    private final CarSensorManagerEx.OnSensorChangedListenerEx mSensorChangeListener = 
        new CarSensorManagerEx.OnSensorChangedListenerEx() {
        @Override
        public void onSensorChanged(final CarSensorEvent event) {
            switch (event.sensorType) {
                case CarSensorManagerEx.SENSOR_TYPE_IGNITION_STATE: {
                    if ( mListener == null ) break;
                    int state = event.intValues[0];
                    Log.d(TAG, "onSensorChanged="+state);

                    if ( state == CarSensorEvent.IGNITION_STATE_LOCK 
                        || state == CarSensorEvent.IGNITION_STATE_OFF
                        || state == CarSensorEvent.IGNITION_STATE_ACC ) {
                        if ( mIGNOn ) updateIGNStatus(false);
                        
                    } else if ( state == CarSensorEvent.IGNITION_STATE_ON
                        || state == CarSensorEvent.IGNITION_STATE_START ) {
                        if ( !mIGNOn ) updateIGNStatus(true);
                    }
                    break;
                }  
            }
        }

        public void onSensorChanged(final CarSensorEventEx event) {
        }
    };

    public ClimateBaseController getController(ControllerType type) {
        switch(type) {
            case AIR_CIRCULATION: return mAirCirculation;  
            case AIR_CONDITIONER: return mAirConditioner; 
            case AIR_CLEANING: return mAirCleaning; 
            case SYNC: return mSync; 
            case DRIVER_SEAT: return mDRSeat; 
            case DRIVER_SEAT_OPTION: return mDRSeatOption; 
            case DRIVER_TEMPERATURE: return mDRTemp; 
            case PASSENGER_SEAT: return mPSSeat; 
            case PASSENGER_SEAT_OPTION: return mPSSeatOption; 
            case PASSENGER_TEMPERATURE: return mPSTemp; 
            case FAN_SPEED: return mFanSpeed; 
            case FAN_DIRECTION: return mFanDirection; 
            case DEFOG: return mDefog; 
            case MODE_OFF: return mModeOff; 
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
        mAirCleaning = new ClimateAirCleaningController(mContext, mDataStore); 
        mControllers.add(mAirCleaning); 
        mSync = new ClimateSyncController(mContext, mDataStore); 
        mControllers.add(mSync); 
        mDRSeat = new ClimateDRSeatController(mContext, mDataStore); 
        mControllers.add(mDRSeat); 
        mDRSeatOption = new ClimateDRSeatOptionController(mContext, mDataStore); 
        mControllers.add(mDRSeatOption); 
        mDRTemp = new ClimateDRTempController(mContext, mDataStore); 
        mControllers.add(mDRTemp); 
        mFanDirection = new ClimateFanDirectionController(mContext, mDataStore); 
        mControllers.add(mFanDirection); 
        mFanSpeed = new ClimateFanSpeedController(mContext, mDataStore); 
        mControllers.add(mFanSpeed); 
        mPSSeat = new ClimatePSSeatController(mContext, mDataStore); 
        mControllers.add(mPSSeat); 
        mPSSeatOption = new ClimatePSSeatOptionController(mContext, mDataStore); 
        mControllers.add(mPSSeatOption); 
        mPSTemp = new ClimatePSTempController(mContext, mDataStore); 
        mControllers.add(mPSTemp); 
        mDefog = new ClimateDefogController(mContext, mDataStore); 
        mControllers.add(mDefog); 
        mModeOff = new ClimateModeOffController(mContext, mDataStore); 
        mControllers.add(mModeOff); 
    }

    private boolean isWrongSingnal(int id, CarPropertyValue val) {
        if ( !mChatteringWrongSignal ) return false; 
        switch (id) {
            case CarHvacManagerEx.VENDOR_CANRX_HVAC_MODE_DISPLAY:
            case CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F:
            case CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C:
            case CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT_STATUS:
            case CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT:
            case CarHvacManagerEx.VENDOR_CANRX_HVAC_OPERATE_STATUS: {
                if ( (int)getValue(val) == 0 ) return true;
                break; 
            }
            case CarHvacManagerEx.ID_ZONED_AIR_RECIRCULATION_ON: 
            case CarHvacManagerEx.ID_ZONED_AC_ON:
            case CarHvacManagerEx.ID_ZONED_FAN_SPEED_SETPOINT: {
                return true; 
            }
            default: break; 
        }
        return false;
    }

    private final CarHvacManager.CarHvacEventCallback mHvacCallback =
        new CarHvacManager.CarHvacEventCallback () {
            @Override
            public void onChangeEvent(final CarPropertyValue val) {
                int id = val.getPropertyId(); 
                int areaId = val.getAreaId();
                Log.d(TAG, "HVAC event, id: " + id + ", area: " + areaId);
                if ( isWrongSingnal(id, val) ) {
                    Log.d(TAG, "Wrong Signal !! "); 
                    return;
                }
                switch (id) {
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_MODE_DISPLAY:
                        handleFanPositionUpdate(getValue(val));
                        handleDefogUpdate(getValue(val));
                        handleModeOffUpdate(getValue(val)); 
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
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_SEAT_HEAT:
                        handleSeatOptionUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManagerEx.ID_ZONED_AIR_RECIRCULATION_ON:
                        handleAirCirculationUpdate(getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_DISPLAY_SYNC:
                        handleSyncUpdate(getValue(val));
                        break;
                    case CarHvacManagerEx.ID_ZONED_AC_ON:
                        handleAirConditionerUpdate(getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_AIR_CLEANING_STATUS:
                        handleAirCleaningUpdate(getValue(val));
                        break;
                    case CarHvacManagerEx.VENDOR_CANRX_HVAC_OPERATE_STATUS:
                        handleOperateState(getValue(val));
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
                    if ( mDRTemp.update(temp) && mListener != null ) {
                        if ( ClimateUtils.isValidTemperatureHex(mDRTemp.get()) ) 
                            mListener.onDriverTemperatureChanged(ClimateUtils.temperatureHexToFahrenheit(mDRTemp.get()));
                    } 
                }
            else if ( (id == CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C) && 
                (mDRTemp.getCurrentTemperatureMode() == ClimateDRTempController.MODE.CELSIUS) ) {
                    if ( mDRTemp.update(temp) && mListener != null ) {
                        if ( ClimateUtils.isValidTemperatureHex(mDRTemp.get()) ) 
                            mListener.onDriverTemperatureChanged(ClimateUtils.temperatureHexToCelsius(mDRTemp.get()));
                    }
                       
                } 
        } else {
            if ( (id == CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_F) && 
                (mPSTemp.getCurrentTemperatureMode() == ClimatePSTempController.MODE.FAHRENHEIT) ) {
                    if ( mPSTemp.update(temp) && mListener != null ) {
                        if ( ClimateUtils.isValidTemperatureHex(mPSTemp.get()) ) 
                            mListener.onPassengerTemperatureChanged(ClimateUtils.temperatureHexToFahrenheit(mPSTemp.get()));
                    }
                }
            else if ( (id == CarHvacManagerEx.VENDOR_CANRX_HVAC_TEMPERATURE_C) && 
                (mPSTemp.getCurrentTemperatureMode() == ClimatePSTempController.MODE.CELSIUS) ) {
                    if ( mPSTemp.update(temp) && mListener != null ) {
                        if ( ClimateUtils.isValidTemperatureHex(mPSTemp.get()) ) 
                            mListener.onPassengerTemperatureChanged(ClimateUtils.temperatureHexToCelsius(mPSTemp.get()));
                    }
                } 
        }
    }

    private void handleSeatWarmerUpdate(int zone, int level) {
        if ( mDRSeat == null || mPSSeat == null ) return; 
        
        if (zone == SEAT_DRIVER) {
            if ( mDRSeat.update(level) ) 
                if ( mListener != null ) 
                    mListener.onDriverSeatStatusChanged(mDRSeat.get());
        } else if ( zone == SEAT_PASSENGER ) {
            if ( mPSSeat.update(level) ) 
                if ( mListener != null ) 
                    mListener.onPassengerSeatStatusChanged(mPSSeat.get());
        }
    }

    private void handleSeatOptionUpdate(int zone, int option) {
        if ( mDRSeatOption == null || mPSSeatOption == null ) return; 
        
        if (zone == SEAT_DRIVER) {
            if ( mDRSeatOption.update(option) ) 
                if ( mListener != null ) 
                    mListener.onDriverSeatOptionChanged(mDRSeatOption.get());
        } else if ( zone == SEAT_PASSENGER ) {
            if ( mPSSeatOption.update(option) ) 
                if ( mListener != null ) 
                    mListener.onPassengerSeatOptionChanged(mPSSeatOption.get());
        }
    }

    private void handleAirCirculationUpdate(boolean airCirculationState) {
        if ( mAirCirculation == null ) return; 

        if ( mAirCirculation.update(airCirculationState) )
            if ( mListener != null ) 
                mListener.onAirCirculationChanged(mAirCirculation.get());
    }

    private void handleSyncUpdate(int sync) {
        if ( mSync == null ) return; 
        if ( mSync.checkValid(sync) ) {
            if ( mSync.update(mSync.checkOn(sync)) )
            if ( mListener != null ) 
                mListener.onSyncChanged(mSync.get());
        }
    }

    private void handleAirConditionerUpdate(boolean airConditionderState) {
        if ( mAirConditioner == null ) return; 

        if ( mAirConditioner.update(airConditionderState) )
            if ( mListener != null ) 
                mListener.onAirConditionerChanged(mAirConditioner.get());
    }

    private void handleAirCleaningUpdate(int airCleaningState) {
        if ( mAirCleaning == null ) return; 

        if ( mAirCleaning.update(airCleaningState) )
            if ( mListener != null ) 
                mListener.onAirCleaningChanged(mAirCleaning.get());
    }

    private void handleTempModeUpdate(int mode) {
        if ( mDRTemp == null || mPSTemp == null ) return; 

        if ( mDRTemp.updateMode(mode) && mPSTemp.updateMode(mode) ) {
            if ( mListener == null ) return; 
            if ( mDRTemp.getCurrentTemperatureMode() == ClimateDRTempController.MODE.CELSIUS ) {
                if ( ClimateUtils.isValidTemperatureHex(mDRTemp.get()) ) 
                    mListener.onDriverTemperatureChanged(ClimateUtils.temperatureHexToCelsius(mDRTemp.get()));
            }
            else {
                if ( ClimateUtils.isValidTemperatureHex(mDRTemp.get()) ) 
                    mListener.onDriverTemperatureChanged(ClimateUtils.temperatureHexToFahrenheit(mDRTemp.get()));
            }
                
            if ( mPSTemp.getCurrentTemperatureMode() == ClimatePSTempController.MODE.CELSIUS ) {
                if ( ClimateUtils.isValidTemperatureHex(mPSTemp.get()) ) 
                    mListener.onPassengerTemperatureChanged(ClimateUtils.temperatureHexToCelsius(mPSTemp.get()));
            }
            else {
                if ( ClimateUtils.isValidTemperatureHex(mPSTemp.get()) ) 
                    mListener.onPassengerTemperatureChanged(ClimateUtils.temperatureHexToFahrenheit(mPSTemp.get()));
            }
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

    private void handleDefogUpdate(int val) {
        if ( mDefog == null ) return; 

        if ( mDefog.update(val) )
            if ( mListener != null ) {
                mListener.onFrontDefogStatusChanged(mDefog.get());
            } 
    }

    private void handleModeOffUpdate(int val) {
        if ( mModeOff == null ) return; 
        
        if ( mModeOff.update(mModeOff.convertUpdateValue(val)) ) {
            if ( mListener != null )
                mListener.onModeOffChanged(mModeOff.get());
        }
    }

    private void handleOperateState(int val) {
        boolean operateOn = false; 
        switch(val) {
            case 0x3: operateOn = true; break;
            case 0x0:
            case 0x1:
            case 0x2: operateOn = false; break;
        }
        Log.d(TAG, "operate state = "+val+", current state = "+mOperateStateOn); 
        if ( mOperateStateOn == operateOn ) return;
        mOperateStateOn = operateOn; 
        if ( mListener != null ) mListener.onOperateOnChanged(mOperateStateOn);
    }
}
