package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.content.pm.UserInfo;
import com.android.internal.util.UserIcons;
import android.graphics.Bitmap;

public class SystemUserProfileController extends BaseController<Bitmap> {
    private UserManager mUserManager; 
    private ActivityManager mActivityManager;

    public SystemUserProfileController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
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
    public void disconnect() {
        if ( mContext != null ) mContext.unregisterReceiver(mUserChangeReceiver);
    }

    @Override
    public void fetch() {
        mDataStore.setStatusUserId(getCurrentUserId()); 
    }

    @Override
    public Bitmap get() {
        return getUserBitmap(); 
    }
    
    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mUserManager == null || mActivityManager == null ) return;
            int userid = getCurrentUserId(); 
            boolean shouldPropagate = mDataStore.shouldPropagateStatusUserIdUpdate(userid);
            if ( shouldPropagate ) {
                for ( Listener<Bitmap> listener : mListeners ) 
                    listener.onEvent(getUserBitmap());
            }
        }
    };

    private Bitmap getUserBitmap() {
        if ( mUserManager == null || mContext == null || mDataStore == null ) return null;
        int userid = mDataStore.getStatusUserId(); 
        Bitmap bm = mUserManager.getUserIcon(userid);
        if ( bm == null ) {
            bm = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(
                mContext.getResources(), userid, false));
        }
        return bm; 
    }

    private int getCurrentUserId() {
        if ( mUserManager == null || mActivityManager == null ) return 0; 
        UserInfo user = mUserManager.getUserInfo(mActivityManager.getCurrentUser());
        if ( user == null ) return 0; 
        return user.id; 
    }
}
