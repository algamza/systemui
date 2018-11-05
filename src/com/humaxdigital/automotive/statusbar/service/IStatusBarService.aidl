package com.humaxdigital.automotive.statusbar.service;

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
    int getAntennaStatus();
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
    int getFanDirection();
    int getBlowerSpeed();
    int getPSSeatStatus();
    float getPSTemperature();
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
}