package com.humaxdigital.automotive.statusbar.service;

import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.os.IBinder;
import android.os.Handler;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import android.car.hardware.hvac.CarHvacManager;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.VehicleAreaSeat;
import android.support.car.Car;
import android.support.car.CarNotConnectedException;
import android.support.car.CarConnectionCallback;

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
    private static final String DEMO_MODE_PROPERTY = "android.car.hvac.demo";

    public static final int DRIVER_ZONE_ID = 
        VehicleAreaSeat.SEAT_ROW_1_LEFT |
        VehicleAreaSeat.SEAT_ROW_2_LEFT | 
        VehicleAreaSeat.SEAT_ROW_2_CENTER;

    public static final int PASSENGER_ZONE_ID = 
        VehicleAreaSeat.SEAT_ROW_1_RIGHT |
        VehicleAreaSeat.SEAT_ROW_2_RIGHT;

    // todo : Hardware specific value for the front seats
    public static final int SEAT_ALL = 
        VehicleAreaSeat.SEAT_ROW_1_LEFT |
        VehicleAreaSeat.SEAT_ROW_1_RIGHT | 
        VehicleAreaSeat.SEAT_ROW_2_LEFT |
        VehicleAreaSeat.SEAT_ROW_2_CENTER | 
        VehicleAreaSeat.SEAT_ROW_2_RIGHT;

    private Context mContext; 
    private DataStore mDataStore;
    private boolean mIsInitialized = false;

    private Car mCarApiClient;
    private CarHvacManager mHvacManager;
    private Object mHvacManagerReady = new Object();
    
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
    }

    public void connect() {
        if ( mContext == null ) return; 
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            if (SystemProperties.getBoolean(DEMO_MODE_PROPERTY, false)) {
                IBinder binder = (new LocalHvacPropertyService()).getCarPropertyService();
                initHvacManager(new CarHvacManager(binder, mContext, new Handler()));
                return;
            }
            mCarApiClient = Car.createCar(mContext, mCarConnectionCallback);
            mCarApiClient.connect();
        }
    }

    public void disconnect()  {
        if (mHvacManager != null) {
            mHvacManager.unregisterCallback(mHardwareCallback);
        }
        if (mCarApiClient != null) {
            mCarApiClient.disconnect();
        }
    }

    public void fetch() {
        requestFetch(); 
    }

    public void registerListener(ClimateListener listener) {
        mListener = listener; 
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

    private void initHvacManager(CarHvacManager carHvacManager) {
        mHvacManager = carHvacManager;
        createControllers(); 
        List<CarPropertyConfig> properties = null;
        try {
            properties = mHvacManager.getPropertyList();
            mHvacManager.registerCallback(mHardwareCallback);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in HVAC");
        }
    }

    private void createControllers() {
        mAirCirculation = new ClimateAirCirculationController(mContext, mDataStore, mHvacManager); 
        mControllers.add(mAirCirculation); 
        mDRSeat = new ClimateDRSeatController(mContext, mDataStore, mHvacManager); 
        mControllers.add(mDRSeat); 
        mDRTemp = new ClimateDRTempController(mContext, mDataStore, mHvacManager); 
        mControllers.add(mDRTemp); 
        mFanDirection = new ClimateFanDirectionController(mContext, mDataStore, mHvacManager); 
        mControllers.add(mFanDirection); 
        mFanSpeed = new ClimateFanSpeedController(mContext, mDataStore, mHvacManager); 
        mControllers.add(mFanSpeed); 
        mPSSeat = new ClimatePSSeatController(mContext, mDataStore, mHvacManager); 
        mControllers.add(mPSSeat); 
        mPSTemp = new ClimatePSTempController(mContext, mDataStore, mHvacManager); 
        mControllers.add(mPSTemp); 
    }

    private final CarConnectionCallback mCarConnectionCallback = new CarConnectionCallback() {
        @Override
        public void onConnected(Car car) {
            synchronized (mHvacManagerReady) {
                try {
                    initHvacManager((CarHvacManager) mCarApiClient.getCarManager(
                            android.car.Car.HVAC_SERVICE));
                    mHvacManagerReady.notifyAll();
                } catch (CarNotConnectedException e) {
                    Log.e(TAG, "Car not connected in onServiceConnected");
                }
            }
        }

        @Override
        public void onDisconnected(Car car) {
        }
    };

    private final CarHvacManager.CarHvacEventCallback mHardwareCallback =
        new CarHvacManager.CarHvacEventCallback() {
            @Override
            public void onChangeEvent(final CarPropertyValue val) {
                Log.d(TAG, "HVAC event, id: " + val.getPropertyId());
                int areaId = val.getAreaId();
                switch (val.getPropertyId()) {
                    case CarHvacManager.ID_ZONED_FAN_DIRECTION:
                        handleFanPositionUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT:
                        handleFanSpeedUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManager.ID_ZONED_TEMP_SETPOINT:
                        handleTempUpdate(val);
                        break;
                    case CarHvacManager.ID_ZONED_AIR_RECIRCULATION_ON:
                        handleAirCirculationUpdate(getValue(val));
                        break;
                    case CarHvacManager.ID_ZONED_SEAT_TEMP:
                        handleSeatWarmerUpdate(areaId, getValue(val));
                        break;
                    default:
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Unhandled HVAC event, id: " + val.getPropertyId());
                        }
                }
            }

            @Override
            public void onErrorEvent(final int propertyId, final int zone) {
            }
        };

    private void requestFetch() {
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {

                synchronized (mHvacManagerReady) {
                    while (mHvacManager == null) {
                        try {
                            mHvacManagerReady.wait();
                        } catch (InterruptedException e) {
                            // We got interrupted so we might be shutting down.
                            return null;
                        }
                    }
                }

                for ( ClimateBaseController controller : mControllers ) controller.fetch(); 
                
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

    
    private void handleTempUpdate(CarPropertyValue value) {
        if ( mDRTemp == null || mPSTemp == null ) return; 
        final int zone = value.getAreaId();
        final float temp = (Float)value.getValue();
        final boolean available = value.getStatus() == CarPropertyValue.STATUS_AVAILABLE;
        
        if (zone == VehicleAreaSeat.SEAT_ROW_1_LEFT) {
            if ( mDRTemp.update(temp) ) 
                if ( mListener != null ) 
                    mListener.onDriverTemperatureChanged(mDRTemp.get());
        } else {
            if ( mPSTemp.update(temp) ) 
                if ( mListener != null ) 
                    mListener.onPassengerTemperatureChanged(mPSTemp.get());
        }
    }

    private void handleSeatWarmerUpdate(int zone, int level) {
        if ( mDRSeat == null || mPSSeat == null ) return; 
        
        if (zone == VehicleAreaSeat.SEAT_ROW_1_LEFT) {
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
}
