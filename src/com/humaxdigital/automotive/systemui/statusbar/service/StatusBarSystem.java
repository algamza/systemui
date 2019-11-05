package com.humaxdigital.automotive.systemui.statusbar.service;

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
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;

import android.extension.car.settings.CarExtraSettings;

import com.humaxdigital.automotive.systemui.R; 
import com.humaxdigital.automotive.systemui.common.util.OSDPopup; 
import com.humaxdigital.automotive.systemui.common.user.PerUserService;
import com.humaxdigital.automotive.systemui.common.user.IUserService;
import com.humaxdigital.automotive.systemui.common.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.common.user.IUserWifi;
import com.humaxdigital.automotive.systemui.common.user.IUserAudio;

import com.humaxdigital.automotive.systemui.common.car.CarExClient;
import com.humaxdigital.automotive.systemui.common.CONSTANTS;

public class StatusBarSystem {
    private static final String TAG = "StatusBarSystem";
    
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

    private SystemPowerStateController mPowerStateController; 

    private CarExClient mCarExClient = null; 
    private TMSClient mTMSClient = null; 

    private List<BaseController> mControllers = new ArrayList<>(); 
    private List<StatusBarSystemCallback> mSystemCallbacks = new ArrayList<>();

    private IUserService mUserService;
    private IUserBluetooth mUserBluetooth;
    private IUserAudio mUserAudio;
    private IUserWifi mUserWifi;
    private final Object mServiceBindLock = new Object();
    private boolean mBound = false;

    private DataStore mDataStore = null;
    private Context mContext = null;

    // special case
    private boolean mRearCamera = false; 
    private boolean mFrontCamera = false;
    private boolean mRearGearDetected = false; 
    private boolean mBTCall = false;
    private boolean mEmergencyCall = false;
    private boolean mBluelinkCall = false;  
    private boolean mSVIOn = false;
    private boolean mSVSOn = false; 

    public static abstract class StatusBarSystemCallback {
        public void onSystemInitialized() {}
        public void onUserProfileInitialized() {}
        public void onDateTimeInitialized() {}
        public void onMuteStatusChanged(int status) {}
        public void onBLEStatusChanged(int status) {}
        public void onBTBatteryStatusChanged(int status) {}
        public void onCallStatusChanged(int status) {}
        public void onAntennaStatusChanged(int status) {}
        public void onDataStatusChanged(int status) {}
        public void onWifiStatusChanged(int status) {}
        public void onWirelessChargeStatusChanged(int status) {}
        public void onModeStatusChanged(int status) {}
        public void onDateTimeChanged(String time) {}
        public void onTimeTypeChanged(String type) {}
        public void onUserChanged(BitmapParcelable bitmap) {}
        public void onPowerStateChanged(int state) {}
        public void onUserAgreementMode(boolean on) {}
        public void onUserSwitching(boolean on) {}
        public void onBTCalling(boolean on) {}
        public void onEmergencyMode(boolean on) {}
        public void onBluelinkMode(boolean on) {}
        public void onImmoilizationMode(boolean on) {}
        public void onSlowdownMode(boolean on) {}
        public void onRearCamera(boolean on) {}
    }

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

    public void registerSystemCallback(StatusBarSystemCallback callback) {
        if ( callback == null ) return;
        Log.d(TAG, "registerSystemCallback");
        synchronized (mSystemCallbacks) {
            mSystemCallbacks.add(callback); 
        }
    }

    public void unregisterSystemCallback(StatusBarSystemCallback callback) {
        if ( callback == null ) return;
        Log.d(TAG, "unregisterSystemCallback");
        synchronized (mSystemCallbacks) {
            mSystemCallbacks.remove(callback); 
        }
    }

