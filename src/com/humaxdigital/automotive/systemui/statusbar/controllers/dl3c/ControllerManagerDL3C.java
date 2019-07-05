package com.humaxdigital.automotive.systemui.statusbar.controllers.dl3c;

import android.os.Handler;
import android.os.UserHandle;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.net.Uri;
import android.provider.Settings;
import android.content.ContentResolver;
import android.database.ContentObserver;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarService;
import com.humaxdigital.automotive.systemui.statusbar.controllers.SystemStatusController; 
import com.humaxdigital.automotive.systemui.statusbar.controllers.ControllerManagerBase; 
import com.humaxdigital.automotive.systemui.statusbar.controllers.UserProfileController; 

import java.util.ArrayList;

public class ControllerManagerDL3C extends ControllerManagerBase {
    final static String KEY_IS_HOME_SCREEN = "com.humaxdigital.dn8c.KEY_IS_HOME_SCREEN";
    private DateController mDataController = null;
    private SystemStatusController mSystemController = null;
    private ButtonController mButtonController = null;
    private UserProfileController mUserProfileController = null; 
    private View mButtonView; 
    private View mUserProfileView; 
    private ContentResolver mContentResolver;
    

    @Override
    public void create(Context context, View panel) {
        if ( (panel == null) || (context == null) ) return;
        mDataController = new DateController(context, panel.findViewById(R.id.layout_date));
        mSystemController = new SystemStatusController(context, panel.findViewById(R.id.layout_system));
        mButtonView = panel.findViewById(R.id.layout_button); 
        mUserProfileView = panel.findViewById(R.id.layout_userprofile); 
        mButtonController = new ButtonController(context, mButtonView); 
        mUserProfileController = new UserProfileController(context, mUserProfileView);

        mContentResolver = context.getContentResolver();
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(KEY_IS_HOME_SCREEN), 
            false, mObserver, UserHandle.USER_ALL); 

        updateUI(); 
    }

    @Override
    public void init(StatusBarService service) {
        if ( service == null ) return;
        if ( mDataController != null ) mDataController.init(service.getStatusBarSystem()); 
        if ( mSystemController != null ) mSystemController.init(service.getStatusBarSystem()); 
        if ( mButtonController != null ) mButtonController.init(); 
        if ( mUserProfileController != null ) mUserProfileController.init(service.getStatusBarSystem()); 
    }

    @Override
    public void deinit() {  
        if ( mDataController != null ) mDataController.deinit(); 
        if ( mSystemController != null ) mSystemController.deinit(); 
        if ( mButtonController != null ) mButtonController.deinit();  
        if ( mUserProfileController != null ) mUserProfileController.deinit();  
        mDataController = null;
        mSystemController = null;
        mButtonController = null;
        mUserProfileController = null;
    }

    private void updateUI() {
        if ( mUserProfileView == null || mButtonView == null ) return;
        int home_screen = Settings.Global.getInt(mContentResolver, KEY_IS_HOME_SCREEN, 1);
        if ( home_screen == 1 ) {
            mUserProfileView.setVisibility(View.VISIBLE);
            mButtonView.setVisibility(View.GONE);
        } else {
            mUserProfileView.setVisibility(View.GONE);
            mButtonView.setVisibility(View.VISIBLE);
        }
    }

    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri, int userId) {
            updateUI();
        }
    };
}
