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
import com.humaxdigital.automotive.systemui.droplist.impl.CarExtensionClient;

import com.humaxdigital.automotive.systemui.droplist.user.UserDroplistService;
import com.humaxdigital.automotive.systemui.droplist.user.IUserService;
import com.humaxdigital.automotive.systemui.droplist.user.IUserBluetooth;
import com.humaxdigital.automotive.systemui.droplist.user.IUserWifi;
import com.humaxdigital.automotive.systemui.droplist.user.IUserAudio;

import android.car.CarNotConnectedException;
import android.extension.car.util.PowerState;
import android.extension.car.CarSystemManager;
import android.car.hardware.CarPropertyValue;
import android.extension.car.value.CarEventValue;

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
    private CarExtensionClient mCarClient; 
    private CarSystemManager mCarSystem; 
    private List<BaseImplement> mImplements = new ArrayList<>();
    private boolean isPowerOn = true;
    private boolean mIsVolumeSettingsActivated = false; 

    private ContentResolver mVRContentResolver;
    private ContentObserver mVRObserver;
    private final String SETTINGS_VR = "vr_shown";

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

        for ( BaseImplement impl : mImplements ) impl.create();

        registUserSwicher();
        registApplicationActionReceiver();
        bindToUserService();

        initVRObserver();
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
        public void onPowerChanged(boolean on) {}
        public void onVolumeSettingsActivated(boolean on) {}
        public void onVRStateChanged(boolean on) {}
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
    public boolean isPowerOn() {
        return isPowerOn; 
    }
    public boolean isVolumeSettingsActivated() {
        return mIsVolumeSettingsActivated; 
    }
    public void openThemeSetting() {
        Log.d(TAG, "openThemeSetting");
        openActivity(ACTION_OPEN_THEME_SETTING); 
    }

    public void openSetup() {
        Log.d(TAG, "openSetup");
        openActivity(ACTION_OPEN_SETUP); 
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
            if ( mBluetooth != null ) mBluetooth.refresh();

            unbindToUserService();
            bindToUserService();
        }
    };

    private void bindToUserService() {
        Log.d(TAG, "bindToUserService");
        Intent intent = new Intent(this, UserDroplistService.class); 
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
                if ( mWifi != null ) mWifi.fetch(mUserWifi);

            } catch( RemoteException e ) {
                Log.e(TAG, "error:"+e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            if ( mMute != null ) mMute.fetch(null);
            if ( mWifi != null ) mWifi.fetch(null);
            mUserBluetooth = null;
            mUserWifi = null;
            mUserAudio = null;
        }
    };

    private final CarExtensionClient.CarExClientListener mCarClientListener = 
        new CarExtensionClient.CarExClientListener() {
        @Override
        public void onConnected() {
            if ( mMute != null ) mMute.fetchEx(mCarClient);
            if ( mBrightness != null ) mBrightness.fetchEx(mCarClient);
            if ( mAutoMode != null ) mAutoMode.fetchEx(mCarClient);
            if ( mQuietMode != null ) mQuietMode.fetchEx(mCarClient);
            
            if ( mCarClient != null ) {
                mCarSystem = mCarClient.getSystemManager();
                if ( mCarSystem == null ) return;
                try {
                    mCarSystem.registerCallback(mSystemCallback);
                    isPowerOn = mCarSystem.getCurrentPowerState() 
                        == PowerState.POWER_STATE_POWER_OFF ? false:true;
                } catch (CarNotConnectedException e) {
                    Log.e(TAG, "Car is not connected!");
                }
                
                Log.d(TAG, "isPowerOn="+isPowerOn); 
                for ( SystemCallback callback : mCallbacks )
                    callback.onPowerChanged(isPowerOn);
            }
        }

        @Override
        public void onDisconnected() {
            if ( mMute != null ) mMute.fetchEx(null);
            if ( mBrightness != null ) mBrightness.fetchEx(null);
            if ( mAutoMode != null ) mAutoMode.fetchEx(null);
            if ( mQuietMode != null ) mQuietMode.fetchEx(null);
        }
    };

    private void openActivity(String action) {
        if ( !action.equals("") ) {
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    private final CarSystemManager.CarSystemEventCallback mSystemCallback = 
        new CarSystemManager.CarSystemEventCallback () {
        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            switch(value.getPropertyId()) {
                case CarSystemManager.VENDOR_SYSTEM_LASTPOWERSTATE: {
                    Log.d(TAG, "VENDOR_SYSTEM_LASTPOWERSTATE="+value.getValue()); 
                    if( value.getValue().equals(PowerState.POWER_STATE_POWER_OFF) ) {
                        if ( !isPowerOn ) return;
                        isPowerOn = false;
                    } else {
                        if ( isPowerOn ) return;
                        isPowerOn = true;
                    }
                    for ( SystemCallback callback : mCallbacks ) 
                        callback.onPowerChanged(isPowerOn);
                    break;
                }
            }
        }
        @Override
        public void onChangeEvent(final CarEventValue value) {
        }
        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
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

    private void initVRObserver() {
        mVRContentResolver = getContentResolver();
        if ( mVRContentResolver == null ) return; 
        mVRObserver = createVRObserver(); 
        mVRContentResolver.registerContentObserver(
            Settings.Global.getUriFor(SETTINGS_VR), 
            false, mVRObserver, UserHandle.USER_CURRENT); 
    }
}
