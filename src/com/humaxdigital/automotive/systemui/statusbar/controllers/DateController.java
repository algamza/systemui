package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.Context;
import android.view.View;

import android.os.Handler;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.DateView;

import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable;
import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarSystem;

import android.util.Log; 
import java.util.Objects; 

public class DateController implements StatusBarSystem.StatusBarSystemCallback {
    private static String TAG = "DateController"; 
    private View mParentView; 
    private Context mContext;
    private DateView mDateVew;
    private DateView mDateNoonView;
    private StatusBarSystem mService; 
    private String mTime = ""; 
    private String mType = "12";
    private Boolean mIsValidTime = true; 

    private boolean mIsPowerOff; 
    private boolean mUserAgreementMode;
    private boolean mUserSwitching; 
    private boolean mRearCameraMode; 

    private Handler mHandler; 

    public DateController(Context context, View view) {
        mContext = Objects.requireNonNull(context);
        mParentView = Objects.requireNonNull(view);
        mHandler = new Handler(mContext.getMainLooper());
    }

    public void init(StatusBarSystem service) {
        if ( service == null ) return;
        mService = service; 
        mService.registerSystemCallback(this); 
        if ( mService.isDateTimeInitialized() ) initView(); 
        checkSpecialCase();
    }

    public void deinit() {
        if ( mService == null ) return;
        mService.unregisterSystemCallback(this); 
    }

    private void initView() {
        mParentView.setOnClickListener(mOnClickListener);

        mDateVew = mParentView.findViewById(R.id.text_date_time);
        mDateNoonView = mParentView.findViewById(R.id.text_date_noon);
        
        if ( mService != null ) {
            mTime = mService.getDateTime(); 
            mType = mService.getTimeType();
        }
        
        updateClockUI(mTime, mType);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if ( mService == null ) return;
            if ( mIsValidTime ) 
                mService.openDateTimeSetting(); 
        }
    }; 

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

    private void checkSpecialCase() {
        if ( mService == null ) return;

        if ( mService.isPowerOff() ) mIsPowerOff = true;
        if ( mService.isUserAgreement() ) mUserAgreementMode = true;
        if ( mService.isUserSwitching() ) mUserSwitching = true;
        if ( mService.isRearCamera() ) mRearCameraMode = true;

        Log.d(TAG, "checkUserIconDisable:mIsPowerOff="+mIsPowerOff+
            ", mUserAgreementMode="+mUserAgreementMode+
            ", mUserSwitching="+mUserSwitching+
            ", mRearCameraMode="+mRearCameraMode); 
        if ( mIsPowerOff || mUserAgreementMode 
            || mUserSwitching || mRearCameraMode ) 
            setUsable(true);
        else 
            setUsable(false);
    }
    
    private void setUsable(boolean usable) {
        if ( usable ) {
            mParentView.setOnClickListener(null);
        } else {
            mParentView.setOnClickListener(mOnClickListener);
        }
    }


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

    @Override
    public void onPowerStateChanged(int state) {
        Log.d(TAG, "onPowerStateChanged="+state);
        mIsPowerOff = (state == 2)?true:false;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkSpecialCase(); 
            }
        }); 
    }

    @Override
    public void onUserAgreementMode(boolean on) {
        Log.d(TAG, "onUserAgreementMode="+on);
        mUserAgreementMode = on; 
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkSpecialCase(); 
            }
        }); 
    }

    @Override
    public void onUserSwitching(boolean on) {
        Log.d(TAG, "onUserSwitching="+on);
        mUserSwitching = on; 
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkSpecialCase(); 
            }
        }); 
    }

    @Override
    public void onRearCamera(boolean on) {
        Log.d(TAG, "onRearCamera="+on);
        mRearCameraMode = on; 
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkSpecialCase(); 
            }
        }); 
    }
}
