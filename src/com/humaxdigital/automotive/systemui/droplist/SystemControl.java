package com.humaxdigital.automotive.systemui.droplist;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.Bundle;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;

import android.provider.Settings;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;

import com.android.internal.annotations.GuardedBy;

import android.extension.car.CarTMSManager;
import android.extension.car.settings.CarExtraSettings;

import com.humaxdigital.automotive.systemui.droplist.impl.ModeImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.BaseImplement;
import com.humaxdigital.automotive.systemui.droplist.impl.BeepImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.BluetoothImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.BrightnessImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.ClusterCheckImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.ClusterBrightnessImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.DisplayImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.MuteImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.QuietModeImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.SetupImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.ThemeImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.WifiImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.CallingImpl;
import com.humaxdigital.automotive.systemui.droplist.impl.CarExtensionClient;

import com.humaxdigital.automotive.systemui.user.PerUserService;
import com.humaxdigital.automotive.systemui.user.IUserService;
import com.humaxdigital.automotive.systemui.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.user.IUserWifi;
import com.humaxdigital.automotive.systemui.user.IUserAudio;

import java.util.ArrayList;
import java.util.List;
import android.util.Log; 

public class SystemControl extends Service {
    public enum SystemAutoMode {
        AUTOMATIC,
        DAYLIGHT,
        NIGHT
    };

    public enum SystemTheme {
        THEME1,
        THEME2,
        THEME3
    };

    private static final String ACTION_OPEN_WIFI_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_WIFI"; 
    private static final String ACTION_OPEN_BLUETOOTH_SETTING = "com.humaxdigital.dn8c.ACTION_BLUETOOTH_SETTINGS";  
    private static final String ACTION_OPEN_QUIET_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_SOUND_QUIET_MODE"; 
    private static final String ACTION_OPEN_AUTOMATIC_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_DISPLAY"; 
    private static final String ACTION_OPEN_THEME_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_ADVANCED_THEME_STYLE"; 
    private static final String ACTION_OPEN_SETUP = "com.humaxdigital.dn8c.ACTION_SETTINGS"; 
    private static final String ACTION_VOLUME_SETTINGS_STARTED = "com.humaxdigital.setup.ACTION_VOLUME_SETTINGS_STARTED";
    private static final String ACTION_VOLUME_SETTINGS_STOPPED = "com.humaxdigital.setup.ACTION_VOLUME_SETTINGS_STOPPED";

    private static final String TAG = "SystemControl";
    private List<SystemCallback> mCallbacks = new ArrayList<>();

    public class LocalBinder extends Binder {
        SystemControl getService() {
            return SystemControl.this;
        }
    }

    private final Binder mBinder = new LocalBinder();
    private final Object mServiceBindLock = new Object();
    @GuardedBy("mServiceBindLock")
    private boolean mBound = false;
    private IUserService mUserService;
    private IUserBluetooth mUserBluetooth;
    private IUserAudio mUserAudio;
    private IUserWifi mUserWifi;
 
    private DisplayImpl mDisplay;
    private BluetoothImpl mBluetooth;
    private ModeImpl mAutoMode;
    private BeepImpl mBeep;
    private BrightnessImpl mBrightness;
    private ClusterBrightnessImpl mClusterBrightness; 
    private ClusterCheckImpl mClusterCheck; 
    private MuteImpl mMute;
    private QuietModeImpl mQuietMode;
    private SetupImpl mSetup;
    private ThemeImpl mTheme;
    private WifiImpl mWifi;
    private CallingImpl mCalling; 
    private CarExtensionClient mCarClient; 
    private List<BaseImplement> mImplements = new ArrayList<>();
    private boolean mIsVolumeSettingsActivated = false; 

    private ContentResolver mContentResolver;
    private ContentObserver mVRObserver;
    private ContentObserver mPowerObserver;
    private final String SETTINGS_VR = "vr_shown";

