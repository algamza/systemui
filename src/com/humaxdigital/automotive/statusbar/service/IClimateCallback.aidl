package com.humaxdigital.automotive.statusbar.service;

oneway interface IClimateCallback {
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
    void onIGNOnChanged(boolean on); 
}