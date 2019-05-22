package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.Context;
import android.view.View;

import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarService;

public abstract class ControllerManagerBase {
    public void create(Context context, View panel) {}
    public void init(IStatusBarService service) {}
    public void deinit() {}
}