    private boolean mAVOn = true; 
    private boolean mPowerOn = true;

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "onCreate");

        mCarClient = new CarExtensionClient(this)
            .registerListener(mCarClientListener)
            .connect();

        mDisplay =  new DisplayImpl(this);
        mImplements.add(mDisplay);

        mBluetooth = new BluetoothImpl(this);
        mBluetooth.setListener(mBluetoothListener);
        mImplements.add(mBluetooth);

        mAutoMode = new ModeImpl(this);
        mAutoMode.setListener(mAutoModeListener);
        mImplements.add(mAutoMode);

        mBeep = new BeepImpl(this);
        mBeep.setListener(mBeepListener);
        mImplements.add(mBeep);

        mBrightness = new BrightnessImpl(this);
        mBrightness.setListener(mBrightnessListener);
        mImplements.add(mBrightness);

        mClusterCheck = new ClusterCheckImpl(this);
        mClusterCheck.setListener(mClusterCheckListener);
        mImplements.add(mClusterCheck);

        mMute = new MuteImpl(this);
        mMute.setListener(mMuteListener);
        mImplements.add(mMute);

        mQuietMode = new QuietModeImpl(this);
        mQuietMode.setListener(mQuietModeListener);
        mImplements.add(mQuietMode);

        mSetup = new SetupImpl(this);
        mImplements.add(mSetup);

        mTheme = new ThemeImpl(this);
        mTheme.setListener(mThemeListener);
        mImplements.add(mTheme);

        mWifi = new WifiImpl(this);
        mWifi.setListener(mWifiListener);
        mImplements.add(mWifi);

        mCalling = new CallingImpl(this);
        mCalling.setListener(mCallingListener);
        mImplements.add(mCalling);

        for ( BaseImplement impl : mImplements ) impl.create();

        registUserSwicher();
        registApplicationActionReceiver();
        bindToUserService();

        initObserver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        
        unbindToUserService();
        unregistApplicationActionReceiver();
        unregistUserSwicher();
        for ( BaseImplement impl : mImplements ) impl.destroy();

        if ( mCarClient != null ) {
            mCarClient.unregisterListener(mCarClientListener).disconnect();
            mCarClient = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    public static abstract class SystemCallback {
        public void onBluetoothOnChanged(boolean isOn) {}
        public void onWifiOnChanged(boolean isOn) {}
        public void onBeepOnChanged(boolean isOn) {}
        public void onQuietModeOnChanged(boolean isOn) {}
        public void onMuteOnChanged(boolean isOn) {}
        public void onAutomaticModeChanged(SystemAutoMode mode) {}
        public void onBrightnessChanged(int brightness) {}
        public void onClusterBrightnessChanged(int brightness) {}
        public void onClusterChecked(boolean checked) {}
        public void onThemeChanged(SystemTheme theme) {}
        public void onVolumeSettingsActivated(boolean on) {}
        public void onVRStateChanged(boolean on) {}
        public void onCallingChanged(boolean on) {}
        public void onAVOnChanged(boolean on) {}
        public void onPowerOnChanged(boolean on) {}
        public void onEmergencyModeChanged(boolean enable) {}
        public void onBluelinkCallModeChanged(boolean enable) {}
        public void onImmobilizationModeChanged(boolean enable) {}
        public void onSlowdownModeChanged(boolean enable) {}
    }

    public void requestRefresh(final Runnable r, final Handler h) {
        @SuppressLint("StaticFieldLeak") final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                h.post(r);
            }
        };
        task.execute();
    }

    public void registerCallback(SystemCallback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void setBluetoothOn(boolean isOn) {
        if ( mBluetooth != null ) mBluetooth.set(isOn); 
    }
    public boolean getBluetoothOn() {
        return mBluetooth == null ? false : mBluetooth.get();
    }
    public void openBluetoothSetting() {
        openActivity(ACTION_OPEN_BLUETOOTH_SETTING); 
    };

    public void setWifiOn(boolean isOn) {
        if ( mWifi != null ) mWifi.set(isOn); 
    }
    public boolean getWifiOn() {
        return mWifi == null ? false : mWifi.get();
    }
    public void openWifiSetting() {
        openActivity(ACTION_OPEN_WIFI_SETTING); 
    };

    public void setMuteOn(boolean isOn) {
        if ( mMute != null ) mMute.set(isOn);
    }
    public boolean getMuteOn() {
        return mMute == null ? false : mMute.get();
    }

    public void setQuietModeOn(boolean isOn) {
        if ( mQuietMode != null ) mQuietMode.set(isOn);
    }
    public boolean getQuietModeOn() {
        return mQuietMode == null ? false : mQuietMode.get();
    }
    public void openQuietModeSetting() {
        openActivity(ACTION_OPEN_QUIET_SETTING); 
    };

    public void setBeepOn(boolean isOn) {
        if ( mBeep != null ) mBeep.set(isOn);
    }
    public boolean getBeepOn() {
        return mBeep == null ? false : mBeep.get();
    }

    public void setAutomaticMode(int mode) {
        if ( mAutoMode != null ) mAutoMode.set(mode);
    }
    public int getAutomaticMode() {
        if ( mAutoMode == null ) return 0; 
        return mAutoMode.get();
    }
    public void openAutomaticSetting() {
        openActivity(ACTION_OPEN_AUTOMATIC_SETTING); 
    };

    public void setBrightness(int progress) {
        if ( mBrightness != null ) mBrightness.set(progress);
    }
    public int getBrightness() {
        if ( mBrightness == null ) return 0;
        return mBrightness.get();
    }
    public void setClusterBrightness(int progress) {
        if ( mClusterBrightness != null ) mClusterBrightness.set(progress);
    }
    public int getClusterBrightness() {
        if ( mClusterBrightness == null ) return 0;
        return mClusterBrightness.get();
    }
    public void setClusterCheck(boolean check) {
        if ( mClusterCheck != null ) mClusterCheck.set(check);
    }
    public boolean getClusterCheck() {
        if ( mClusterCheck == null ) return false;
        return mClusterCheck.get();
    }

    public void displayOff() {
        if ( mDisplay != null ) mDisplay.set(false); 
    }

    public void setThemeMode(int mode) {
        if ( mTheme != null ) mTheme.set(mode);
    }
    public int getThemeMode() {
        if ( mTheme == null ) return 0; 
        return mTheme.get();
    }
    public boolean isCalling() {
        if ( mCalling == null ) return false; 
        return mCalling.get();
    }
    public boolean isVolumeSettingsActivated() {
        return mIsVolumeSettingsActivated; 
    }
    public boolean isAVOn() {
        if ( mContentResolver == null ) return false; 
        int state = Settings.Global.getInt(mContentResolver, 
            CarExtraSettings.Global.POWER_STATE, 
            CarExtraSettings.Global.POWER_STATE_NORMAL);
        if ( state == CarExtraSettings.Global.POWER_STATE_POWER_OFF 
            || state == CarExtraSettings.Global.POWER_STATE_AV_OFF ) {
            mAVOn = false;
        } else {
            mAVOn = true; 
        }
        return mAVOn; 
    }
    public boolean isPowerOn() {
        if ( mContentResolver == null ) return false; 
        int state = Settings.Global.getInt(mContentResolver, 
            CarExtraSettings.Global.POWER_STATE, 
            CarExtraSettings.Global.POWER_STATE_NORMAL);
        if ( state == CarExtraSettings.Global.POWER_STATE_POWER_OFF ) {
            mPowerOn = false;
        } else {
            mPowerOn = true; 
        }
        return mPowerOn; 
    }
    public void openThemeSetting() {
        Log.d(TAG, "openThemeSetting");
        openActivity(ACTION_OPEN_THEME_SETTING); 
    }

    public void openSetup() {
        Log.d(TAG, "openSetup");
        openActivity(ACTION_OPEN_SETUP); 
    }

    public void performClick() {
        if ( mUserAudio != null ) {
            try {
                mUserAudio.performClick();
            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            } 
        }
    }

    private void registUserSwicher() {
        Log.d(TAG, "registUserSwicher");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        this.registerReceiverAsUser(mUserChangeReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistUserSwicher() {
        this.unregisterReceiver(mUserChangeReceiver);
    }

    private void registApplicationActionReceiver() {
        Log.d(TAG, "registReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_VOLUME_SETTINGS_STARTED);
        filter.addAction(ACTION_VOLUME_SETTINGS_STOPPED);
        registerReceiverAsUser(mApplicationActionReceiver, UserHandle.ALL, filter, null, null);
    }

    private void unregistApplicationActionReceiver() {
        Log.d(TAG, "unregistReceiver");
        unregisterReceiver(mApplicationActionReceiver);
    }

    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if ( mBrightness != null ) mBrightness.refresh();
            if ( mClusterCheck != null ) mClusterCheck.refresh();
            if ( mAutoMode != null ) mAutoMode.refresh();
            if ( mTheme != null ) mTheme.refresh();
            if ( mBeep != null ) mBeep.refresh();
            if ( mQuietMode != null ) mQuietMode.refresh();
            if ( mWifi != null ) mWifi.refresh();

            unbindToUserService();
            bindToUserService();
        }
    };

    private void bindToUserService() {
        Log.d(TAG, "bindToUserService");
        Intent intent = new Intent(this, PerUserService.class); 
        synchronized (mServiceBindLock) {
            mBound = true;
            boolean result = this.bindServiceAsUser(intent, mUserServiceConnection,
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
                this.unbindService(mUserServiceConnection);
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

                if ( mMute != null ) mMute.fetch(mUserAudio);
                if ( mClusterBrightness != null ) mClusterBrightness.fetch(mUserAudio);  
                if ( mWifi != null ) mWifi.fetch(mUserWifi);
                if ( mBluetooth != null ) mBluetooth.fetch(mUserBluetooth); 

            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            if ( mMute != null ) mMute.fetch(null);
            if ( mWifi != null ) mWifi.fetch(null);
            if ( mClusterBrightness != null ) mClusterBrightness.fetch(null);  
            if ( mBluetooth != null ) mBluetooth.fetch(null); 
            mUserBluetooth = null;
            mUserWifi = null;
            mUserAudio = null;
        }
    };

    private final CarExtensionClient.CarExClientListener mCarClientListener = 
        new CarExtensionClient.CarExClientListener() {
        @Override
        public void onConnected() {
            if ( mBrightness != null ) mBrightness.fetchEx(mCarClient);
            if ( mAutoMode != null ) mAutoMode.fetchEx(mCarClient);
            if ( mQuietMode != null ) mQuietMode.fetchEx(mCarClient);
            if ( mCarClient != null ) 
                mCarClient.getTMSManager().registerCallback(mTMSEventListener); 
            
        }

        @Override
        public void onDisconnected() {
            if ( mCarClient != null ) 
                mCarClient.getTMSManager().unregisterCallback(mTMSEventListener); 
            if ( mBrightness != null ) mBrightness.fetchEx(null);
            if ( mAutoMode != null ) mAutoMode.fetchEx(null);
            if ( mQuietMode != null ) mQuietMode.fetchEx(null);
            mCarClient = null;
        }
    };

    private void openActivity(String action) {
        if ( !action.equals("") ) {
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            this.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    private BaseImplement.Listener mBluetoothListener = new BaseImplement.Listener<Boolean>() {
        @Override
        public void onChange(Boolean e) {
            for ( SystemCallback callback : mCallbacks ) {
                callback.onBluetoothOnChanged(e);
            }
        }
    };
    private BaseImplement.Listener mAutoModeListener = new BaseImplement.Listener<Integer>() {
        @Override
        public void onChange(Integer e) {
            ModeImpl.Mode impmode = ModeImpl.Mode.values()[e]; 
            SystemAutoMode mode = SystemAutoMode.AUTOMATIC;

            switch(impmode) {
                case AUTOMATIC: mode = SystemAutoMode.AUTOMATIC; break;
                case DAYLIGHT: mode = SystemAutoMode.DAYLIGHT; break;
                case NIGHT: mode = SystemAutoMode.NIGHT; break;
                default: break;
            }
            
            for ( SystemCallback callback : mCallbacks ) {
                callback.onAutomaticModeChanged(mode);
            }
        }
    };

    private BaseImplement.Listener mThemeListener = new BaseImplement.Listener<Integer>() {
        @Override
        public void onChange(Integer e) {
            ThemeImpl.Theme imptheme = ThemeImpl.Theme.values()[e]; 
            SystemTheme mode = SystemTheme.THEME1;
            switch(imptheme) {
                case THEME1: mode = SystemTheme.THEME1; break;
                case THEME2: mode = SystemTheme.THEME2; break;
                case THEME3: mode = SystemTheme.THEME3; break;
                default: break;
            }
            
            for ( SystemCallback callback : mCallbacks ) {
                callback.onThemeChanged(mode);
            }
        }
    };
    private BaseImplement.Listener mBeepListener = new BaseImplement.Listener<Boolean>() {
        @Override
        public void onChange(Boolean e) {
            for ( SystemCallback callback : mCallbacks ) {
                callback.onBeepOnChanged(e);
            }
        }
    };
    private BaseImplement.Listener mBrightnessListener = new BaseImplement.Listener<Integer>() {
        @Override
        public void onChange(Integer e) {
            for ( SystemCallback callback : mCallbacks ) {
                callback.onBrightnessChanged(e);
            }
        }
    };
    private BaseImplement.Listener mClusterCheckListener = new BaseImplement.Listener<Boolean>() {
        @Override
        public void onChange(Boolean e) {
            for ( SystemCallback callback : mCallbacks ) {
                callback.onClusterChecked(e);
            }
        }
    };
    private BaseImplement.Listener mMuteListener = new BaseImplement.Listener<Boolean>() {
        @Override
        public void onChange(Boolean e) {
            for ( SystemCallback callback : mCallbacks ) {
                callback.onMuteOnChanged(e);
            }
        }
    };
    private BaseImplement.Listener mQuietModeListener = new BaseImplement.Listener<Boolean>() {
        @Override
        public void onChange(Boolean e) {
            for ( SystemCallback callback : mCallbacks ) {
                callback.onQuietModeOnChanged(e);
            }
        }
    };
    private BaseImplement.Listener mWifiListener = new BaseImplement.Listener<Boolean>() {
        @Override
        public void onChange(Boolean e) {
            for ( SystemCallback callback : mCallbacks ) {
                callback.onWifiOnChanged(e);
            }
        }
    };
    private BaseImplement.Listener mCallingListener = new BaseImplement.Listener<Boolean>() {
        @Override
        public void onChange(Boolean e) {
            for ( SystemCallback callback : mCallbacks ) {
                callback.onCallingChanged(e);
            }
        }
    };

    private final BroadcastReceiver mApplicationActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if ( action == null ) return;
            Log.d(TAG, "mApplicationActionReceiver="+action);
            switch(action) {
                case ACTION_VOLUME_SETTINGS_STARTED: {
                    mIsVolumeSettingsActivated = true;
                    for ( SystemCallback callback : mCallbacks ) 
                        callback.onVolumeSettingsActivated(mIsVolumeSettingsActivated);
                    break;
                }
                
                case ACTION_VOLUME_SETTINGS_STOPPED: {
                    mIsVolumeSettingsActivated = false;
                    for ( SystemCallback callback : mCallbacks ) 
                        callback.onVolumeSettingsActivated(mIsVolumeSettingsActivated);
                    break;
                }
            }
        }
    };

    private CarTMSManager.CarTMSEventListener mTMSEventListener = 
        new CarTMSManager.CarTMSEventListener(){
        @Override
        public void onEmergencyMode(boolean enabled) {
            Log.d(TAG, "onEmergencyMode = "+enabled); 
        }
        @Override
        public void onBluelinkCallMode(boolean enabled) {
            Log.d(TAG, "onBluelinkCallMode = "+enabled);  
        }
        @Override
        public void onImmobilizationMode(boolean enabled) {
            Log.d(TAG, "onImmobilizationMode = "+enabled); 
        }
        @Override
        public void onSlowdownMode(boolean enabled) {
            Log.d(TAG, "onSlowdownMode = "+enabled); 
        }                
    };

    private ContentObserver createPowerObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                int state = Settings.Global.getInt(getContentResolver(), 
                    CarExtraSettings.Global.POWER_STATE, 
                    CarExtraSettings.Global.POWER_STATE_NORMAL);
                Log.d(TAG, "onChange="+state);
                if ( state == CarExtraSettings.Global.POWER_STATE_POWER_OFF ) {
                    if ( mPowerOn ) {
                        mPowerOn = false; 
                        for ( SystemCallback callback : mCallbacks )
                            callback.onPowerOnChanged(mPowerOn); 
                    }
                    if ( mAVOn ) {
                        mAVOn = false;
                        for ( SystemCallback callback : mCallbacks )
                            callback.onAVOnChanged(mAVOn);
                    }
                } else if ( state == CarExtraSettings.Global.POWER_STATE_AV_OFF ) {
                    if ( mAVOn ) {
                        mAVOn = false;
                        for ( SystemCallback callback : mCallbacks )
                            callback.onAVOnChanged(mAVOn);
                    }
                    if ( !mPowerOn ) {
                        mPowerOn = true; 
                        for ( SystemCallback callback : mCallbacks )
                            callback.onPowerOnChanged(mPowerOn); 
                    }
                } else {
                    if ( !mAVOn ) {
                        mAVOn = true;
                        for ( SystemCallback callback : mCallbacks )
                            callback.onAVOnChanged(mAVOn);
                    }
                    if ( !mPowerOn ) {
                        mPowerOn = true; 
                        for ( SystemCallback callback : mCallbacks )
                            callback.onPowerOnChanged(mPowerOn); 
                    }
                }
            }
        };
        return observer; 
    }

    private ContentObserver createVRObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                int on = Settings.Global.getInt(getContentResolver(), SETTINGS_VR, 0);
                Log.d(TAG, "onChange="+on);
                if ( on == 1 ) {
                    for ( SystemCallback callback : mCallbacks )
                        callback.onVRStateChanged(true);
                }
            }
        };
        return observer; 
    }

    private void initObserver() {
        mContentResolver = getContentResolver();
        if ( mContentResolver == null ) return; 
        mVRObserver = createVRObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(SETTINGS_VR), 
            false, mVRObserver, UserHandle.USER_ALL);
        mPowerObserver = createPowerObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CarExtraSettings.Global.POWER_STATE), 
            false, mPowerObserver, UserHandle.USER_ALL);
        int state = Settings.Global.getInt(getContentResolver(), 
            CarExtraSettings.Global.POWER_STATE, 
            CarExtraSettings.Global.POWER_STATE_NORMAL);
        if ( state == CarExtraSettings.Global.POWER_STATE_POWER_OFF ) {
            mAVOn = false;
            mPowerOn = false; 
        } else if ( state == CarExtraSettings.Global.POWER_STATE_AV_OFF ) {
            mAVOn = false;
            mPowerOn = true; 
        } else {
            mAVOn = true;
            mPowerOn = true; 
        }
    }
}
