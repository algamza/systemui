package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.Bundle;
import android.os.Binder;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.Handler;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;

import android.app.Service;

import android.provider.Settings;
import android.database.ContentObserver;
import android.net.Uri;
import android.telephony.TelephonyManager;

import android.car.CarNotConnectedException;
import android.extension.car.CarTMSManager;
import android.extension.car.settings.CarExtraSettings;

import android.util.Slog;

import com.humaxdigital.automotive.systemui.statusbar.service.CarExtensionClient.CarExClientListener;

public class StatusBarService extends Service {

    private static final String TAG = "StatusBarService";
    
    private static final String CAMERA_START = "com.humaxdigital.automotive.camera.ACTION_CAM_STARTED";
    private static final String CAMERA_STOP = "com.humaxdigital.automotive.camera.ACTION_CAM_STOPED";

    private Context mContext = this; 
    private DataStore mDataStore = new DataStore();
    private CarExtensionClient mCarExClient = null;
    private StatusBarServiceBinder mStatusBarServiceBinder = null; 
    private StatusBarClimate mStatusBarClimate = null; 
    private StatusBarSystem mStatusBarSystem = null; 
    private StatusBarDev mStatusBarDev = null;

    private TelephonyManager mTelephony = null;
    private CarTMSManager mTMSManager = null;

    private boolean mUserAgreement = false; 
    private boolean mFrontCamera = false;
    private boolean mRearCamera = false;
    private boolean mPowerOff = false; 
    private boolean mBTCall = false; 
    private boolean mEmergencyCall = false; 
    private boolean mBluelinkCall = false; 

