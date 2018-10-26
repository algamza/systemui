package com.humaxdigital.automotive.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.os.RemoteException;

import android.os.UserHandle;
import android.os.BatteryManager; 

import android.util.Log;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.StatusIconView;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.ISystemCallback; 

public class SystemStatusController implements BaseController {
    enum MuteStatus { AV_MUTE, NAV_MUTE, AV_NAV_MUTE }
    enum BLEStatus { BLE_0, BLE_1, BLE_2, BLE_3 }
    enum BTBatteryStatus { BT_BATTERY_0, BT_BATTERY_1, BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }
    enum BTCallStatus { STREAMING_CONNECTED, HANDS_FREE_CONNECTED, HF_FREE_STREAMING_CONNECTED
        , CALL_HISTORY_DOWNLOADING, CONTACTS_HISTORY_DOWNLOADING, TMU_CALLING, BT_CALLING, BT_PHONE_MIC_MUTE }
    enum AntennaStatus { BT_ANTENNA_NO, BT_ANTENNA_0, BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, BT_ANTENNA_5
        , TMU_ANTENNA_NO, TMU_ANTENNA_0, TMU_ANTENNA_1, TMU_ANTENNA_2, TMU_ANTENNA_3, TMU_ANTENNA_4, TMU_ANTENNA_5}
    enum DataStatus { DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
    enum WifiStatus { WIFI_1, WIFI_2, WIFI_3, WIFI_4 }
    enum WirelessChargeStatus { WIRELESS_CHARGING_1, WIRELESS_CHARGING_2, WIRELESS_CHARGING_3
        , WIRELESS_CHARGE_100, WIRELESS_CHARGING_ERROR }
    enum ModeStatus { LOCATION_SHARING }

    private Context mContext;
    private Resources mRes;
    private View mStatusBar;

    private StatusIconView mIconAudio;
    private int mAudioState;
    private StatusIconView mIconBle;
    private int mBleState;
    private StatusIconView mIconBattery;
    private StatusIconView mIconBt;
    private int mBTState;
    private StatusIconView mIconSi;
    private int mSiState;
    private StatusIconView mIconData;
    private int mDataState;
    private StatusIconView mIconWifi;
    private WifiStatus mWifiState;
    private StatusIconView mIconWireless;
    private int mWiressState;
    private StatusIconView mIconLocation;
    private int mLocationState;

    private BatteryManager mBatteryManager; 
    private Intent mBatteryStatus;

    private IStatusBarService mService; 

    public SystemStatusController(Context context, View view) {
        if ( (view == null) || (context == null) ) return;
        mContext = context;
        if ( mContext != null ) mRes = mContext.getResources();
        mStatusBar = view;
        initManager(mContext); 
        
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
    public void update() {

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if ( mContext != null ) 
        {
            mContext.unregisterReceiver(mBatteryReceiver);
        }
    }
    
    private void initManager(Context context) {
        if ( context == null ) return;
        initBattery(context); 
    }
    
    private void initView() {
        if ( (mStatusBar == null) || (mRes == null) ) return;
        mStatusBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // todo : remove ! is temp 
                Intent intent = new Intent("com.humaxdigital.automotive.droplist.action.OPEN_DROPLIST"); 
                mContext.sendBroadcast(intent);
            }
        });

