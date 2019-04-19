package com.humaxdigital.automotive.statusbar.droplist.user;

import com.humaxdigital.automotive.statusbar.droplist.user.IUserWifiCallback;

interface IUserWifi {
    void registCallback(IUserWifiCallback callback);
    void unregistCallback(IUserWifiCallback callback);
    boolean isEnabled(); 
    void setWifiEnable(boolean enable); 
}