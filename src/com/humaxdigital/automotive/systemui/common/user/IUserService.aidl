package com.humaxdigital.automotive.systemui.common.user;

import com.humaxdigital.automotive.systemui.common.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.common.user.IUserWifi;
import com.humaxdigital.automotive.systemui.common.user.IUserAudio;

interface IUserService {
    IUserBluetooth getUserBluetooth();
    IUserWifi getUserWifi();
    IUserAudio getUserAudio();
}