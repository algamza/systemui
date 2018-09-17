package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.humaxdigital.automotive.statusbar.R;

public class PanelController {
    private ClimateController mClimate;
    private DateController mDate;
    private SystemStatusController mSystem;
    private UserProfileController mUser;

    public PanelController(Context context, View panel) {
        initControllers(context, panel);
    }

    private void initControllers(Context context, View panel) {

        if ( panel == null ) return;
        mClimate = new ClimateController(context, panel.findViewById(R.id.layout_climate));
        mDate = new DateController(context, panel.findViewById(R.id.layout_date));
        mSystem = new SystemStatusController(context, panel.findViewById(R.id.layout_system));
        mUser = new UserProfileController(context, panel.findViewById(R.id.layout_userprofile));
    }
}
