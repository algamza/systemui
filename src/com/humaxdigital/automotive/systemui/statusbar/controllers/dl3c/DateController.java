package com.humaxdigital.automotive.systemui.statusbar.controllers.dl3c;

import android.os.RemoteException;
import android.os.Handler;

import android.content.Context;

import android.view.View;
import android.widget.TextView;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.DateView;

import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable;
import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarSystem;
import com.humaxdigital.automotive.systemui.statusbar.service.IStatusBarSystemCallback; 

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
    
    private IStatusBarSystem mService; 
    private Boolean mIsValidTime = true; 
    private Handler mHandler; 

    private HashMap<String,String> mMonthEng = new HashMap<>();

    public DateController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view;
        mHandler = new Handler(mContext.getMainLooper());
        initMonthEng();
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

    private void initMonthEng() {
        mMonthEng.put("1", "Jan."); 
        mMonthEng.put("2", "Feb."); 
        mMonthEng.put("3", "Mar."); 
        mMonthEng.put("4", "Apr."); 
        mMonthEng.put("5", "May."); 
        mMonthEng.put("6", "Jun."); 
        mMonthEng.put("7", "Jul."); 
        mMonthEng.put("8", "Aug."); 
        mMonthEng.put("9", "Sep."); 
        mMonthEng.put("10", "Oct."); 
        mMonthEng.put("11", "Nov."); 
        mMonthEng.put("12", "Dec."); 
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
                //try {
                //    if ( mIsValidTime ) 
                //        Log.d(TAG, "open date setting");  
                        //mService.openDateTimeSetting(); 
                //} catch( RemoteException e ) {
                //    e.printStackTrace();
                //}
            }
        });

        mDateVew = mParentView.findViewById(R.id.text_date);
        mTimeView = mParentView.findViewById(R.id.text_time);
        mApmView = mParentView.findViewById(R.id.text_apm);

        updateDateTime(); 
        updateUI();
    }

    private void updateDateTime() {
        String format = ""; 
        try {
            format = mService.getYearDateTime(); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
        Log.d(TAG, "updateDateTime="+format); 
        String[] arr = format.split(":");
        if ( arr.length != 6 ) return;
        mTextDate = mMonthEng.get(arr[1]) + " " + arr[2]; 
        mTextTime = arr[3]+":"+arr[4];  
        mTextApm = arr[5]; 
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
        public void onTimeTypeChanged(String type) throws RemoteException {
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
        public void onUserChanged(BitmapParcelable data) throws RemoteException {
        }
        public void onCallingStateChanged(boolean on) throws RemoteException {
        }
    };
}
