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

import android.util.Log; 
import java.util.ArrayList;
import java.util.List;

public class UserProfileClient {
    private final String TAG = "UserProfileClient"; 
    private Context mContext; 
    private int mCurrentUserID = 0; 
    private UserManager mUserManager; 
    private ActivityManager mActivityManager;
    private List<UserChangeListener> mListeners = new ArrayList<>(); 

    public interface UserChangeListener {
        void onUserChanged(int userid); 
    }

    public UserProfileClient(Context context) {
        if ( context == null ) return;
        mContext = context; 
        mUserManager = (UserManager)mContext.getSystemService(Context.USER_SERVICE); 
        mActivityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE); 
    }

    public void connect() {
        if ( mContext == null ) return;
        IntentFilter filter = new IntentFilter();
        //filter.addAction(Intent.ACTION_USER_REMOVED);
        //filter.addAction(Intent.ACTION_USER_ADDED);
        //filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        //filter.addAction(Intent.ACTION_USER_STOPPED);
        //filter.addAction(Intent.ACTION_USER_UNLOCKED);
        mContext.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    public void disconnect() {
        mListeners.clear(); 
        if ( mContext != null ) 
            mContext.unregisterReceiver(mUserChangeReceiver);
    }

    public void registerCallback(UserChangeListener listener) {
        if ( listener == null ) return; 
        mListeners.add(listener); 
    }

    public void unregisterCallback(UserChangeListener listener) {
        if ( listener == null ) return; 
        mListeners.remove(listener);
    }

    public int getCurrentUserID() {
        if ( mUserManager == null || mActivityManager == null ) return 0; 
        UserInfo user = mUserManager.getUserInfo(mActivityManager.getCurrentUser());
        if ( user == null ) return 0; 
        mCurrentUserID = user.id; 
        Log.d(TAG, "get current user : id = " + mCurrentUserID ); 
        return mCurrentUserID; 
    }

    public Bitmap getUserBitmap(int id) {
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
            UserInfo user = mUserManager.getUserInfo(mActivityManager.getCurrentUser());
            if ( user == null ) return; 
            if ( user.id == mCurrentUserID ) return;
            Log.d(TAG, "user changed : old user = " + mCurrentUserID + ", new user = " +  user.id); 
            mCurrentUserID = user.id; 
            for ( UserChangeListener listener : mListeners ) {
                listener.onUserChanged(mCurrentUserID);
            }
        }
    };

    public UserHandle getUserHandle(int id) {
        if ( mUserManager == null ) return null; 
        Log.d(TAG, "getUserHandle:id="+id); 
        UserInfo user = mUserManager.getUserInfo(id);
        if ( user == null ) return null; 
        return user.getUserHandle(); 
    }
}
