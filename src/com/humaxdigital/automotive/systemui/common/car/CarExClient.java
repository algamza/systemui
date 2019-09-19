package com.humaxdigital.automotive.systemui.common.car; 

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
import android.extension.car.CarClusterManager; 
import android.extension.car.CarHvacManagerEx;
import android.extension.car.CarBLEManager;
import android.extension.car.CarUSMManager;
import android.car.CarNotConnectedException;

import android.util.Log;
import java.util.ArrayList;

public class CarExClient {
    private static final String TAG = "CarExClient";

    private enum STATE {
        IDLE, 
        CONNECTED, 
        CONNECTING 
    }
    public interface CarExClientListener {
        public void onConnected(); 
        public void onDisconnected(); 
    }

    private STATE mState = STATE.IDLE; 
    private Context mContext = null; 
    private ArrayList<CarExClientListener> mListeners = new ArrayList<>();
    private CarEx mCarEx = null ;
    private CarAudioManagerEx mCarAudio = null; 
    private CarSystemManager mCarSystem = null;
    private CarSensorManagerEx mCarSensor = null; 
    private CarTMSManager mCarTms = null; 
    private CarClusterManager mCarCluster = null;
    private CarHvacManagerEx mCarHvacManager = null; 
    private CarBLEManager mCarBLEManager = null; 
    private CarUSMManager mUsmManager = null; 


    private static CarExClient mInstance = null; 
    private CarExClient() {}
    public static synchronized CarExClient getInstance() {
        if ( mInstance == null ) 
            mInstance = new CarExClient(); 
        return mInstance; 
    }

    public synchronized void connect(Context context, CarExClientListener listener) {
        if ( context == null || listener == null ) 
        mContext = context; 
        mListeners.add(listener); 
        if ( mState == STATE.CONNECTED ) listener.onConnected(); 
        if ( (mState == STATE.IDLE) 
            && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ) {
            mCarEx = CarEx.createCar(mContext, mServiceConnectionListenerClient);
            mState = STATE.CONNECTING; 
            mCarEx.connect();
        }
    }

    public synchronized void disconnect(CarExClientListener listener) {
        if ( listener == null ) return;
        mListeners.remove(listener);
    }

    public synchronized CarHvacManagerEx getHvacManager() {
        if ( mState != STATE.CONNECTED ) return null; 
        return mCarHvacManager;
    }

    public synchronized CarBLEManager getBLEManager() {
        if ( mState != STATE.CONNECTED ) return null; 
        return mCarBLEManager;
    }

    public synchronized CarUSMManager getUsmManager() {
        if ( mState != STATE.CONNECTED ) return null; 
        return mUsmManager;
    }
    
    public synchronized CarAudioManagerEx getAudioManager() {
        if ( mState != STATE.CONNECTED ) return null; 
        return mCarAudio; 
    }

    public synchronized CarSystemManager getSystemManager() {
        if ( mState != STATE.CONNECTED ) return null; 
        return mCarSystem; 
    }

    public synchronized CarSensorManagerEx getSensorManager() {
        if ( mState != STATE.CONNECTED ) return null; 
        return mCarSensor; 
    }

    public synchronized CarTMSManager getTMSManager() {
        if ( mState != STATE.CONNECTED ) return null; 
        return mCarTms; 
    }

    public synchronized CarClusterManager getClusterManager() {
        if ( mState != STATE.CONNECTED ) return null; 
        return mCarCluster; 
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
                    mCarCluster = (CarClusterManager) mCarEx.getCarManager(CarEx.CLUSTER_SERVICE); 
                    mCarHvacManager = (CarHvacManagerEx) mCarEx.getCarManager(android.car.Car.HVAC_SERVICE);
                    mCarBLEManager = (CarBLEManager) mCarEx.getCarManager(android.extension.car.CarEx.BLE_SERVICE);
                    mUsmManager = (CarUSMManager) mCarEx.getCarManager(android.extension.car.CarEx.USM_SERVICE);
                    mState = STATE.CONNECTED; 
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
                mState = STATE.IDLE; 
                for ( CarExClientListener listener : mListeners ) {
                    if ( listener != null ) listener.onDisconnected();
                }
                mCarAudio = null;
                mCarSystem = null;
                mCarSensor = null;
                mCarTms = null;
                mCarCluster = null; 
                mContext = null;
            }
        }
    };
}