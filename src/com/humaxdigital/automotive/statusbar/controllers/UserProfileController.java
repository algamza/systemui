package com.humaxdigital.automotive.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;

import android.os.UserHandle;
import android.os.UserManager;
import android.app.ActivityManager;
import android.content.pm.UserInfo;
import com.android.internal.util.UserIcons;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.util.Log;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.UserProfileView;

public class UserProfileController {
    private Context mContext;
    private UserProfileView mUserProfileView;
    private Drawable mUserImage;
    private UserManager mUserManager;
    private ActivityManager mActivityManager;

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUser();
        }
    };

    public UserProfileController(Context context, View view) {
        mContext = context;
        initUserProfile(); 
        initView(view);
    }
    private void initView(View view) {
        if ( view == null ) return;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent("android.car.settings.ADD_ACCOUNT_SETTINGS"); 
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                //if ( mContext != null ) mContext.startActivity(intent); 
            }
        });

        mUserProfileView = view.findViewById(R.id.img_useprofile);

        updateUser();
    }

    private void initUserProfile() {
        if ( mContext == null ) return;
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE); 
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE); 

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_REMOVED);
        filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        filter.addAction(Intent.ACTION_USER_STOPPED);
        filter.addAction(Intent.ACTION_USER_UNLOCKED);
        mContext.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if ( mContext != null ) mContext.unregisterReceiver(mUserChangeReceiver);
    }

    private void updateUser() {
        if ( (mUserManager == null) 
            || (mActivityManager == null) 
            || (mUserProfileView == null) ) 
            return; 
        
        UserInfo user = mUserManager.getUserInfo(mActivityManager.getCurrentUser());

        if ( user == null ) return; 
   
        Bitmap bm = mUserManager.getUserIcon(user.id);
        if ( bm == null ) 
        {
            bm = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(
                mContext.getResources(), user.id, false));
        }
        
        if ( bm != null ) mUserProfileView.setImageBitmap(bm); 
    }
}
