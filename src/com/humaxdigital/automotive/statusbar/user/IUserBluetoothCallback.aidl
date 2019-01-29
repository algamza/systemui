package com.humaxdigital.automotive.statusbar.user;

interface IUserBluetoothCallback {
    void onBluetoothEnableChanged(int enable); 
    void onConnectionStateChanged(int profile, int state); 
    void onBatteryStateChanged(int level); 
    void onAntennaStateChanged(int level); 
    void onBluetoothCallingStateChanged(int state); 
    void onBluetoothMicMuteStateChanged(int state); 
    void onContactsDownloadStateChanged(int state); 
    void onCallHistoryDownloadStateChanged(int state); 
}