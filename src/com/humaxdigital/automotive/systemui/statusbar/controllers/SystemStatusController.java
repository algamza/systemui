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

import com.humaxdigital.automotive.systemui.common.util.ProductConfig;
import com.humaxdigital.automotive.systemui.common.util.OSDPopup; 
import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.ui.SystemView;

import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarSystem;
import com.humaxdigital.automotive.systemui.statusbar.service.BitmapParcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects; 

public class SystemStatusController implements StatusBarSystem.StatusBarSystemCallback {
    private static final String TAG = "SystemStatusController"; 

    enum MuteStatus { 
        NONE(0), AV_MUTE(1), NAV_MUTE(2), AV_NAV_MUTE(3); 
        private final int state; 
        MuteStatus(int state) { this.state = state; }
        public int state() { return state; }
    }
    enum BLEStatus { 
        NONE(0), BLE_CONNECTED(1), BLE_CONNECTING(2), BLE_CONNECTION_FAIL(3), BLE_DISCONNECTED(4);
        private final int state; 
        BLEStatus(int state) { this.state = state; }
        public int state() { return state; }
    }
    enum BTBatteryStatus { 
        NONE(0), BT_BATTERY_0(1), BT_BATTERY_1(2), BT_BATTERY_2(3), 
        BT_BATTERY_3(4), BT_BATTERY_4(5), BT_BATTERY_5(6); 
        private final int state; 
        BTBatteryStatus(int state) { this.state = state; }
        public int state() { return state; }
    }
    enum CallStatus { 
        NONE(0), HANDS_FREE_CONNECTED(1), STREAMING_CONNECTED(2), 
        HF_FREE_STREAMING_CONNECTED(3), CALL_HISTORY_DOWNLOADING(4), 
        CONTACTS_HISTORY_DOWNLOADING(5), TMU_CALLING(6), BT_CALLING(7), BT_PHONE_MIC_MUTE(8);
        private final int state; 
        CallStatus(int state) { this.state = state; }
        public int state() { return state; } 
    }
    enum AntennaStatus { NONE(0), BT_ANTENNA_NO(1), BT_ANTENNA_1(2), BT_ANTENNA_2(3), 
        BT_ANTENNA_3(4), BT_ANTENNA_4(5), BT_ANTENNA_5(6), TMU_ANTENNA_NO(7), TMU_ANTENNA_0(8), 
        TMU_ANTENNA_1(9), TMU_ANTENNA_2(10), TMU_ANTENNA_3(11), TMU_ANTENNA_4(12), TMU_ANTENNA_5(13);
        private final int state; 
        AntennaStatus(int state) { this.state = state; }
        public int state() { return state; } 
    
    }
    enum DataStatus { 
        NONE(0), DATA_4G(1), DATA_4G_NO(2), DATA_E(3), DATA_E_NO(4); 
        private final int state; 
        DataStatus(int state) { this.state = state; }
        public int state() { return state; } 
    }
    enum WifiStatus { 
        NONE(0), WIFI_1(1), WIFI_2(2), WIFI_3(3), WIFI_4(4); 
        private final int state; 
        WifiStatus(int state) { this.state = state; }
        public int state() { return state; } 
    }
    enum WirelessChargeStatus { 
        NONE(0), CHARGED(1), CHARGING(2), ERROR(3);
        private final int state; 
        WirelessChargeStatus(int state) { this.state = state; }
        public int state() { return state; } 
    }
    enum ModeStatus { 
        NONE(0), LOCATION_SHARING(1); 
        private final int state; 
        ModeStatus(int state) { this.state = state; }
        public int state() { return state; } 
    }

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
        mContext = Objects.requireNonNull(context);
        mRes = mContext.getResources();
        mStatusBar = Objects.requireNonNull(view);  
        mHandler = new Handler(mContext.getMainLooper());
        initView();
    }

    public void init(StatusBarSystem service) {
        if ( service == null ) return;
        mService = service; 
        mService.registerSystemCallback(this); 
        if ( mService.isSystemInitialized() ) fetch(); 
    }

    public void deinit() {
        if ( mService != null ) 
            mService.unregisterSystemCallback(this); 
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private void initView() {
        Drawable none = ResourcesCompat.getDrawable(mRes, R.drawable.co_clear, null);

        if ( ProductConfig.getFeature() == ProductConfig.FEATURE.AVNT ) {
            mLocationSharing = new SystemView(mContext)
                .addIcon(ModeStatus.NONE.state(), none)
                .addIcon(ModeStatus.LOCATION_SHARING.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_location_sharing, null))
                .inflate(); 
            mSystemViews.add(mLocationSharing);
        }

        mWirelessCharging = new SystemView(mContext)
            .addIcon(WirelessChargeStatus.NONE.state(), none)
            .addIconAnimation(WirelessChargeStatus.CHARGING.state(), new ArrayList<Drawable>() {{
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_1, null));
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_2, null));
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_3, null));
            }})
            .addIcon(WirelessChargeStatus.CHARGED.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_100, null))
            .addIcon(WirelessChargeStatus.ERROR.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_error, null))
            .inflate(); 
        mSystemViews.add(mWirelessCharging);

        if ( ProductConfig.getFeature() != ProductConfig.FEATURE.AVNT ) {
            mWifi = new SystemView(mContext)
                .addIcon(WifiStatus.NONE.state(), none)
                .addIcon(WifiStatus.WIFI_1.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_1, null))
                .addIcon(WifiStatus.WIFI_2.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_2, null))
                .addIcon(WifiStatus.WIFI_3.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_3, null))
                .addIcon(WifiStatus.WIFI_4.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_4, null))
                .inflate(); 
            mSystemViews.add(mWifi);
        }

        if ( ProductConfig.getFeature() == ProductConfig.FEATURE.AVNT ) {
            mPhoneData = new SystemView(mContext)
                .addIcon(DataStatus.NONE.state(), none)
                .addIcon(DataStatus.DATA_4G.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_4g, null))
                .addIcon(DataStatus.DATA_4G_NO.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_4g_dis, null))
                .addIcon(DataStatus.DATA_E.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_e, null))
                .addIcon(DataStatus.DATA_E_NO.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_e_dis, null))
                .inflate(); 
            mSystemViews.add(mPhoneData);
        }

        mAntenna = new SystemView(mContext)
            .addIcon(AntennaStatus.NONE.state(), none)
            .addIcon(AntennaStatus.BT_ANTENNA_NO.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_no, null))
            .addIcon(AntennaStatus.BT_ANTENNA_1.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_01, null))
            .addIcon(AntennaStatus.BT_ANTENNA_2.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_02, null))
            .addIcon(AntennaStatus.BT_ANTENNA_3.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_03, null))
            .addIcon(AntennaStatus.BT_ANTENNA_4.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_04, null))
            .addIcon(AntennaStatus.BT_ANTENNA_5.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_05, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_NO.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_no, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_0.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_00, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_1.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_01, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_2.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_02, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_3.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_03, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_4.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_04, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_5.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_05, null))
            .inflate(); 
        mSystemViews.add(mAntenna);

        mPhone = new SystemView(mContext)
            .addIcon(CallStatus.NONE.state(), none)
            .addIcon(CallStatus.STREAMING_CONNECTED.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_audio, null))
            .addIcon(CallStatus.HANDS_FREE_CONNECTED.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_hf, null))
            .addIcon(CallStatus.HF_FREE_STREAMING_CONNECTED.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_hf_audio, null))
            .addIcon(CallStatus.CALL_HISTORY_DOWNLOADING.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_list_down, null))
            .addIcon(CallStatus.CONTACTS_HISTORY_DOWNLOADING.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ph_down, null))
            .addIcon(CallStatus.TMU_CALLING.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_tmu_calling, null))
            .addIcon(CallStatus.BT_CALLING.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_bt_calling, null))
            .addIcon(CallStatus.BT_PHONE_MIC_MUTE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_bt_mute, null))
            .inflate(); 
        mSystemViews.add(mPhone);

        mBtBattery = new SystemView(mContext)
            .addIcon(BTBatteryStatus.NONE.state(), none)
            .addIcon(BTBatteryStatus.BT_BATTERY_0.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_00, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_1.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_01, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_2.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_02, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_3.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_03, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_4.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_04, null))
            .addIcon(BTBatteryStatus.BT_BATTERY_5.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_05, null))
            .inflate(); 
        mSystemViews.add(mBtBattery);

        mBle = new SystemView(mContext)
            .addIcon(BLEStatus.NONE.state(), none)
            .addIcon(BLEStatus.BLE_CONNECTED.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_03, null))
            .addIconAnimation(BLEStatus.BLE_CONNECTING.state(), new ArrayList<Drawable>() {{
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_00, null));
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_01, null));
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_02, null));
            }})
            .addIcon(BLEStatus.BLE_CONNECTION_FAIL.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_error, null))
            .addIcon(BLEStatus.BLE_DISCONNECTED.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_no, null))
            .inflate(); 
        mSystemViews.add(mBle);

        mMute = new SystemView(mContext)
            .addIcon(MuteStatus.NONE.state(), none)
            .addIcon(MuteStatus.AV_MUTE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_audio_off, null))
            .addIcon(MuteStatus.NAV_MUTE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_navi_off, null))
            .addIcon(MuteStatus.AV_NAV_MUTE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_naviaudio_off, null))
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
        if ( mBle != null ) mBle.update(BLEStatus.values()[mService.getBLEStatus()].state());
        if ( mBtBattery != null ) mBtBattery.update(BTBatteryStatus.values()[mService.getBTBatteryStatus()].state());
        if ( mPhone != null ) mPhone.update(CallStatus.values()[mService.getCallStatus()].state());
        if ( mAntenna != null ) mAntenna.update(AntennaStatus.values()[mService.getAntennaStatus()].state());
        if ( mPhoneData != null ) mPhoneData.update(DataStatus.values()[mService.getDataStatus()].state());
        if ( mWirelessCharging != null ) mWirelessCharging.update(WirelessChargeStatus.values()[mService.getWirelessChargeStatus()].state());
        if ( mWifi != null ) mWifi.update(WifiStatus.values()[mService.getWifiStatus()].state());
        if ( mLocationSharing != null ) mLocationSharing.update(ModeStatus.values()[mService.getModeStatus()].state());
    }

    private void updateMute(int state) {
        if ( mMute == null ) return;
        if ( ProductConfig.getFeature() == ProductConfig.FEATURE.AVNT ) {
            MuteStatus mute_state = MuteStatus.values()[state]; 
            if ( mute_state == MuteStatus.AV_MUTE ) {
                mMute.update(MuteStatus.AV_NAV_MUTE.state());
            } else {
                mMute.update(mute_state.state());
            }
        } else {
            mMute.update(MuteStatus.values()[state].state());
        }
    }

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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onMuteStatusChanged="+status); 
                updateMute(status);
            }
        }); 
    }
    @Override
    public void onBLEStatusChanged(int status) {
        if ( mBle == null ) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onBLEStatusChanged="+status); 
                mBle.update(BLEStatus.values()[status].state());
            }
        }); 
    }
    @Override
    public void onBTBatteryStatusChanged(int status) {
        if ( mBtBattery == null ) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onBTBatteryStatusChanged="+status); 
                mBtBattery.update(BTBatteryStatus.values()[status].state());
            }
        }); 
    }
    @Override
    public void onCallStatusChanged(int status) {
        if ( mPhone == null ) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCallStatusChanged="+status); 
                mPhone.update(CallStatus.values()[status].state());
            }
        }); 
    }
    @Override
    public void onAntennaStatusChanged(int status) {
        if ( mAntenna == null ) return;           
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onAntennaStatusChanged="+status); 
                mAntenna.update(AntennaStatus.values()[status].state());
            }
        }); 
        
    }
    @Override
    public void onDataStatusChanged(int status) {
        if ( mPhoneData == null ) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onDataStatusChanged="+status); 
                mPhoneData.update(DataStatus.values()[status].state());
            }
        }); 
    }
    @Override
    public void onWifiStatusChanged(int status) {
        if ( mWifi == null ) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onWifiStatusChanged="+status); 
                mWifi.update(WifiStatus.values()[status].state());
            }
        }); 
    }
    @Override
    public void onWirelessChargeStatusChanged(int status) {
        if ( mWirelessCharging == null ) return;
        if ( WirelessChargeStatus.values()[status] == WirelessChargeStatus.CHARGING ) {
            OSDPopup.send(mContext, 
                mContext.getResources().getString(R.string.STR_WIRELESS_CHARGING_ID), 
                R.drawable.co_ic_osd_battery_charging);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onWirelessChargeStatusChanged="+status); 
                mWirelessCharging.update(WirelessChargeStatus.values()[status].state());
            }
        });
    }
    @Override
    public void onModeStatusChanged(int status) {
        if ( mLocationSharing == null ) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onModeStatusChanged="+status); 
                mLocationSharing.update(ModeStatus.values()[status].state());
            }
        }); 
    }
}
