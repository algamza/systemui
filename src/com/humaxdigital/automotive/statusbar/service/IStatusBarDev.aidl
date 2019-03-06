package com.humaxdigital.automotive.statusbar.service;

import android.os.Bundle;

interface IStatusBarDev {
    Bundle invokeDevCommand(String command, in Bundle args);
}