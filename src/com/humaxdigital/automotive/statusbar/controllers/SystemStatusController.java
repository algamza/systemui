package com.humaxdigital.automotive.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.FrameLayout;
import android.os.RemoteException;
import android.os.Build; 
import android.os.Handler;

import android.util.Log;

import com.humaxdigital.automotive.statusbar.ProductConfig;
import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.SystemView;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.ISystemCallback; 

import java.util.ArrayList;
import java.util.List;

public class SystemStatusController implements BaseController {
    static final String TAG = "SystemStatusController"; 

    enum MuteStatus { NONE, AV_MUTE, NAV_MUTE, AV_NAV_MUTE }
    enum BLEStatus { NONE, BLE_CONNECTED, BLE_CONNECTING, BLE_CONNECTION_FAIL }
    enum BTBatteryStatus { NONE, BT_BATTERY_0, BT_BATTERY_1, BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }
    enum BTCallStatus { NONE, STREAMING_CONNECTED, HANDS_FREE_CONNECTED, HF_FREE_STREAMING_CONNECTED
        , CALL_HISTORY_DOWNLOADING, CONTACTS_HISTORY_DOWNLOADING, TMU_CALLING, BT_CALLING, BT_PHONE_MIC_MUTE }
    enum AntennaStatus { NONE, BT_ANTENNA_NO, BT_ANTENNA_0, BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5
        , TMU_ANTENNA_NO, TMU_ANTENNA_0, TMU_ANTENNA_1, TMU_ANTENNA_2, TMU_ANTENNA_3, TMU_ANTENNA_4, TMU_ANTENNA_5}
    enum BTAntennaStatus { NONE, BT_ANTENNA_NO, BT_ANTENNA_0, BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5 }
    enum TMSAntennaStatus { NONE,  TMU_ANTENNA_NO, TMU_ANTENNA_0, TMU_ANTENNA_1, TMU_ANTENNA_2, TMU_ANTENNA_3, TMU_ANTENNA_4, TMU_ANTENNA_5 }
    enum DataStatus { NONE, DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
    enum WifiStatus { NONE, WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
    enum WirelessChargeStatus { NONE, CHARGED, CHARGING, ERROR }
    enum ModeStatus { NONE, LOCATION_SHARING }

    private final String PACKAGE_NAME = "com.humaxdigital.automotive.statusbar"; 
    private Context mContext;
    private Resources mRes;
    private View mStatusBar;
    private Handler mHandler; 

    private SystemView mMute;
    private SystemView mBle;
    private SystemView mBtBattery;
    private SystemView mBtPhone;
    private SystemView mAntenna;
    private SystemView mPhoneData;
    private SystemView mWirelessCharging;
    private SystemView mWifi;
    private SystemView mLocationSharing;
    private final List<SystemView> mSystemViews = new ArrayList<>();
    
    private BTAntennaStatus mCurrentBTAntennaStatus = BTAntennaStatus.NONE; 
    private TMSAntennaStatus mCurrentTMSAntennaStatus = TMSAntennaStatus.NONE; 

    private IStatusBarService mService; 

