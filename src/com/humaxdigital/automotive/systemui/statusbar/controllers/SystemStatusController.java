package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.FrameLayout;
import android.os.Build; 
import android.os.Handler;

import android.util.Log;

import com.humaxdigital.automotive.systemui.util.ProductConfig;
import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.SystemView;

import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarSystem;
import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable;

import java.util.ArrayList;
import java.util.List;

public class SystemStatusController {
    static final String TAG = "SystemStatusController"; 

    enum MuteStatus { NONE, AV_MUTE, NAV_MUTE, AV_NAV_MUTE }
    enum BLEStatus { NONE, BLE_CONNECTED, BLE_CONNECTING, BLE_CONNECTION_FAIL }
    enum BTBatteryStatus { NONE, BT_BATTERY_0, BT_BATTERY_1, BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }
    enum CallStatus { NONE, HANDS_FREE_CONNECTED, STREAMING_CONNECTED, HF_FREE_STREAMING_CONNECTED
        , CALL_HISTORY_DOWNLOADING, CONTACTS_HISTORY_DOWNLOADING, TMU_CALLING, BT_CALLING, BT_PHONE_MIC_MUTE }
    enum AntennaStatus { NONE, BT_ANTENNA_NO, BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5
        , TMU_ANTENNA_NO, TMU_ANTENNA_0, TMU_ANTENNA_1, TMU_ANTENNA_2, TMU_ANTENNA_3, TMU_ANTENNA_4, TMU_ANTENNA_5}
    enum DataStatus { NONE, DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
    enum WifiStatus { NONE, WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
    enum WirelessChargeStatus { NONE, CHARGED, CHARGING, ERROR }
    enum ModeStatus { NONE, LOCATION_SHARING }

    private final String PACKAGE_NAME = "com.humaxdigital.automotive.systemui"; 
    private Context mContext;
    private Resources mRes;
    private View mStatusBar;
    private Handler mHandler; 

    private SystemView mMute;
    private SystemView mBle;
    private SystemView mBtBattery;
    private SystemView mPhone;
    private SystemView mAntenna;
    private SystemView mPhoneData;
    private SystemView mWirelessCharging;
    private SystemView mWifi;
    private SystemView mLocationSharing;
    private final List<SystemView> mSystemViews = new ArrayList<>();

    private StatusBarSystem mService; 

    public SystemStatusController(Context context, View view) {
        if ( (view == null) || (context == null) ) return;
        mContext = context;
        if ( mContext != null ) mRes = mContext.getResources();
        mStatusBar = view;  
        mHandler = new Handler(mContext.getMainLooper());
        initView();
    }

    public void init(StatusBarSystem service) {
        if ( service == null ) return;
        mService = service; 
        mService.registerSystemCallback(mSystemCallback); 
        if ( mService.isSystemInitialized() ) fetch(); 
    }

