package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarService;

import java.util.ArrayList;

public class ControllerManager extends ControllerManagerBase {
    private ClimateController mClimateController = null;
    private DateController mDataController = null;
    private SystemStatusController mSystemController = null;
    private UserProfileController mUserProfileController = null;

    @Override
    public void create(Context context, View panel) {
        if ( (panel == null) || (context == null) ) return;
        
        mClimateController = new ClimateController(context, panel.findViewById(R.id.layout_climate));
        mDataController = new DateController(context, panel.findViewById(R.id.layout_date));
        mSystemController = new SystemStatusController(context, panel.findViewById(R.id.layout_system));
        mUserProfileController = new UserProfileController(context, panel.findViewById(R.id.layout_userprofile));
    }

    @Override
    public void init(StatusBarService service) {
        if ( service == null ) return;
        if ( mClimateController != null ) mClimateController.init(service.getStatusBarClimate()); 
        if ( mDataController != null ) mDataController.init(service.getStatusBarSystem()); 
        if ( mSystemController != null ) mSystemController.init(service.getStatusBarSystem()); 
        if ( mUserProfileController != null ) mUserProfileController.init(service.getStatusBarSystem()); 
    }

    @Override
    public void deinit() {  
        if ( mClimateController != null ) mClimateController.deinit(); 
        if ( mDataController != null ) mDataController.deinit(); 
        if ( mSystemController != null ) mSystemController.deinit(); 
        if ( mUserProfileController != null ) mUserProfileController.deinit();  
        mClimateController = null;
        mDataController = null;
        mSystemController = null;
        mUserProfileController = null;
    }
}
