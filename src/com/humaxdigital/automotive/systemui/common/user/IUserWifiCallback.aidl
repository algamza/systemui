package com.humaxdigital.automotive.systemui.common.user;

oneway interface IUserWifiCallback {
    void onWifiEnableChanged(boolean enable); 
    void onWifiConnectionChanged(boolean connected);
    void onWifiRssiChanged(int rssi);
}