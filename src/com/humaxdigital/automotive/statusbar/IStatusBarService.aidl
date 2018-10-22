package com.humaxdigital.automotive.statusbar;

import com.humaxdigital.automotive.statusbar.ISystemCallback;
import com.humaxdigital.automotive.statusbar.IClimateCallback;
import com.humaxdigital.automotive.statusbar.IStatusBarCallback;

interface IStatusBarService {
    void registerStatusBarCallback(IStatusBarCallback callback);
    void unregisterStatusBarCallback(IStatusBarCallback callback);

    String getSystemDateTime(); 
    void registerSystemCallback(ISystemCallback callback);
    void unregisterSystemCallback(ISystemCallback callback);

    float getClimateDRTemperature(); 
    void registerClimateCallback(IClimateCallback callback);
    void unregisterClimateCallback(IClimateCallback callback);
}