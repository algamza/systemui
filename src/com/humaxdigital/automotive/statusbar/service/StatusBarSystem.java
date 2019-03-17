package com.humaxdigital.automotive.statusbar.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.RemoteException;

import android.graphics.Bitmap;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.ServiceConnection;
import android.content.ComponentName;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import com.humaxdigital.automotive.statusbar.user.PerUserService;
import com.humaxdigital.automotive.statusbar.user.IUserService;
import com.humaxdigital.automotive.statusbar.user.IUserBluetooth;
import com.humaxdigital.automotive.statusbar.user.IUserWifi;
import com.humaxdigital.automotive.statusbar.user.IUserAudio;

public class StatusBarSystem extends IStatusBarSystem.Stub {
    private static final String TAG = "StatusBarSystem";
    
    private static final String OPEN_DATE_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_CLOCK";
    private static final String OPEN_USERPROFILE_SETTING = "com.humaxdigital.automotive.app.USERPROFILE";

    private SystemDateTimeController mDateTimeController;
    private SystemUserProfileController mUserProfileController;

    private SystemDataController mDataController; 
    private SystemWifiController mWifiController; 
    private SystemLocationController mLocationController; 
    private SystemAntennaController mAntennaController; 
    private SystemBLEController mBLEController; 
    private SystemBTBatteryController mBTBatteryController; 
    private SystemCallController mCallController; 
    private SystemMuteController mMuteController; 
    private SystemWirelessChargeController mWirelessChargeController; 

    private CarExtensionClient mCarExClient = null; 
    private TMSClient mTMSClient = null; 

    private List<BaseController> mControllers = new ArrayList<>(); 
    private List<IStatusBarSystemCallback> mSystemCallbacks = new ArrayList<>();

    private IUserService mUserService;
    private IUserBluetooth mUserBluetooth;
    private IUserAudio mUserAudio;
    private IUserWifi mUserWifi;
    private final Object mServiceBindLock = new Object();
    private boolean mBound = false;

    private DataStore mDataStore = null;
    private Context mContext = null;

    private boolean mTouchDisable = false; 
  
    public StatusBarSystem(Context context, DataStore datastore) {
        if ( context == null || datastore == null ) return;
        Log.d(TAG, "StatusBarSystem");
        
        mContext = context; 
        mDataStore = datastore; 

        mTMSClient = new TMSClient(mContext); 
        mTMSClient.connect(); 

        createSystemManager(); 

        bindToUserService();
        registUserSwicher();
    }

    public void destroy() {
        Log.d(TAG, "destroy");
        unregistUserSwicher();
        unbindToUserService();

        if ( mTMSClient != null ) mTMSClient.disconnect();

        mSystemCallbacks.clear(); 

        if ( mDataController != null )  mDataController.removeListener(mSystemDataListener);
        if ( mWifiController != null )  mWifiController.removeListener(mSystemWifiListener);
        if ( mLocationController != null )  mLocationController.removeListener(mSystemBLEListener);
        if ( mAntennaController != null )  mAntennaController.removeListener(mSystemAntennaListener);
        if ( mBLEController != null )  mBLEController.removeListener(mSystemBLEListener);
        if ( mBTBatteryController != null )  mBTBatteryController.removeListener(mSystemBTBatteryListener);
        if ( mCallController != null )  mCallController.removeListener(mSystemCallListener);
        if ( mMuteController != null )  mMuteController.removeListener(mSystemMuteListener);
        if ( mDateTimeController != null )  {
            mDateTimeController.removeTimeTypeListener(mTimeTypeListener); 
            mDateTimeController.removeListener(mDateTimeListener);
        }
        if ( mUserProfileController != null )  mUserProfileController.removeListener(mUserProfileListener);

        for ( BaseController controller : mControllers ) controller.disconnect();

        mControllers.clear();

        mCarExClient = null;
        mTMSClient = null;
        mDataController = null;
        mWifiController = null;
        mLocationController = null;
        mAntennaController = null;
        mBLEController = null;
        mBTBatteryController = null;
        mCallController = null;
        mMuteController = null;
        mDateTimeController = null;
        mUserProfileController = null;
    }

