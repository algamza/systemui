package com.humaxdigital.automotive.statusbar.service;

import com.humaxdigital.automotive.statusbar.service.BitmapParcelable;

oneway interface IStatusBarSystemCallback {
    void onInitialized();
    void onMuteStatusChanged(int status);
    void onBLEStatusChanged(int status);
    void onBTBatteryStatusChanged(int status);
    void onCallStatusChanged(int status);
    void onAntennaStatusChanged(int status);
    void onDataStatusChanged(int status);
    void onWifiStatusChanged(int status);
    void onWirelessChargeStatusChanged(int status);
    void onModeStatusChanged(int status);
    void onDateTimeChanged(String time); 
    void onTimeTypeChanged(String type);
    void onUserChanged(in BitmapParcelable bitmap); 
}