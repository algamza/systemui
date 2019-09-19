package com.humaxdigital.automotive.systemui.common.user;

import com.humaxdigital.automotive.systemui.common.user.IUserWifiCallback;

interface IUserWifi {
    void registCallback(IUserWifiCallback callback);
    void unregistCallback(IUserWifiCallback callback);
    boolean isEnabled(); 
    boolean isConnected();
    int getRssi(); 
    void setWifiEnable(boolean enable); 
}