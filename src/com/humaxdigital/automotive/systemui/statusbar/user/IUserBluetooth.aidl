package com.humaxdigital.automotive.systemui.statusbar.user;

import com.humaxdigital.automotive.systemui.statusbar.user.IUserBluetoothCallback;

interface IUserBluetooth {
    void registCallback(IUserBluetoothCallback callback);
    void unregistCallback(IUserBluetoothCallback callback);
    int getBatteryLevel(); 
    int getAntennaLevel(); 
    int getBluetoothCallingState(); 
    int getContactsDownloadState(); 
    int getCallHistoryDownloadState(); 
    boolean isHeadsetDeviceConnected();
    boolean isA2dpDeviceConnected();
    boolean isBluetoothEnabled(); 
}