    public void destroy() {
        Log.d(TAG, "destroy");
        unregistUserSwicher();
        unbindToUserService();

        if ( mTMSClient != null ) mTMSClient.disconnect();

        synchronized (mSystemCallbacks) {
            mSystemCallbacks.clear(); 
        }

        if ( mDataController != null )  mDataController.removeListener(mSystemDataListener);
        if ( mWifiController != null )  mWifiController.removeListener(mSystemWifiListener);
        if ( mLocationController != null )  mLocationController.removeListener(mSystemBLEListener);
        if ( mAntennaController != null )  mAntennaController.removeListener(mSystemAntennaListener);
        if ( mBLEController != null )  mBLEController.removeListener(mSystemBLEListener);
        if ( mBTBatteryController != null )  mBTBatteryController.removeListener(mSystemBTBatteryListener);
        if ( mCallController != null )  mCallController.removeListener(mSystemCallListener);
        if ( mMuteController != null )  mMuteController.removeListener(mSystemMuteListener);
        if ( mWirelessChargeController != null ) mWirelessChargeController.removeListener(mSystemWirelessChargeListener);
        if ( mDateTimeController != null )  {
            mDateTimeController.removeTimeTypeListener(mTimeTypeListener); 
            mDateTimeController.removeListener(mDateTimeListener);
        }
        if ( mUserProfileController != null )  mUserProfileController.removeListener(mUserProfileListener);
        if ( mPowerStateController != null ) mPowerStateController.removeListener(mPowerStateControllerListener); 

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
        mWirelessChargeController = null; 
        mDateTimeController = null;
        mUserProfileController = null;
        mPowerStateController = null; 
    }

