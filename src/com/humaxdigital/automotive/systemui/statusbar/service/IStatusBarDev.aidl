package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.Bundle;

interface IStatusBarDev {
    Bundle invokeDevCommand(String command, in Bundle args);
}