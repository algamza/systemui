package com.humaxdigital.automotive.systemui.statusbar.service;

import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarSystemCallback;
import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable;

interface IStatusBarSystem {
    boolean isSystemInitialized();
    boolean isUserProfileInitialized();
    boolean isDateTimeInitialized();
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