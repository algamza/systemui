package com.humaxdigital.automotive.statusbar.service;

import com.humaxdigital.automotive.statusbar.service.IStatusBarSystemCallback;
import com.humaxdigital.automotive.statusbar.service.BitmapParcelable;

interface IStatusBarSystem {
    boolean isInitialized();
    int getMuteStatus();
    int getBLEStatus();
    int getBTBatteryStatus();
    int getCallStatus();
    int getAntennaStatus();
    int getDataStatus();
    int getWifiStatus();
    int getWirelessChargeStatus();
    int getModeStatus();
    String getDateTime(); 
    String getTimeType();
    void openDateTimeSetting(); 
    BitmapParcelable getUserProfileImage(); 
    void openUserProfileSetting(); 
    void registerSystemCallback(IStatusBarSystemCallback callback);
    void unregisterSystemCallback(IStatusBarSystemCallback callback);
}