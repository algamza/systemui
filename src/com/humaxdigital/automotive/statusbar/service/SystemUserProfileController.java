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

public class SystemUserProfileController extends BaseController<Bitmap> {
    private final String TAG = "SystemUserProfileController"; 
    private UserProfileClient mClient; 

    public SystemUserProfileController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
    }

    @Override
    public void disconnect() {
    }

    public void fetch(UserProfileClient client) {
        if ( client == null ) return;
        mClient = client; 
        mClient.registerCallback(mUserChangeListener);
    }

    @Override
    public Bitmap get() {
        if ( mClient == null ) return null; 
        int id = mClient.getCurrentUserID(); 
        Log.d(TAG, "get:id=" + id); 
        return mClient.getUserBitmap(id);
    }

    private final UserProfileClient.UserChangeListener mUserChangeListener = 
        new UserProfileClient.UserChangeListener() {
        @Override
        public void onUserChanged(int userid) {
            if ( mClient == null ) return;
            for ( Listener<Bitmap> listener : mListeners ) 
                listener.onEvent(mClient.getUserBitmap(userid));
        }
    }; 
}
