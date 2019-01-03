package com.humaxdigital.automotive.statusbar.service;

oneway interface ISystemCallback {
        void onMuteStatusChanged(int status);
        void onBLEStatusChanged(int status);
        void onBTBatteryStatusChanged(int status);
        void onBTCallStatusChanged(int status);
        void onBTAntennaStatusChanged(int status);
        void onTMSAntennaStatusChanged(int status);
        void onDataStatusChanged(int status);
        void onWifiStatusChanged(int status);
        void onWirelessChargeStatusChanged(int status);
        void onModeStatusChanged(int status);
}