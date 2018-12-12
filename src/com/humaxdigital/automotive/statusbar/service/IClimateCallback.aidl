package com.humaxdigital.automotive.statusbar.service;

oneway interface IClimateCallback {
    void onDRTemperatureChanged(float temp);
    void onDRSeatStatusChanged(int status);
    void onAirCirculationChanged(boolean isOn);
    void onAirConditionerChanged(boolean isOn);
    void onFanDirectionChanged(int direction);
    void onBlowerSpeedChanged(int status);
    void onPSSeatStatusChanged(int status);
    void onPSTemperatureChanged(float temp);
    void onIGNOnChanged(boolean on); 
}