    public void deinit() {
        if ( mService != null ) mService.unregisterSystemCallback(mSystemCallback); 
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private void initView() {
        if ( (mStatusBar == null) || (mRes == null) ) return;

        Drawable none = ResourcesCompat.getDrawable(mRes, R.drawable.co_clear, null);

        if ( ProductConfig.getFeature() == ProductConfig.FEATURE.AVNT ) {
            mLocationSharing = new SystemView(mContext)
                .addIcon(ModeStatus.NONE.ordinal(), none)
                .addIcon(ModeStatus.LOCATION_SHARING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_location_sharing, null))
                .inflate(); 
            mSystemViews.add(mLocationSharing);
        }

        mWirelessCharging = new SystemView(mContext)
            .addIcon(WirelessChargeStatus.NONE.ordinal(), none)
            .addIconAnimation(WirelessChargeStatus.CHARGING.ordinal(), new ArrayList<Drawable>() {{
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_1, null));
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_2, null));
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_3, null));
            }})
            .addIcon(WirelessChargeStatus.CHARGED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_100, null))
            .addIcon(WirelessChargeStatus.ERROR.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_error, null))
            .inflate(); 
        mSystemViews.add(mWirelessCharging);

        if ( ProductConfig.getFeature() != ProductConfig.FEATURE.AVNT ) {
            mWifi = new SystemView(mContext)
                .addIcon(WifiStatus.NONE.ordinal(), none)
                .addIcon(WifiStatus.WIFI_1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_1, null))
                .addIcon(WifiStatus.WIFI_2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_2, null))
                .addIcon(WifiStatus.WIFI_3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_3, null))
                .addIcon(WifiStatus.WIFI_4.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_4, null))
                .inflate(); 
            mSystemViews.add(mWifi);
        }

        if ( ProductConfig.getFeature() == ProductConfig.FEATURE.AVNT ) {
            mPhoneData = new SystemView(mContext)
                .addIcon(DataStatus.NONE.ordinal(), none)
                .addIcon(DataStatus.DATA_4G.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_4g, null))
                .addIcon(DataStatus.DATA_4G_NO.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_4g_dis, null))
                .addIcon(DataStatus.DATA_E.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_e, null))
                .addIcon(DataStatus.DATA_E_NO.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_e_dis, null))
                .inflate(); 
            mSystemViews.add(mPhoneData);
        }

        mAntenna = new SystemView(mContext)
            .addIcon(AntennaStatus.NONE.ordinal(), none)
            .addIcon(AntennaStatus.BT_ANTENNA_NO.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_no, null))
            .addIcon(AntennaStatus.BT_ANTENNA_1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_01, null))
            .addIcon(AntennaStatus.BT_ANTENNA_2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_02, null))
            .addIcon(AntennaStatus.BT_ANTENNA_3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_03, null))
            .addIcon(AntennaStatus.BT_ANTENNA_4.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_04, null))
            .addIcon(AntennaStatus.BT_ANTENNA_5.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_05, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_NO.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_no, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_0.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_00, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_01, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_02, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_03, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_4.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_04, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_5.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_05, null))
            .inflate(); 
        mSystemViews.add(mAntenna);

        mPhone = new SystemView(mContext)
            .addIcon(CallStatus.NONE.ordinal(), none)
            .addIcon(CallStatus.STREAMING_CONNECTED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_audio, null))
            .addIcon(CallStatus.HANDS_FREE_CONNECTED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_hf, null))
            .addIcon(CallStatus.HF_FREE_STREAMING_CONNECTED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_hf_audio, null))
            .addIcon(CallStatus.CALL_HISTORY_DOWNLOADING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_list_down, null))
            .addIcon(CallStatus.CONTACTS_HISTORY_DOWNLOADING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ph_down, null))
            .addIcon(CallStatus.TMU_CALLING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_tmu_calling, null))
            .addIcon(CallStatus.BT_CALLING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_bt_calling, null))
            .addIcon(CallStatus.BT_PHONE_MIC_MUTE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_bt_mute, null))
            .inflate(); 
        mSystemViews.add(mPhone);

        mBtBattery = new SystemView(mContext)
            .addIcon(BTBatteryStatus.NONE.ordinal(), none)
            .addIcon(BTBatteryStatus.BT_BATTERY_0.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_00, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_01, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_02, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_03, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_4.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_04, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_5.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_05, null))
            .inflate(); 
        mSystemViews.add(mBtBattery);

        mBle = new SystemView(mContext)
            .addIcon(BLEStatus.NONE.ordinal(), none)
            .addIcon(BLEStatus.BLE_CONNECTED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_03, null))
            .addIconAnimation(BLEStatus.BLE_CONNECTING.ordinal(), new ArrayList<Drawable>() {{
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_00, null));
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_01, null));
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_02, null));
            }})
            .addIcon(BLEStatus.BLE_CONNECTION_FAIL.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_error, null))
            .inflate(); 
        mSystemViews.add(mBle);

        mMute = new SystemView(mContext)
            .addIcon(MuteStatus.NONE.ordinal(), none)
            .addIcon(MuteStatus.AV_MUTE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_audio_off, null))
            .addIcon(MuteStatus.NAV_MUTE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_navi_off, null))
            .addIcon(MuteStatus.AV_NAV_MUTE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_naviaudio_off, null))
            .inflate(); 
        mSystemViews.add(mMute);

        for ( int i = 0; i<mSystemViews.size(); i++ ) {
            int resid = mContext.getResources().getIdentifier("system_menu_"+i, "id", PACKAGE_NAME);
            if ( resid < 0 ) continue; 
            ((FrameLayout)mStatusBar.findViewById(resid)).addView(((SystemView)mSystemViews.get(i).inflate()));
        }
    }

    private void fetch() {
        if ( mService == null ) return; 
        updateMute(mService.getMuteStatus());
        if ( mBle != null ) mBle.update(BLEStatus.values()[mService.getBLEStatus()].ordinal());
        if ( mBtBattery != null ) mBtBattery.update(BTBatteryStatus.values()[mService.getBTBatteryStatus()].ordinal());
        if ( mPhone != null ) mPhone.update(CallStatus.values()[mService.getCallStatus()].ordinal());
        if ( mAntenna != null ) mAntenna.update(AntennaStatus.values()[mService.getAntennaStatus()].ordinal());
        if ( mPhoneData != null ) mPhoneData.update(DataStatus.values()[mService.getDataStatus()].ordinal());
        if ( mWirelessCharging != null ) mWirelessCharging.update(WirelessChargeStatus.values()[mService.getWirelessChargeStatus()].ordinal());
        if ( mWifi != null ) mWifi.update(WifiStatus.values()[mService.getWifiStatus()].ordinal());
        if ( mLocationSharing != null ) mLocationSharing.update(ModeStatus.values()[mService.getModeStatus()].ordinal());
    }

    private void updateMute(int state) {
        if ( mMute == null ) return;
        if ( ProductConfig.getFeature() == ProductConfig.FEATURE.AVNT ) {
            MuteStatus mute_state = MuteStatus.values()[state]; 
            if ( mute_state == MuteStatus.AV_MUTE ) {
                mMute.update(MuteStatus.AV_NAV_MUTE.ordinal());
            } else {
                mMute.update(mute_state.ordinal());
            }
        } else {
            mMute.update(MuteStatus.values()[state].ordinal());
        }
    }

    private final StatusBarSystem.StatusBarSystemCallback mSystemCallback 
        = new StatusBarSystem.StatusBarSystemCallback() {
        @Override
        public void onSystemInitialized() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    fetch(); 
                }
            });  
        }
        @Override
        public void onMuteStatusChanged(int status) {
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateMute(status);
                }
            }); 
        }
        @Override
        public void onBLEStatusChanged(int status) {
            if ( mBle == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBle.update(BLEStatus.values()[status].ordinal());
                }
            }); 
        }
        @Override
        public void onBTBatteryStatusChanged(int status) {
            if ( mBtBattery == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBtBattery.update(BTBatteryStatus.values()[status].ordinal());
                }
            }); 
        }
        @Override
        public void onCallStatusChanged(int status) {
            if ( mPhone == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPhone.update(CallStatus.values()[status].ordinal());
                }
            }); 
        }
        @Override
        public void onAntennaStatusChanged(int status) {
            if ( mAntenna == null ) return;
            if ( mHandler == null ) return;             
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAntenna.update(AntennaStatus.values()[status].ordinal());
                }
            }); 
            
        }
        @Override
        public void onDataStatusChanged(int status) {
            if ( mPhoneData == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPhoneData.update(DataStatus.values()[status].ordinal());
                }
            }); 
        }
        @Override
        public void onWifiStatusChanged(int status) {
            if ( mWifi == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWifi.update(WifiStatus.values()[status].ordinal());
                }
            }); 
        }
        @Override
        public void onWirelessChargeStatusChanged(int status) {
            if ( mWirelessCharging == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWirelessCharging.update(WirelessChargeStatus.values()[status].ordinal());
                }
            }); 
            
        }
        @Override
        public void onModeStatusChanged(int status) {
            if ( mLocationSharing == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLocationSharing.update(ModeStatus.values()[status].ordinal());
                }
            }); 
        }
    };
}
