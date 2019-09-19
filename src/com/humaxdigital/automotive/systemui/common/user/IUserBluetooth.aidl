package com.humaxdigital.automotive.systemui.common.user;

import com.humaxdigital.automotive.systemui.common.user.IUserBluetoothCallback;

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