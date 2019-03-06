package com.humaxdigital.automotive.statusbar.service;

import com.humaxdigital.automotive.statusbar.service.IStatusBarClimateCallback;

interface IStatusBarClimate {
    boolean isInitialized();
    int getIGNStatus();
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
    void registerClimateCallback(IStatusBarClimateCallback callback);
    void unregisterClimateCallback(IStatusBarClimateCallback callback);
}