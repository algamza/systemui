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
import android.car.hardware.CarSensorEvent;
import android.car.hardware.CarSensorEvent.GearData;
import android.extension.car.value.CarSensorEventEx;
import android.extension.car.CarTMSManager;
import android.extension.car.CarSensorManagerEx;
import android.extension.car.settings.CarExtraSettings;

import android.util.Log;

import com.humaxdigital.automotive.systemui.common.car.CarExClient;
import com.humaxdigital.automotive.systemui.common.CONSTANTS;

public class StatusBarService extends Service {

    private static final String TAG = "StatusBarService";
    
    private Context mContext = this; 
    private DataStore mDataStore = new DataStore();
    private final Binder mStatusBarServiceBinder = new StatusBarServiceBinder();  
    private StatusBarClimate mStatusBarClimate = null; 
    private StatusBarSystem mStatusBarSystem = null; 
    private StatusBarDev mStatusBarDev = null;

    private TelephonyManager mTelephony = null;
    private CarTMSManager mTMSManager = null;
    private CarSensorManagerEx mSensorManager = null;
    private ContentResolver mContentResolver;
    private ContentObserver mUserAgreementObserver;
    private ContentObserver mUserSwitchingObserver; 

    private boolean mUserAgreement = false; 
    private boolean mUserSwitching = false; 
    private boolean mFrontCamera = false;
    private boolean mRearCamera = false;
    private boolean mPowerOff = false; 
    private boolean mBTCall = false; 
    private boolean mEmergencyCall = false; 
    private boolean mBluelinkCall = false; 
    private boolean mRearGearDetected = false; 
    private boolean mSVIOn = false; 
    private boolean mSVSOn = false; 

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
        createObserver(); 
    }

    @Override
    public void onDestroy() {
        destroyObserver(); 
        unregistReceiver();
        CarExClient.INSTANCE.disconnect(mCarExClientListener); 
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

    public boolean isUserSwitching() {
        mUserSwitching = _isUserSwitching();
        Log.d(TAG, "isUserSwitching="+mUserSwitching);
        return mUserSwitching; 
    }

    public boolean isFrontCamera() {
        Log.d(TAG, "isFrontCamera="+mFrontCamera);
        return mFrontCamera; 
    }

    public boolean isRearCamera() {
        Log.d(TAG, "isRearCamera="+mRearCamera);
        return mRearCamera; 
    }

    public boolean isRearGearDetected() {
        Log.d(TAG, "isRearGearDetected="+mRearGearDetected);
        return mRearGearDetected; 
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

    public boolean isSVIOn() {
        Log.d(TAG, "isSVIOn="+mSVIOn);
        return mSVIOn; 
    }

    public boolean isSVSOn() {
        Log.d(TAG, "isSVSOn="+mSVSOn);
        return mSVSOn; 
    }

    public boolean isBluelinkCall() {
        Log.d(TAG, "isBluelinkCall="+mBluelinkCall);
        return mBluelinkCall; 
    }

    public boolean isBTCall() {
        Log.d(TAG, "isBTCall="+mBTCall);
        return mBTCall; 
    }

    private void createObserver() {
        if ( mContext == null ) return;
        mContentResolver = mContext.getContentResolver();
        mUserAgreementObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                boolean is_usergreement = _isUserAgreement();
                if ( mStatusBarSystem != null ) 
                    mStatusBarSystem.onUserAgreement(is_usergreement); 
            }
        };
        mUserSwitchingObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                boolean is_userswitching = _isUserSwitching();
                if ( mStatusBarSystem != null ) 
                    mStatusBarSystem.onUserSwitching(is_userswitching); 
            }
        };
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT), 
            false, mUserAgreementObserver, UserHandle.USER_CURRENT); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CarExtraSettings.Global.USERPROFILE_USER_SWITCHING_START_FINISH), 
            false, mUserSwitchingObserver, UserHandle.USER_CURRENT); 
    }

    private void destroyObserver() {
        if ( mContentResolver != null ) {
            mContentResolver.unregisterContentObserver(mUserAgreementObserver);
            mContentResolver.unregisterContentObserver(mUserSwitchingObserver);
        }
        mContentResolver = null;
        mUserAgreementObserver = null;
        mUserSwitchingObserver = null;
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
        if ( mContext == null ) return false; 
        int is_agreement = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT,
            CarExtraSettings.Global.FALSE);   
        if ( is_agreement == CarExtraSettings.Global.FALSE ) return false; 
        else return true;
    }

    private boolean _isUserSwitching() {
        if ( mContext == null ) return false; 
        int isUserSwitching = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_USER_SWITCHING_START_FINISH, 
            CarExtraSettings.Global.FALSE);
        if ( isUserSwitching == CarExtraSettings.Global.TRUE ) return true; 
        else return false;
    }

    private void createCarExClient() {
        if ( mContext == null ) return; 
        CarExClient.INSTANCE.connect(mContext, mCarExClientListener); 
    }

    private CarExClient.CarExClientListener mCarExClientListener = 
        new CarExClient.CarExClientListener() {
        @Override
        public void onConnected() {
            CarExClient client = CarExClient.INSTANCE; 
            if ( client == null ) return;
            if ( mStatusBarClimate != null ) mStatusBarClimate.fetchCarExClient(client);
            if ( mStatusBarSystem != null ) mStatusBarSystem.fetchCarExClient(client);
            mTMSManager = client.getTMSManager(); 
            if ( mTMSManager != null ) mTMSManager.registerCallback(mTMSEventListener); 
            mSensorManager = client.getSensorManager();
            if ( mSensorManager != null ) {
                try {
                    mSensorManager.registerListener(
                        mSensorChangeListener, 
                        CarSensorManagerEx.SENSOR_TYPE_GEAR, 
                        CarSensorManagerEx.SENSOR_RATE_NORMAL);
                    CarSensorEvent event = mSensorManager.getLatestSensorEvent(CarSensorManagerEx.SENSOR_TYPE_GEAR);
                    GearData gear = event.getGearData(null);
                    if( gear.equals(CarSensorEvent.GEAR_REVERSE) ) {
                        mRearGearDetected = true;
                        if ( mStatusBarClimate != null ) mStatusBarClimate.onRearGearDetected(false); 
                        if ( mStatusBarSystem != null ) mStatusBarSystem.onRearGearDetected(false); 
                    }
                } catch (CarNotConnectedException e) {
                    Log.e(TAG, "Car is not connected!", e);
                }
            }
        }

        @Override
        public void onDisconnected() {
            if ( mSensorManager != null ) 
                mSensorManager.unregisterListener(mSensorChangeListener);
            if ( mTMSManager != null )
                mTMSManager.unregisterCallback(mTMSEventListener); 
            if ( mStatusBarClimate != null ) mStatusBarClimate.fetchCarExClient(null);
            if ( mStatusBarSystem != null ) mStatusBarSystem.fetchCarExClient(null);
            mTMSManager = null; 
            mSensorManager = null;
        }
    }; 

    private void registReceiver() {
        Log.d(TAG, "registReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(CONSTANTS.ACTION_CAMERA_START);
        filter.addAction(CONSTANTS.ACTION_CAMERA_STOP);
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
                case CONSTANTS.ACTION_CAMERA_START: {
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
                case CONSTANTS.ACTION_CAMERA_STOP: {
                    Log.d(TAG, "CONSTANTS.ACTION_CAMERA_STOP");
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
        @Override
        public void onImmobilizationMode(boolean enabled) {
            Log.d(TAG, "onImmobilizationMode = "+enabled); 
            mSVIOn = enabled; 
            if ( mStatusBarClimate != null ) mStatusBarClimate.onSVIOn(enabled); 
            if ( mStatusBarSystem != null ) mStatusBarSystem.onSVIOn(enabled); 
        }
        @Override
        public void onSlowdownMode(boolean enabled) {
            Log.d(TAG, "onSlowdownMode = "+enabled); 
            mSVSOn = enabled; 
            if ( mStatusBarClimate != null ) mStatusBarClimate.onSVSOn(enabled); 
            if ( mStatusBarSystem != null ) mStatusBarSystem.onSVSOn(enabled); 
        }                
    };

    private final CarSensorManagerEx.OnSensorChangedListenerEx mSensorChangeListener =
        new CarSensorManagerEx.OnSensorChangedListenerEx () {
        public void onSensorChanged(final CarSensorEvent event) {
            switch (event.sensorType) {
                case CarSensorManagerEx.SENSOR_TYPE_GEAR: {
                    GearData gear = event.getGearData(null);
                    Log.d(TAG, "onSensorChanged:SENSOR_TYPE_GEAR:gear=" + gear.gear);
                    if( gear.gear == CarSensorEventEx.GEAR_R ) {
                        if ( mRearGearDetected ) break;
                        mRearGearDetected = true;
                        mStatusBarClimate.onRearGearDetected(true); 
                        mStatusBarSystem.onRearGearDetected(true); 
                    } else {
                        if ( !mRearGearDetected ) break;
                        mRearGearDetected = false;
                        mStatusBarClimate.onRearGearDetected(false); 
                        mStatusBarSystem.onRearGearDetected(false); 
                    }
                    break;
                }
                default: break;
            }
        }

        public void onSensorChanged(final CarSensorEventEx event) {
        }
    };
}
