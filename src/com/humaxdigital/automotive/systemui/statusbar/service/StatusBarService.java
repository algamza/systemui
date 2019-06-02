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
    private StatusBarServiceBinder mStatusBarServiceBinder = null; 
    private StatusBarClimate mStatusBarClimate = null; 
    private StatusBarSystem mStatusBarSystem = null; 
    private StatusBarDev mStatusBarDev = null; 

    private ContentResolver mContentResolver;
    private ContentObserver mUserAgreementObserver;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind"); 
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
        registCameraReceiver();
        initSettingProvider();
    }

    @Override
    public void onDestroy() {
        unregistCameraReceiver();
        if ( mCarExClient != null ) mCarExClient.disconnect(); 
        if ( mStatusBarServiceBinder != null ) {
            mStatusBarServiceBinder.destroy();
            mStatusBarServiceBinder = null;
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    public void createStatusBarClimate() {
        Log.d(TAG, "createStatusBarClimate");
        if ( mStatusBarClimate == null ) 
            mStatusBarClimate = new StatusBarClimate(mContext, mDataStore);
    }

    public void createStatusBarSystem() {
        Log.d(TAG, "createStatusBarSystem");
        if ( mStatusBarSystem == null ) 
            mStatusBarSystem = new StatusBarSystem(mContext, mDataStore);
    }

    public void createStatusBarDev() {
        Log.d(TAG, "createStatusBarDev");
        if ( mStatusBarDev == null ) 
            mStatusBarDev = new StatusBarDev(mContext);
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
            Log.d(TAG, "getStatusBarClimate");
            return mStatusBarClimate;
        }
        @Override
        public IStatusBarSystem getStatusBarSystem() {
            Log.d(TAG, "getStatusBarSystem");
            return mStatusBarSystem;
        }
        @Override
        public IStatusBarDev getStatusBarDev() {
            Log.d(TAG, "getStatusBarDev");
            return mStatusBarDev;
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
        }

        @Override
        public void onDisconnected() {
            if ( mStatusBarClimate != null ) mStatusBarClimate.fetchCarExClient(null);
            if ( mStatusBarSystem != null ) mStatusBarSystem.fetchCarExClient(null);
        }
    }; 

    private void registCameraReceiver() {
        Log.d(TAG, "registCameraReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(CAMERA_START);
        filter.addAction(CAMERA_STOP);
        if ( mContext != null )
            mContext.registerReceiverAsUser(mCameraEvtReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistCameraReceiver() {
        Log.d(TAG, "unregistCameraReceiver");
        if ( mContext != null ) mContext.unregisterReceiver(mCameraEvtReceiver);
    }

    private final BroadcastReceiver mCameraEvtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
           
            String action = intent.getAction();
            
            if ( action == null ) return;
            Log.d(TAG, "CameraEvtReceiver="+action);
            if ( action.equals(CAMERA_START) ) {
                Bundle extras = intent.getExtras();
                if ( extras == null ) return;
                if ( extras.getString("CAM_DISPLAY_MODE").equals("REAR_CAM_MODE") ) {
                    mStatusBarClimate.onRearCamera(true); 
                    mStatusBarSystem.onRearCamera(true); 
                } else {
                    mStatusBarClimate.onFrontCamera(true); 
                    mStatusBarSystem.onFrontCamera(true); 
                }
            }
            else if ( action.equals(CAMERA_STOP) ) {
                mStatusBarClimate.onFrontCamera(false); 
                mStatusBarSystem.onFrontCamera(false);
                mStatusBarClimate.onRearCamera(false); 
                mStatusBarSystem.onRearCamera(false); 
            }
        }
    };

    private void initSettingProvider() {
        if ( mContext == null ) return;
        Log.d(TAG, "init"); 
        mContentResolver = mContext.getContentResolver();
        if ( mContentResolver == null ) return; 
        mUserAgreementObserver = createUserAgreementObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT), 
            false, mUserAgreementObserver, UserHandle.USER_CURRENT); 
        int is_agreement = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT,
            CarExtraSettings.Global.FALSE);     
        if ( is_agreement == CarExtraSettings.Global.FALSE ) {
            mStatusBarClimate.onUserAgreement(false); 
            mStatusBarSystem.onUserAgreement(false);
        }
        else {
            mStatusBarClimate.onUserAgreement(true); 
            mStatusBarSystem.onUserAgreement(true);
        }        
    }

    private ContentObserver createUserAgreementObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if ( mContext == null ) return;
                int is_agreement = Settings.Global.getInt(mContext.getContentResolver(), 
                    CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT,
                    CarExtraSettings.Global.FALSE);  
                if ( is_agreement == CarExtraSettings.Global.FALSE ) {
                    mStatusBarClimate.onUserAgreement(false); 
                    mStatusBarSystem.onUserAgreement(false);
                }
                else {
                    mStatusBarClimate.onUserAgreement(true); 
                    mStatusBarSystem.onUserAgreement(true);
                } 
            }
        };
        return observer; 
    }
}