    public SystemStatusController(Context context, View view) {
        if ( (view == null) || (context == null) ) return;
        mContext = context;
        if ( mContext != null ) mRes = mContext.getResources();
        mStatusBar = view;  
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    public void init(IStatusBarService service) {
        mService = service; 
        try {
            if ( mService != null ) mService.registerSystemCallback(mSystemCallback); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
        initView();
        fetch(); 
    }

    @Override
    public void deinit() {
        try {
            if ( mService != null ) mService.unregisterSystemCallback(mSystemCallback); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
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
                .addIcon(ModeStatus.LOCATION_SHARING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_location_shar, null))
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
            .addIcon(AntennaStatus.BT_ANTENNA_NO.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_no, null))
            .addIcon(AntennaStatus.BT_ANTENNA_0.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_00, null))
            .addIcon(AntennaStatus.BT_ANTENNA_1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_01, null))
            .addIcon(AntennaStatus.BT_ANTENNA_2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_02, null))
            .addIcon(AntennaStatus.BT_ANTENNA_3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_03, null))
            .addIcon(AntennaStatus.BT_ANTENNA_4.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_04, null))
            .addIcon(AntennaStatus.BT_ANTENNA_5.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_05, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_01, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_02, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_03, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_4.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_04, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_5.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_05, null))
            .addIcon(AntennaStatus.TMU_ANTENNA_NO.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_no, null))
            .inflate(); 
        mSystemViews.add(mAntenna);

        mBtPhone = new SystemView(mContext)
            .addIcon(BTCallStatus.NONE.ordinal(), none)
            .addIcon(BTCallStatus.STREAMING_CONNECTED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_audio, null))
            .addIcon(BTCallStatus.HANDS_FREE_CONNECTED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_hf, null))
            .addIcon(BTCallStatus.HF_FREE_STREAMING_CONNECTED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_hf_audio, null))
            .addIcon(BTCallStatus.CALL_HISTORY_DOWNLOADING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_list_down, null))
            .addIcon(BTCallStatus.CONTACTS_HISTORY_DOWNLOADING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ph_down, null))
            .addIcon(BTCallStatus.TMU_CALLING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_tmu_calling, null))
            .addIcon(BTCallStatus.BT_CALLING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_bt_calling, null))
            .addIcon(BTCallStatus.BT_PHONE_MIC_MUTE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_bt_mute, null))
            .inflate(); 
        mSystemViews.add(mBtPhone);

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
                add(ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_03, null));
            }})
            .addIcon(BLEStatus.BLE_CONNECTING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_01, null))
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
        try {
            if ( mMute != null ) mMute.update(MuteStatus.values()[mService.getMuteStatus()].ordinal());
            if ( mBle != null ) mBle.update(BLEStatus.values()[mService.getBLEStatus()].ordinal());
            if ( mBtBattery != null ) mBtBattery.update(BTBatteryStatus.values()[mService.getBTBatteryStatus()].ordinal());
            if ( mBtPhone != null ) mBtPhone.update(BTCallStatus.values()[mService.getBTCallStatus()].ordinal());
            if ( mAntenna != null ) {
                mCurrentBTAntennaStatus = convertToBTAntennaStatus(mService.getBTAntennaStatus()); 
                mCurrentTMSAntennaStatus = convertToTMSAntennaStatus(mService.getTMSAntennaStatus()); 
                mAntenna.update(conventToAntennaStatus(mCurrentBTAntennaStatus, mCurrentTMSAntennaStatus).ordinal());
            }
            if ( mPhoneData != null ) mPhoneData.update(DataStatus.values()[mService.getDataStatus()].ordinal());
            if ( mWirelessCharging != null ) mWirelessCharging.update(WirelessChargeStatus.values()[mService.getWirelessChargeStatus()].ordinal());
            if ( mWifi != null ) mWifi.update(WifiStatus.values()[mService.getWifiStatus()].ordinal());
            if ( mLocationSharing != null ) mLocationSharing.update(ModeStatus.values()[mService.getModeStatus()].ordinal());
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    private BTAntennaStatus convertToBTAntennaStatus(int bt_antenna) {
        BTAntennaStatus bt_status = BTAntennaStatus.NONE; 
        if ( bt_antenna == BTAntennaStatus.NONE.ordinal() ) bt_status = BTAntennaStatus.NONE;
        else if ( bt_antenna == BTAntennaStatus.BT_ANTENNA_NO.ordinal() ) bt_status = BTAntennaStatus.BT_ANTENNA_NO;
        else if ( bt_antenna == BTAntennaStatus.BT_ANTENNA_0.ordinal() ) bt_status = BTAntennaStatus.BT_ANTENNA_0;
        else if ( bt_antenna == BTAntennaStatus.BT_ANTENNA_1.ordinal() ) bt_status = BTAntennaStatus.BT_ANTENNA_1;
        else if ( bt_antenna == BTAntennaStatus.BT_ANTENNA_2.ordinal() ) bt_status = BTAntennaStatus.BT_ANTENNA_2;
        else if ( bt_antenna == BTAntennaStatus.BT_ANTENNA_3.ordinal() ) bt_status = BTAntennaStatus.BT_ANTENNA_3;
        else if ( bt_antenna == BTAntennaStatus.BT_ANTENNA_4.ordinal() ) bt_status = BTAntennaStatus.BT_ANTENNA_4;
        else if ( bt_antenna == BTAntennaStatus.BT_ANTENNA_5.ordinal() ) bt_status = BTAntennaStatus.BT_ANTENNA_5;
        return bt_status; 
    }

    private TMSAntennaStatus convertToTMSAntennaStatus(int tmu_antenna) {
        TMSAntennaStatus tmu_status = TMSAntennaStatus.NONE; 
        if ( tmu_antenna == TMSAntennaStatus.NONE.ordinal() ) tmu_status = TMSAntennaStatus.NONE;
        else if ( tmu_antenna == TMSAntennaStatus.TMU_ANTENNA_NO.ordinal() ) tmu_status = TMSAntennaStatus.TMU_ANTENNA_NO;
        else if ( tmu_antenna == TMSAntennaStatus.TMU_ANTENNA_0.ordinal() ) tmu_status = TMSAntennaStatus.TMU_ANTENNA_0;
        else if ( tmu_antenna == TMSAntennaStatus.TMU_ANTENNA_1.ordinal() ) tmu_status = TMSAntennaStatus.TMU_ANTENNA_1;
        else if ( tmu_antenna == TMSAntennaStatus.TMU_ANTENNA_2.ordinal() ) tmu_status = TMSAntennaStatus.TMU_ANTENNA_2;
        else if ( tmu_antenna == TMSAntennaStatus.TMU_ANTENNA_3.ordinal() ) tmu_status = TMSAntennaStatus.TMU_ANTENNA_3;
        else if ( tmu_antenna == TMSAntennaStatus.TMU_ANTENNA_4.ordinal() ) tmu_status = TMSAntennaStatus.TMU_ANTENNA_4;
        else if ( tmu_antenna == TMSAntennaStatus.TMU_ANTENNA_5.ordinal() ) tmu_status = TMSAntennaStatus.TMU_ANTENNA_5;
        return tmu_status; 
    }

    private AntennaStatus conventToAntennaStatus(BTAntennaStatus bt_status, TMSAntennaStatus tmu_status) {
        AntennaStatus status = AntennaStatus.NONE; 
        switch(bt_status) {
            case NONE: status = AntennaStatus.NONE; break; 
            case BT_ANTENNA_NO: status = AntennaStatus.BT_ANTENNA_NO; break; 
            case BT_ANTENNA_0: status = AntennaStatus.BT_ANTENNA_0; break; 
            case BT_ANTENNA_1: status = AntennaStatus.BT_ANTENNA_1; break; 
            case BT_ANTENNA_2: status = AntennaStatus.BT_ANTENNA_2; break; 
            case BT_ANTENNA_3: status = AntennaStatus.BT_ANTENNA_3; break; 
            case BT_ANTENNA_4: status = AntennaStatus.BT_ANTENNA_4; break; 
            case BT_ANTENNA_5: status = AntennaStatus.BT_ANTENNA_5; break; 
        }
        if ( status != AntennaStatus.NONE ) return status; 
        switch(tmu_status) {
            case NONE: status = AntennaStatus.NONE; break; 
            case TMU_ANTENNA_NO: status = AntennaStatus.TMU_ANTENNA_NO; break; 
            case TMU_ANTENNA_0: status = AntennaStatus.TMU_ANTENNA_0; break; 
            case TMU_ANTENNA_1: status = AntennaStatus.TMU_ANTENNA_1; break; 
            case TMU_ANTENNA_2: status = AntennaStatus.TMU_ANTENNA_2; break; 
            case TMU_ANTENNA_3: status = AntennaStatus.TMU_ANTENNA_3; break; 
            case TMU_ANTENNA_4: status = AntennaStatus.TMU_ANTENNA_4; break; 
            case TMU_ANTENNA_5: status = AntennaStatus.TMU_ANTENNA_5; break; 
        }
        return status; 
    }

    private final ISystemCallback.Stub mSystemCallback = new ISystemCallback.Stub() {
        public void onMuteStatusChanged(int status) throws RemoteException {
            if ( mMute == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMute.update(MuteStatus.values()[status].ordinal());
                }
            }); 
        }
        public void onBLEStatusChanged(int status) throws RemoteException {
            if ( mBle == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBle.update(BLEStatus.values()[status].ordinal());
                }
            }); 
        }
        public void onBTBatteryStatusChanged(int status) throws RemoteException {
            if ( mBtBattery == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBtBattery.update(BTBatteryStatus.values()[status].ordinal());
                }
            }); 
        }
        public void onBTCallStatusChanged(int status) throws RemoteException {
            if ( mBtPhone == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBtPhone.update(BTCallStatus.values()[status].ordinal());
                }
            }); 
        }
        public void onBTAntennaStatusChanged(int status) throws RemoteException {
            if ( mAntenna == null ) return;
            if ( mHandler == null ) return; 
            mCurrentBTAntennaStatus = convertToBTAntennaStatus(status);
            AntennaStatus antenna = conventToAntennaStatus(mCurrentBTAntennaStatus, mCurrentTMSAntennaStatus);             
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAntenna.update(antenna.ordinal());
                }
            }); 
            
        }
        public void onTMSAntennaStatusChanged(int status) throws RemoteException {
            if ( mAntenna == null ) return;
            if ( mHandler == null ) return; 
            mCurrentTMSAntennaStatus = convertToTMSAntennaStatus(status);
            AntennaStatus antenna = conventToAntennaStatus(mCurrentBTAntennaStatus, mCurrentTMSAntennaStatus);   
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAntenna.update(antenna.ordinal());
                }
            }); 
            
        }
        public void onDataStatusChanged(int status) throws RemoteException {
            if ( mPhoneData == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mPhoneData.update(DataStatus.values()[status].ordinal());
                }
            }); 
        }
        public void onWifiStatusChanged(int status) throws RemoteException {
            if ( mWifi == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWifi.update(WifiStatus.values()[status].ordinal());
                }
            }); 
        }
        public void onWirelessChargeStatusChanged(int status) throws RemoteException {
            if ( mWirelessCharging == null ) return;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWirelessCharging.update(WirelessChargeStatus.values()[status].ordinal());
                }
            }); 
            
        }
        public void onModeStatusChanged(int status) throws RemoteException {
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
