package com.humaxdigital.automotive.systemui.statusbar.user;

import com.humaxdigital.automotive.systemui.statusbar.user.IUserWifiCallback;

interface IUserWifi {
    void registCallback(IUserWifiCallback callback);
    void unregistCallback(IUserWifiCallback callback);
    boolean isEnabled(); 
    boolean isConnected();
    int getRssi(); 
}