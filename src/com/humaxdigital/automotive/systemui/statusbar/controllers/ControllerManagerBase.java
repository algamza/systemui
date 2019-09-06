package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.Context;
import android.view.View;
import android.content.res.Configuration;

import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarService;

public abstract class ControllerManagerBase {
    public void create(Context context, View panel) {}
    public void init(StatusBarService service) {}
    public void deinit() {}
    public void configurationChange(Configuration newConfig) {}
}