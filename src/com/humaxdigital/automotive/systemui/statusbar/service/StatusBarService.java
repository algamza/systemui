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

import android.util.Log;

import com.humaxdigital.automotive.systemui.statusbar.service.CarExtensionClient.CarExClientListener;

public class StatusBarService extends Service {

    private static final String TAG = "StatusBarService";
    
    private static final String CAMERA_START = "com.humaxdigital.automotive.camera.ACTION_CAM_STARTED";
    private static final String CAMERA_STOP = "com.humaxdigital.automotive.camera.ACTION_CAM_STOPED";

    private Context mContext = this; 
    private DataStore mDataStore = new DataStore();
    private CarExtensionClient mCarExClient = null;
    private final Binder mStatusBarServiceBinder = new StatusBarServiceBinder();  
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

    public class StatusBarServiceBinder extends Binder {
        public StatusBarService getService() {
            return StatusBarService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind"); 
        return mStatusBarServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
        if ( mStatusBarClimate != null ) mStatusBarClimate.destroy();
        if ( mStatusBarSystem != null ) mStatusBarSystem.destroy();
        if ( mStatusBarDev != null ) mStatusBarDev.destroy();
        mStatusBarClimate = null;
        mStatusBarSystem = null;
        mStatusBarDev = null;
        mTelephony = null;

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public StatusBarClimate getStatusBarClimate() {
        Log.d(TAG, "getStatusBarClimate");
        return mStatusBarClimate;
    }

    public StatusBarSystem getStatusBarSystem() {
        Log.d(TAG, "getStatusBarSystem");
        return mStatusBarSystem;
    }

    public StatusBarDev getStatusBarDev() {
        Log.d(TAG, "getStatusBarDev");
        return mStatusBarDev;
    }

    public boolean isUserAgreement() {
        mUserAgreement = _isUserAgreement();
        Log.d(TAG, "isUserAgreement="+mUserAgreement);
        return mUserAgreement; 
    }

    public boolean isFrontCamera() {
        Log.d(TAG, "isFrontCamera="+mFrontCamera);
        return mFrontCamera; 
    }

    public boolean isRearCamera() {
        Log.d(TAG, "isRearCamera="+mRearCamera);
        return mRearCamera; 
    }

    public boolean isPowerOff() {
        if ( mStatusBarSystem == null ) return false;
        mPowerOff = mStatusBarSystem.isPowerOff();
        Log.d(TAG, "isPowerOff="+mPowerOff);  
        return mPowerOff; 
    }

    public boolean isEmergencyCall() {
        Log.d(TAG, "isEmergencyCall="+mEmergencyCall);
        return mEmergencyCall; 
    }

    public boolean isBluelinkCall() {
        Log.d(TAG, "isBluelinkCall="+mBluelinkCall);
        return mBluelinkCall; 
    }

    public boolean isBTCall() {
        Log.d(TAG, "isBTCall="+mBTCall);
        return mBTCall; 
    }

    private void createStatusBarClimate() {
        Log.d(TAG, "createStatusBarClimate");
        if ( mStatusBarClimate == null ) 
            mStatusBarClimate = new StatusBarClimate(mContext, mDataStore);
    }

    private void createStatusBarSystem() {
        Log.d(TAG, "createStatusBarSystem");
        if ( mStatusBarSystem == null ) 
            mStatusBarSystem = new StatusBarSystem(mContext, mDataStore);
    }

    private void createStatusBarDev() {
        Log.d(TAG, "createStatusBarDev");
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
        Log.d(TAG, "registReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(CAMERA_START);
        filter.addAction(CAMERA_STOP);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED); 
        if ( mContext != null )
            mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistReceiver() {
        Log.d(TAG, "unregistReceiver");
        if ( mContext != null ) mContext.unregisterReceiver(mReceiver);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ( action == null || mStatusBarClimate == null || mStatusBarSystem == null ) return;
            Log.d(TAG, "mReceiver="+action);
            switch(action) {
                case CAMERA_START: {
                    Bundle extras = intent.getExtras();
                    if ( extras == null ) return;
                    if ( extras.getString("CAM_DISPLAY_MODE").equals("REAR_CAM_MODE") ) {
                        Log.d(TAG, "CAM_DISPLAY_MODE=REAR_CAM_MODE");
                        mRearCamera = true;
                        mFrontCamera = false;
                        mStatusBarClimate.onRearCamera(true); 
                        mStatusBarSystem.onRearCamera(true); 
                        mStatusBarClimate.onFrontCamera(false); 
                        mStatusBarSystem.onFrontCamera(false);
                    } else {
                        Log.d(TAG, "CAM_DISPLAY_MODE=FRONT_CAM_MODE");
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
                    Log.d(TAG, "CAMERA_STOP");
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
                    Log.d(TAG, "ACTION_PHONE_STATE_CHANGED="+phone_state);
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
            Log.d(TAG, "onEmergencyMode = "+enabled); 
            mEmergencyCall = enabled; 
            if ( mStatusBarClimate != null ) mStatusBarClimate.onEmergencyCall(enabled); 
            if ( mStatusBarSystem != null ) mStatusBarSystem.onEmergencyCall(enabled); 
        }
        @Override
        public void onBluelinkCallMode(boolean enabled) {
            Log.d(TAG, "onBluelinkCallMode = "+enabled); 
            mBluelinkCall = enabled; 
            if ( mStatusBarClimate != null ) mStatusBarClimate.onBluelinkCall(enabled); 
            if ( mStatusBarSystem != null ) mStatusBarSystem.onBluelinkCall(enabled); 
        }
    };
}
