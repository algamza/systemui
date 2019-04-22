package com.humaxdigital.automotive.systemui.droplist.user;

import com.humaxdigital.automotive.systemui.droplist.user.IUserWifiCallback;

interface IUserWifi {
    void registCallback(IUserWifiCallback callback);
    void unregistCallback(IUserWifiCallback callback);
    boolean isEnabled(); 
    void setWifiEnable(boolean enable); 
}