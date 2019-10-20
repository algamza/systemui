package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;

import android.view.View;
import android.os.Handler;
import android.graphics.Bitmap;
import android.util.Log;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.UserProfileView;

import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarSystem;
import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable; 

import com.humaxdigital.automotive.systemui.common.util.OSDPopup; 

public class UserProfileController {
    static final String TAG = "UserProfileController"; 
    private Context mContext;
    private View mParentView; 
    private UserProfileView mUserProfileView;
    private StatusBarSystem mService; 
    private Handler mHandler; 
    private boolean mIsPowerOff; 
    private boolean mUserAgreementMode;
    private boolean mUserSwitching; 
    private boolean mBTCalling; 
    private boolean mEmergencyMode; 
    private boolean mBluelinkMode; 
    private boolean mImmoilizationMode; 
    private boolean mSlowdownMode; 
    private boolean mRearCameraMode; 

    public UserProfileController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view; 
        mHandler = new Handler(mContext.getMainLooper());
    }

    public void init(StatusBarSystem service) {
        if ( service == null ) return;
        mService = service; 
        mService.registerSystemCallback(mUserProfileCallback);
        if ( mService.isUserProfileInitialized() ) initView();
        checkUserIconDisable();
    }

    public void deinit() {
        if ( mService != null ) mService.unregisterSystemCallback(mUserProfileCallback);
    }

    private void checkUserIconDisable() {
        if ( mService == null ) return;
        if ( mService.isPowerOff() ) mIsPowerOff = true;
        if ( mService.isUserAgreement() ) mUserAgreementMode = true;
        if ( mService.isUserSwitching() ) mUserSwitching = true;
        if ( mService.isBTCalling() ) mBTCalling = true; 
        if ( mService.isEmergencyMode() ) mEmergencyMode = true;
        if ( mService.isBluelinkMode() ) mBluelinkMode = true;
        if ( mService.isImmoilizationMOde() ) mImmoilizationMode = true;
        if ( mService.isSlowdownMode() ) mSlowdownMode = true;
        if ( mService.isRearCamera() ) mRearCameraMode = true;

        Log.d(TAG, "checkUserIconDisable:mIsPowerOff="+mIsPowerOff+
            ", mUserAgreementMode="+mUserAgreementMode+
            ", mUserSwitching="+mUserSwitching+
            ", mBTCalling="+mBTCalling+
            ", mEmergencyMode="+mEmergencyMode+
            ", mBluelinkMode="+mBluelinkMode+
            ", mImmoilizationMode="+mImmoilizationMode+
            ", mSlowdownMode="+mSlowdownMode+
            ", mRearCameraMode="+mRearCameraMode); 
        if ( mIsPowerOff 
            || mUserAgreementMode
            || mUserSwitching
            || mBTCalling
            || mEmergencyMode
            || mBluelinkMode
            || mImmoilizationMode
            || mSlowdownMode
            || mRearCameraMode ) setUserIconDisable(true);
        else setUserIconDisable(false);
    }

    private void initView() {
        if ( mParentView == null ) return;
        mParentView.setOnClickListener(mOnClickListener);
        mUserProfileView = mParentView.findViewById(R.id.img_useprofile);
        if ( mUserProfileView != null ) mUserProfileView.setImageBitmap(getUserBitmap()); 
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if ( mService == null ) return;
            mService.openUserProfileSetting(); 
        }
    }; 

    private Bitmap getUserBitmap() {
        if ( mService == null ) return null; 
        Bitmap img = null; 
        img = mService.getUserProfileImage().getBitmap();
        return img; 
    }

    private void setUserIconDisable(boolean disable) {
        if ( mUserProfileView == null ) return; 
        if ( disable ) {
            mUserProfileView.setAlpha(0.4f); 
            if ( mParentView != null ) mParentView.setOnClickListener(null);
        } else {
            mUserProfileView.setAlpha(1.0f); 
            if ( mParentView != null ) mParentView.setOnClickListener(mOnClickListener);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private final StatusBarSystem.StatusBarSystemCallback mUserProfileCallback 
        = new StatusBarSystem.StatusBarSystemCallback() {
        
        @Override
        public void onUserProfileInitialized() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    initView();
                }
            });  
        }
        @Override
        public void onUserChanged(BitmapParcelable data) {
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ( mUserProfileView != null ) 
                        mUserProfileView.setImageBitmap(getUserBitmap()); 
                }
            }); 
        }

        @Override
        public void onPowerStateChanged(int state) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onPowerStateChanged="+state);
            mIsPowerOff = (state == 2)?true:false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }

        @Override
        public void onUserAgreementMode(boolean on) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onUserAgreementMode="+on);
            mUserAgreementMode = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }

        @Override
        public void onUserSwitching(boolean on) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onUserSwitching="+on);
            mUserSwitching = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }

        @Override
        public void onBTCalling(boolean on) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onBTCalling="+on);
            mBTCalling = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }

        @Override
        public void onEmergencyMode(boolean on) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onEmergencyMode="+on);
            mEmergencyMode = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }

        @Override
        public void onBluelinkMode(boolean on) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onBluelinkMode="+on);
            mBluelinkMode = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }

        @Override
        public void onImmoilizationMode(boolean on) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onImmoilizationMode="+on);
            mImmoilizationMode = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }

        @Override
        public void onSlowdownMode(boolean on) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onSlowdownMode="+on);
            mSlowdownMode = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }

        @Override
        public void onRearCamera(boolean on) {
            if ( mHandler == null ) return; 
            Log.d(TAG, "onRearCamera="+on);
            mRearCameraMode = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkUserIconDisable(); 
                }
            }); 
        }
    };
}
