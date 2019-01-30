package com.humaxdigital.automotive.statusbar.service;

import android.os.IBinder;

import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.car.CarNotConnectedException;
import android.extension.car.CarEx;
import android.extension.car.CarHvacManagerEx;
import android.extension.car.CarBLEManager;
import android.extension.car.CarTMSManager;
import android.extension.car.CarUSMManager;
import android.extension.car.CarSensorManagerEx;

import android.util.Log;

public class CarExtensionClient {
    private static final String TAG = "CarExtensionClient";

    public interface CarExClientListener {
        public void onConnected(); 
        public void onDisconnected(); 
    }

    private Context mContext; 
    private CarExClientListener mListener; 

    private Object mClientReady = new Object();

    private CarEx mCarEx;
    private CarHvacManagerEx mCarHvacManagerEx;
    private CarBLEManager mCarBLEManager;
    private CarTMSManager mCarTMSManager;
    private CarUSMManager mUsmManager;
    private CarSensorManagerEx mCarSensorManagerEx;

    public CarExtensionClient(Context context) {
        mContext = context; 
    }

    public CarExtensionClient connect() {
        if ( mContext == null ) return this; 
        if (mContext.getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            mCarEx = CarEx.createCar(mContext, mServiceConnectionListenerClient);
            mCarEx.connect();
        }
        return this; 
    }

    public void disconnect() {
        if ( mCarEx == null ) return; 
        mCarEx.disconnect(); 
    }

    public CarExtensionClient registerListener(CarExClientListener listener) {
        mListener = listener; 
        return this; 
    }

    public CarHvacManagerEx getHvacManagerEx() {
        return mCarHvacManagerEx;
    }
    
    public CarBLEManager getBLEManager() {
        return mCarBLEManager;
    }

    public CarUSMManager getUsmManager() {
        return mUsmManager;
    }

    public CarTMSManager getTMSManager() {
        return mCarTMSManager;
    }

    public CarSensorManagerEx getSensorManagerEx() {
        return mCarSensorManagerEx;
    }

    private final ServiceConnection mServiceConnectionListenerClient =
            new ServiceConnection () {
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this) {
                try {
                    mCarHvacManagerEx = (CarHvacManagerEx) mCarEx.getCarManager(android.car.Car.HVAC_SERVICE);
                    mCarBLEManager = (CarBLEManager) mCarEx.getCarManager(android.extension.car.CarEx.BLE_SERVICE);
                    mUsmManager = (CarUSMManager) mCarEx.getCarManager(android.extension.car.CarEx.USM_SERVICE);
                    mCarTMSManager = (CarTMSManager) mCarEx.getCarManager(android.extension.car.CarEx.TMS_SERVICE);
                    mCarSensorManagerEx = (CarSensorManagerEx) mCarEx.getCarManager(android.car.Car.SENSOR_SERVICE);
                    if ( mListener != null ) mListener.onConnected();
                } catch (CarNotConnectedException e) {
                    Log.e(TAG, "Car is not connected!", e);
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (this) {
                if ( mListener != null ) mListener.onDisconnected();
                mCarHvacManagerEx = null;
                mCarBLEManager = null; 
                mUsmManager = null; 
                mCarTMSManager = null; 
                mCarSensorManagerEx = null;
            }
        }
    };
}