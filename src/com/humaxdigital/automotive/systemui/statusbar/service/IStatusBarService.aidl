package com.humaxdigital.automotive.systemui.statusbar.service;

import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarClimate;
import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarSystem;
import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarDev;

interface IStatusBarService {
    IStatusBarClimate getStatusBarClimate();
    IStatusBarSystem getStatusBarSystem();
    IStatusBarDev getStatusBarDev();
    boolean isUserAgreement();
    boolean isFrontCamera();
    boolean isRearCamera();
    boolean isPowerOff();
    boolean isEmergencyCall();
    boolean isBluelinkCall();
    boolean isBTCall();
}