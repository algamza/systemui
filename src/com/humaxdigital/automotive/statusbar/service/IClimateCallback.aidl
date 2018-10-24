package com.humaxdigital.automotive.statusbar.service;

oneway interface IClimateCallback {
    void onDRTemperatureChanged(float temp);
    void onDRSeatStatusChanged(int status);
    void onIntakeStatusChanged(int status);
    void onClimateModeChanged(int status);
    void onBlowerSpeedChanged(int status);
    void onPSSeatStatusChanged(int status);
    void onPSTemperatureChanged(float temp);
}