    public void fetchCarExClient(CarExtensionClient client) {
        Log.d(TAG, "fetchCarExClient");
        mCarExClient = client; 

        if ( mCarExClient == null ) {
            if ( mTMSClient != null ) mTMSClient.fetch(null);
            if ( mAntennaController != null ) mAntennaController.fetchTMSClient(null); 
            if ( mCallController != null ) mCallController.fetchTMSClient(null);
            if ( mLocationController != null ) mLocationController.fetchTMSClient(null); 
            if ( mBLEController != null ) mBLEController.fetch(null); 
            if ( mWirelessChargeController != null ) mWirelessChargeController.fetch(null); 
            return;
        }

        if ( mTMSClient != null ) {
            mTMSClient.fetch(mCarExClient.getTMSManager()); 
            if ( mAntennaController != null ) 
                mAntennaController.fetchTMSClient(mTMSClient); 
            if ( mCallController != null )    
                mCallController.fetchTMSClient(mTMSClient);
            if ( mLocationController != null ) 
                mLocationController.fetchTMSClient(mTMSClient); 
        }
        if ( mBLEController != null ) 
            mBLEController.fetch(mCarExClient.getBLEManager()); 
        if ( mWirelessChargeController != null ) 
            mWirelessChargeController.fetch(mCarExClient.getSystemManager()); 

        try {
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onSystemInitialized();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void touchDisable(boolean disable) {
        mTouchDisable = disable; 
    }

    private void createSystemManager() {
        Log.d(TAG, "createSystemManager");
        if ( mContext == null || mDataStore == null ) return; 
        mDateTimeController = new SystemDateTimeController(mContext, mDataStore); 
        mDateTimeController.addListener(mDateTimeListener);
        mDateTimeController.addTimeTypeListener(mTimeTypeListener); 
        mControllers.add(mDateTimeController); 

        mUserProfileController = new SystemUserProfileController(mContext, mDataStore); 
        mUserProfileController.addListener(mUserProfileListener);
        mControllers.add(mUserProfileController); 

        mLocationController = new SystemLocationController(mContext, mDataStore); 
        mLocationController.addListener(mSystemLocationListener);
        mControllers.add(mLocationController); 
        
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

        mCallController = new SystemCallController(mContext, mDataStore); 
        mCallController.addListener(mSystemCallListener);
        mControllers.add(mCallController); 
        
        mMuteController = new SystemMuteController(mContext, mDataStore); 
        mMuteController.addListener(mSystemMuteListener);
        mControllers.add(mMuteController); 

        mWirelessChargeController = new SystemWirelessChargeController(mContext, mDataStore); 
        mWirelessChargeController.addListener(mSystemWirelessChargeListener);
        mControllers.add(mWirelessChargeController); 

        for ( BaseController controller : mControllers ) controller.connect(); 
        for ( BaseController controller : mControllers ) controller.fetch();

        try {
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onUserProfileInitialized();
                callback.onDateTimeInitialized();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean isSystemInitialized() throws RemoteException {
        boolean init = false; 
        if ( mCarExClient == null ) init = false;
        else init = true;
        Log.d(TAG, "isSystemInitialized="+init);
        return init; 
    }
    @Override
    public boolean isUserProfileInitialized() throws RemoteException {
        boolean init = false; 
        if ( mUserProfileController == null ) init = false;
        else init = true;
        Log.d(TAG, "isUserProfileInitialized="+init);
        return init; 
    }
    @Override
    public boolean isDateTimeInitialized() throws RemoteException {
        boolean init = false; 
        if ( mDateTimeController == null ) init = false;
        else init = true;
        Log.d(TAG, "isDateTimeInitialized="+init);
        return init; 
    }

    @Override
    public int getMuteStatus() throws RemoteException { 
        if ( mMuteController == null ) return 0; 
        int status = mMuteController.get();
        Log.d(TAG, "getMuteStatus="+status);
        return status;
    }
    @Override
    public int getBLEStatus() throws RemoteException { 
        if ( mBLEController == null ) return 0; 
        int status = mBLEController.get(); 
        Log.d(TAG, "getBLEStatus="+status);
        return status;
    }
    @Override
    public int getBTBatteryStatus() throws RemoteException { 
        if ( mBTBatteryController == null ) return 0; 
        int status = mBTBatteryController.get(); 
        Log.d(TAG, "getBTBatteryStatus="+status);
        return status;
    }
    @Override
    public int getCallStatus() throws RemoteException { 
        if ( mCallController == null ) return 0; 
        int status = mCallController.get(); 
        Log.d(TAG, "getCallStatus="+status);
        return status;
    }
    @Override
    public int getAntennaStatus() throws RemoteException { 
        if ( mAntennaController == null ) return 0; 
        int status = mAntennaController.get(); 
        Log.d(TAG, "getAntennaStatus="+status);
        return status;
    }
    @Override
    public int getDataStatus() throws RemoteException {  
        if ( mDataController == null ) return 0; 
        int status = mDataController.get(); 
        Log.d(TAG, "getDataStatus="+status);
        return status;
    }
    @Override
    public int getWifiStatus() throws RemoteException { 
        if ( mWifiController == null ) return 0; 
        int status = mWifiController.get(); 
        Log.d(TAG, "getWifiStatus="+status);
        return status;
    }
    @Override
    public int getWirelessChargeStatus() throws RemoteException { 
        if ( mWirelessChargeController == null ) return 0; 
        int status = mWirelessChargeController.get(); 
        Log.d(TAG, "getWirelessChargeStatus="+status);
        return status;
    }
    @Override
    public int getModeStatus() throws RemoteException { 
        if ( mLocationController == null ) return 0; 
        int status = mLocationController.get(); 
        Log.d(TAG, "getModeStatus="+status);
        return status;
    }
    @Override
    public String getDateTime() throws RemoteException { 
        if ( mDateTimeController == null ) return null; 
        String date = mDateTimeController.get();
        Log.d(TAG, "getDateTime="+date);
        return date;
    } 
    @Override
    public String getTimeType() throws RemoteException { 
        if ( mDateTimeController == null ) return null; 
        String date = mDateTimeController.getTimeType();
        Log.d(TAG, "getTimeType="+date);
        return date;
    } 
    @Override
    public void openDateTimeSetting() throws RemoteException {
        if ( mTouchDisable ) return;
        if ( !OPEN_DATE_SETTING.equals("") ) {
            Log.d(TAG, "openDateTimeSetting="+OPEN_DATE_SETTING);
            Intent intent = new Intent(OPEN_DATE_SETTING);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    } 
    @Override
    public BitmapParcelable getUserProfileImage() throws RemoteException { 
        if ( mUserProfileController == null ) return null; 
        Log.d(TAG, "getUserProfileImage");
        return new BitmapParcelable(mUserProfileController.get()); 
    } 
    @Override
    public void openUserProfileSetting() throws RemoteException {
        if ( mTouchDisable ) return;
        if ( !OPEN_USERPROFILE_SETTING.equals("") ) {
            Log.d(TAG, "openUserProfileSetting="+OPEN_USERPROFILE_SETTING);
            Intent intent = new Intent(OPEN_USERPROFILE_SETTING);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    } 
    @Override
    public void registerSystemCallback(IStatusBarSystemCallback callback) throws RemoteException {
        if ( callback == null ) return;
        Log.d(TAG, "registerSystemCallback");
        synchronized (mSystemCallbacks) {
            mSystemCallbacks.add(callback); 
        }
    }
    @Override
    public void unregisterSystemCallback(IStatusBarSystemCallback callback) throws RemoteException {
        if ( callback == null ) return;
        Log.d(TAG, "unregisterSystemCallback");
        synchronized (mSystemCallbacks) {
            mSystemCallbacks.remove(callback); 
        }
    }

    private BaseController.Listener mDateTimeListener = new BaseController.Listener<String>() {
        @Override
        public void onEvent(String date) {
            Log.d(TAG, "onDateTimeChanged="+date);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onDateTimeChanged(date); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private SystemDateTimeController.SystemTimeTypeListener mTimeTypeListener 
        = new SystemDateTimeController.SystemTimeTypeListener() {
        @Override
        public void onTimeTypeChanged(String type) {
            Log.d(TAG, "onTimeTypeChanged="+type);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onTimeTypeChanged(type); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mUserProfileListener = new BaseController.Listener<Bitmap>() {
        @Override
        public void onEvent(Bitmap bitmap) {
            Log.d(TAG, "onUserChanged");
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
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
            Log.d(TAG, "onAntennaStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
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
            Log.d(TAG, "onBLEStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
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
            Log.d(TAG, "onBTBatteryStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onBTBatteryStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemCallListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onCallStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onCallStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private BaseController.Listener mSystemMuteListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onMuteStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
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
            Log.d(TAG, "onWirelessChargeStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
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
            Log.d(TAG, "onModeStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
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
            Log.d(TAG, "onWifiStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
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
            Log.d(TAG, "onDataStatusChanged="+status);
            for ( IStatusBarSystemCallback callback : mSystemCallbacks ) {
                try {
                    callback.onDataStatusChanged(status); 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }; 

    private void registUserSwicher() {
        Log.d(TAG, "registUserSwicher");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        if ( mContext != null ) 
            mContext.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistUserSwicher() {
        Log.d(TAG, "unregistUserSwicher");
        if ( mContext != null ) mContext.unregisterReceiver(mUserChangeReceiver);
    }
    
    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mUserChangeReceiver");
            unbindToUserService();
            bindToUserService();
        }
    };

    private void bindToUserService() {
        Log.d(TAG, "bindToUserService");
        if ( mContext == null ) return;
        Intent intent = new Intent(mContext, PerUserService.class); 
        synchronized (mServiceBindLock) {
            mBound = true;
            boolean result = false; 
            if ( mContext != null ) result = mContext.bindServiceAsUser(intent, mUserServiceConnection,
                    Context.BIND_AUTO_CREATE, UserHandle.CURRENT);
            if ( result == false ) {
                Log.e(TAG, "bindToUserService() failed to get valid connection");
                unbindToUserService();
            }
        }
    }

    private void unbindToUserService() {
        Log.d(TAG, "unbindToUserService");
        synchronized (mServiceBindLock) {
            if (mBound) {
                if ( mContext != null ) 
                    mContext.unbindService(mUserServiceConnection);
                mBound = false;
            }
        }
    }
    
    private final ServiceConnection mUserServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mUserService = IUserService.Stub.asInterface(service);
            if ( mUserService == null ) return;
            try {
                mUserBluetooth = mUserService.getUserBluetooth();
                mUserWifi = mUserService.getUserWifi();
                mUserAudio = mUserService.getUserAudio();

                if ( mBTBatteryController != null ) mBTBatteryController.fetchUserBluetooth(mUserBluetooth); 
                if ( mAntennaController != null ) mAntennaController.fetchUserBluetooth(mUserBluetooth); 
                if ( mCallController != null ) mCallController.fetchUserBluetooth(mUserBluetooth); 
                if ( mMuteController != null ) mMuteController.fetchUserAudio(mUserAudio); 
                if ( mCallController != null ) mCallController.fetchUserAudio(mUserAudio); 
                if ( mWifiController != null ) mWifiController.fetchUserWifi(mUserWifi); 

            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            if ( mBTBatteryController != null ) mBTBatteryController.fetchUserBluetooth(null); 
            if ( mAntennaController != null ) mAntennaController.fetchUserBluetooth(null); 
            if ( mCallController != null ) mCallController.fetchUserBluetooth(null); 
            if ( mMuteController != null ) mMuteController.fetchUserAudio(null); 
            if ( mCallController != null ) mCallController.fetchUserAudio(null); 
            if ( mWifiController != null ) mWifiController.fetchUserWifi(null); 
            
            mUserBluetooth = null;
            mUserWifi = null;
            mUserAudio = null;
        }
    };
}
