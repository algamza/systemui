package com.humaxdigital.automotive.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.view.View;
import android.os.RemoteException;
import android.os.Handler;
import android.graphics.Bitmap;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.UserProfileView;

import com.humaxdigital.automotive.statusbar.service.IStatusBarSystem;
import com.humaxdigital.automotive.statusbar.service.IStatusBarSystemCallback; 
import com.humaxdigital.automotive.statusbar.service.BitmapParcelable; 

public class UserProfileController {
    private Context mContext;
    private View mParentView; 
    private UserProfileView mUserProfileView;
    private IStatusBarSystem mService; 
    private Handler mHandler; 

    public UserProfileController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view; 
        mHandler = new Handler(mContext.getMainLooper());
    }

    public void init(IStatusBarSystem service) {
        if ( service == null ) return;
        mService = service; 
        try {
            mService.registerSystemCallback(mUserProfileCallback);
            if ( mService.isUserProfileInitialized() ) initView();
        } catch( RemoteException e ) {
            e.printStackTrace();
        } 
    }

    public void deinit() {
        try {
            if ( mService != null ) mService.unregisterSystemCallback(mUserProfileCallback);
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    private void initView() {
        if ( mParentView == null ) return;
        mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mService == null ) return;
                try {
                    mService.openUserProfileSetting(); 
                } catch( RemoteException e ) {
                    e.printStackTrace();
                }
            }
        });

        mUserProfileView = mParentView.findViewById(R.id.img_useprofile);
        if ( mUserProfileView != null ) mUserProfileView.setImageBitmap(getUserBitmap()); 
    }

    private Bitmap getUserBitmap() {
        if ( mService == null ) return null; 
        Bitmap img = null; 
        try {
            img = mService.getUserProfileImage().getBitmap();
        } catch( RemoteException e ) {
            e.printStackTrace();
            return null; 
        }
        return img; 
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private final IStatusBarSystemCallback.Stub mUserProfileCallback = new IStatusBarSystemCallback.Stub() {
        public void onSystemInitialized() throws RemoteException {
        }
        public void onUserProfileInitialized() throws RemoteException {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    initView();
                }
            });  
        }
        public void onDateTimeInitialized() throws RemoteException {
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
        public void onDateTimeChanged(String time) throws RemoteException {
        }
        public void onTimeTypeChanged(String type) throws RemoteException {
        }
        public void onUserChanged(BitmapParcelable data) throws RemoteException {
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ( mUserProfileView != null ) 
                        mUserProfileView.setImageBitmap(getUserBitmap()); 
                }
            }); 
        }
    };
}
