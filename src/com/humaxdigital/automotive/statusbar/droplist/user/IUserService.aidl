package com.humaxdigital.automotive.statusbar.droplist.user;

import com.humaxdigital.automotive.statusbar.droplist.user.IUserBluetooth;
import com.humaxdigital.automotive.statusbar.droplist.user.IUserWifi;
import com.humaxdigital.automotive.statusbar.droplist.user.IUserAudio;

interface IUserService {
    IUserBluetooth getUserBluetooth();
    IUserWifi getUserWifi();
    IUserAudio getUserAudio();
}