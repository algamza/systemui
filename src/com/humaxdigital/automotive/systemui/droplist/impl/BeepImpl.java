package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.provider.Settings;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;

import android.util.Log;

public class BeepImpl extends BaseImplement<Boolean> {
    private static final String TAG = "BeepImpl"; 
    private ContentResolver mContentResolver;
    private ContentObserver mBeepObserver;

    public BeepImpl(Context context) {
        super(context);
    }

    @Override
    public void create() {
        init();
    }

    @Override
    public void destroy() {
        cleanup();
    }

    @Override
    public Boolean get() {
        boolean beepon = isBeepOn();
        Log.d(TAG, "get="+beepon);
        return beepon;
    }

    @Override
    public void set(Boolean e) {
        Log.d(TAG, "set="+e); 
        setBeepOn(e);
    }

    public void refresh() {
        if ( mContentResolver == null ) return; 
        if ( mBeepObserver != null )  {
            mContentResolver.unregisterContentObserver(mBeepObserver); 
        }
        Log.d(TAG, "refresh");
        mBeepObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SOUND_EFFECTS_ENABLED), 
            false, mBeepObserver, UserHandle.USER_CURRENT); 

        if ( mListener != null ) mListener.onChange(isBeepOn()); 
    }

    private void cleanup() {
        if ( mContentResolver != null ) 
            mContentResolver.unregisterContentObserver(mBeepObserver); 
        mBeepObserver = null;
        mContentResolver = null;
    }

    private void init() {
        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        mBeepObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SOUND_EFFECTS_ENABLED), 
            false, mBeepObserver, UserHandle.USER_CURRENT); 
    }


    private void setBeepOn(boolean on) {
        Log.d(TAG, "setBeepOn="+on);
        Settings.System.putIntForUser(mContext.getContentResolver(), 
            Settings.System.SOUND_EFFECTS_ENABLED,
            on?1:0, UserHandle.USER_CURRENT); 
    }

    private boolean isBeepOn() {
        int on = 0; 
        try {
            on = Settings.System.getIntForUser(mContext.getContentResolver(), 
                        Settings.System.SOUND_EFFECTS_ENABLED,
                        UserHandle.USER_CURRENT);
        } catch(Settings.SettingNotFoundException e) {
            Log.e(TAG, "error : " + e ); 
        }
        Log.d(TAG, "isBeepOn="+on);
        return on==0?false:true;
    }

    private ContentObserver createObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                Log.d(TAG, "onChange");
                if ( mListener != null ) 
                    mListener.onChange(isBeepOn()); 
            }
        };
        return observer; 
    }
}
