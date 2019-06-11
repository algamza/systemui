package com.humaxdigital.automotive.systemui.user;

import android.os.Binder;
import android.os.IBinder;

import android.content.Intent;

import android.app.Service;
import android.util.Log;

import com.humaxdigital.automotive.systemui.user.IUserService; 
import com.humaxdigital.automotive.systemui.user.IUserBluetooth; 
import com.humaxdigital.automotive.systemui.user.IUserWifi; 
import com.humaxdigital.automotive.systemui.user.IUserAudio; 


public class PerUserService extends Service {
    private final String TAG = "PerUserService"; 
    private UserSeviceBinder mUserServiceBinder; 
    private UserBluetooth mUserBluetooth; 
    private UserAudio mUserAudio; 
    private UserWifi mUserWifi; 

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind"); 
        return mUserServiceBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate"); 
        mUserServiceBinder = new UserSeviceBinder(this); 
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy"); 
        if ( mUserServiceBinder != null ) {
            mUserServiceBinder.destroy();
            mUserServiceBinder = null;
        }
        
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void createUserBluetooth() {
        Log.d(TAG, "createUserBluetooth");
        mUserBluetooth = new UserBluetooth(this);
    }

    public void createUserWifi() {
        Log.d(TAG, "createUserWifi");
        mUserWifi = new UserWifi(this);
    }

    public void createUserAudio() {
        Log.d(TAG, "createUserAudio");
        mUserAudio = new UserAudio(this);
    }

    private final class UserSeviceBinder extends IUserService.Stub {
        private PerUserService mPerUserService;

        public UserSeviceBinder(PerUserService service) {
            mPerUserService = service; 
        }

        public void destroy() {
            if ( mUserBluetooth != null ) mUserBluetooth.destroy();
            if ( mUserWifi != null ) mUserWifi.destroy();
            if ( mUserAudio != null ) mUserAudio.destroy();
            mUserBluetooth = null;
            mUserWifi = null;
            mUserAudio = null;
            mPerUserService = null;
        }

        @Override
        public IUserBluetooth getUserBluetooth() {
            if (mUserBluetooth == null) {
                mPerUserService.createUserBluetooth();
            }
            return mUserBluetooth;
        }

        @Override
        public IUserWifi getUserWifi() {
            if (mUserWifi == null) {
                mPerUserService.createUserWifi();
            }
            return mUserWifi;
        }

        @Override
        public IUserAudio getUserAudio() {
            if (mUserAudio == null) {
                mPerUserService.createUserAudio();
            }
            return mUserAudio;
        }
    }
}