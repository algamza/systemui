package com.humaxdigital.automotive.statusbar.controllers;

import android.os.RemoteException;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.service.IStatusBarService;

import java.util.ArrayList;

public class ControllerManager {
    private ClimateController mClimateController = null;
    private DateController mDataController = null;
    private SystemStatusController mSystemController = null;
    private UserProfileController mUserProfileController = null;

    public ControllerManager(Context context, View panel) {
        if ( (panel == null) || (context == null) ) return;
        
        mClimateController = new ClimateController(context, panel.findViewById(R.id.layout_climate));
        mDataController = new DateController(context, panel.findViewById(R.id.layout_date));
        mSystemController = new SystemStatusController(context, panel.findViewById(R.id.layout_system));
        mUserProfileController = new UserProfileController(context, panel.findViewById(R.id.layout_userprofile));
    }

    public void init(IStatusBarService service) {
        if ( service == null ) return;
        try {
            if ( mClimateController != null ) mClimateController.init(service.getStatusBarClimate()); 
            if ( mDataController != null ) mDataController.init(service.getStatusBarSystem()); 
            if ( mSystemController != null ) mSystemController.init(service.getStatusBarSystem()); 
            if ( mUserProfileController != null ) mUserProfileController.init(service.getStatusBarSystem()); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

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
