package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.UserHandle;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.provider.Settings;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import android.extension.car.settings.CarExtraSettings;

import com.humaxdigital.automotive.systemui.R; 
import com.humaxdigital.automotive.systemui.common.util.OSDPopup; 
import com.humaxdigital.automotive.systemui.common.util.CommonMethod; 
import com.humaxdigital.automotive.systemui.common.car.CarExClient;
import com.humaxdigital.automotive.systemui.common.CONSTANTS;

public class StatusBarClimate {
    private static final String TAG = "StatusBarClimate";

    private ClimateControllerManager mClimateManager = null;
    private CarExClient mCarExClient = null; 
    private List<StatusBarClimateCallback> mClimateCallbacks = new ArrayList<>();
    private DataStore mDataStore = null;
    private Context mContext = null;
    private Timer mTimer = new Timer();
    private TimerTask mTimerTaskClimateChattering = null;
    private boolean mTryOpenClimate = false; 
    private final int OPEN_CLIMATE_CHATTERING_TIME = 2000; 
    
    // special case
    private boolean mRearCamera = false; 
    private boolean mFrontCamera = false;
    private boolean mRearGearDetected = false; 
    private boolean mBTCall = false;
    private boolean mEmergencyCall = false;
    private boolean mBluelinkCall = false; 
    private boolean mSVIOn = false;
    private boolean mSVSOn = false; 

    public static abstract class StatusBarClimateCallback {
        public void onInitialized() {}
        public void onDRTemperatureChanged(float temp) {}
        public void onDRSeatStatusChanged(int status) {}
        public void onDRSeatOptionChanged(int option) {}
        public void onAirCirculationChanged(boolean isOn) {}
        public void onAirConditionerChanged(boolean isOn) {}
        public void onAirCleaningChanged(int status) {}
        public void onSyncChanged(boolean sync) {}
        public void onFanDirectionChanged(int direction) {}
        public void onBlowerSpeedChanged(int status) {}
        public void onPSSeatStatusChanged(int status) {}
        public void onPSSeatOptionChanged(int option) {}
        public void onPSTemperatureChanged(float temp) {}
        public void onFrontDefogStatusChanged(int status) {}
        public void onModeOffChanged(boolean off) {}
        public void onIGNOnChanged(boolean on) {}
        public void onOperateOnChanged(boolean on) {}
        public void onRearCameraOn(boolean on) {}  
    }

    public StatusBarClimate(Context context, DataStore datastore) {
        if ( context == null || datastore == null ) return;
        Log.d(TAG, "StatusBarClimate");
        mContext = context; 
        mDataStore = datastore; 

        mClimateManager = new ClimateControllerManager(mContext, mDataStore)
            .registerListener(mClimateManagerListener); 
    }
    
    public void registerClimateCallback(StatusBarClimateCallback callback) {
        Log.d(TAG, "registerClimateCallback");
        if ( callback == null ) return;
        synchronized (mClimateCallbacks) {
            mClimateCallbacks.add(callback); 
        }
    }

    public void unregisterClimateCallback(StatusBarClimateCallback callback) {
        Log.d(TAG, "unregisterClimateCallback");
        if ( callback == null ) return;
        synchronized (mClimateCallbacks) {
            mClimateCallbacks.remove(callback); 
        }
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

    public void fetchCarExClient(CarExClient client) {
        Log.d(TAG, "fetchCarExClient");
        mCarExClient = client; 

        if ( mClimateManager != null ) {
            if ( mCarExClient == null ) mClimateManager.fetch(null, null, null);
            else mClimateManager.fetch(
                mCarExClient.getHvacManager(), 
                mCarExClient.getUsmManager(), 
                mCarExClient.getSensorManager());
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

    public void onRearGearDetected(boolean on) {
        Log.d(TAG, "onRearGearDetected="+on);
        mRearGearDetected = on; 
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

    public void onSVIOn(boolean on) {
        Log.d(TAG, "onSVIOn="+on);
        mSVIOn = on; 
    }

    public void onSVSOn(boolean on) {
        Log.d(TAG, "onSVSOn="+on);
        mSVSOn = on; 
    }

    public boolean isInitialized() {
        if ( mClimateManager == null ) return false;
        boolean init = mClimateManager.isInitialized(); 
        Log.d(TAG, "isInitialized="+init);
        return init; 
    }
    
    public int getIGNStatus() { 
        if ( mClimateManager == null ) return 0;
        int status = mClimateManager.getIGNStatus();
        Log.d(TAG, "getIGNStatus="+status);
        return status;
    }

    public boolean isOperateOn() { 
        if ( mClimateManager == null ) return false;
        boolean on = mClimateManager.isOperateOn();
        Log.d(TAG, "isOperateOn="+on);
        return on;
    }

    public float getDRTemperature() { 
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

    public int getDRSeatStatus() { 
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.DRIVER_SEAT).get(); 
        Log.d(TAG, "getDRSeatStatus="+status);
        return status;
    }

    public void setDRSeatStatus(int state) { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setDRSeatStatus="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.DRIVER_SEAT).set(state); 
    }

    public int getDRSeatOption() { 
        if ( mClimateManager == null ) return 0; 
        int option = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.DRIVER_SEAT_OPTION).get(); 
        Log.d(TAG, "getDRSeatOption="+option);
        return option;
    }

    public boolean getAirCirculationState() { 
        if ( mClimateManager == null ) return false; 
        boolean status = (boolean)mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CIRCULATION).get(); 
        Log.d(TAG, "getAirCirculationState="+status);
        return status;
    }
  
    public void setAirCirculationState(boolean state) { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setAirCirculationState="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CIRCULATION).set(state); 
    }
   
    public boolean getAirConditionerState() { 
        if ( mClimateManager == null ) return false; 
        boolean status = (boolean)mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CONDITIONER).get();
        Log.d(TAG, "getAirConditionerState="+status);
        return status; 
    }
 
    public void setAirConditionerState(boolean state) { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setAirConditionerState="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CONDITIONER).set(state); 
    }
 
    public int getAirCleaningState() { 
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CLEANING).get();
        Log.d(TAG, "getAirCleaningState="+status);
        return status;  
    }
 
