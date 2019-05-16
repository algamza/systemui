package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.Context;
import android.view.View;
import android.os.RemoteException;

import android.os.Handler;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.DateView;

import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable;
import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarSystem;
import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarSystemCallback; 

import android.util.Log; 


public class DateController {
    private static String TAG = "DateController"; 
    private View mParentView; 
    private Context mContext;
    private DateView mDateVew;
    private DateView mDateNoonView;
    private IStatusBarSystem mService; 
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

    public void init(IStatusBarSystem service) {
        if ( service == null ) return;
        mService = service; 
        try {
            mService.registerSystemCallback(mDateTimeCallback); 
            if ( mService.isDateTimeInitialized() ) initView(); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    public void deinit() {
        if ( mService == null ) return;
        try {
            mService.unregisterSystemCallback(mDateTimeCallback); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    private void initView() {
        if ( mParentView == null || mService == null ) return;
        mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mService == null ) return;
                try {
                    if ( mIsValidTime ) 
                        mService.openDateTimeSetting(); 
                } catch( RemoteException e ) {
                    e.printStackTrace();
                }
            }
        });

        mDateVew = mParentView.findViewById(R.id.text_date_time);
        mDateNoonView = mParentView.findViewById(R.id.text_date_noon);
        
        try {
            mTime = mService.getDateTime(); 
            mType = mService.getTimeType();
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
        
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
            mDateVew.setText(time);
            mDateNoonView.setText("");
        } else {
            if ( time.contains("AM") ) {
                date = time.substring(0, time.indexOf("AM"));
                noon = "AM";
            }
            else if ( time.contains("PM") ) {
                date = time.substring(0, time.indexOf("PM"));
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

    private final IStatusBarSystemCallback.Stub mDateTimeCallback = new IStatusBarSystemCallback.Stub() {
        public void onSystemInitialized() throws RemoteException {
        }
        public void onUserProfileInitialized() throws RemoteException {
        }
        public void onDateTimeInitialized() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    initView(); 
                }
            });  
        }
        public void onMuteStatusChanged(int status) throws RemoteException {
        }
        public void onBLEStatusChanged(int status) throws RemoteException {
        }
        public void onBTBatteryStatusChanged(int status) throws RemoteException {
        }
        public void onCallStatusChanged(int status) throws RemoteException {
        }
        public void onAntennaStatusChanged(int status) throws RemoteException {
        }
        public void onDataStatusChanged(int status) throws RemoteException {
        }
        public void onWifiStatusChanged(int status) throws RemoteException {
        }
        public void onWirelessChargeStatusChanged(int status) throws RemoteException {
        }
        public void onModeStatusChanged(int status) throws RemoteException {
        }
        @Override
        public void onDateTimeChanged(String time) throws RemoteException {
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
        public void onTimeTypeChanged(String type) throws RemoteException {
            if ( mHandler == null ) return; 
            mType = type; 
            try {
                mTime = mService.getDateTime(); 
            } catch( RemoteException e ) {
                e.printStackTrace();
            }

            Log.d(TAG, "onTimeTypeChanged:type="+type+", time="+mTime);
            
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateClockUI(mTime, mType);
                }
            }); 
        }
        public void onUserChanged(BitmapParcelable data) throws RemoteException {
        }
    };
}
