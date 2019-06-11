package com.humaxdigital.automotive.systemui.user;

import com.humaxdigital.automotive.systemui.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.user.IUserWifi;
import com.humaxdigital.automotive.systemui.user.IUserAudio;

interface IUserService {
    IUserBluetooth getUserBluetooth();
    IUserWifi getUserWifi();
    IUserAudio getUserAudio();
}