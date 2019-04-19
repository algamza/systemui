package com.humaxdigital.automotive.statusbar.droplist.impl;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.net.Uri;
import android.os.UserHandle;

import android.extension.car.settings.CarExtraSettings;
import android.extension.car.CarEx;
import android.extension.car.CarSensorManagerEx;
import android.car.hardware.CarSensorEvent;
import android.extension.car.value.CarSensorEventEx;
import android.car.CarNotConnectedException;

public class BrightnessImpl extends BaseImplement<Integer> {
    private final String TAG = "BrightnessImpl"; 
    private enum Mode {
        AUTOMATIC,
        DAYLIGHT,
        NIGHT
    };
    private CarExtensionClient mCarClient = null;
    private CarSensorManagerEx mCarSensorEx = null;
    private ContentResolver mContentResolver;
    private ContentObserver mModeObserver; 
    private ContentObserver mBrightnessDayObserver; 
    private ContentObserver mBrightnessNightObserver; 
    private boolean mIsSensorNight = false;
    private Mode mCurrentMode = Mode.AUTOMATIC; 
    private int mCurrentNightValue = 0;
    private int mCurrentDayValue = 0;

    public BrightnessImpl(Context context) {
        super(context);
    }

    @Override
    public void create() {
        if ( mContext == null ) return;
        createObserver();
        updateValue();
    }

    @Override
    public void destroy() {
        removeObserver();
        mListener = null;
    }

    @Override
    public Integer get() {
        if ( mContext == null ) return 0; 
        int brightness = getCurrentBrightness();
        Log.d(TAG, "get="+brightness+", mode="+mCurrentMode);
        return brightness;
    }

    private int getCurrentBrightness() {
        if ( mContext == null ) return 0; 
        int brightness = 0;
        switch(mCurrentMode) {
            case AUTOMATIC: {
                if ( mIsSensorNight ) brightness = mCurrentNightValue; 
                else brightness = mCurrentDayValue; 
                break;
            }
            case DAYLIGHT: brightness = mCurrentDayValue; break;
            case NIGHT: brightness = mCurrentNightValue; break;
        }
        Log.d(TAG, "getCurrentBrightness="+brightness);
        return brightness; 
    }

    @Override
    public void set(Integer e) {
        if ( mContext == null ) return; 

        switch(mCurrentMode) {
            case AUTOMATIC: {
                if ( mIsSensorNight ) setNightBrightness(e); 
                else setDayBrightness(e);
                break;
            }
            case DAYLIGHT: setDayBrightness(e); break;
            case NIGHT: setNightBrightness(e); break; 
        }
        Log.d(TAG, "set="+e+", mode="+mCurrentMode);
    }

