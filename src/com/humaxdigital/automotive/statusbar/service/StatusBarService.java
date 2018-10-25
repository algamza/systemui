package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
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
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.content.IntentFilter;
import android.provider.Settings;
import android.content.BroadcastReceiver;

import android.os.UserHandle;
import android.os.UserManager;
import android.app.ActivityManager;
import android.content.pm.UserInfo;
import com.android.internal.util.UserIcons;

import android.graphics.Bitmap;


import android.car.VehicleAreaSeat;
import android.car.VehicleAreaWindow;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.hvac.CarHvacManager;
import android.support.car.Car;
import android.support.car.CarNotConnectedException;
import android.support.car.CarConnectionCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import javax.annotation.concurrent.GuardedBy;

public class StatusBarService extends Service {

    public static class SystemStatus {
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
    }
    public static class ClimateStatus {
        enum SeatStatus { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
        enum ClimateModeStatus { FLOOR, FACE, FLOOR_FACE, FLOOR_DEFROST }
        enum BlowerSpeedStatus { STEP_1, STEP_2, STEP_3, STEP_4, STEP_5, STEP_6, STEP_7, STEP_8 }
    }

    private static final String TAG = "StatusBarService";
    private static final String DEMO_MODE_PROPERTY = "android.car.hvac.demo";
    private static final String OPEN_HVAC_APP = "android.car.intent.action.TOGGLE_HVAC_CONTROLS";
    private static final String OPEN_DATE_SETTING = "";
    private static final String OPEN_USERPROFILE_SETTING = "";
    
    private static final int DRIVER_ZONE_ID = 
        VehicleAreaSeat.SEAT_ROW_1_LEFT |
        VehicleAreaSeat.SEAT_ROW_2_LEFT | 
        VehicleAreaSeat.SEAT_ROW_2_CENTER;

    private static final int PASSENGER_ZONE_ID = 
        VehicleAreaSeat.SEAT_ROW_1_RIGHT |
        VehicleAreaSeat.SEAT_ROW_2_RIGHT;

    public static final int[] AIRFLOW_STATES = new int[]{
        CarHvacManager.FAN_DIRECTION_FLOOR,
        CarHvacManager.FAN_DIRECTION_FACE,
        (CarHvacManager.FAN_DIRECTION_FACE | CarHvacManager.FAN_DIRECTION_FLOOR),
        (CarHvacManager.FAN_DIRECTION_FLOOR | CarHvacManager.FAN_DIRECTION_DEFROST)
    };
    // Hardware specific value for the front seats
    public static final int SEAT_ALL = 
        VehicleAreaSeat.SEAT_ROW_1_LEFT |
        VehicleAreaSeat.SEAT_ROW_1_RIGHT | 
        VehicleAreaSeat.SEAT_ROW_2_LEFT |
        VehicleAreaSeat.SEAT_ROW_2_CENTER | 
        VehicleAreaSeat.SEAT_ROW_2_RIGHT;

    private final IStatusBarService.Stub mBinder = new IStatusBarService.Stub() {
        public boolean isInitialized() throws RemoteException {
            return mIsInitialized; 
        }
        public void registerStatusBarCallback(IStatusBarCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mStatusBarCallbacks) {
                mStatusBarCallbacks.add(callback); 
            }
        }
        public void unregisterStatusBarCallback(IStatusBarCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mStatusBarCallbacks) {
                mStatusBarCallbacks.remove(callback); 
            }
        }

