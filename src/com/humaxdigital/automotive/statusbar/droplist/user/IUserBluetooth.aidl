package com.humaxdigital.automotive.statusbar.droplist.user;

import com.humaxdigital.automotive.statusbar.droplist.user.IUserBluetoothCallback;

interface IUserBluetooth {
    void registCallback(IUserBluetoothCallback callback);
    void unregistCallback(IUserBluetoothCallback callback);
    boolean isEnabled(); 
    void setBluetoothEnable(boolean enable); 
}