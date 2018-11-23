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

import android.util.Log;

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
    enum DataStatus { NONE, DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
    enum WifiStatus { NONE, WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
    enum WirelessChargeStatus { NONE, WIRELESS_CHARGING_1, WIRELESS_CHARGING_2, WIRELESS_CHARGING_3
        , WIRELESS_CHARGE_100, WIRELESS_CHARGING_ERROR }
    enum ModeStatus { NONE, LOCATION_SHARING }

    private final String OPEN_DROPLIST = "com.humaxdigital.automotive.droplist.action.OPEN_DROPLIST"; 
    private final String PACKAGE_NAME = "com.humaxdigital.automotive.statusbar"; 
    private Context mContext;
    private Resources mRes;
    private View mStatusBar;

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

    private IStatusBarService mService; 

    static boolean FEATURE_AVNT = false;

    public SystemStatusController(Context context, View view) {
        if ( (view == null) || (context == null) ) return;
        mContext = context;
        if ( mContext != null ) mRes = mContext.getResources();
        mStatusBar = view;  
        checkProduct(); 
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

    private void checkProduct() {
        String model = Build.MODEL;  
        Log.d(TAG, "model="+model);    
        String[] array = model.split("-");
        if ( array.length >= 2 ) {
            if ( array[1].contains("T") ) {
                FEATURE_AVNT = true; 
            } else if ( array[1].contains("N") ) {
                FEATURE_AVNT = false; 
            } else if ( array[1].contains("L") ) {
                FEATURE_AVNT = false;  
            }
        }
    }

    private void initView() {
        if ( (mStatusBar == null) || (mRes == null) ) return;
        mStatusBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // todo : remove ! is temp 
                Intent intent = new Intent(OPEN_DROPLIST); 
                mContext.sendBroadcast(intent);
            }
        });

        Drawable none = ResourcesCompat.getDrawable(mRes, R.drawable.co_clear, null);

        if ( FEATURE_AVNT ) {
            mLocationSharing = new SystemView(mContext)
                .addIcon(ModeStatus.NONE.ordinal(), none)
                .addIcon(ModeStatus.LOCATION_SHARING.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_location_shar, null))
                .inflate(); 
            mSystemViews.add(mLocationSharing);
        }

        mWirelessCharging = new SystemView(mContext)
            .addIcon(WirelessChargeStatus.NONE.ordinal(), none)
            .addIcon(WirelessChargeStatus.WIRELESS_CHARGING_1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_1, null))
            .addIcon(WirelessChargeStatus.WIRELESS_CHARGING_2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_2, null))
            .addIcon(WirelessChargeStatus.WIRELESS_CHARGING_3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_3, null))
            .addIcon(WirelessChargeStatus.WIRELESS_CHARGE_100.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_100, null))
            .addIcon(WirelessChargeStatus.WIRELESS_CHARGING_ERROR.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_error, null))
            .inflate(); 
        mSystemViews.add(mWirelessCharging);

        if ( !FEATURE_AVNT ) {
            mWifi = new SystemView(mContext)
                .addIcon(WifiStatus.NONE.ordinal(), none)
                .addIcon(WifiStatus.WIFI_1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_1, null))
                .addIcon(WifiStatus.WIFI_2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_2, null))
                .addIcon(WifiStatus.WIFI_3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_3, null))
                .addIcon(WifiStatus.WIFI_4.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_4, null))
                .inflate(); 
            mSystemViews.add(mWifi);
        }

        mPhoneData = new SystemView(mContext)
            .addIcon(DataStatus.NONE.ordinal(), none)
            .addIcon(DataStatus.DATA_4G.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_4g, null))
            .addIcon(DataStatus.DATA_4G_NO.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_4g_dis, null))
            .addIcon(DataStatus.DATA_E.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_e, null))
            .addIcon(DataStatus.DATA_E_NO.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_e_dis, null))
            .inflate(); 
        mSystemViews.add(mPhoneData);

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
            if ( mAntenna != null ) mAntenna.update(AntennaStatus.values()[mService.getAntennaStatus()].ordinal());
            if ( mPhoneData != null ) mPhoneData.update(DataStatus.values()[mService.getDataStatus()].ordinal());
            if ( mWirelessCharging != null ) mWirelessCharging.update(WirelessChargeStatus.values()[mService.getWirelessChargeStatus()].ordinal());
            if ( mWifi != null ) mWifi.update(WifiStatus.values()[mService.getWifiStatus()].ordinal());
            if ( mLocationSharing != null ) mLocationSharing.update(ModeStatus.values()[mService.getModeStatus()].ordinal());
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    private final ISystemCallback.Stub mSystemCallback = new ISystemCallback.Stub() {
        public void onMuteStatusChanged(int status) throws RemoteException {
            if ( mMute == null ) return;
            mMute.update(MuteStatus.values()[status].ordinal());
        }
        public void onBLEStatusChanged(int status) throws RemoteException {
            if ( mBle == null ) return;
            mBle.update(BLEStatus.values()[status].ordinal());
        }
        public void onBTBatteryStatusChanged(int status) throws RemoteException {
            if ( mBtBattery == null ) return;
            mBtBattery.update(BTBatteryStatus.values()[status].ordinal());
        }
        public void onBTCallStatusChanged(int status) throws RemoteException {
            if ( mBtPhone == null ) return;
            mBtPhone.update(BTCallStatus.values()[status].ordinal());
        }
        public void onAntennaStatusChanged(int status) throws RemoteException {
            if ( mAntenna == null ) return;
            mAntenna.update(AntennaStatus.values()[status].ordinal());
        }
        public void onDataStatusChanged(int status) throws RemoteException {
            if ( mPhoneData == null ) return;
            mPhoneData.update(DataStatus.values()[status].ordinal());
        }
        public void onWifiStatusChanged(int status) throws RemoteException {
            if ( mWifi == null ) return;
            mWifi.update(WifiStatus.values()[status].ordinal());
        }
        public void onWirelessChargeStatusChanged(int status) throws RemoteException {
            if ( mWirelessCharging == null ) return;
            mWirelessCharging.update(WirelessChargeStatus.values()[status].ordinal());
        }
        public void onModeStatusChanged(int status) throws RemoteException {
            if ( mLocationSharing == null ) return;
            mLocationSharing.update(ModeStatus.values()[status].ordinal());
        }
    };
}