    @Override
    public IBinder onBind(Intent intent) {
        Slog.d(TAG, "onBind"); 
        return mStatusBarServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mStatusBarServiceBinder = new StatusBarServiceBinder(); 
        if ( mStatusBarServiceBinder != null ) {
            this.createStatusBarClimate();
            this.createStatusBarSystem();
            this.createStatusBarDev();
        }
        createCarExClient(); 
        registReceiver();
        mTelephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void onDestroy() {
        unregistReceiver();
        if ( mCarExClient != null ) mCarExClient.disconnect(); 
        if ( mStatusBarServiceBinder != null ) {
            mStatusBarServiceBinder.destroy();
            mStatusBarServiceBinder = null;
        }
        mTelephony = null;

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    public void createStatusBarClimate() {
        Slog.d(TAG, "createStatusBarClimate");
        if ( mStatusBarClimate == null ) 
            mStatusBarClimate = new StatusBarClimate(mContext, mDataStore);
    }

    public void createStatusBarSystem() {
        Slog.d(TAG, "createStatusBarSystem");
        if ( mStatusBarSystem == null ) 
            mStatusBarSystem = new StatusBarSystem(mContext, mDataStore);
    }

    public void createStatusBarDev() {
        Slog.d(TAG, "createStatusBarDev");
        if ( mStatusBarDev == null ) 
            mStatusBarDev = new StatusBarDev(mContext);
    }

    private boolean _isUserAgreement() {
        int is_agreement = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT,
            CarExtraSettings.Global.FALSE);   
        if ( is_agreement == CarExtraSettings.Global.FALSE ) return false; 
        else return true;
    }

    private final class StatusBarServiceBinder extends IStatusBarService.Stub {
        public StatusBarServiceBinder() {
        }

        public void destroy() {
            if ( mStatusBarClimate != null ) mStatusBarClimate.destroy();
            if ( mStatusBarSystem != null ) mStatusBarSystem.destroy();
            if ( mStatusBarDev != null ) mStatusBarDev.destroy();
            mStatusBarClimate = null;
            mStatusBarSystem = null;
            mStatusBarDev = null;
        }

        @Override
        public IStatusBarClimate getStatusBarClimate() {
            Slog.d(TAG, "getStatusBarClimate");
            return mStatusBarClimate;
        }
        @Override
        public IStatusBarSystem getStatusBarSystem() {
            Slog.d(TAG, "getStatusBarSystem");
            return mStatusBarSystem;
        }
        @Override
        public IStatusBarDev getStatusBarDev() {
            Slog.d(TAG, "getStatusBarDev");
            return mStatusBarDev;
        }

        @Override
        public boolean isUserAgreement() {
            mUserAgreement = _isUserAgreement();
            Slog.d(TAG, "isUserAgreement="+mUserAgreement);
            return mUserAgreement; 
        }
    
        @Override
        public boolean isFrontCamera() {
            Slog.d(TAG, "isFrontCamera="+mFrontCamera);
            return mFrontCamera; 
        }
    
        @Override
        public boolean isRearCamera() {
            Slog.d(TAG, "isRearCamera="+mRearCamera);
            return mRearCamera; 
        }
    
        @Override
        public boolean isPowerOff() {
            Slog.d(TAG, "isPowerOff="+mPowerOff);
            return mPowerOff; 
        }
    
        @Override
        public boolean isEmergencyCall() {
            Slog.d(TAG, "isEmergencyCall="+mEmergencyCall);
            return mEmergencyCall; 
        }
    
        @Override
        public boolean isBluelinkCall() {
            Slog.d(TAG, "isBluelinkCall="+mBluelinkCall);
            return mBluelinkCall; 
        }
    
        @Override
        public boolean isBTCall() {
            Slog.d(TAG, "isBTCall="+mBTCall);
            return mBTCall; 
        }
    }

    private void createCarExClient() {
        if ( mContext == null ) return; 
        mCarExClient = new CarExtensionClient(mContext)
            .registerListener(mCarExClientListener)
            .connect(); 
    }

    private CarExtensionClient.CarExClientListener mCarExClientListener = 
        new CarExtensionClient.CarExClientListener() {
        @Override
        public void onConnected() {
            if ( mCarExClient == null ) return; 
            if ( mStatusBarClimate != null ) mStatusBarClimate.fetchCarExClient(mCarExClient);
            if ( mStatusBarSystem != null ) mStatusBarSystem.fetchCarExClient(mCarExClient);
            mTMSManager = mCarExClient.getTMSManager(); 
            if ( mTMSManager != null ) mTMSManager.registerCallback(mTMSEventListener);
        }

        @Override
        public void onDisconnected() {
            if ( mStatusBarClimate != null ) mStatusBarClimate.fetchCarExClient(null);
            if ( mStatusBarSystem != null ) mStatusBarSystem.fetchCarExClient(null);
            mTMSManager = null; 
        }
    }; 

    private void registReceiver() {
        Slog.d(TAG, "registReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(CAMERA_START);
        filter.addAction(CAMERA_STOP);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED); 
        if ( mContext != null )
            mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistReceiver() {
        Slog.d(TAG, "unregistReceiver");
        if ( mContext != null ) mContext.unregisterReceiver(mReceiver);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( action == null || mStatusBarClimate == null || mStatusBarSystem == null ) return;
            Slog.d(TAG, "mReceiver="+action);
            switch(action) {
                case CAMERA_START: {
                    Bundle extras = intent.getExtras();
                    if ( extras == null ) return;
                    if ( extras.getString("CAM_DISPLAY_MODE").equals("REAR_CAM_MODE") ) {
                        Slog.d(TAG, "CAM_DISPLAY_MODE=REAR_CAM_MODE");
                        mRearCamera = true;
                        mFrontCamera = false;
                        mStatusBarClimate.onRearCamera(true); 
                        mStatusBarSystem.onRearCamera(true); 
                        mStatusBarClimate.onFrontCamera(false); 
                        mStatusBarSystem.onFrontCamera(false);
                    } else {
                        Slog.d(TAG, "CAM_DISPLAY_MODE=FRONT_CAM_MODE");
                        mRearCamera = false;
                        mFrontCamera = true;
                        mStatusBarClimate.onRearCamera(false); 
                        mStatusBarSystem.onRearCamera(false); 
                        mStatusBarClimate.onFrontCamera(true); 
                        mStatusBarSystem.onFrontCamera(true); 
                    }
                    break;
                }
                case CAMERA_STOP: {
                    Slog.d(TAG, "CAMERA_STOP");
                    mRearCamera = false;
                    mFrontCamera = false;
                    mStatusBarClimate.onFrontCamera(false); 
                    mStatusBarSystem.onFrontCamera(false);
                    mStatusBarClimate.onRearCamera(false); 
                    mStatusBarSystem.onRearCamera(false); 
                    break;
                }
                case TelephonyManager.ACTION_PHONE_STATE_CHANGED: {
                    String phone_state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
                    Slog.d(TAG, "ACTION_PHONE_STATE_CHANGED="+phone_state);
                    if ( phone_state == null ) break;
                    boolean btcall = isBTCalling();
                    mBTCall = btcall; 
                    mStatusBarClimate.onBTCall(btcall); 
                    mStatusBarSystem.onBTCall(btcall); 
                    
                    break;
                }
            }
        }
    };

    private boolean isBTCalling() {
        if ( mTelephony == null ) return false; 
        int state = mTelephony.getCallState(); 
        switch(state) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
            case TelephonyManager.CALL_STATE_RINGING: {
                return true; 
            }
            case TelephonyManager.CALL_STATE_IDLE: break; 
        }
        return false; 
    }

    private CarTMSManager.CarTMSEventListener mTMSEventListener = 
        new CarTMSManager.CarTMSEventListener(){
        @Override
        public void onEmergencyMode(boolean enabled) {
            Slog.d(TAG, "onEmergencyMode = "+enabled); 
            mEmergencyCall = enabled; 
            if ( mStatusBarClimate != null ) mStatusBarClimate.onEmergencyCall(enabled); 
            if ( mStatusBarSystem != null ) mStatusBarSystem.onEmergencyCall(enabled); 
        }
        @Override
        public void onBluelinkCallMode(boolean enabled) {
            Slog.d(TAG, "onBluelinkCallMode = "+enabled); 
            mBluelinkCall = enabled; 
            if ( mStatusBarClimate != null ) mStatusBarClimate.onBluelinkCall(enabled); 
            if ( mStatusBarSystem != null ) mStatusBarSystem.onBluelinkCall(enabled); 
        }
    };
}
