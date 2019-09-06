package com.humaxdigital.automotive.systemui.statusbar.controllers.dl3c;

import android.os.Handler;

import android.content.Context;
import android.content.res.Configuration;

import android.view.View;
import android.widget.TextView;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.DateView;

import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable;
import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarSystem;

import android.util.Log; 

import java.util.HashMap;

public class DateController {
    private static String TAG = "DateController"; 
    private View mParentView; 
    private Context mContext;

    private TextView mDateVew;
    private TextView mTimeView;
    private TextView mApmView;

    private String mTextDate = "";
    private String mTextTime = "";
    private String mTextApm = "";
    
    private StatusBarSystem mService; 
    private Boolean mIsValidTime = true; 
    private Handler mHandler; 

    private HashMap<String,Integer> mMonthStr = new HashMap<>();

    public DateController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view;
        mHandler = new Handler(mContext.getMainLooper());
        initMonthEng();
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

    public void configurationChange(Configuration newConfig) {
        if ( mDateVew == null || mContext == null ) return;
        updateDateTime();
        if ( mDateVew != null ) mDateVew.setText(mTextDate); 
    }

    private void initMonthEng() {
        mMonthStr.put("1", R.string.STR_JAN_04_ID); 
        mMonthStr.put("2", R.string.STR_FEB_04_ID); 
        mMonthStr.put("3", R.string.STR_MAR_04_ID); 
        mMonthStr.put("4", R.string.STR_APR_04_ID); 
        mMonthStr.put("5", R.string.STR_MAY_09_ID); 
        mMonthStr.put("6", R.string.STR_JUN_04_ID); 
        mMonthStr.put("7", R.string.STR_JUL_04_ID); 
        mMonthStr.put("8", R.string.STR_AUG_04_ID); 
        mMonthStr.put("9", R.string.STR_SEP_04_ID); 
        mMonthStr.put("10", R.string.STR_OCT_04_ID); 
        mMonthStr.put("11", R.string.STR_NOV_04_ID); 
        mMonthStr.put("12", R.string.STR_DEC_04_ID); 
    }

    private void initView() {
        if ( mParentView == null || mService == null ) return;
        mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mService == null ) return;
            }
        });

        mDateVew = mParentView.findViewById(R.id.text_date);
        mTimeView = mParentView.findViewById(R.id.text_time);
        mApmView = mParentView.findViewById(R.id.text_apm);

        updateDateTime(); 
        updateUI();
    }

    private void updateDateTime() {
        if ( mContext == null ) return;
        String format = ""; 
        format = mService.getYearDateTime(); 
        Log.d(TAG, "updateDateTime="+format); 
        String[] arr = format.split(":");
        if ( arr.length < 5 ) return;
        mTextDate = mContext.getResources().getString(mMonthStr.get(arr[1])) + " " + arr[2]; 
        mTextTime = arr[3]+":"+arr[4];  
        if ( arr.length == 6 ) mTextApm = arr[5]; 
        else if ( arr.length == 5 ) mTextApm = ""; 
    }

    private void updateUI() {
        if ( mDateVew != null ) mDateVew.setText(mTextDate); 
        if ( mTimeView != null ) mTimeView.setText(mTextTime); 
        if ( mApmView != null ) mApmView.setText(mTextApm);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private final StatusBarSystem.StatusBarSystemCallback mDateTimeCallback 
        = new StatusBarSystem.StatusBarSystemCallback() {
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
            Log.d(TAG, "onDateTimeChanged:"+time); 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateDateTime(); 
                    updateUI();
                }
            }); 
        }
        @Override
        public void onTimeTypeChanged(String type) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onTimeTypeChanged:"+type); 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateDateTime(); 
                    updateUI();
                }
            }); 
        }
    };
}