    public void fetchCarExClient(CarExClient client) {
        Log.d(TAG, "fetchCarExClient");
        mCarExClient = client; 

        if ( mCarExClient == null ) {
            if ( mTMSClient != null ) mTMSClient.fetch(null);
            if ( mAntennaController != null ) mAntennaController.fetchTMSClient(null); 
            if ( mCallController != null ) mCallController.fetchTMSClient(null);
            if ( mLocationController != null ) mLocationController.fetchTMSClient(null); 
            if ( mDataController != null ) mDataController.fetchTMSClient(null); 
            if ( mBLEController != null ) mBLEController.fetch(null); 
            if ( mWirelessChargeController != null ) mWirelessChargeController.fetch(null); 
            if ( mPowerStateController != null ) mPowerStateController.fetch(null); 
            if ( mMuteController != null ) mMuteController.fetchAudioEx(null);
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
            if ( mDataController != null ) 
                mDataController.fetchTMSClient(mTMSClient); 
        }
        if ( mBLEController != null ) 
            mBLEController.fetch(mCarExClient.getBLEManager()); 
        if ( mWirelessChargeController != null ) 
            mWirelessChargeController.fetch(mCarExClient.getSystemManager()); 
        if ( mPowerStateController != null ) 
            mPowerStateController.fetch(mCarExClient.getSystemManager()); 
        if ( mMuteController != null ) 
            mMuteController.fetchAudioEx(mCarExClient.getAudioManager());
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onSystemInitialized();
            }
        }
    }

    public void onFrontCamera(boolean on) {
        Log.d(TAG, "onFrontCamera="+on);
        mFrontCamera = on; 
    }

    public void onRearCamera(boolean on) {
        Log.d(TAG, "onRearCamera="+on);
        mRearCamera = on; 
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                    callback.onRearCamera(on);
            }
        }
    }

    public void onRearGearDetected(boolean on) {
        Log.d(TAG, "onRearGearDetected="+on);
        mRearGearDetected = on; 
    }

    public void onBTCall(boolean on) {
        Log.d(TAG, "onBTCall="+on);
        mBTCall = on; 
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                    callback.onBTCalling(on);
            }
        }
    }

    public void onEmergencyCall(boolean on) {
        Log.d(TAG, "onEmergencyCall="+on);
        mEmergencyCall = on; 
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                    callback.onEmergencyMode(on);
            }
        }
    }

    public void onBluelinkCall(boolean on) {
        Log.d(TAG, "onBluelinkCall="+on);
        mBluelinkCall = on; 
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onBluelinkMode(on);
            }
        }
    }

    public void onSVIOn(boolean on) {
        Log.d(TAG, "onSVIOn="+on);
        mSVIOn = on; 
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onImmoilizationMode(on);
            }
        }
    }
    public void onSVSOn(boolean on) {
        Log.d(TAG, "onSVSOn="+on);
        mSVSOn = on; 
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onSlowdownMode(on);
            }
        }
    }
    public void onUserAgreement(boolean on) {
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onUserAgreementMode(on);
            }
        }
    }

    public void onUserSwitching(boolean on) {
        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onUserSwitching(on);
            }
        }
    }

    public boolean isPowerOff() {
        if ( mPowerStateController == null ) return true;
        int state = mPowerStateController.get(); 
        if ( state == SystemPowerStateController.State.POWER_OFF.ordinal() ) return true;
        return false; 
    }

    public boolean isAVOff() {
        if ( mPowerStateController == null ) return true;
        int state = mPowerStateController.get(); 
        if ( state == SystemPowerStateController.State.AV_OFF.ordinal() 
            || state == SystemPowerStateController.State.POWER_OFF.ordinal() ) return true;
        return false; 
    }
    
    public boolean isBTCalling() {
        Log.d(TAG, "isBTCalling="+mBTCall);
        return mBTCall; 
    }

    public boolean isEmergencyMode() {
        Log.d(TAG, "isEmergencyMode="+mEmergencyCall);
        return mEmergencyCall; 
    }

    public boolean isBluelinkMode() {
        Log.d(TAG, "isBluelinkMode="+mBluelinkCall);
        return mBluelinkCall; 
    }

    public boolean isImmoilizationMOde() {
        Log.d(TAG, "isImmoilizationMOde="+mSVIOn);
        return mSVIOn; 
    }

    public boolean isSlowdownMode() {
        Log.d(TAG, "isSlowdownMode="+mSVSOn);
        return mSVSOn; 
    }

    public boolean isRearCamera() {
        Log.d(TAG, "isRearCamera="+mRearCamera);
        return mRearCamera; 
    }

    public boolean isSystemInitialized() {
        boolean init = false; 
        if ( mCarExClient == null ) init = false;
        else init = true;
        Log.d(TAG, "isSystemInitialized="+init);
        return init; 
    }

    public boolean isUserProfileInitialized() {
        boolean init = false; 
        if ( mUserProfileController == null ) init = false;
        else init = true;
        Log.d(TAG, "isUserProfileInitialized="+init);
        return init; 
    }
 
    public boolean isDateTimeInitialized() {
        boolean init = false; 
        if ( mDateTimeController == null ) init = false;
        else init = true;
        Log.d(TAG, "isDateTimeInitialized="+init);
        return init; 
    }

    public int getMuteStatus() { 
        if ( mMuteController == null ) return 0; 
        int status = mMuteController.get();
        Log.d(TAG, "getMuteStatus="+status);
        return status;
    }
 
    public int getBLEStatus() { 
        if ( mBLEController == null ) return 0; 
        int status = mBLEController.get(); 
        Log.d(TAG, "getBLEStatus="+status);
        return status;
    }
  
    public int getBTBatteryStatus() { 
        if ( mBTBatteryController == null ) return 0; 
        int status = mBTBatteryController.get(); 
        Log.d(TAG, "getBTBatteryStatus="+status);
        return status;
    }
    
    public int getCallStatus() { 
        if ( mCallController == null ) return 0; 
        int status = mCallController.get(); 
        Log.d(TAG, "getCallStatus="+status);
        return status;
    }
   
    public int getAntennaStatus() { 
        if ( mAntennaController == null ) return 0; 
        int status = mAntennaController.get(); 
        Log.d(TAG, "getAntennaStatus="+status);
        return status;
    }
    
    public int getDataStatus() {  
        if ( mDataController == null ) return 0; 
        int status = mDataController.get(); 
        Log.d(TAG, "getDataStatus="+status);
        return status;
    }
    
    public int getWifiStatus() { 
        if ( mWifiController == null ) return 0; 
        int status = mWifiController.get(); 
        Log.d(TAG, "getWifiStatus="+status);
        return status;
    }
    
    public int getWirelessChargeStatus() { 
        if ( mWirelessChargeController == null ) return 0; 
        int status = mWirelessChargeController.get(); 
        Log.d(TAG, "getWirelessChargeStatus="+status);
        return status;
    }
    
    public int getModeStatus() { 
        if ( mLocationController == null ) return 0; 
        int status = mLocationController.get(); 
        Log.d(TAG, "getModeStatus="+status);
        return status;
    }
    
    public String getDateTime() { 
        if ( mDateTimeController == null ) return null; 
        String date = mDateTimeController.get();
        Log.d(TAG, "getDateTime="+date);
        return date;
    } 
    
    public String getYearDateTime() { 
        if ( mDateTimeController == null ) return null; 
        String date = mDateTimeController.getYearDateTime();
        Log.d(TAG, "getYearDateTime="+date);
        return date;
    } 
    
    public String getTimeType() { 
        if ( mDateTimeController == null ) return null; 
        String date = mDateTimeController.getTimeType();
        Log.d(TAG, "getTimeType="+date);
        return date;
    } 
    
    public void openDateTimeSetting() {
        if ( mContext == null ) return;        
        if ( mPowerStateController != null && (mPowerStateController.get() 
            == SystemPowerStateController.State.POWER_OFF.ordinal()) ) {
            Log.d(TAG, "Current Power Off"); 
            return; 
        }  
        
        if ( mRearCamera ) {
            Log.d(TAG, "Current Rear Camera"); 
            OSDPopup.send(mContext, 
                mContext.getResources().getString(R.string.STR_MESG_18334_ID));
            return;
        }

        if ( mRearGearDetected ) {
            Log.d(TAG, "Current Rear Gear"); 
            OSDPopup.send(mContext, 
                mContext.getResources().getString(R.string.STR_MESG_18334_ID));
            return;
        }

        if ( isUserAgreement() ) {
            Log.d(TAG, "Current UserAgreement"); 
            return; 
        }

        if ( isUserSwitching() ) {
            Log.d(TAG, "Current userSwitching"); 
            return; 
        }

        if ( mSVSOn || mSVIOn ) {
            Log.d(TAG, "Current svs = "+mSVSOn+", svi = "+mSVIOn);
            OSDPopup.send(mContext, 
                mContext.getResources().getString(R.string.STR_FEATURE_CURRENTLY_UNAVAILABLE_ID));
            return;
        }

        if ( !CONSTANTS.OPEN_DATE_SETTING.equals("") ) {
            Log.d(TAG, "openDateTimeSetting="+CONSTANTS.OPEN_DATE_SETTING);
            vrCloseRequest();
            Intent intent = new Intent(CONSTANTS.OPEN_DATE_SETTING);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    } 
    
    public BitmapParcelable getUserProfileImage() { 
        if ( mUserProfileController == null ) return null; 
        Log.d(TAG, "getUserProfileImage");
        return new BitmapParcelable(mUserProfileController.get()); 
    } 
    
    public void openUserProfileSetting() {        
        if ( mPowerStateController != null && (mPowerStateController.get() 
            == SystemPowerStateController.State.POWER_OFF.ordinal()) ) {
            Log.d(TAG, "Current Power Off"); 
            return; 
        } 
        
        if ( mRearCamera ) {
            Log.d(TAG, "Current Rear Camera"); 
            OSDPopup.send(mContext, 
                mContext.getResources().getString(R.string.STR_MESG_18334_ID));
            return;
        }

        if ( mRearGearDetected ) {
            Log.d(TAG, "Current Rear Gear"); 
            OSDPopup.send(mContext, 
                mContext.getResources().getString(R.string.STR_MESG_18334_ID));
            return;
        }

        if ( isUserAgreement() ) {
            Log.d(TAG, "Current UserAgreement"); 
            return; 
        }

        if ( isUserSwitching() ) {
            Log.d(TAG, "Current userSwitching"); 
            return; 
        }
        // changed special case
        if ( mBTCall ) {
            Log.d(TAG, "Current BT Call"); 
            //OSDPopup.send(mContext, 
            //    mContext.getResources().getString(R.string.STR_MESG_14845_ID));
            return;
        }
        
        if ( mEmergencyCall || mBluelinkCall ) {
            Log.d(TAG, "Current emergency="+mEmergencyCall+", bluelinkcall="+mBluelinkCall); 
            return; 
        }

        if ( mSVSOn || mSVIOn ) {
            Log.d(TAG, "Current svs = "+mSVSOn+", svi = "+mSVIOn);
            OSDPopup.send(mContext, 
                mContext.getResources().getString(R.string.STR_FEATURE_CURRENTLY_UNAVAILABLE_ID));
            return;
        }

        if ( !CONSTANTS.OPEN_USERPROFILE_SETTING.equals("") ) {
            Log.d(TAG, "openUserProfileSetting="+CONSTANTS.OPEN_USERPROFILE_SETTING);
            vrCloseRequest();
            Intent intent = new Intent(CONSTANTS.OPEN_USERPROFILE_SETTING);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    } 

    
    public boolean isUserAgreement() {
        if ( mContext == null ) return false; 
        int is_agreement = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT,
            CarExtraSettings.Global.FALSE);   
        if ( is_agreement == CarExtraSettings.Global.FALSE ) return false; 
        else return true;
    }

    public boolean isUserSwitching() {
        if ( mContext == null ) return false; 
        int isUserSwitching = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_USER_SWITCHING_START_FINISH, 
            CarExtraSettings.Global.FALSE);
        if ( isUserSwitching == CarExtraSettings.Global.TRUE ) return true; 
        else return false;
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

        mPowerStateController = new SystemPowerStateController(mContext, mDataStore); 
        mPowerStateController.addListener(mPowerStateControllerListener);
        mControllers.add(mPowerStateController); 

        for ( BaseController controller : mControllers ) controller.connect(); 
        for ( BaseController controller : mControllers ) controller.fetch();

        synchronized (mSystemCallbacks) {
            for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                callback.onUserProfileInitialized();
                callback.onDateTimeInitialized();
            }
        }
    }

    private void vrCloseRequest() {
        if ( mContext == null ) return;
        Log.d(TAG, "vrCloseRequest");
        Intent intent = new Intent(); 
        ComponentName name = new ComponentName(CONSTANTS.VR_PACKAGE_NAME, CONSTANTS.VR_RECEIVER_NAME);
        intent.setComponent(name);
        intent.setAction(CONSTANTS.VR_DISMISS_ACTION);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private BaseController.Listener mDateTimeListener = new BaseController.Listener<String>() {
        @Override
        public void onEvent(String date) {
            Log.d(TAG, "onDateTimeChanged="+date);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onDateTimeChanged(date); 
                }
            }
        }
    }; 

    private SystemDateTimeController.SystemTimeTypeListener mTimeTypeListener 
        = new SystemDateTimeController.SystemTimeTypeListener() {
        @Override
        public void onTimeTypeChanged(String type) {
            Log.d(TAG, "onTimeTypeChanged="+type);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onTimeTypeChanged(type); 
                }
            }
        }
    }; 

    private BaseController.Listener mUserProfileListener = new BaseController.Listener<Bitmap>() {
        @Override
        public void onEvent(Bitmap bitmap) {
            Log.d(TAG, "onUserChanged");
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onUserChanged(new BitmapParcelable(bitmap)); 
                }
            }
        }
    }; 


    private BaseController.Listener mSystemAntennaListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onAntennaStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onAntennaStatusChanged(status); 
                }
            }
        }
    }; 

    private BaseController.Listener mSystemBLEListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onBLEStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onBLEStatusChanged(status); 
                }
            }
        }
    }; 

    private BaseController.Listener mSystemBTBatteryListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onBTBatteryStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onBTBatteryStatusChanged(status); 
                }
            }
        }
    }; 

    private BaseController.Listener mSystemCallListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onCallStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onCallStatusChanged(status); 
                }
            }
        }
    }; 

    private BaseController.Listener mSystemMuteListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onMuteStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onMuteStatusChanged(status); 
                }
            }
        }
    }; 

    private BaseController.Listener mSystemWirelessChargeListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onWirelessChargeStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onWirelessChargeStatusChanged(status); 
                }
            }
        }
    }; 
    
    private BaseController.Listener mSystemLocationListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onModeStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onModeStatusChanged(status); 
                }
            }
        }
    }; 

    private BaseController.Listener mSystemWifiListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onWifiStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onWifiStatusChanged(status); 
                }
            }
        }
    }; 

    private BaseController.Listener mSystemDataListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer status) {
            Log.d(TAG, "onDataStatusChanged="+status);
            synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                        callback.onDataStatusChanged(status); 
                }
            }
        }
    }; 

    private BaseController.Listener mPowerStateControllerListener = new BaseController.Listener<Integer>() {
        @Override
        public void onEvent(Integer state) {
            Log.d(TAG, "mPowerStateControllerListener="+state);
                synchronized (mSystemCallbacks) {
                for ( StatusBarSystemCallback callback : mSystemCallbacks ) {
                    callback.onPowerStateChanged(state); 
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
            if ( mDateTimeController != null ) mDateTimeController.refresh();
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
