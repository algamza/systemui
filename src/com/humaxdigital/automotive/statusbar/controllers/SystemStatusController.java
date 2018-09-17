package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;

import android.content.Intent;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.StatusIconView;

public class SystemStatusController {
    private Context mContext;
    private Resources mRes;
    private View mStatusBar;
    private StatusIconView mIconAudio;
    private int mAudioState;
    private StatusIconView mIconBle;
    private int mBleState;
    private StatusIconView mIconBattery;
    private int mBatteryState;
    private StatusIconView mIconBt;
    private int mBTState;
    private StatusIconView mIconSi;
    private int mSiState;
    private StatusIconView mIconData;
    private int mDataState;
    private StatusIconView mIconWifi;
    private int mWifiState;
    private StatusIconView mIconWireless;
    private int mWiressState;
    private StatusIconView mIconLocation;
    private int mLocationState;

    public SystemStatusController(Context context, View view) {
        if ( view == null ) return;
        mContext = context;
        if ( mContext != null ) mRes = mContext.getResources();
        mStatusBar = view;

        initView();
    }
    private void initView() {
        if ( (mStatusBar == null) || (mRes == null) ) return;
        mStatusBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent("com.humaxdigital.automotive.droplist.action.OPEN_DROPLIST"); 
                if ( mContext != null ) mContext.sendBroadcast(intent); 
            }
        });

        mIconAudio = mStatusBar.findViewById(R.id.img_status_1);
        if ( mIconAudio != null ) {
            Drawable audio_off = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_audio_off, null);
            Drawable navi_off = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_navi_off, null);
            Drawable naviaudio_off = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_naviaudio_off, null);
            if ( (audio_off != null) && (navi_off !=null) && (naviaudio_off != null) )
            {
                mIconAudio.setIcon(1, audio_off);
                mIconAudio.setIcon(2, navi_off);
                mIconAudio.setIcon(3, naviaudio_off);
                // getCurrent audio state
                mAudioState = 1;
                mIconAudio.update(mAudioState);
            }
        }
        mIconBle = mStatusBar.findViewById(R.id.img_status_2);
        if ( mIconBle != null ) {
            Drawable ble = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_ble, null);
            if ( ble != null ) {
                mIconBle.setIcon(1, ble);
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
                mIconBattery.setIcon(0, b0);
                mIconBattery.setIcon(1, b1);
                mIconBattery.setIcon(2, b2);
                mIconBattery.setIcon(3, b3);
                mIconBattery.setIcon(4, b4);
                mIconBattery.setIcon(5, b5);
                // getCurrent battery state
                mBatteryState = 3;
                mIconBattery.update(mBatteryState);
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
                mIconBt.setIcon(1, audio);
                mIconBt.setIcon(2, hf);
                mIconBt.setIcon(3, hf_audio);
                mIconBt.setIcon(4, list_down);
                mIconBt.setIcon(5, ph_down);
                mIconBt.setIcon(6, tmu_calling);
                mIconBt.setIcon(7, bt_calling);
                mIconBt.setIcon(8, bt_mute);
                // get current bt state
                mBTState = 4;
                mIconBt.update(4);
            }
        }
        mIconSi = mStatusBar.findViewById(R.id.img_status_5);
        if ( mIconSi != null ) {
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
            if ( (si0 != null) && (si1 != null) && (si2 != null) && (si3 != null) && (si4 != null) && (si5 != null)
                    && (sibt1 != null) && (sibt2 != null) && (sibt3 != null) && (sibt4 != null) && (sibt5 != null) && (sibtno != null) ) {
                mIconSi.setIcon(0, si0);
                mIconSi.setIcon(1, si1);
                mIconSi.setIcon(2, si2);
                mIconSi.setIcon(3, si3);
                mIconSi.setIcon(4, si4);
                mIconSi.setIcon(5, si5);
                mIconSi.setIcon(6, sibt1);
                mIconSi.setIcon(7, sibt2);
                mIconSi.setIcon(8, sibt3);
                mIconSi.setIcon(9, sibt4);
                mIconSi.setIcon(10, sibt5);
                mIconSi.setIcon(11, sibtno);
                // get current bt state
                mSiState = 4;
                mIconSi.update(mSiState);
            }
        }
        mIconData = mStatusBar.findViewById(R.id.img_status_6);
        if ( mIconData != null ) {
            Drawable data = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_data, null);
            Drawable data_dis = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_data_dis, null);
            Drawable data_lte = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_data_lte, null);
            Drawable data_lte_dis = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_data_lte_dis, null);
            if ( (data != null) && (data_dis != null) && ( data_lte != null) && (data_lte_dis != null) ) {
                mIconData.setIcon(1, data);
                mIconData.setIcon(2, data_dis);
                mIconData.setIcon(3, data_lte);
                mIconData.setIcon(4, data_lte_dis);
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
                mIconWifi.setIcon(1, wifi1);
                mIconWifi.setIcon(2, wifi2);
                mIconWifi.setIcon(3, wifi3);
                mIconWifi.setIcon(4, wifi4);
                // get current wifi state
                mWifiState = 4;
                mIconWifi.update(mWifiState);
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
                mIconWireless.setIcon(1, w1);
                mIconWireless.setIcon(2, w2);
                mIconWireless.setIcon(3, w3);
                mIconWireless.setIcon(4, w100);
                mIconWireless.setIcon(5, werr);
            }
            // getWireless state
            mWiressState = 3;
            mIconWireless.update(mWifiState);
        }
        mIconLocation = mStatusBar.findViewById(R.id.img_status_9);
        if ( mIconLocation != null ) {
            Drawable location = ResourcesCompat.getDrawable(mRes, R.drawable.co_ic_location_shar, null);
            if ( location != null ) {
                mIconLocation.setIcon(1, location);
                // get current location state
                mLocationState = 1;
                mIconLocation.update(mLocationState);
            }
        }
    }
}
