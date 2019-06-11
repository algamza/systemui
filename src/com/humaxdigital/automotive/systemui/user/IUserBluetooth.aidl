package com.humaxdigital.automotive.systemui.user;

import com.humaxdigital.automotive.systemui.user.IUserBluetoothCallback;

interface IUserBluetooth {
    void registCallback(IUserBluetoothCallback callback);
    void unregistCallback(IUserBluetoothCallback callback);
    int getBatteryLevel(); 
    int getAntennaLevel(); 
    int getContactsDownloadState(); 
    int getCallHistoryDownloadState(); 
    boolean isHeadsetDeviceConnected();
    boolean isA2dpDeviceConnected();
    boolean isBluetoothEnabled(); 
    boolean isEnabled(); 
    void setBluetoothEnable(boolean enable); 
}