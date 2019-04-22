package com.humaxdigital.automotive.systemui.droplist.controllers;

import android.view.View;
import android.content.Context;

import com.humaxdigital.automotive.systemui.droplist.SystemControl;

public interface BaseController {
    public interface  Listener {
        void onClose(); 
    }
    BaseController init(View view);
    void fetch(SystemControl system);
    View getView();
    BaseController setListener(Listener listener); 
    void refresh(Context context); 
}
