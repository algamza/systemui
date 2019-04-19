package com.humaxdigital.automotive.statusbar.volumedialog;

import android.os.IBinder;

import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.car.CarNotConnectedException;
import android.extension.car.CarEx;
import android.extension.car.CarAudioManagerEx;
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
    private CarSensorManagerEx mCarSensorManagerEx;
    private CarAudioManagerEx mCarAudioManagerEx; 


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

    public CarExtensionClient unregisterListener() {
        mListener = null; 
        return this; 
    }

    public CarAudioManagerEx getAudioManagerEx() {
        return mCarAudioManagerEx;
    }

    public CarSensorManagerEx getSensorManagerEx() {
        return mCarSensorManagerEx;
    }

    private final ServiceConnection mServiceConnectionListenerClient =
            new ServiceConnection () {
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this) {
                try {
                    mCarSensorManagerEx = (CarSensorManagerEx) mCarEx.getCarManager(android.car.Car.SENSOR_SERVICE);
                    mCarAudioManagerEx = (CarAudioManagerEx) mCarEx.getCarManager(android.car.Car.AUDIO_SERVICE);
                    if ( mListener != null ) mListener.onConnected();
                } catch (CarNotConnectedException e) {
                    Log.e(TAG, "Car is not connected!", e);
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (this) {
                if ( mListener != null ) mListener.onDisconnected(); 
                mCarSensorManagerEx = null;
                mCarAudioManagerEx = null;
            }
        }
    };
}