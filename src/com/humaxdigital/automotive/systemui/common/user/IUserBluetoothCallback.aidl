package com.humaxdigital.automotive.systemui.common.user;

interface IUserBluetoothCallback {
    void onBluetoothEnableChanged(int enable); 
    void onHeadsetConnectionStateChanged(int state); 
    void onA2dpConnectionStateChanged(int state); 
    void onBatteryStateChanged(int level); 
    void onAntennaStateChanged(int level); 
    void onBluetoothMicMuteStateChanged(int state); 
    void onContactsDownloadStateChanged(int state); 
    void onCallHistoryDownloadStateChanged(int state); 
}