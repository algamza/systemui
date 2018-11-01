 package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;

import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.os.RemoteException;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.widget.Toast; 

/*
import android.bluetooth.BluetoothAdapter; 
import android.bluetooth.BluetoothManager; 
import android.bluetooth.BluetoothGatt; 
*/

import java.util.ArrayList;
import java.util.List;


public class StatusBarService extends Service {

    private static final String TAG = "StatusBarService";
    
    private static final String OPEN_HVAC_APP = "android.car.intent.action.TOGGLE_HVAC_CONTROLS";
    private static final String OPEN_DATE_SETTING = "";
    private static final String OPEN_USERPROFILE_SETTING = "";
    
    private Context mContext = this; 
    
    //private BluetoothManager mBTMgr; 
    //private BluetoothAdapter mBTAdapter; 

    private ClimateControllerManager mClimateManager; 

    private SystemDateTimeController mDateTimeController;
    private SystemUserProfileController mUserProfileController;

    private SystemDataController mDataController; 
    private SystemWifiController mWifiController; 
    private SystemLocationController mLocationController; 
    private SystemAntennaController mAntennaController; 
    private SystemBLEController mBLEController; 
    private SystemBTBatteryController mBTBatteryController; 
    private SystemBTCallController mBTCallController; 
    private SystemMuteController mMuteController; 
    private SystemWirelessChargeController mWirelessChargeController; 

    private List<BaseController> mControllers = new ArrayList<>(); 

    private DataStore mDataStore = new DataStore();

    private List<ISystemCallback> mSystemCallbacks = new ArrayList<>();
    private List<IClimateCallback> mClimateCallbacks = new ArrayList<>();
    private List<IStatusBarCallback> mStatusBarCallbacks = new ArrayList<>();
    private List<IDateTimeCallback> mDateTimeCallbacks = new ArrayList<>();
    private List<IUserProfileCallback> mUserProfileCallbacks = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        createSystemManager(); 
        connectSystemManager(); 
        fetchSystemManager(); 