        mIconAudio = mStatusBar.findViewById(R.id.img_status_1);
        if ( mIconAudio != null ) {
            Drawable audio_off = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_audio_off, null);
            Drawable navi_off = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_navi_off, null);
            Drawable naviaudio_off = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_naviaudio_off, null);
            if ( (audio_off != null) && (navi_off !=null) && (naviaudio_off != null) )
            {
                mIconAudio.setIcon(MuteStatus.AV_MUTE.ordinal(), audio_off);
                mIconAudio.setIcon(MuteStatus.NAV_MUTE.ordinal(), navi_off);
                mIconAudio.setIcon(MuteStatus.AV_NAV_MUTE.ordinal(), naviaudio_off);
                // getCurrent audio state
                mAudioState = 1;
                mIconAudio.update(mAudioState);
            }
        }

        mIconBle = mStatusBar.findViewById(R.id.img_status_2);
        if ( mIconBle != null ) {
            Drawable ble0 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_00, null);
            Drawable ble1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_01, null);
            Drawable ble2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_02, null);
            Drawable ble3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble_03, null);
            if ( ble0 != null && ble1 != null && ble2 != null && ble3 != null ) {
                mIconBle.setIcon(BLEStatus.BLE_0.ordinal(), ble0);
                mIconBle.setIcon(BLEStatus.BLE_1.ordinal(), ble1);
                mIconBle.setIcon(BLEStatus.BLE_2.ordinal(), ble2);
                mIconBle.setIcon(BLEStatus.BLE_3.ordinal(), ble3);
                // getCurrent ble state
                mBleState = 1;
                mIconBle.update(mBleState);
            }
        }

        mIconBattery = mStatusBar.findViewById(R.id.img_status_3);
        if ( mIconBattery != null ) {
            Drawable b0 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_00, null);
            Drawable b1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_01, null);
            Drawable b2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_02, null);
            Drawable b3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_03, null);
            Drawable b4 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_04, null);
            Drawable b5 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_battery_05, null);
            if ( (b0 != null) && (b1 != null) && (b2 != null) && (b3 != null) && (b4 != null) && (b5 != null) )
            {
                mIconBattery.setIcon(BTBatteryStatus.BT_BATTERY_0.ordinal(), b0);
                mIconBattery.setIcon(BTBatteryStatus.BT_BATTERY_1.ordinal(), b1);
                mIconBattery.setIcon(BTBatteryStatus.BT_BATTERY_2.ordinal(), b2);
                mIconBattery.setIcon(BTBatteryStatus.BT_BATTERY_3.ordinal(), b3);
                mIconBattery.setIcon(BTBatteryStatus.BT_BATTERY_4.ordinal(), b4);
                mIconBattery.setIcon(BTBatteryStatus.BT_BATTERY_5.ordinal(), b5);
            }
            
        }
        mIconBt = mStatusBar.findViewById(R.id.img_status_4);
        if ( mIconBt != null ) {
            Drawable audio = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_audio, null);
            Drawable hf = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_hf, null);
            Drawable hf_audio = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_hf_audio, null);
            Drawable list_down = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_list_down, null);
            Drawable ph_down = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ph_down, null);
            Drawable tmu_calling = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_tmu_calling, null);
            Drawable bt_calling = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_bt_calling, null);
            Drawable bt_mute = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_bt_mute, null);
            if ( (audio != null) && (hf != null) && (hf_audio != null) && (list_down != null) && (ph_down != null) &&
                    (tmu_calling != null) && (bt_calling != null) && (bt_mute != null) ) {
                mIconBt.setIcon(BTCallStatus.STREAMING_CONNECTED.ordinal(), audio);
                mIconBt.setIcon(BTCallStatus.HANDS_FREE_CONNECTED.ordinal(), hf);
                mIconBt.setIcon(BTCallStatus.HF_FREE_STREAMING_CONNECTED.ordinal(), hf_audio);
                mIconBt.setIcon(BTCallStatus.CALL_HISTORY_DOWNLOADING.ordinal(), list_down);
                mIconBt.setIcon(BTCallStatus.CONTACTS_HISTORY_DOWNLOADING.ordinal(), ph_down);
                mIconBt.setIcon(BTCallStatus.TMU_CALLING.ordinal(), tmu_calling);
                mIconBt.setIcon(BTCallStatus.BT_CALLING.ordinal(), bt_calling);
                mIconBt.setIcon(BTCallStatus.BT_PHONE_MIC_MUTE.ordinal(), bt_mute);
                // get current bt state
                mBTState = 4;
                mIconBt.update(4);
            }
        }
        mIconSi = mStatusBar.findViewById(R.id.img_status_5);
        if ( mIconSi != null ) {
            Drawable sino = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_no, null);
            Drawable si0 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_00, null);
            Drawable si1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_01, null);
            Drawable si2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_02, null);
            Drawable si3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_03, null);
            Drawable si4 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_04, null);
            Drawable si5 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_si_05, null);
            Drawable sibt1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_01, null);
            Drawable sibt2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_02, null);
            Drawable sibt3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_03, null);
            Drawable sibt4 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_04, null);
            Drawable sibt5 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_05, null);
            Drawable sibtno = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_sibt_no, null);
            if ( (sino != null) && (si0 != null) && (si1 != null) && (si2 != null) && (si3 != null) && (si4 != null) && (si5 != null)
                    && (sibt1 != null) && (sibt2 != null) && (sibt3 != null) && (sibt4 != null) && (sibt5 != null) && (sibtno != null) ) {
                mIconSi.setIcon(AntennaStatus.BT_ANTENNA_NO.ordinal(), sino);
                mIconSi.setIcon(AntennaStatus.BT_ANTENNA_0.ordinal(), si0);
                mIconSi.setIcon(AntennaStatus.BT_ANTENNA_1.ordinal(), si1);
                mIconSi.setIcon(AntennaStatus.BT_ANTENNA_2.ordinal(), si2);
                mIconSi.setIcon(AntennaStatus.BT_ANTENNA_3.ordinal(), si3);
                mIconSi.setIcon(AntennaStatus.BT_ANTENNA_4.ordinal(), si4);
                mIconSi.setIcon(AntennaStatus.BT_ANTENNA_5.ordinal(), si5);
                mIconSi.setIcon(AntennaStatus.TMU_ANTENNA_1.ordinal(), sibt1);
                mIconSi.setIcon(AntennaStatus.TMU_ANTENNA_2.ordinal(), sibt2);
                mIconSi.setIcon(AntennaStatus.TMU_ANTENNA_3.ordinal(), sibt3);
                mIconSi.setIcon(AntennaStatus.TMU_ANTENNA_4.ordinal(), sibt4);
                mIconSi.setIcon(AntennaStatus.TMU_ANTENNA_5.ordinal(), sibt5);
                mIconSi.setIcon(AntennaStatus.TMU_ANTENNA_NO.ordinal(), sibtno);
                // get current bt state
                mSiState = 4;
                mIconSi.update(mSiState);
            }
        }
        mIconData = mStatusBar.findViewById(R.id.img_status_6);
        if ( mIconData != null ) {
            Drawable data_4g = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_4g, null);
            Drawable data_4g_dis = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_4g_dis, null);
            Drawable data_e = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_e, null);
            Drawable data_e_dis = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_e_dis, null);
            if ( (data_4g != null) && (data_4g_dis != null) && ( data_e != null) && (data_e_dis != null) ) {
                mIconData.setIcon(DataStatus.DATA_4G.ordinal(), data_4g);
                mIconData.setIcon(DataStatus.DATA_4G_NO.ordinal(), data_4g_dis);
                mIconData.setIcon(DataStatus.DATA_E.ordinal(), data_e);
                mIconData.setIcon(DataStatus.DATA_E_NO.ordinal(), data_e_dis);
                // get current data state
                mDataState = 3;
                mIconData.update(mDataState);
            }
        }
        mIconWifi = mStatusBar.findViewById(R.id.img_status_7);
        if ( mIconWifi != null ) {
            Drawable wifi1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_1, null);
            Drawable wifi2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_2, null);
            Drawable wifi3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_3, null);
            Drawable wifi4 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wifi_4, null);
            if ( (wifi1 != null) && (wifi2 != null) && (wifi3 != null) && (wifi4 != null) ) {
                mIconWifi.setIcon(WifiStatus.WIFI_1.ordinal(), wifi1);
                mIconWifi.setIcon(WifiStatus.WIFI_2.ordinal(), wifi2);
                mIconWifi.setIcon(WifiStatus.WIFI_3.ordinal(), wifi3);
                mIconWifi.setIcon(WifiStatus.WIFI_4.ordinal(), wifi4);
                int status = 0; 
                try {
                    if ( mService != null ) status = mService.getWifiStatus();
                } catch( RemoteException e ) {
                    e.printStackTrace();
                }
                mWifiState = WifiStatus.values()[status]; 
                mIconWifi.update(mWifiState.ordinal());
            }
        }
        mIconWireless = mStatusBar.findViewById(R.id.img_status_8);
        if ( mIconWireless != null ) {
            Drawable w1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_1, null);
            Drawable w2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_2, null);
            Drawable w3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_3, null);
            Drawable w100 = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_100, null);
            Drawable werr = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_wireless_charging_error, null);
            if ( (w1 != null) && (w2 != null) && (w3 != null) && (w100 != null) && (werr != null) ) {
                mIconWireless.setIcon(WirelessChargeStatus.WIRELESS_CHARGING_1.ordinal(), w1);
                mIconWireless.setIcon(WirelessChargeStatus.WIRELESS_CHARGING_2.ordinal(), w2);
                mIconWireless.setIcon(WirelessChargeStatus.WIRELESS_CHARGING_3.ordinal(), w3);
                mIconWireless.setIcon(WirelessChargeStatus.WIRELESS_CHARGE_100.ordinal(), w100);
                mIconWireless.setIcon(WirelessChargeStatus.WIRELESS_CHARGING_ERROR.ordinal(), werr);
            }
            // getWireless state
            mWiressState = 3;
            mIconWireless.update(mWiressState);
        }
        mIconLocation = mStatusBar.findViewById(R.id.img_status_9);
        if ( mIconLocation != null ) {
            Drawable location = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_location_shar, null);
            if ( location != null ) {
                mIconLocation.setIcon(ModeStatus.LOCATION_SHARING.ordinal(), location);
                // get current location state
                mLocationState = ModeStatus.LOCATION_SHARING.ordinal();
                mIconLocation.update(mLocationState);
            }
        }
        updateBattery();
    }

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL;
    
            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
            */
            updateBattery(); 
        }
    };

    private void initBattery(Context context) {
        if ( context == null ) return;
        
        mBatteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mBatteryStatus = context.registerReceiver(null, ifilter);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        context.registerReceiverAsUser(mBatteryReceiver, UserHandle.ALL, filter, null, null);
    }

    private void updateBattery() {
        if ( (mIconBattery == null) || (mBatteryStatus == null) ) return;
        
        int level = mBatteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = mBatteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = (level / (float)scale) * 100;
        mIconBattery.update((int)(batteryPct/17)); 
    }

    private final ISystemCallback.Stub mSystemCallback = new ISystemCallback.Stub() {

        public void onMuteStatusChanged(int status) throws RemoteException {
        }
        public void onBLEStatusChanged(int status) throws RemoteException {
        }
        public void onBTBatteryStatusChanged(int status) throws RemoteException {
        }
        public void onBTCallStatusChanged(int status) throws RemoteException {
        }
        public void onAntennaStatusChanged(int stataus) throws RemoteException {
        }
        public void onDataStatusChanged(int status) throws RemoteException {
        }
        public void onWifiStatusChanged(int status) throws RemoteException {
            if ( mIconWifi == null ) return;
            mWifiState = WifiStatus.values()[status]; 
            mIconWifi.update(mWifiState.ordinal());
        }
        public void onWirelessChargeStatusChanged(int status) throws RemoteException {
        }
        public void onModeStatusChanged(int status) throws RemoteException {
        }
    };
}
