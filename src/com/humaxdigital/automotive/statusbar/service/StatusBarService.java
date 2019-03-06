 package com.humaxdigital.automotive.statusbar.service;

import android.os.Binder;
import android.os.IBinder;

import android.content.Context;
import android.content.Intent;

import android.app.Service;

import android.util.Log;

import com.humaxdigital.automotive.statusbar.service.CarExtensionClient.CarExClientListener;

public class StatusBarService extends Service {

    private static final String TAG = "StatusBarService";
    
    private Context mContext = this; 
    private DataStore mDataStore = new DataStore();
    private CarExtensionClient mCarExClient = null;
    private StatusBarServiceBinder mStatusBarServiceBinder = null; 
    private StatusBarClimate mStatusBarClimate = null; 
    private StatusBarSystem mStatusBarSystem = null; 
    private StatusBarDev mStatusBarDev = null; 

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
    }

    @Override
    public void onDestroy() {
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
}