        public int getMuteStatus() throws RemoteException { return 0; }
        public int getBLEStatus() throws RemoteException { return 0; }
        public int getBTBatteryStatus() throws RemoteException { return 0; }
        public int getBTCallStatus() throws RemoteException { return 0; }
        public int getAntennaStatus() throws RemoteException { return 0; }
        public int getDataStatus() throws RemoteException { return 0; }
        public int getWifiStatus() throws RemoteException { return 0; }
        public int getWirelessChargeStatus() throws RemoteException { return 0; }
        public int getModeStatus() throws RemoteException { return 0; }
        public void registerSystemCallback(ISystemCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mSystemCallbacks) {
                mSystemCallbacks.add(callback); 
            }
        }
        public void unregisterSystemCallback(ISystemCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mSystemCallbacks) {
                mSystemCallbacks.remove(callback); 
            }
        }
 
        public float getDRTemperature() throws RemoteException { 
            return getDriverTemperature(); 
        }
        public int getDRSeatStatus() throws RemoteException { 
            return getDriverSeatWarmerLevel().ordinal(); 
        }
        public boolean getAirCirculationState() throws RemoteException { 
            return getAirCirculation(); 
        }
        public void setAirCirculationState(boolean state) throws RemoteException { 
            setAirCirculation(state);
        }
        public int getFanDirection() throws RemoteException {
            // todo : check humax specification 
            return getAirflowStatus(DRIVER_ZONE_ID); 
        }
        public int getBlowerSpeed() throws RemoteException { 
            return getFanSpeed();
        }
        public int getPSSeatStatus() throws RemoteException {
            return getPassengerSeatWarmerLevel().ordinal();
         }
        public float getPSTemperature() throws RemoteException { 
            return getPassengerTemperature(); 
        }
        public void openClimateSetting() throws RemoteException {
            Intent intent = new Intent(OPEN_HVAC_APP);
            mContext.sendBroadcast(intent);
        }
        public void registerClimateCallback(IClimateCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mClimateCallbacks) {
                mClimateCallbacks.add(callback); 
            }
        }
        public void unregisterClimateCallback(IClimateCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mClimateCallbacks) {
                mClimateCallbacks.remove(callback); 
            }
        }

        public String getDateTime() throws RemoteException { 
            return getCurrentTime();
         } 
        public void openDateTimeSetting() throws RemoteException {
            // todo : open date time setting 
        } 
        public void registerDateTimeCallback(IDateTimeCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mDateTimeCallbacks) {
                mDateTimeCallbacks.add(callback); 
            }
        }
        public void unregisterDateTimeCallback(IDateTimeCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mDateTimeCallbacks) {
                mDateTimeCallbacks.remove(callback); 
            }
        }

        public int getUserProfileImage() throws RemoteException { return 0; } 
        public void openUserProfileSetting() throws RemoteException {
            // todo : open user profile setting 
        } 
        public void registerUserProfileCallback(IUserProfileCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mUserProfileCallbacks) {
                mUserProfileCallbacks.add(callback); 
            }
        }
        public void unregisterUserProfileCallback(IUserProfileCallback callback) throws RemoteException {
            if ( callback == null ) return;
            synchronized (mUserProfileCallbacks) {
                mUserProfileCallbacks.remove(callback); 
            }
        }
    };

    private List<ISystemCallback> mSystemCallbacks = new ArrayList<>();
    private List<IClimateCallback> mClimateCallbacks = new ArrayList<>();
    private List<IStatusBarCallback> mStatusBarCallbacks = new ArrayList<>();
    private List<IDateTimeCallback> mDateTimeCallbacks = new ArrayList<>();
    private List<IUserProfileCallback> mUserProfileCallbacks = new ArrayList<>();

    private Car mCarApiClient;
    private CarHvacManager mHvacManager;
    private Object mHvacManagerReady = new Object();

    private UserManager mUserManager;
    private ActivityManager mActivityManager;
    private Context mContext = this; 
    private boolean mIsInitialized = false;

    @GuardedBy("mCallbacks")
    //private List<Callback> mCallbacks = new ArrayList<>();
    private DataStore mDataStore = new DataStore();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            if (SystemProperties.getBoolean(DEMO_MODE_PROPERTY, false)) {
                IBinder binder = (new LocalHvacPropertyService()).getCarPropertyService();
                initHvacManager(new CarHvacManager(binder, this, new Handler()));
                return;
            }

            mCarApiClient = Car.createCar(this, mCarConnectionCallback);
            mCarApiClient.connect();
        } 
        
        requestHvacRefresh();

        initDateTime();
        initUserProfile();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHvacManager != null) {
            mHvacManager.unregisterCallback(mHardwareCallback);
        }
        if (mCarApiClient != null) {
            mCarApiClient.disconnect();
        }

        mSystemCallbacks.clear(); 
        mClimateCallbacks.clear(); 
        mStatusBarCallbacks.clear(); 
        mDateTimeCallbacks.clear();
        mUserProfileCallbacks.clear();

        if ( mContext != null ) {
            mContext.unregisterReceiver(mDateTimeChangedReceiver);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void initDateTime() {
        if ( mContext == null ) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        mContext.registerReceiver(mDateTimeChangedReceiver, filter);
    }

    private final BroadcastReceiver mDateTimeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mDateTimeCallbacks) {
                for ( IDateTimeCallback callback : mDateTimeCallbacks ) {
                    try {
                        callback.onDateTimeChanged(getCurrentTime()); 
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private String getCurrentTime() {
        DateFormat df = new SimpleDateFormat("h:mm a");
        return df.format(Calendar.getInstance().getTime());
    }

    private void initUserProfile() {
        if ( mContext == null ) return;
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE); 
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE); 

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_REMOVED);
        filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_INFO_CHANGED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        filter.addAction(Intent.ACTION_USER_STOPPED);
        filter.addAction(Intent.ACTION_USER_UNLOCKED);
        mContext.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mUserProfileCallbacks) {
                for ( IUserProfileCallback callback : mUserProfileCallbacks ) {
                    try {
                        //callback.onUserChanged(getCurrentUserBitmap()); 
                        callback.onUserChanged(0);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private Bitmap getCurrentUserBitmap() {
        if ( mUserManager == null || mActivityManager == null ) return null;
        UserInfo user = mUserManager.getUserInfo(mActivityManager.getCurrentUser());
        if ( user == null ) return null; 
        Bitmap bm = mUserManager.getUserIcon(user.id);
        if ( bm == null ) {
            bm = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(
                mContext.getResources(), user.id, false));
        }

        return bm; 
    }

    private void initHvacManager(CarHvacManager carHvacManager) {
        mHvacManager = carHvacManager;
        List<CarPropertyConfig> properties = null;
        try {
            properties = mHvacManager.getPropertyList();
            mHvacManager.registerCallback(mHardwareCallback);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in HVAC");
        }
    }

    private final CarConnectionCallback mCarConnectionCallback =
        new CarConnectionCallback() {
            @Override
            public void onConnected(Car car) {
                synchronized (mHvacManagerReady) {
                    try {
                        initHvacManager((CarHvacManager) mCarApiClient.getCarManager(
                                android.car.Car.HVAC_SERVICE));
                        mHvacManagerReady.notifyAll();
                    } catch (CarNotConnectedException e) {
                        Log.e(TAG, "Car not connected in onServiceConnected");
                    }
                }
            }

            @Override
            public void onDisconnected(Car car) {
            }
        };

    private final CarHvacManager.CarHvacEventCallback mHardwareCallback =
        new CarHvacManager.CarHvacEventCallback() {
            @Override
            public void onChangeEvent(final CarPropertyValue val) {
                Log.d(TAG, "HVAC event, id: " + val.getPropertyId());
                int areaId = val.getAreaId();
                switch (val.getPropertyId()) {
                    case CarHvacManager.ID_ZONED_FAN_DIRECTION:
                        handleFanPositionUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT:
                        handleFanSpeedUpdate(areaId, getValue(val));
                        break;
                    case CarHvacManager.ID_ZONED_TEMP_SETPOINT:
                        handleTempUpdate(val);
                        break;
                    case CarHvacManager.ID_ZONED_AIR_RECIRCULATION_ON:
                        handleAirCirculationUpdate(getValue(val));
                        break;
                    case CarHvacManager.ID_ZONED_SEAT_TEMP:
                        handleSeatWarmerUpdate(areaId, getValue(val));
                        break;
                    default:
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Unhandled HVAC event, id: " + val.getPropertyId());
                        }
                }
            }

            @Override
            public void onErrorEvent(final int propertyId, final int zone) {
            }
        };

    @SuppressWarnings("unchecked")
    public static <E> E getValue(CarPropertyValue propertyValue) {
        return (E) propertyValue.getValue();
    }

    public static boolean isAvailable(CarPropertyValue propertyValue) {
        return propertyValue.getStatus() == CarPropertyValue.STATUS_AVAILABLE;
    }

    public boolean isDriverTemperatureControlAvailable() {
        return isTemperatureControlAvailable(DRIVER_ZONE_ID);
    }

    public boolean isPassengerTemperatureControlAvailable() {
        return isTemperatureControlAvailable(PASSENGER_ZONE_ID);
    }

    private boolean isTemperatureControlAvailable(int zone) {
        if (mHvacManager != null) {
            try {
                return mHvacManager.isPropertyAvailable(
                        CarHvacManager.ID_ZONED_TEMP_SETPOINT, zone);
            } catch (android.car.CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in isTemperatureControlAvailable");
            }
        }

        return false;
    }

    private void fetchTemperature(int zone) {
        if (mHvacManager != null) {
            try {
                float value = mHvacManager.getFloatProperty(
                    CarHvacManager.ID_ZONED_TEMP_SETPOINT, zone);
                boolean available = mHvacManager.isPropertyAvailable(
                    CarHvacManager.ID_ZONED_TEMP_SETPOINT, zone);
                mDataStore.setTemperature(zone, value, available);
            } catch (android.car.CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in fetchTemperature");
            }
        }
    }

    public float getDriverTemperature() {
        float temp = mDataStore.getTemperature(DRIVER_ZONE_ID); 
        return celsiusToFahrenheit(temp); 
    }

    public float getPassengerTemperature() {
        float temp = mDataStore.getTemperature(PASSENGER_ZONE_ID); 
        return celsiusToFahrenheit(temp); 
    }

    private void handleTempUpdate(CarPropertyValue value) {
        final int zone = value.getAreaId();
        final float temp = (Float)value.getValue();
        final boolean available = value.getStatus() == CarPropertyValue.STATUS_AVAILABLE;
        boolean shouldPropagate = mDataStore.shouldPropagateTempUpdate(zone, temp, available);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Temp Update, zone: " + zone + " temp: " + temp +
                    "available: " + available + " should propagate: " + shouldPropagate);
        }
        if (shouldPropagate) {
            if ( available ) {
                synchronized (mClimateCallbacks) {
                    for ( IClimateCallback callback : mClimateCallbacks ) {
                        try {
                            if (zone == VehicleAreaSeat.SEAT_ROW_1_LEFT) {
                                callback.onDRTemperatureChanged(celsiusToFahrenheit(temp)); 
                            } else {
                                callback.onPSTemperatureChanged(celsiusToFahrenheit(temp)); 
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private float celsiusToFahrenheit(float c) {
        return c * 9 / 5 + 32;
    }

    private float fahrenheitToCelsius(float f) {
        return (f - 32) * 5 / 9;
    }
    
    private void fetchSeatWarmer(int zone) {
        if ( mHvacManager == null ) return;
        try {
            int level = mHvacManager.getIntProperty(
                CarHvacManager.ID_ZONED_SEAT_TEMP, zone);
            mDataStore.setSeatWarmerLevel(zone, level);
        } catch (android.car.CarNotConnectedException e) {
            Log.e(TAG, "Car not connected in fetchSeatWarmer");
        }
    }

    public ClimateStatus.SeatStatus getDriverSeatWarmerLevel() {
        int temp = mDataStore.getSeatWarmerLevel(DRIVER_ZONE_ID); 
        return seatWarmerLeveltoStatus(temp);
    }

    public ClimateStatus.SeatStatus getPassengerSeatWarmerLevel() {
        int temp = mDataStore.getSeatWarmerLevel(PASSENGER_ZONE_ID); 
        return seatWarmerLeveltoStatus(temp);
    }

    void handleSeatWarmerUpdate(int zone, int level) {
        boolean shouldPropagate = mDataStore.shouldPropagateSeatWarmerLevelUpdate(zone, level);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Seat Warmer Update, zone: " + zone + " level: " + level +
                    " should propagate: " + shouldPropagate);
        }
        if (shouldPropagate) {
            synchronized (mClimateCallbacks) {
                for ( IClimateCallback callback : mClimateCallbacks ) {
                    try {
                        if (zone == VehicleAreaSeat.SEAT_ROW_1_LEFT) {
                            callback.onDRSeatStatusChanged(seatWarmerLeveltoStatus(level).ordinal()); 
                        } else {
                            callback.onPSSeatStatusChanged(seatWarmerLeveltoStatus(level).ordinal()); 
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private ClimateStatus.SeatStatus seatWarmerLeveltoStatus(int level) {
        ClimateStatus.SeatStatus status = ClimateStatus.SeatStatus.NONE; 
        // todo : match enum 
        switch(level) {
            case 0: status = ClimateStatus.SeatStatus.NONE; break;
            case 1: status = ClimateStatus.SeatStatus.HEATER1; break; 
            case 2: status = ClimateStatus.SeatStatus.HEATER2; break;
            case 3: status = ClimateStatus.SeatStatus.HEATER3; break;
            case 4: status = ClimateStatus.SeatStatus.COOLER1; break;
            case 5: status = ClimateStatus.SeatStatus.COOLER2; break; 
            case 6: status = ClimateStatus.SeatStatus.COOLER3; break;
            default: break; 
        }
        return status;
    }

    private void fetchAirCirculation() {
        if (mHvacManager != null) {
            try {
                mDataStore.setAirCirculationState(mHvacManager
                        .getBooleanProperty(CarHvacManager.ID_ZONED_AIR_RECIRCULATION_ON,
                                SEAT_ALL));
            } catch (android.car.CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in fetchAirCirculationState");
            }
        }
    }

    public boolean getAirCirculation() {
        return mDataStore.getAirCirculationState();
    }

    public void setAirCirculation(final boolean state) {
        mDataStore.setAirCirculationState(state);
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... unused) {
                if (mHvacManager != null) {
                    try {
                        mHvacManager.setBooleanProperty(
                                CarHvacManager.ID_ZONED_AIR_RECIRCULATION_ON,
                                SEAT_ALL, state);
                    } catch (android.car.CarNotConnectedException e) {
                        Log.e(TAG, "Car not connected in setAcState");
                    }
                }
                return null;
            }
        };
        task.execute();
    }

    private void handleAirCirculationUpdate(boolean airCirculationState) {
        boolean shouldPropagate
                = mDataStore.shouldPropagateAirCirculationUpdate(airCirculationState);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Air Circulation Update: " + airCirculationState +
                        " should propagate: " + shouldPropagate);
        }
        if (shouldPropagate) {
            for ( IClimateCallback callback : mClimateCallbacks ) {
                try {
                    callback.onAirCirculationChanged(airCirculationState); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void fetchFanSpeed() {
        if (mHvacManager != null) {
            // todo : Car specific workaround.
            int zone = SEAT_ALL; 
            try {
                mDataStore.setFanSpeed(mHvacManager.getIntProperty(
                        CarHvacManager.ID_ZONED_FAN_SPEED_SETPOINT, zone));
            } catch (android.car.CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in fetchFanSpeed");
            }
        }
    }

    public int getFanSpeed() {
        return fanSpeedToStatus(mDataStore.getFanSpeed()).ordinal();
    }

    private void handleFanSpeedUpdate(int zone, int speed) {
        boolean shouldPropagate = mDataStore.shouldPropagateFanSpeedUpdate(zone, speed);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Fan Speed Update, zone: " + zone + " speed: " + speed +
                        " should propagate: " + shouldPropagate);
        }
    
        if (shouldPropagate) {
            for ( IClimateCallback callback : mClimateCallbacks ) {
                try {
                    callback.onBlowerSpeedChanged(fanSpeedToStatus(speed).ordinal()); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ClimateStatus.BlowerSpeedStatus fanSpeedToStatus(int speed) {
        ClimateStatus.BlowerSpeedStatus status = ClimateStatus.BlowerSpeedStatus.STEP_1; 
        // todo : change speed to status
        switch(speed) {
            case 1: status = ClimateStatus.BlowerSpeedStatus.STEP_1; break;
            case 2: status = ClimateStatus.BlowerSpeedStatus.STEP_2; break;
            case 3: status = ClimateStatus.BlowerSpeedStatus.STEP_3; break;
            case 4: status = ClimateStatus.BlowerSpeedStatus.STEP_4; break;
            case 5: status = ClimateStatus.BlowerSpeedStatus.STEP_5; break;
            case 6: status = ClimateStatus.BlowerSpeedStatus.STEP_6; break;
            case 7: status = ClimateStatus.BlowerSpeedStatus.STEP_7; break;
            case 8: status = ClimateStatus.BlowerSpeedStatus.STEP_8; break;
            default: break;
        }
        return status;
    }

    private void fetchAirflow(int zone) {
        if (mHvacManager != null) {
            zone = SEAT_ALL; // Car specific workaround.
            try {
                int val = mHvacManager.getIntProperty(CarHvacManager.ID_ZONED_FAN_DIRECTION, zone);
                mDataStore.setAirflow(zone, fanPositionToAirflowIndex(val));
            } catch (android.car.CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in fetchAirFlow");
            }
        }
    }

    public int getAirflowStatus(int zone) {
        return airflowIndexToStatus(mDataStore.getAirflow(zone)).ordinal();
    }

    private void handleFanPositionUpdate(int zone, int position) {
        int index = fanPositionToAirflowIndex(position);
        boolean shouldPropagate = mDataStore.shouldPropagateFanPositionUpdate(zone, index);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Fan Position Update, zone: " + zone + " position: " + position +
                    " should propagate: " + shouldPropagate);
        }
        if (shouldPropagate) {
            for ( IClimateCallback callback : mClimateCallbacks ) {
                try {
                    callback.onFanDirectionChanged(airflowIndexToStatus(index).ordinal()); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int fanPositionToAirflowIndex(int fanPosition) {
        for (int i = 0; i < AIRFLOW_STATES.length; i++) {
            if (fanPosition == AIRFLOW_STATES[i]) {
                return i;
            }
        }
        Log.e(TAG, "Unknown fan position " + fanPosition + ". Returning default.");
        return 0;
    }

    private ClimateStatus.ClimateModeStatus airflowIndexToStatus(int index) {
        ClimateStatus.ClimateModeStatus status = ClimateStatus.ClimateModeStatus.FLOOR;
        switch(index) {
            case 0: status = ClimateStatus.ClimateModeStatus.FLOOR; break;
            case 1: status = ClimateStatus.ClimateModeStatus.FACE; break;
            case 2: status = ClimateStatus.ClimateModeStatus.FLOOR_FACE; break; 
            case 3: status = ClimateStatus.ClimateModeStatus.FLOOR_DEFROST; break;
            default: break; 
        }
        return status; 
    }

    public void requestHvacRefresh() {
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {

                synchronized (mHvacManagerReady) {
                    while (mHvacManager == null) {
                        try {
                            mHvacManagerReady.wait();
                        } catch (InterruptedException e) {
                            // We got interrupted so we might be shutting down.
                            return null;
                        }
                    }
                }

                fetchTemperature(DRIVER_ZONE_ID);
                fetchTemperature(PASSENGER_ZONE_ID);
                fetchSeatWarmer(DRIVER_ZONE_ID);
                fetchSeatWarmer(PASSENGER_ZONE_ID);
                fetchFanSpeed();
                fetchAirflow(DRIVER_ZONE_ID);
                fetchAirflow(PASSENGER_ZONE_ID);
                fetchAirCirculation();
                
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                for ( IStatusBarCallback callback : mStatusBarCallbacks ) {
                    try {
                        callback.onInitialized(); 
                    } catch (RemoteException e) 
                    {
                        e.printStackTrace();
                    }
                }
                mIsInitialized = true;
            }
        };
        task.execute();
    }
}
