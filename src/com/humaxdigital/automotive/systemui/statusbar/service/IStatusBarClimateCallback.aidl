package com.humaxdigital.automotive.systemui.statusbar.service;

oneway interface IStatusBarClimateCallback {
    void onInitialized();
    void onDRTemperatureChanged(float temp);
    void onDRSeatStatusChanged(int status);
    void onAirCirculationChanged(boolean isOn);
    void onAirConditionerChanged(boolean isOn);
    void onAirCleaningChanged(int status);
    void onFanDirectionChanged(int direction);
    void onBlowerSpeedChanged(int status);
    void onPSSeatStatusChanged(int status);
    void onPSTemperatureChanged(float temp);
    void onFrontDefogStatusChanged(int status); 
    void onModeOffChanged(boolean off); 
    void onIGNOnChanged(boolean on); 
    void onOperateOnChanged(boolean on);
    void onRearCameraOn(boolean on);  
}