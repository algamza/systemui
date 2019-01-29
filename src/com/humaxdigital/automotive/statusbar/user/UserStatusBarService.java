package com.humaxdigital.automotive.statusbar.user;

import android.os.Binder;
import android.os.IBinder;

import android.content.Intent;

import android.app.Service;
import android.util.Log;

public class UserStatusBarService extends Service {
    private final String TAG = "PerUserCarService"; 

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void createUserBluetooth() {
        Log.d(TAG, "createUserBluetooth");
        //mCarBluetoothUserService = new CarBluetoothUserService(this);
    }

    private final class UserSeviceBinder extends IUserService.Stub {
        private UserStatusBarService mUserStatusBarService;

        public UserSeviceBinder(UserStatusBarService service) {
            //mUserStatusBarService = service;
        }

        @Override
        public IUserBluetooth getUserBluetooth() {
            // Create the bluetoothUserService when needed.
            //if (mCarBluetoothUserService == null) {
            //    mCarUserService.createBluetoothUserService();
            //}
            return null; //mCarBluetoothUserService;
        }
    }
}