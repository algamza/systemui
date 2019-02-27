package com.humaxdigital.automotive.statusbar.user;

import com.humaxdigital.automotive.statusbar.user.IUserBluetooth;
import com.humaxdigital.automotive.statusbar.user.IUserWifi;
import com.humaxdigital.automotive.statusbar.user.IUserAudio;

interface IUserService {
    IUserBluetooth getUserBluetooth();
    IUserWifi getUserWifi();
    IUserAudio getUserAudio();
}