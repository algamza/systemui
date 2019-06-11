package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.view.View;
import android.os.Handler;
import android.graphics.Bitmap;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.UserProfileView;

import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarSystem;
import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable; 

import com.humaxdigital.automotive.systemui.util.OSDPopup; 

public class UserProfileController {
    private Context mContext;
    private View mParentView; 
    private UserProfileView mUserProfileView;
    private StatusBarSystem mService; 
    private Handler mHandler; 

    public UserProfileController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view; 
        mHandler = new Handler(mContext.getMainLooper());
    }

    public void init(StatusBarSystem service) {
        if ( service == null ) return;
        mService = service; 
        mService.registerSystemCallback(mUserProfileCallback);
        if ( mService.isUserProfileInitialized() ) initView();
    }

    public void deinit() {
        if ( mService != null ) mService.unregisterSystemCallback(mUserProfileCallback);
    }

    private void initView() {
        if ( mParentView == null ) return;
        mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mService == null ) return;
                mService.openUserProfileSetting(); 
            }
        });

        mUserProfileView = mParentView.findViewById(R.id.img_useprofile);
        if ( mUserProfileView != null ) mUserProfileView.setImageBitmap(getUserBitmap()); 
    }

    private Bitmap getUserBitmap() {
        if ( mService == null ) return null; 
        Bitmap img = null; 
        img = mService.getUserProfileImage().getBitmap();
        return img; 
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private final StatusBarSystem.StatusBarSystemCallback mUserProfileCallback 
        = new StatusBarSystem.StatusBarSystemCallback() {
        
        @Override
        public void onUserProfileInitialized() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    initView();
                }
            });  
        }
        @Override
        public void onUserChanged(BitmapParcelable data) {
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ( mUserProfileView != null ) 
                        mUserProfileView.setImageBitmap(getUserBitmap()); 
                }
            }); 
        }
    };
}
