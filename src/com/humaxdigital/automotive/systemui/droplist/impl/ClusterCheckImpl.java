package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;

import android.extension.car.settings.CarExtraSettings;

public class ClusterCheckImpl extends BaseImplement<Boolean> {
    private final String TAG = "ClusterCheckImpl"; 
   
    private ContentResolver mContentResolver;
    private ContentObserver mObserver; 

    public ClusterCheckImpl(Context context) {
        super(context); 
    }

    @Override
    public void create() {
        createObserver();
    }

    @Override
    public void destroy() {
        removeObserver();
        mListener = null;
    }

    @Override
    public Boolean get() {
        int check = getCurrentCheck();
        Log.d(TAG, "get="+check);
        return check == 1 ? true:false;
    }

    @Override
    public void set(Boolean e) {
        Log.d(TAG, "set="+e);
        Settings.System.putIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_LINK_CLUSTER, 
            e?1:0, UserHandle.USER_CURRENT);
    }

    private void createObserver() {
        mContentResolver = mContext.getContentResolver();
        mObserver = createCheckObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.DISPLAY_BRIGHTNESS_LINK_CLUSTER), 
            false, mObserver, UserHandle.USER_CURRENT); 
    }

    private void removeObserver() {
        if ( mContentResolver != null ) {
            if ( mObserver != null ) mContentResolver.unregisterContentObserver(mObserver);
        }
        mObserver = null;
        mContentResolver = null;
    }

    private ContentObserver createCheckObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                int check = getCurrentCheck(); 
                Log.d(TAG, "createObserver:onChange="+check);
                if ( mListener != null ) mListener.onChange(check==1?true:false); 
            }
        };
        return observer; 
    }

    private int getCurrentCheck() {
        int check = Settings.System.getIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_LINK_CLUSTER, 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_LINK_CLUSTER_DEFAULT, 
            UserHandle.USER_CURRENT);
        Log.d(TAG, "getCurrentCheck="+check);
        return check; 
    }

    public void refresh() {
        Log.d(TAG, "refresh"); 
        removeObserver();
        createObserver(); 
        int check = getCurrentCheck();
        if ( mListener != null ) mListener.onChange(check==1?true:false); 
    }
}