    private void setDayBrightness(int val) {
        Log.d(TAG, "setDayBrightness="+val);
        Settings.System.putIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_DAYLIGHT, 
            val, UserHandle.USER_CURRENT);
    }

    private void setNightBrightness(int val) {
        Log.d(TAG, "setNightBrightness="+val);
        Settings.System.putIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_NIGHT, 
            val, UserHandle.USER_CURRENT);
    }

    public void fetchEx(CarExtensionClient client) {
        Log.d(TAG, "fetchEx");
        mCarClient = client; 
        try {
            if ( client == null ) {
                if ( mCarSensorEx != null ) {
                    mCarSensorEx.unregisterListener(mSensorChangeListener); 
                    mCarSensorEx = null;
                }

                return;
            }
            mCarSensorEx = mCarClient.getSensorManager();
            if ( mCarSensorEx == null ) return;
            mCarSensorEx.registerListener(mSensorChangeListener, 
                CarSensorManagerEx.SENSOR_TYPE_NIGHT_MODE, 
                CarSensorManagerEx.SENSOR_RATE_NORMAL);
            CarSensorEventEx evt = mCarSensorEx.getLatestSensorEventEx(
                    CarSensorManagerEx.SENSOR_TYPE_NIGHT_MODE);
            if ( evt != null ) {
                CarSensorEventEx.NightModeData night = evt.getNightModeData(null);
                if ( night != null ) {
                    mIsSensorNight = night.isNightMode; 
                    Log.d(TAG, "isNightMode="+night.isNightMode);
                }
            }
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
    }

    private void createObserver() {
        if ( mContext == null ) return;
        mContentResolver = mContext.getContentResolver();
        mModeObserver = createModeObserver(); 
        mBrightnessDayObserver = createBrightnessDayObserver();
        mBrightnessNightObserver = createBrightnessNightObserver(); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.DISPLAY_MODE_TYPE), 
            false, mModeObserver, UserHandle.USER_CURRENT); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.DISPLAY_BRIGHTNESS_DAYLIGHT), 
            false, mBrightnessDayObserver, UserHandle.USER_CURRENT); 
        mContentResolver.registerContentObserver(
            Settings.System.getUriFor(CarExtraSettings.System.DISPLAY_BRIGHTNESS_NIGHT), 
            false, mBrightnessNightObserver, UserHandle.USER_CURRENT); 
    }

    private void removeObserver() {
        if ( mContentResolver != null ) {
            if ( mModeObserver != null ) mContentResolver.unregisterContentObserver(mModeObserver);
            if ( mBrightnessDayObserver != null ) mContentResolver.unregisterContentObserver(mBrightnessDayObserver);
            if ( mBrightnessNightObserver != null ) mContentResolver.unregisterContentObserver(mBrightnessNightObserver);
        }
        mModeObserver = null;
        mBrightnessDayObserver = null;
        mBrightnessNightObserver = null;
        mContentResolver = null;
    }

    private void updateValue() {
        if ( mContext == null ) return;
        int mode = Settings.System.getIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.DISPLAY_MODE_TYPE, 
            CarExtraSettings.System.DISPLAY_MODE_TYPE_DEFAULT, 
            UserHandle.USER_CURRENT);
        if ( mode == CarExtraSettings.System.DISPLAY_MODE_TYPE_AUTO ) 
            mCurrentMode = Mode.AUTOMATIC; 
        else if ( mode == CarExtraSettings.System.DISPLAY_MODE_TYPE_DAYLIGHT ) 
            mCurrentMode = Mode.DAYLIGHT; 
        else if ( mode == CarExtraSettings.System.DISPLAY_MODE_TYPE_NIGHT ) 
            mCurrentMode = Mode.NIGHT; 
        mCurrentDayValue = Settings.System.getIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_DAYLIGHT, 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_DAYLIGHT_DEFAULT,
            UserHandle.USER_CURRENT);
        mCurrentNightValue = Settings.System.getIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_NIGHT, 
            CarExtraSettings.System.DISPLAY_BRIGHTNESS_NIGHT_DEFAULT,
            UserHandle.USER_CURRENT);
        Log.d(TAG, "updateValue:mode="+mCurrentMode+", nightval="+mCurrentNightValue+", dayval="+mCurrentDayValue);
    }

    private ContentObserver createModeObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                Log.d(TAG, "mode:onChange="+mCurrentMode);
                int mode = getCurrentMode(); 
                if ( mode == CarExtraSettings.System.DISPLAY_MODE_TYPE_AUTO ) {
                    if ( mCurrentMode == Mode.AUTOMATIC ) return;
                    mCurrentMode = Mode.AUTOMATIC; 
                }
                else if ( mode == CarExtraSettings.System.DISPLAY_MODE_TYPE_NIGHT ) {
                    if ( mCurrentMode == Mode.NIGHT ) return;
                    mCurrentMode = Mode.NIGHT; 
                } else if ( mode == CarExtraSettings.System.DISPLAY_MODE_TYPE_DAYLIGHT ) {
                    if ( mCurrentMode == Mode.DAYLIGHT ) return;
                    mCurrentMode = Mode.DAYLIGHT; 
                }
                sendBrightnessChangeEvent();
            }
        };
        return observer; 
    }

    private int getCurrentMode() {
        int mode = 0; 
        if ( mContext == null ) return mode; 
        mode = Settings.System.getIntForUser(mContext.getContentResolver(), 
            CarExtraSettings.System.DISPLAY_MODE_TYPE, 
            CarExtraSettings.System.DISPLAY_MODE_TYPE_DEFAULT, 
            UserHandle.USER_CURRENT);
        Log.d(TAG, "getCurrentMode="+mode);
        return mode; 
    }

    private ContentObserver createBrightnessDayObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                mCurrentDayValue = Settings.System.getIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.DISPLAY_BRIGHTNESS_DAYLIGHT, 
                    CarExtraSettings.System.DISPLAY_BRIGHTNESS_DAYLIGHT_DEFAULT,
                    UserHandle.USER_CURRENT);
                if ( mCurrentMode != Mode.DAYLIGHT ) return;
                if ( mListener != null ) mListener.onChange(mCurrentDayValue); 
            }
        };
        return observer; 
    }

    private ContentObserver createBrightnessNightObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                mCurrentNightValue = Settings.System.getIntForUser(mContext.getContentResolver(), 
                    CarExtraSettings.System.DISPLAY_BRIGHTNESS_NIGHT, 
                    CarExtraSettings.System.DISPLAY_BRIGHTNESS_NIGHT_DEFAULT,
                    UserHandle.USER_CURRENT);
                if ( mCurrentMode != Mode.NIGHT ) return;
                if ( mListener != null ) mListener.onChange(mCurrentNightValue); 
            }
        };
        return observer; 
    }

    public void refresh() {
        Log.d(TAG, "refresh"); 
        removeObserver();
        createObserver(); 
        updateValue();
        sendBrightnessChangeEvent();
    }

    void sendBrightnessChangeEvent() {
        int brightness = getCurrentBrightness();
        Log.d(TAG, "sendBrightnessChangeEvent="+brightness);
        if ( mListener != null ) mListener.onChange(brightness); 
    }

    private final CarSensorManagerEx.OnSensorChangedListenerEx mSensorChangeListener =
        new CarSensorManagerEx.OnSensorChangedListenerEx () {
        @Override
        public void onSensorChanged(final CarSensorEvent event) {
        }

        public void onSensorChanged(final CarSensorEventEx event) {
            if ( event == null ) return;
            switch (event.sensorType) {
                case CarSensorManagerEx.SENSOR_TYPE_NIGHT_MODE: {
                    CarSensorEventEx.NightModeData night = event.getNightModeData(null);
                    if ( night == null ) break; 
                    if ( mIsSensorNight == night.isNightMode ) break;
                    mIsSensorNight = night.isNightMode; 
                    Log.d(TAG, "SENSOR_TYPE_NIGHT_MODE="+mIsSensorNight);
                    if ( mCurrentMode != Mode.AUTOMATIC ) break; 
                    sendBrightnessChangeEvent();
                    break;
                }
                default: break;
            }
        }
    };
}
