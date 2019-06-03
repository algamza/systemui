package com.humaxdigital.automotive.systemui.statusbar.service;


import android.os.RemoteException;
import android.os.UserHandle;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import android.extension.car.settings.CarExtraSettings;

import com.humaxdigital.automotive.systemui.R; 
import com.humaxdigital.automotive.systemui.util.OSDPopup; 

public class StatusBarClimate extends IStatusBarClimate.Stub {
    private static final String TAG = "StatusBarClimate";
    private static final String OPEN_HVAC_APP = "com.humaxdigital.automotive.climate.CLIMATE";
    
    private ClimateControllerManager mClimateManager = null;
    private CarExtensionClient mCarExClient = null; 
    private List<IStatusBarClimateCallback> mClimateCallbacks = new ArrayList<>();
    private DataStore mDataStore = null;
    private Context mContext = null;
    
    // special case
    private boolean mRearCamera = false; 
    private boolean mFrontCamera = false;
    private boolean mBTCall = false;
    private boolean mEmergencyCall = false;
    private boolean mBluelinkCall = false; 

    public StatusBarClimate(Context context, DataStore datastore) {
        if ( context == null || datastore == null ) return;
        Log.d(TAG, "StatusBarClimate");
        mContext = context; 
        mDataStore = datastore; 

        mClimateManager = new ClimateControllerManager(mContext, mDataStore)
            .registerListener(mClimateManagerListener); 
    }

    public void destroy() {
        Log.d(TAG, "destroy");
        
        mContext = null;
        mDataStore = null;
        mCarExClient = null;
        mClimateCallbacks.clear(); 
        mClimateManager.fetch(null, null, null); 
        mClimateManager = null;
    }

    public void fetchCarExClient(CarExtensionClient client) {
        Log.d(TAG, "fetchCarExClient");
        mCarExClient = client; 

        if ( mClimateManager != null ) {
            if ( mCarExClient == null ) mClimateManager.fetch(null, null, null);
            else mClimateManager.fetch(
                mCarExClient.getHvacManagerEx(), 
                mCarExClient.getUsmManager(), 
                mCarExClient.getSensorManagerEx());
        }
    }

    public void onFrontCamera(boolean on) {
        Log.d(TAG, "onFrontCamera="+on);
        mFrontCamera = on; 
    }

    public void onRearCamera(boolean on) {
        Log.d(TAG, "onRearCamera="+on);
        mRearCamera = on; 
    }

    public void onBTCall(boolean on) {
        Log.d(TAG, "onBTCall="+on);
        mBTCall = on; 
    }

    public void onEmergencyCall(boolean on) {
        Log.d(TAG, "onEmergencyCall="+on);
        mEmergencyCall = on; 
    }

    public void onBluelinkCall(boolean on) {
        Log.d(TAG, "onBluelinkCall="+on);
        mBluelinkCall = on; 
    }

