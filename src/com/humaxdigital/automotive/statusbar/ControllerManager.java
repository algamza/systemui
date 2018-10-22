package com.humaxdigital.automotive.statusbar;

import android.content.Context;
import android.util.Log;
import android.view.View;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.IStatusBarService;

import com.humaxdigital.automotive.statusbar.controllers.BaseController;
import com.humaxdigital.automotive.statusbar.controllers.ClimateController;
import com.humaxdigital.automotive.statusbar.controllers.DateController;
import com.humaxdigital.automotive.statusbar.controllers.SystemStatusController;
import com.humaxdigital.automotive.statusbar.controllers.UserProfileController;

import java.util.ArrayList;

public class ControllerManager {
    
    ArrayList<BaseController> mControllers = new ArrayList<>();

    public ControllerManager(Context context, View panel) {
        if ( (panel == null) || (context == null) ) return;
        mControllers.add(new ClimateController(context, panel.findViewById(R.id.layout_climate)));
        mControllers.add(new DateController(context, panel.findViewById(R.id.layout_date)));
        mControllers.add(new SystemStatusController(context, panel.findViewById(R.id.layout_system)));
        mControllers.add(new UserProfileController(context, panel.findViewById(R.id.layout_userprofile)));
    }

    public void init(IStatusBarService service) {
        if ( service == null ) return;
        for ( BaseController controller : mControllers ) {
            controller.init(service); 
        }
    }

    public void deinit() {
        for ( BaseController controller : mControllers ) {
            controller.deinit(); 
        }
    }

    public void update() {
        for ( BaseController controller : mControllers ) {
            controller.update(); 
        }
    }
}
