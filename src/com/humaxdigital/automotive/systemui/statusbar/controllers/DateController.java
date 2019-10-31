package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.Context;
import android.view.View;

import android.os.Handler;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.DateView;

import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable;
import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarSystem;

import android.util.Log; 


public class DateController {
    private static String TAG = "DateController"; 
    private View mParentView; 
    private Context mContext;
    private DateView mDateVew;
    private DateView mDateNoonView;
    private StatusBarSystem mService; 
    private String mTime = ""; 
    private String mType = "12";
    private Boolean mIsValidTime = true; 

    private Handler mHandler; 

    public DateController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view;
        mHandler = new Handler(mContext.getMainLooper());
    }

    public void init(StatusBarSystem service) {
        if ( service == null ) return;
        mService = service; 
        mService.registerSystemCallback(mDateTimeCallback); 
        if ( mService.isDateTimeInitialized() ) initView(); 
    }

    public void deinit() {
        if ( mService == null ) return;
        mService.unregisterSystemCallback(mDateTimeCallback); 
    }

    private void initView() {
        if ( mParentView == null || mService == null ) return;
        mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mService == null ) return;
                if ( mIsValidTime ) 
                    mService.openDateTimeSetting(); 
            }
        });

        mDateVew = mParentView.findViewById(R.id.text_date_time);
        mDateNoonView = mParentView.findViewById(R.id.text_date_noon);
        
        mTime = mService.getDateTime(); 
        mType = mService.getTimeType();
        
        updateClockUI(mTime, mType);
    }

    private Boolean isValidTime(String time) {
        if ( time.contains("error") ) return false;
        return true; 
    }

    private void updateClockUI(String time, String type) {
        if ( mDateVew == null || mDateNoonView == null 
            || type == null || time == null ) return;
        
        if ( !isValidTime(time) ) {
            mIsValidTime = false;
            mDateVew.setText("-- : --");
            mDateNoonView.setText("");
            return;
        }

        mIsValidTime = true;

        String date = "";
        String noon = "";

        if ( type.equals("24") ) {
            time = time.trim(); 
            mDateVew.setText(time);
            mDateNoonView.setText("");
        } else {
            if ( time.contains("AM") ) {
                date = time.substring(0, time.indexOf("AM"));
                date = date.trim();
                noon = "AM";
            }
            else if ( time.contains("PM") ) {
                date = time.substring(0, time.indexOf("PM"));
                date = date.trim();
                noon = "PM";
            }
            mDateVew.setText(date);
            mDateNoonView.setText(noon);
        }
        Log.d(TAG, "time="+time+", type="+type+", date="+date+", noon="+noon); 
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private final StatusBarSystem.StatusBarSystemCallback mDateTimeCallback = new StatusBarSystem.StatusBarSystemCallback() {
        @Override
        public void onDateTimeInitialized() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    initView(); 
                }
            });  
        }
        @Override
        public void onDateTimeChanged(String time) {
            if ( mHandler == null ) return; 
            mTime = time; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateClockUI(mTime, mType);
                }
            }); 
        }

        @Override
        public void onTimeTypeChanged(String type) {
            if ( mHandler == null ) return; 
            mType = type; 
            mTime = mService.getDateTime(); 

            Log.d(TAG, "onTimeTypeChanged:type="+type+", time="+mTime);
            
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateClockUI(mTime, mType);
                }
            }); 
        }
    };
}
