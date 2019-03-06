package com.humaxdigital.automotive.statusbar.service;

import com.humaxdigital.automotive.statusbar.service.IStatusBarClimate;
import com.humaxdigital.automotive.statusbar.service.IStatusBarSystem;
import com.humaxdigital.automotive.statusbar.service.IStatusBarDev;

interface IStatusBarService {
    IStatusBarClimate getStatusBarClimate();
    IStatusBarSystem getStatusBarSystem();
    IStatusBarDev getStatusBarDev();
}