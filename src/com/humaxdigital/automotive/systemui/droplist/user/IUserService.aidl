package com.humaxdigital.automotive.systemui.droplist.user;

import com.humaxdigital.automotive.systemui.droplist.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.droplist.user.IUserWifi;
import com.humaxdigital.automotive.systemui.droplist.user.IUserAudio;

interface IUserService {
    IUserBluetooth getUserBluetooth();
    IUserWifi getUserWifi();
    IUserAudio getUserAudio();
}