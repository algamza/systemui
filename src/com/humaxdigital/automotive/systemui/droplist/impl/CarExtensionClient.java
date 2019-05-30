package com.humaxdigital.automotive.systemui.droplist.impl; 

import android.os.IBinder;

import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;

import android.extension.car.CarEx;
import android.extension.car.CarAudioManagerEx;
import android.extension.car.CarSystemManager;
import android.extension.car.CarSensorManagerEx; 
import android.extension.car.CarTMSManager;
import android.car.CarNotConnectedException;
import android.car.media.ICarVolumeCallback;

import android.util.Log;

import java.util.ArrayList;

public class CarExtensionClient {
    private static final String TAG = "CarExtensionClient";

    public interface CarExClientListener {
        public void onConnected(); 
        public void onDisconnected(); 
    }

    private Context mContext; 
    private ArrayList<CarExClientListener> mListeners = new ArrayList<>();

    private Object mClientReady = new Object();

    private CarEx mCarEx;
    private CarAudioManagerEx mCarAudio; 
    private CarSystemManager mCarSystem;
    private CarSensorManagerEx mCarSensor; 
    private CarTMSManager mCarTms; 

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
        mCarEx = null; 
        mListeners.clear();
        mCarAudio = null;
        mCarSystem = null;
        mCarSensor = null;
        mCarTms = null;
    }

    public CarExtensionClient registerListener(CarExClientListener listener) {
        mListeners.add(listener); 
        return this; 
    }

    public CarExtensionClient unregisterListener(CarExClientListener listener) {
        mListeners.remove(listener); 
        return this; 
    }

    public CarAudioManagerEx getAudioManager() {
        return mCarAudio; 
    }

    public CarSystemManager getSystemManager() {
        return mCarSystem; 
    }

    public CarSensorManagerEx getSensorManager() {
        return mCarSensor; 
    }

    public CarTMSManager getTMSManager() {
        return mCarTms; 
    }

    private final ServiceConnection mServiceConnectionListenerClient =
            new ServiceConnection () {
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this) {
                try {
                    mCarAudio = (CarAudioManagerEx)mCarEx.getCarManager(android.car.Car.AUDIO_SERVICE);
                    mCarSystem = (CarSystemManager)mCarEx.getCarManager(android.extension.car.CarEx.SYSTEM_SERVICE);
                    mCarSensor = (CarSensorManagerEx)mCarEx.getCarManager(android.car.Car.SENSOR_SERVICE); 
                    mCarTms = (CarTMSManager)mCarEx.getCarManager(android.extension.car.CarEx.TMS_SERVICE); 
                    for ( CarExClientListener listener : mListeners ) {
                        if ( listener != null ) listener.onConnected();
                    }
                } catch (CarNotConnectedException e) {
                    Log.e(TAG, "Car is not connected!", e);
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (this) {
                for ( CarExClientListener listener : mListeners ) {
                    if ( listener != null ) listener.onDisconnected();
                }
                mCarAudio = null;
                mCarSystem = null;
                mCarSensor = null;
            }
        }
    };
}