package com.humaxdigital.automotive.systemui.statusbar.controllers.dl3c;

import android.os.RemoteException;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.systemui.statusbar.controllers.SystemStatusController; 
import com.humaxdigital.automotive.systemui.statusbar.controllers.ControllerManagerBase; 

import java.util.ArrayList;

public class ControllerManagerDL3C extends ControllerManagerBase {

    private DateController mDataController = null;
    private SystemStatusController mSystemController = null;
    private ButtonController mButtonController = null;

    @Override
    public void create(Context context, View panel) {
        if ( (panel == null) || (context == null) ) return;
        mDataController = new DateController(context, panel.findViewById(R.id.layout_date));
        mSystemController = new SystemStatusController(context, panel.findViewById(R.id.layout_system));
        mButtonController = new ButtonController(context, panel.findViewById(R.id.layout_button)); 
    }

    @Override
    public void init(IStatusBarService service) {
        if ( service == null ) return;
        try {
            if ( mDataController != null ) mDataController.init(service.getStatusBarSystem()); 
            if ( mSystemController != null ) mSystemController.init(service.getStatusBarSystem()); 
            if ( mButtonController != null ) mButtonController.init(); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void deinit() {  
        if ( mDataController != null ) mDataController.deinit(); 
        if ( mSystemController != null ) mSystemController.deinit(); 
        if ( mButtonController != null ) mButtonController.deinit();  
        mDataController = null;
        mSystemController = null;
        mButtonController = null;
    }
}