    private boolean isUserAgreement() {
        int is_agreement = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT,
            CarExtraSettings.Global.FALSE);   
        if ( is_agreement == CarExtraSettings.Global.FALSE ) return false; 
        else return true;
    }

    @Override
    public boolean isInitialized() throws RemoteException {
        if ( mClimateManager == null ) return false;
        boolean init = mClimateManager.isInitialized(); 
        Log.d(TAG, "isInitialized="+init);
        return init; 
    }
    
    @Override
    public int getIGNStatus() throws RemoteException { 
        if ( mClimateManager == null ) return 0;
        int status = mClimateManager.getIGNStatus();
        Log.d(TAG, "getIGNStatus="+status);
        return status;
    }
    @Override
    public boolean isOperateOn() throws RemoteException { 
        if ( mClimateManager == null ) return false;
        boolean on = mClimateManager.isOperateOn();
        Log.d(TAG, "isOperateOn="+on);
        return on;
    }
    @Override
    public float getDRTemperature() throws RemoteException { 
        float ret = 0.0f; 
        if ( mClimateManager == null ) return ret; 
        if ( ((ClimateDRTempController)mClimateManager.getController(
            ClimateControllerManager.ControllerType.DRIVER_TEMPERATURE)).getCurrentTemperatureMode() == 
            ClimateDRTempController.MODE.CELSIUS ) {
            ret = ClimateUtils.temperatureHexToCelsius((int)mClimateManager.getController(
                ClimateControllerManager.ControllerType.DRIVER_TEMPERATURE).get()); 
        } else {
            ret = ClimateUtils.temperatureHexToFahrenheit((int)mClimateManager.getController(
                ClimateControllerManager.ControllerType.DRIVER_TEMPERATURE).get()); 
        }
        Log.d(TAG, "getDRTemperature="+ret);
        return ret; 
    }
    @Override
    public int getDRSeatStatus() throws RemoteException { 
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.DRIVER_SEAT).get(); 
        Log.d(TAG, "getDRSeatStatus="+status);
        return status;
    }
    @Override
    public boolean getAirCirculationState() throws RemoteException { 
        if ( mClimateManager == null ) return false; 
        boolean status = (boolean)mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CIRCULATION).get(); 
        Log.d(TAG, "getAirCirculationState="+status);
        return status;
    }
    @Override
    public void setAirCirculationState(boolean state) throws RemoteException { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setAirCirculationState="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CIRCULATION).set(state); 
    }
    @Override
    public boolean getAirConditionerState() throws RemoteException { 
        if ( mClimateManager == null ) return false; 
        boolean status = (boolean)mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CONDITIONER).get();
        Log.d(TAG, "getAirConditionerState="+status);
        return status; 
    }
    @Override
    public void setAirConditionerState(boolean state) throws RemoteException { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setAirConditionerState="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CONDITIONER).set(state); 
    }
    @Override
    public int getAirCleaningState() throws RemoteException { 
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CLEANING).get();
        Log.d(TAG, "getAirCleaningState="+status);
        return status;  
    }
    @Override
    public void setAirCleaningState(int state) throws RemoteException { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setAirCleaningState="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CLEANING).set(state); 
    }
    @Override
    public int getFanDirection() throws RemoteException {
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_DIRECTION).get();
        Log.d(TAG, "getFanDirection="+status);
        return status;   
    }
    @Override
    public void setFanDirection(int state) throws RemoteException { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setFanDirection="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_DIRECTION).set(state); 
    }
    @Override
    public int getBlowerSpeed() throws RemoteException { 
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_SPEED).get();
        Log.d(TAG, "getBlowerSpeed="+status);
        return status;   
    }
    @Override
    public void setBlowerSpeed(int state) throws RemoteException {
        if ( mClimateManager == null ) return;
        Log.d(TAG, "setBlowerSpeed="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_SPEED).set(state);
    }
    @Override
    public int getPSSeatStatus() throws RemoteException {
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.PASSENGER_SEAT).get(); 
        Log.d(TAG, "getPSSeatStatus="+status);
        return status;  
    }
    @Override
    public float getPSTemperature() throws RemoteException { 
        float ret = 0.0f; 
        if ( mClimateManager == null ) return 0.0f; 
        if ( ((ClimatePSTempController)mClimateManager.getController(
            ClimateControllerManager.ControllerType.PASSENGER_TEMPERATURE))
            .getCurrentTemperatureMode() == ClimatePSTempController.MODE.CELSIUS ) {
            ret = ClimateUtils.temperatureHexToCelsius((int)mClimateManager.getController(
                ClimateControllerManager.ControllerType.PASSENGER_TEMPERATURE).get()); 
        } else {
            ret = ClimateUtils.temperatureHexToFahrenheit((int)mClimateManager.getController(
                ClimateControllerManager.ControllerType.PASSENGER_TEMPERATURE).get()); 
        }
        Log.d(TAG, "getPSTemperature="+ret);
        return ret; 
    }
    @Override
    public int getFrontDefogState() throws RemoteException { 
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.DEFOG).get();
        Log.d(TAG, "getFrontDefogState="+status);
        return status;   
    }
    @Override
    public boolean isModeOff() throws RemoteException { 
        if ( mClimateManager == null ) return false; 
        boolean off = (boolean)mClimateManager.getController(ClimateControllerManager.ControllerType.MODE_OFF).get();
        Log.d(TAG, "isModeOff="+off);
        return off;   
    }
    @Override
    public void openClimateSetting() throws RemoteException {
        Log.d(TAG, "openClimateSetting : front camera="+mFrontCamera+", rear camera="+mRearCamera); 
        if ( isUserAgreement() ) {
            Log.d(TAG, "Current UserAgreement"); 
            return; 
        }
        if ( mRearCamera ) {
            Log.d(TAG, "Current Rear Camera Mode : only climate toggle");
            return;
        }
        if ( mEmergencyCall || mBluelinkCall ) {
            Log.d(TAG, "Current emergency="+mEmergencyCall+", bluelinkcall="+mBluelinkCall); 
            return; 
        }
        if ( !OPEN_HVAC_APP.equals("") ) {
            Log.d(TAG, "openClimateSetting="+OPEN_HVAC_APP);
            Intent intent = new Intent(OPEN_HVAC_APP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }
    @Override
    public void registerClimateCallback(IStatusBarClimateCallback callback) throws RemoteException {
        Log.d(TAG, "registerClimateCallback");
        if ( callback == null ) return;
        synchronized (mClimateCallbacks) {
            mClimateCallbacks.add(callback); 
        }
    }
    @Override
    public void unregisterClimateCallback(IStatusBarClimateCallback callback) throws RemoteException {
        Log.d(TAG, "unregisterClimateCallback");
        if ( callback == null ) return;
        synchronized (mClimateCallbacks) {
            mClimateCallbacks.remove(callback); 
        }
    }

    private ClimateControllerManager.ClimateListener mClimateManagerListener = 
        new ClimateControllerManager.ClimateListener() {
        @Override
        public void onInitialized() {
            Log.d(TAG, "onInitialized");
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) {
                    callback.onInitialized();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDriverTemperatureChanged(float temp) {
            Log.d(TAG, "onDriverTemperatureChanged="+temp);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onDRTemperatureChanged(temp); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onDriverSeatStatusChanged(int status) {
            Log.d(TAG, "onDriverSeatStatusChanged="+status);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onDRSeatStatusChanged(status); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAirCirculationChanged(boolean isOn) {
            Log.d(TAG, "onAirCirculationChanged="+isOn);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onAirCirculationChanged(isOn); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAirConditionerChanged(boolean isOn) {
            Log.d(TAG, "onAirConditionerChanged="+isOn);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onAirConditionerChanged(isOn); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAirCleaningChanged(int status) {
            Log.d(TAG, "onAirCleaningChanged="+status);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onAirCleaningChanged(status); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFanDirectionChanged(int status) {
            Log.d(TAG, "onFanDirectionChanged="+status);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onFanDirectionChanged(status); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFanSpeedStatusChanged(int status) {
            Log.d(TAG, "onFanSpeedStatusChanged="+status);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onBlowerSpeedChanged(status); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPassengerSeatStatusChanged(int status) {
            Log.d(TAG, "onPassengerSeatStatusChanged="+status);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onPSSeatStatusChanged(status); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPassengerTemperatureChanged(float temp) {
            Log.d(TAG, "onPassengerTemperatureChanged="+temp);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onPSTemperatureChanged(temp); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFrontDefogStatusChanged(int status) {
            Log.d(TAG, "onFrontDefogStatusChanged="+status);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onFrontDefogStatusChanged(status); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onModeOffChanged(boolean off) {
            Log.d(TAG, "onModeOffChanged="+off);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onModeOffChanged(off); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIGNOnChanged(boolean on) {
            Log.d(TAG, "onIGNOnChanged="+on);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onIGNOnChanged(on); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onOperateOnChanged(boolean on) {
            Log.d(TAG, "onOperateOnChanged="+on);
            try {
                for ( IStatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onOperateOnChanged(on); 
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }; 
}
