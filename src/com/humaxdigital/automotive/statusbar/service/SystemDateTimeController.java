package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.DateFormat;


public class SystemDateTimeController extends BaseController<String> {
    
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
        mContext.registerReceiver(mDateTimeChangedReceiver, filter);
    }

    @Override
    public void disconnect() {
        if ( mContext != null ) mContext.unregisterReceiver(mDateTimeChangedReceiver);
    }

    @Override
    public void fetch() {
        if ( mDataStore == null ) return;
        mDataStore.setDateTime(getCurrentTime());
    }

    @Override
    public String get() {
        if ( mDataStore == null ) return ""; 
        return mDataStore.getDateTime(); 
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
        DateFormat df = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        return df.format(Calendar.getInstance().getTime());
    }
}
