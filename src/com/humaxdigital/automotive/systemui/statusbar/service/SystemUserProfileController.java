package com.humaxdigital.automotive.systemui.statusbar.service;

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
import android.util.Log; 

import java.util.ArrayList;
import java.util.List;

public class SystemUserProfileController extends BaseController<Bitmap> {
    private final String TAG = "SystemUserProfileController"; 
    private int mCurrentUserID = 0; 
    private UserManager mUserManager; 
    private ActivityManager mActivityManager;
    private List<UserChangeListener> mUserChangeListeners = new ArrayList<>(); 

    public interface UserChangeListener {
        void onUserChanged(int userid); 
    }

    public SystemUserProfileController(Context context, DataStore store) {
        super(context, store);
        mUserManager = (UserManager)mContext.getSystemService(Context.USER_SERVICE); 
        mActivityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE); 
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        IntentFilter filter = new IntentFilter();
        //filter.addAction(Intent.ACTION_USER_REMOVED);
        //filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        //filter.addAction(Intent.ACTION_USER_STOPPED);
        //filter.addAction(Intent.ACTION_USER_UNLOCKED);
        mContext.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    @Override
    public void disconnect() {
        mUserChangeListeners.clear(); 
        if ( mContext != null ) 
            mContext.unregisterReceiver(mUserChangeReceiver);
    }

    @Override
    public Bitmap get() {
        int id = getCurrentUserID(); 
        Log.d(TAG, "get:id=" + id); 
        return getUserBitmap(id);
    }

    public void registerUserChangeCallback(UserChangeListener listener) {
        if ( listener == null ) return; 
        mUserChangeListeners.add(listener); 
    }

    public void unregisterUserChangeCallback(UserChangeListener listener) {
        if ( listener == null ) return; 
        mUserChangeListeners.remove(listener);
    }

    private int getCurrentUserID() {
        if ( mUserManager == null || mActivityManager == null ) return 0; 
        UserInfo user = mUserManager.getUserInfo(mActivityManager.getCurrentUser());
        if ( user == null ) return 0; 
        mCurrentUserID = user.id; 
        Log.d(TAG, "get current user : id = " + mCurrentUserID ); 
        return mCurrentUserID; 
    }

    private Bitmap getUserBitmap(int id) {
        if ( mUserManager == null || mContext == null ) return null;
        Log.d(TAG, "getUserBitmap"); 
        Bitmap bm = mUserManager.getUserIcon(id);
        if ( bm == null ) {
            bm = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(
                mContext.getResources(), id, false));
        }
        return bm; 
    }

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mUserManager == null || mActivityManager == null ) return;
            switch(intent.getAction()) {
                case Intent.ACTION_USER_INFO_CHANGED: {
                    Log.d(TAG, "ACTION_USER_INFO_CHANGED"); 
                    UserInfo user = mUserManager.getUserInfo(mActivityManager.getCurrentUser());
                    if ( user == null ) return; 
                    for ( Listener<Bitmap> listener : mListeners ) 
                        listener.onEvent(getUserBitmap(user.id));
                    break;
                }
                case Intent.ACTION_USER_SWITCHED: {
                    Log.d(TAG, "ACTION_USER_SWITCHED"); 
                    UserInfo user = mUserManager.getUserInfo(mActivityManager.getCurrentUser());
                    if ( user == null ) return; 
                    if ( user.id == mCurrentUserID ) return;
                    Log.d(TAG, "user changed : old user = " + mCurrentUserID + ", new user = " +  user.id); 
                    mCurrentUserID = user.id; 
                    for ( UserChangeListener listener : mUserChangeListeners ) 
                        listener.onUserChanged(mCurrentUserID);
                    for ( Listener<Bitmap> listener : mListeners ) 
                        listener.onEvent(getUserBitmap(mCurrentUserID));
                    break;
                }
            }

        }
    };
}