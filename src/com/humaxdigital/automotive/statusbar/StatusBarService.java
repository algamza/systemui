package com.humaxdigital.automotive.statusbar;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

public class StatusBarService extends Service {

    private final IStatusBarService.Stub mBinder = new IStatusBarService.Stub() {

        @Override
        public void registerStatusBarCallback(IStatusBarCallback callback) throws RemoteException {
            return; 
        }

        @Override
        public void unregisterStatusBarCallback(IStatusBarCallback callback) throws RemoteException {
            return;
        }

        @Override
        public String getSystemDateTime() throws RemoteException {
            return ""; 
        }

        @Override
        public void registerSystemCallback(ISystemCallback callback) throws RemoteException {
            return; 
        }

        @Override
        public void unregisterSystemCallback(ISystemCallback callback) throws RemoteException {
            return;
        }

        @Override
        public float getClimateDRTemperature() throws RemoteException {
            return 0.0f; 
        }

        @Override
        public void registerClimateCallback(IClimateCallback callback) throws RemoteException {
            return;
        }

        @Override
        public void unregisterClimateCallback(IClimateCallback callback) throws RemoteException {
            return;
        }
    };

    private List<ISystemCallback> mSystemCallbacks = new ArrayList<>();
    private List<IClimateCallback> mClimateCallbacks = new ArrayList<>();
    private List<IStatusBarCallback> mStatusBarCallbacks = new ArrayList<>();

    /*
    public class LocalBinder extends Binder {
        StatusBarService getService() {
            return StatusBarService.this;
        }
    }
    private final LocalBinder mBinder = new LocalBinder();
    */
/*
    public static abstract class SystemCallback {
        enum MuteStatus { AV_MUTE, NAV_MUTE, AV_NAV_MUTE }
        enum BLEStatus { BLE_0, BLE_1, BLE_2, BLE_3 }
        enum BTBatteryStatus { BT_BATTERY_0, BT_BATTERY_1, BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }
        enum BTCallStatus { STREAMING_CONNECTED, HANDS_FREE_CONNECTED, HF_FREE_STREAMING_CONNECTED
            , CALL_HISTORY_DOWNLOADING, CONTACTS_HISTORY_DOWNLOADING, TMU_CALLING, BT_CALLING, BT_PHONE_MIC_MUTE }
        enum AntennaStatus { BT_ANTENNA_NO, BT_ANTENNA_0, BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5
            , TMU_ANTENNA_NO, TMU_ANTENNA_0, TMU_ANTENNA_1, TMU_ANTENNA_2, TMU_ANTENNA_3, TMU_ANTENNA_4, TMU_ANTENNA_5}
        enum DataStatus { DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
        enum WifiStatus { WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
        enum WirelessChargeStatus { WIRELESS_CHARGING, WIRELESS_CHARGE_100, WIRELESS_CHARGING_ERROR }
        enum ModeStatus { LOCATION_SHARING, QUIET_MODE }

        public void onUserChanged(Bitmap bitmap) {
        }
        public void onDateTimeChanged(String time) {
        }
        public void onMuteStatusChanged(MuteStatus status) {
        }
        public void onBLEStatusChanged(BLEStatus status) {
        }
        public void onBTBatteryStatusChanged(BTBatteryStatus status) {
        }
        public void onBTCallStatusChanged(BTCallStatus status) {
        }
        public void onAntennaStatusChanged(AntennaStatus stataus) {
        }
        public void onDataStatusChanged(DataStatus status) {
        }
        public void onWifiStatusChanged(WifiStatus status) {
        }
        public void onWirelessChargeStatusChanged(WirelessChargeStatus status) {
        }
        public void onModeStatusChanged(ModeStatus status) {
        }
    }

    public static abstract class ClimateCallback {
        enum SeatStatus { NONE, HEATER1, HEATER2, HEATER3, COOLER1, COOLER2, COOLER3 }
        enum IntakeStatus { FRE, REC }
        enum ClimateModeStatus { BELOW, MIDDLE, BELOW_MIDDLE, BELOW_WINDOW }
        enum BlowerSpeedStatus { STEP_1, STEP_2, STEP_3, STEP_4, STEP_5, STEP_6, STEP_7, STEP_8 }

        public void onDRTemperatureChanged(float temp) {
        }
        public void onDRSeatStatusChanged(SeatStatus status) {
        }
        public void onIntakeStatusChanged(IntakeStatus status) {
        }
        public void onClimateModeChanged(ClimateModeStatus status) {
        }
        public void onBlowerSpeedChanged(BlowerSpeedStatus status) {
        }
        public void onPSSeatStatusChanged(SeatStatus status) {
        }
        public void onPSTemperatureChanged(float temp) {
        }
    }
    

    private List<SystemCallback> mSystemCallbacks = new ArrayList<>();
    private List<ClimateCallback> mClimateCallbacks = new ArrayList<>();
    */

    private Object mHvacManagerReady = new Object();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        synchronized (mHvacManagerReady) {
            // todo : connect managers
            mHvacManagerReady.notifyAll();
        }
    }

    public void requestRefresh(final Runnable r, final Handler h) {
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                synchronized (mHvacManagerReady) {
                    /*
                    while(mTestObj == null) {
                        try {
                            mHvacManagerReady.wait();
                        } catch (InterruptedException e) {
                            return null;
                        }
                    }
                    */
                }

                // todo : update controller

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Log.d("TEST", "######### onPostExecute");
                h.post(r);
            }
        };
        task.execute();
    }
}
