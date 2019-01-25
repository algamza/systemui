package com.humaxdigital.automotive.statusbar.service;

import android.os.Bundle;

import com.humaxdigital.automotive.statusbar.service.ISystemCallback;
import com.humaxdigital.automotive.statusbar.service.IClimateCallback;
import com.humaxdigital.automotive.statusbar.service.IStatusBarCallback;
import com.humaxdigital.automotive.statusbar.service.IDateTimeCallback;
import com.humaxdigital.automotive.statusbar.service.IUserProfileCallback;
import com.humaxdigital.automotive.statusbar.service.BitmapParcelable;

interface IStatusBarService {
    boolean isInitialized();
    void registerStatusBarCallback(IStatusBarCallback callback);
    void unregisterStatusBarCallback(IStatusBarCallback callback);

    int getMuteStatus();
    int getBLEStatus();
    int getBTBatteryStatus();
    int getBTCallStatus();
    int getBTAntennaStatus();
    int getTMSAntennaStatus();
    int getDataStatus();
    int getWifiStatus();
    int getWirelessChargeStatus();
    int getModeStatus();
    void registerSystemCallback(ISystemCallback callback);
    void unregisterSystemCallback(ISystemCallback callback);

    float getDRTemperature();
    int getDRSeatStatus();
    boolean getAirCirculationState();
    void setAirCirculationState(boolean state);
    boolean getAirConditionerState();
    void setAirConditionerState(boolean state);
    int getAirCleaningState();
    void setAirCleaningState(int state);
    int getFanDirection();
    void setFanDirection(int state); 
    int getBlowerSpeed();
    int getPSSeatStatus();
    float getPSTemperature();
    int getFrontDefogState(); 
    void openClimateSetting(); 
    void registerClimateCallback(IClimateCallback callback);
    void unregisterClimateCallback(IClimateCallback callback);

    String getDateTime(); 
    void openDateTimeSetting(); 
    void registerDateTimeCallback(IDateTimeCallback callback);
    void unregisterDateTimeCallback(IDateTimeCallback callback);

    BitmapParcelable getUserProfileImage(); 
    void openUserProfileSetting(); 
    void registerUserProfileCallback(IUserProfileCallback callback);
    void unregisterUserProfileCallback(IUserProfileCallback callback);

    Bundle invokeDevCommand(String command, in Bundle args);
}