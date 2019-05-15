package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.Handler;
import android.os.UserHandle;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.provider.Settings;
import android.database.ContentObserver;

import android.os.Build;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date; 
import java.text.SimpleDateFormat;
import java.text.DateFormat;


public class SystemDateTimeController extends BaseController<String> {
    private static final String TAG = "SystemDateTimeController"; 
    private ContentResolver mContentResolver;
    private ContentObserver mObserver;
    private List<SystemTimeTypeListener> mTimeTypeListeners = new ArrayList<>();
    private TimeType mCurrentTimeType = TimeType.TYPE_12; 

    public enum TimeType {
        TYPE_12,
        TYPE_24
    }
    public interface SystemTimeTypeListener {
        void onTimeTypeChanged(String type); 
    }
    public SystemDateTimeController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        mContext.registerReceiverAsUser(mDateTimeChangedReceiver, UserHandle.ALL, filter, null, null);

        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        mObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.TIME_12_24), 
            false, mObserver, UserHandle.USER_CURRENT); 
        //checkValidTime();
    }

    @Override
    public void disconnect() {
        if ( mContext != null ) mContext.unregisterReceiver(mDateTimeChangedReceiver);
        
        if ( mContentResolver != null ) 
        mContentResolver.unregisterContentObserver(mObserver); 
        mObserver = null;
        mContentResolver = null;
    }

    @Override
    public void fetch() {
        if ( mDataStore == null ) return;
        if ( getTimeType().equals("12") ) {
            mCurrentTimeType = TimeType.TYPE_12; 
        } else if ( getTimeType().equals("24") ) {
            mCurrentTimeType = TimeType.TYPE_24; 
        }
        mDataStore.setDateTime(getCurrentTime());
    }

    @Override
    public String get() {
        if ( mDataStore == null ) return ""; 
        return mDataStore.getDateTime(); 
    }

    public void addTimeTypeListener(SystemTimeTypeListener listener) {
        mTimeTypeListeners.add(listener); 
    }

    public void removeTimeTypeListener(SystemTimeTypeListener listener) {
        if ( mTimeTypeListeners.isEmpty() ) return;
        mTimeTypeListeners.remove(listener);
    }

    public void refresh() {
        if ( mContentResolver == null ) return; 
        if ( mObserver != null )  {
            mContentResolver.unregisterContentObserver(mObserver); 
        }
        Log.d(TAG, "refresh");
        mObserver = createObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.TIME_12_24), 
            false, mObserver, UserHandle.USER_CURRENT); 

        String type = getTimeType();
        String time = getCurrentTime(); 
        if ( mDataStore != null ) 
            mDataStore.shouldPropagateDateTimeUpdate(time);
        for ( SystemTimeTypeListener listener : mTimeTypeListeners ) {
            listener.onTimeTypeChanged(type); 
        }
    }

    public String getTimeType() {
        String type = "12"; 
        if ( mContext == null ) return type;
        
        String _type = Settings.System.getStringForUser(mContext.getContentResolver(), 
                    Settings.System.TIME_12_24,
                    UserHandle.USER_CURRENT);

        if ( _type != null ) type = _type; 

        if ( type.equals("12") ) {
            mCurrentTimeType = TimeType.TYPE_12; 
        } else if ( type.equals("24") ) {
            mCurrentTimeType = TimeType.TYPE_24; 
        }

        Log.d(TAG, "getTimeType:type="+type);
        return type;
    }

    private final BroadcastReceiver mDateTimeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String date = getCurrentTime(); 
            boolean shouldPropagate = mDataStore.shouldPropagateDateTimeUpdate(date);
            if ( shouldPropagate ) {
                for ( Listener<String> listener : mListeners ) 
                    listener.onEvent(date);
            }
        }
    };

    private String getCurrentTime() {

        DateFormat df; 
        if ( mCurrentTimeType == TimeType.TYPE_24 ) {
            df = new SimpleDateFormat("H:mm", Locale.ENGLISH);
        } else {
            df = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        }
        return df.format(Calendar.getInstance().getTime());
    }

    private boolean checkValidTime() {
        DateFormat df = new SimpleDateFormat("yyy:MM:dd:hh:mm:ss a", Locale.ENGLISH);
        String date = df.format(Calendar.getInstance().getTime());
        Log.d(TAG, "current time:"+date);
        checkBuildTime();
        return true;
    }

    private void checkBuildTime() {
        long time = Build.TIME;
        Date date = new Date(time); 
        DateFormat df = new SimpleDateFormat("yyy:MM:dd:hh:mm:ss a", Locale.ENGLISH);
        String date1 = df.format(date);
        Log.d(TAG, "build time :"+date1);
    }

    private ContentObserver createObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                String type = getTimeType();
                String time = getCurrentTime(); 
                Log.d(TAG, "onChange:type="+type+", time="+time);
                if ( mDataStore != null ) 
                    mDataStore.shouldPropagateDateTimeUpdate(time);
                for ( SystemTimeTypeListener listener : mTimeTypeListeners ) {
                    listener.onTimeTypeChanged(type); 
                }
            }
        };
        return observer; 
    }
}