        mClimateManager = new ClimateControllerManager(mContext, mDataStore); 
        if ( mClimateManager != null ) {
            mClimateManager.registerListener(mClimateManagerListener); 
            mClimateManager.connect();
            mClimateManager.fetch(); 
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if ( mClimateManager != null ) mClimateManager.disconnect(); 

        mSystemCallbacks.clear(); 
        mClimateCallbacks.clear(); 
        mStatusBarCallbacks.clear(); 
        mDateTimeCallbacks.clear();
        mUserProfileCallbacks.clear();

        if ( mDataController != null )  mDataController.removeListener(mSystemDataListener);
        if ( mWifiController != null )  mWifiController.removeListener(mSystemDataListener);
        if ( mLocationController != null )  mLocationController.removeListener(mSystemDataListener);
        if ( mAntennaController != null )  mAntennaController.removeListener(mSystemDataListener);
        if ( mBLEController != null )  mBLEController.removeListener(mSystemDataListener);
        if ( mBTBatteryController != null )  mBTBatteryController.removeListener(mSystemDataListener);
        if ( mBTCallController != null )  mBTCallController.removeListener(mSystemDataListener);
        if ( mMuteController != null )  mMuteController.removeListener(mSystemDataListener);
        if ( mDateTimeController != null )  mDateTimeController.removeListener(mSystemDataListener);
        if ( mUserProfileController != null )  mUserProfileController.removeListener(mSystemDataListener);

        for ( BaseController controller : mControllers ) controller.disconnect();
            
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void createSystemManager() {
        mDateTimeController = new SystemDateTimeController(mContext, mDataStore); 
        mDateTimeController.addListener(mDateTimeListener);
        mControllers.add(mDateTimeController); 

        mUserProfileController = new SystemUserProfileController(mContext, mDataStore); 
        mUserProfileController.addListener(mUserProfileListener);
        mControllers.add(mUserProfileController); 

        // todo : security error, add to white list 
        /*
        mLocationController = new SystemLocationController(mContext, mDataStore); 
        mLocationController.addListener(mSystemLocationListener);
        mControllers.add(mLocationController); 
        */

        mDataController = new SystemDataController(mContext, mDataStore); 
        mDataController.addListener(mSystemDataListener);
        mControllers.add(mDataController); 

        mWifiController = new SystemWifiController(mContext, mDataStore); 
        mWifiController.addListener(mSystemWifiListener);
        mControllers.add(mWifiController); 

        mAntennaController = new SystemAntennaController(mContext, mDataStore); 
        mAntennaController.addListener(mSystemAntennaListener);
        mControllers.add(mAntennaController); 

        mBLEController = new SystemBLEController(mContext, mDataStore); 
        mBLEController.addListener(mSystemBLEListener);
        mControllers.add(mBLEController); 

        mBTBatteryController = new SystemBTBatteryController(mContext, mDataStore); 
        mBTBatteryController.addListener(mSystemBTBatteryListener);
        mControllers.add(mBTBatteryController); 

        mBTCallController = new SystemBTCallController(mContext, mDataStore); 
        mBTCallController.addListener(mSystemBTCallListener);
        mControllers.add(mBTCallController); 
        
        mMuteController = new SystemMuteController(mContext, mDataStore); 
        mMuteController.addListener(mSystemMuteListener);
        mControllers.add(mMuteController); 

        mWirelessChargeController = new SystemWirelessChargeController(mContext, mDataStore); 
        mWirelessChargeController.addListener(mSystemWirelessChargeListener);
        mControllers.add(mWirelessChargeController); 
    }

    private void connectSystemManager() {
        for ( BaseController controller : mControllers ) controller.connect(); 
    }

    private void fetchSystemManager() {
        for ( BaseController controller : mControllers ) controller.fetch();
    }
    
    private BaseController.Listener mDateTimeListener = new BaseController.Listener<String>() {
        @Override
        public void onEvent(String date) {
            for ( IDateTimeCallback callback : mDateTimeCallbacks ) {
                try {
                    callback.onDateTimeChanged(date); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mUserProfileListener = new BaseController.Listener<Bitmap>() {
        @Override
        public void onEvent(Bitmap bitmap) {
            for ( IUserProfileCallback callback : mUserProfileCallbacks ) {
                try {
                    callback.onUserChanged(new BitmapParcelable(bitmap)); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 


    private BaseController.Listener mSystemAntennaListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onAntennaStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemBLEListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onBLEStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemBTBatteryListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onBTBatteryStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemBTCallListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onBTCallStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemMuteListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onMuteStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemWirelessChargeListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onWirelessChargeStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 
    
    private BaseController.Listener mSystemLocationListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onModeStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemWifiListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onWifiStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemDataListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            for ( ISystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onDataStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private ClimateControllerManager.ClimateListener mClimateManagerListener = 
        new ClimateControllerManager.ClimateListener() {
            @Override
            public void onInitialized() {
                try {
                    for ( IStatusBarCallback callback : mStatusBarCallbacks ) {
                        callback.onInitialized();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDriverTemperatureChanged(float temp) {
                try {
                    for ( IClimateCallback callback : mClimateCallbacks ) 
                        callback.onDRTemperatureChanged(temp); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onDriverSeatStatusChanged(int status) {
                try {
                    for ( IClimateCallback callback : mClimateCallbacks ) 
                        callback.onDRSeatStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAirCirculationChanged(boolean isOn) {
                try {
                    for ( IClimateCallback callback : mClimateCallbacks ) 
                        callback.onAirCirculationChanged(isOn); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFanDirectionChanged(int status) {
                try {
                    for ( IClimateCallback callback : mClimateCallbacks ) 
                        callback.onFanDirectionChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFanSpeedStatusChanged(int status) {
                try {
                    for ( IClimateCallback callback : mClimateCallbacks ) 
                        callback.onBlowerSpeedChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPassengerSeatStatusChanged(int status) {
                try {
                    for ( IClimateCallback callback : mClimateCallbacks ) 
                        callback.onPSSeatStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPassengerTemperatureChanged(float temp) {
                try {
                    for ( IClimateCallback callback : mClimateCallbacks ) 
                        callback.onPSTemperatureChanged(temp); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }; 

        private final IStatusBarService.Stub mBinder = new IStatusBarService.Stub() {
            public boolean isInitialized() throws RemoteException {
                if ( mClimateManager != null ) return mClimateManager.isInitialized(); 
                return false; 
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
            public int getWifiStatus() throws RemoteException { 
                if ( mWifiController == null ) return 0; 
                return mWifiController.get(); 
            }
            public int getWirelessChargeStatus() throws RemoteException { return 0; }
    
            public int getModeStatus() throws RemoteException { 
                if ( mLocationController == null ) return 0; 
                return mLocationController.get(); 
            }
    
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
                if ( mClimateManager == null ) return 0.0f; 
                return (float)mClimateManager.getController(ClimateControllerManager.ControllerType.DRIVER_TEMPERATURE).get(); 
            }
            public int getDRSeatStatus() throws RemoteException { 
                if ( mClimateManager == null ) return 0; 
                return (int)mClimateManager.getController(ClimateControllerManager.ControllerType.DRIVER_SEAT).get(); 
            }
            public boolean getAirCirculationState() throws RemoteException { 
                if ( mClimateManager == null ) return false; 
                return (boolean)mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CIRCULATION).get(); 
            }
            public void setAirCirculationState(boolean state) throws RemoteException { 
                if ( mClimateManager == null ) return;  
                mClimateManager.getController(ClimateControllerManager.ControllerType.AIR_CIRCULATION).set(state); 
            }
            public int getFanDirection() throws RemoteException {
                if ( mClimateManager == null ) return 0; 
                return (int)mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_DIRECTION).get(); 
            }
            public int getBlowerSpeed() throws RemoteException { 
                if ( mClimateManager == null ) return 0; 
                return (int)mClimateManager.getController(ClimateControllerManager.ControllerType.FAN_SPEED).get(); 
            }
            public int getPSSeatStatus() throws RemoteException {
                if ( mClimateManager == null ) return 0; 
                return (int)mClimateManager.getController(ClimateControllerManager.ControllerType.PASSENGER_SEAT).get(); 
             }
            public float getPSTemperature() throws RemoteException { 
                if ( mClimateManager == null ) return 0; 
                return (float)mClimateManager.getController(ClimateControllerManager.ControllerType.PASSENGER_TEMPERATURE).get(); 
            }
            public void openClimateSetting() throws RemoteException {
                // todo : open hvac application 
                //Intent intent = new Intent(OPEN_HVAC_APP);
                //mContext.sendBroadcast(intent);
                Toast.makeText(getApplicationContext(), "open climate application", Toast.LENGTH_LONG).show(); 
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
                if ( mDateTimeController == null ) return null; 
                return mDateTimeController.get();
             } 
            public void openDateTimeSetting() throws RemoteException {
                // todo : open date time setting 
                Toast.makeText(getApplicationContext(), "open date time setting", Toast.LENGTH_LONG).show();
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
    
            public BitmapParcelable getUserProfileImage() throws RemoteException { 
                if ( mUserProfileController == null ) return null; 
                return new BitmapParcelable(mUserProfileController.get()); 
            } 
            public void openUserProfileSetting() throws RemoteException {
                // todo : open user profile setting 
                Toast.makeText(getApplicationContext(), "open user profile application", Toast.LENGTH_LONG).show();
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
/*
    private void connectBluetoothManager() {
        mBTMgr = (BluetoothManager)getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBTAdapter = mBTMgr.getAdapter(); 

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED); 
        registerReceiver(mBluetoothReceiver, filter);
        
        
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
        }
        

        if ( mBTAdapter == null || mBTMgr == null ) return; 
        if ( !mBTAdapter.isEnabled() ) return;

    }

    private void fetchBluetoothStatus() {

    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Todo : bluetooth 
        }
    };
    */
}
