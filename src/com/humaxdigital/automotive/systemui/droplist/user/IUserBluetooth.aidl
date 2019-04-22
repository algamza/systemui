package com.humaxdigital.automotive.systemui.droplist.user;

import com.humaxdigital.automotive.systemui.droplist.user.IUserBluetoothCallback;

interface IUserBluetooth {
    void registCallback(IUserBluetoothCallback callback);
    void unregistCallback(IUserBluetoothCallback callback);
    boolean isEnabled(); 
    void setBluetoothEnable(boolean enable); 
}