package com.humaxdigital.automotive.systemui.statusbar.service;

import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarClimateCallback;

interface IStatusBarClimate {
    boolean isInitialized();
    int getIGNStatus();
    boolean isOperateOn(); 
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
    void setBlowerSpeed(int state);
    int getPSSeatStatus();
    float getPSTemperature();
    int getFrontDefogState();
    boolean isModeOff(); 
    void openClimateSetting();
    void registerClimateCallback(IStatusBarClimateCallback callback);
    void unregisterClimateCallback(IStatusBarClimateCallback callback);
}