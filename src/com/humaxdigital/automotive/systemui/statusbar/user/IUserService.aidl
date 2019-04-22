package com.humaxdigital.automotive.systemui.statusbar.user;

import com.humaxdigital.automotive.systemui.statusbar.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.statusbar.user.IUserWifi;
import com.humaxdigital.automotive.systemui.statusbar.user.IUserAudio;

interface IUserService {
    IUserBluetooth getUserBluetooth();
    IUserWifi getUserWifi();
    IUserAudio getUserAudio();
}