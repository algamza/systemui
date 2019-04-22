package com.humaxdigital.automotive.systemui.statusbar.user;

oneway interface IUserWifiCallback {
    void onWifiEnableChanged(boolean enable); 
    void onWifiConnectionChanged(boolean connected);
    void onWifiRssiChanged(int rssi);
}