    public void setAirCleaningState(int state) { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setAirCleaningState="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CLEANING).set(state); 
    }

    public boolean getSyncState() { 
        if ( mClimateManager == null ) return false; 
        boolean status = (boolean)mClimateManager.getController(ClimateControllerManager.ControllerType.SYNC).get();
        Log.d(TAG, "getSyncState="+status);
        return status;  
    }
    
    public void setSyncState(boolean state) { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setSyncState="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.SYNC).set(state); 
    }
 
    public int getFanDirection() {
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_DIRECTION).get();
        Log.d(TAG, "getFanDirection="+status);
        return status;   
    }
 
    public void setFanDirection(int state) { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setFanDirection="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_DIRECTION).set(state); 
    }

    public int getBlowerSpeed() { 
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_SPEED).get();
        Log.d(TAG, "getBlowerSpeed="+status);
        return status;   
    }
    
    public void setBlowerSpeed(int state) {
        if ( mClimateManager == null ) return;
        Log.d(TAG, "setBlowerSpeed="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_SPEED).set(state);
    }
    
    public int getPSSeatStatus() {
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.PASSENGER_SEAT).get(); 
        Log.d(TAG, "getPSSeatStatus="+status);
        return status;  
    }

    public void setPSSeatStatus(int state) { 
        if ( mClimateManager == null ) return;  
        Log.d(TAG, "setPSSeatStatus="+state);
        mClimateManager.getController(ClimateControllerManager.ControllerType.PASSENGER_SEAT).set(state); 
    }

    public int getPSSeatOption() {
        if ( mClimateManager == null ) return 0; 
        int option = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.PASSENGER_SEAT_OPTION).get(); 
        Log.d(TAG, "getPSSeatOption="+option);
        return option;  
    }
    
    public float getPSTemperature() { 
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
    
    public int getFrontDefogState() { 
        if ( mClimateManager == null ) return 0; 
        int status = (int)mClimateManager.getController(ClimateControllerManager.ControllerType.DEFOG).get();
        Log.d(TAG, "getFrontDefogState="+status);
        return status;   
    }
    
    public boolean isModeOff() { 
        if ( mClimateManager == null ) return false; 
        boolean off = (boolean)mClimateManager.getController(ClimateControllerManager.ControllerType.MODE_OFF).get();
        Log.d(TAG, "isModeOff="+off);
        return off;   
    }
   
    public void openClimateSetting() {
        Log.d(TAG, "openClimateSetting : front camera="+mFrontCamera+", rear camera="+mRearCamera+", rear gear="+mRearGearDetected); 
        if ( mRearCamera ) {
            Log.d(TAG, "Current Rear Camera Mode : only climate toggle");
            return;
        }
        if ( mRearGearDetected ) {
            Log.d(TAG, "Current Rear Gear : only climate toggle");
            return;
        }
        if ( isUserAgreement() ) {
            if ( isPowerOff() ) powerOn();
            Log.d(TAG, "Current UserAgreement, set power on"); 
            return; 
        }
        if ( isUserSwitching() ) {
            Log.d(TAG, "Current UserSwitching"); 
            return; 
        }
        /*
        if ( mBluelinkCall ) {
            Log.d(TAG, "Current bluelink call on: only climate toggle");
            return;
        }
        */
        if ( mEmergencyCall ) {
            Log.d(TAG, "Current emergency call on: only climate toggle");
            return;
        }
        if ( mSVSOn ) {
            Log.d(TAG, "Current svs on: only climate toggle");
            return;
        }
        if ( mSVIOn ) {
            Log.d(TAG, "Current svs on: only climate toggle");
            return;
        }

        if ( !CONSTANTS.OPEN_HVAC_APP.equals("") ) {
            Log.d(TAG, "openClimateSetting="+CONSTANTS.OPEN_HVAC_APP);

            if ( mTryOpenClimate ) return;
            mTryOpenClimate = true;
            mTimerTaskClimateChattering = new TimerTask() {
                @Override
                public void run() {
                    mTryOpenClimate = false;
                }
            };
            mTimer.schedule(mTimerTaskClimateChattering, OPEN_CLIMATE_CHATTERING_TIME);
            CommonMethod.closeVR(mContext);
            Intent intent = new Intent(CONSTANTS.OPEN_HVAC_APP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    private boolean isUserAgreement() {
        int is_agreement = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT,
            CarExtraSettings.Global.FALSE);   
        Log.d(TAG, "isUserAgreement="+is_agreement);
        if ( is_agreement == CarExtraSettings.Global.FALSE ) return false; 
        else return true;
    }

    private boolean isUserSwitching() {
        if ( mContext == null ) return false; 
        int isUserSwitching = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_USER_SWITCHING_START_FINISH, 
            CarExtraSettings.Global.FALSE);
        Log.d(TAG, "isUserSwitching="+isUserSwitching);
        if ( isUserSwitching == CarExtraSettings.Global.TRUE ) return true; 
        else return false;
    }

    private boolean isPowerOff() {
        int is_power_off = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.POWER_OFF_MODE,
            CarExtraSettings.Global.POWER_OFF_MODE_DEFAULT);   
        Log.d(TAG, "isPowerOff="+is_power_off);
        if ( is_power_off == CarExtraSettings.Global.POWER_OFF_MODE_ON ) return true; 
        else return false;
    }

    private void powerOn() {
        Log.d(TAG, "powerOn");
        Settings.Global.putInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.POWER_OFF_MODE,
            CarExtraSettings.Global.POWER_OFF_MODE_OFF_TO_AV_OFF); 
    }

    private ClimateControllerManager.ClimateListener mClimateManagerListener = 
        new ClimateControllerManager.ClimateListener() {
        @Override
        public void onInitialized() {
            Log.d(TAG, "onInitialized");
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) {
                    callback.onInitialized();
                }
            }
        }

        @Override
        public void onDriverTemperatureChanged(float temp) {
            Log.d(TAG, "onDriverTemperatureChanged="+temp);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onDRTemperatureChanged(temp); 
            }
        }
        
        @Override
        public void onDriverSeatStatusChanged(int status) {
            Log.d(TAG, "onDriverSeatStatusChanged="+status);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onDRSeatStatusChanged(status); 
            }
        }

        @Override
        public void onDriverSeatOptionChanged(int option) {
            Log.d(TAG, "onDriverSeatOptionChanged="+option);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onDRSeatOptionChanged(option); 
            }
        }

        @Override
        public void onAirCirculationChanged(boolean isOn) {
            Log.d(TAG, "onAirCirculationChanged="+isOn);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onAirCirculationChanged(isOn); 
            }
        }

        @Override
        public void onAirConditionerChanged(boolean isOn) {
            Log.d(TAG, "onAirConditionerChanged="+isOn);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onAirConditionerChanged(isOn); 
            }
        }

        @Override
        public void onAirCleaningChanged(int status) {
            Log.d(TAG, "onAirCleaningChanged="+status);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onAirCleaningChanged(status); 
            }
        }

        @Override
        public void onSyncChanged(boolean sync) {
            Log.d(TAG, "onSyncChanged="+sync);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onSyncChanged(sync); 
            }
        }

        @Override
        public void onFanDirectionChanged(int status) {
            Log.d(TAG, "onFanDirectionChanged="+status);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onFanDirectionChanged(status); 
            }
        }

        @Override
        public void onFanSpeedStatusChanged(int status) {
            Log.d(TAG, "onFanSpeedStatusChanged="+status);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onBlowerSpeedChanged(status); 
            }
        }

        @Override
        public void onPassengerSeatStatusChanged(int status) {
            Log.d(TAG, "onPassengerSeatStatusChanged="+status);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onPSSeatStatusChanged(status); 
            }
        }

        @Override
        public void onPassengerSeatOptionChanged(int option) {
            Log.d(TAG, "onPassengerSeatOptionChanged="+option);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onPSSeatOptionChanged(option); 
            }
        }

        @Override
        public void onPassengerTemperatureChanged(float temp) {
            Log.d(TAG, "onPassengerTemperatureChanged="+temp);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onPSTemperatureChanged(temp); 
            }
        }

        @Override
        public void onFrontDefogStatusChanged(int status) {
            Log.d(TAG, "onFrontDefogStatusChanged="+status);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onFrontDefogStatusChanged(status); 
            }
        }

        @Override
        public void onModeOffChanged(boolean off) {
            Log.d(TAG, "onModeOffChanged="+off);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onModeOffChanged(off); 
            }
        }

        @Override
        public void onIGNOnChanged(boolean on) {
            Log.d(TAG, "onIGNOnChanged="+on);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onIGNOnChanged(on); 
            }
        }

        @Override
        public void onOperateOnChanged(boolean on) {
            Log.d(TAG, "onOperateOnChanged="+on);
            synchronized (mClimateCallbacks) {
                for ( StatusBarClimateCallback callback : mClimateCallbacks ) 
                    callback.onOperateOnChanged(on); 
            }
        }
    }; 
}
