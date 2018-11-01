package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;


public class SystemBTBatteryController extends BaseController<Integer> {
    private enum BTBatteryStatus { NONE, BT_BATTERY_0, BT_BATTERY_1, 
        BT_BATTERY_2, BT_BATTERY_3, BT_BATTERY_4, BT_BATTERY_5 }

    public SystemBTBatteryController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void fetch() {
        if ( mDataStore == null ) return;
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        return 0; 
    }

    private BTBatteryStatus convertToStatus(int mode) {
        BTBatteryStatus status = BTBatteryStatus.NONE; 
        // todo : check status 
        switch(mode) {
            case 0: status = BTBatteryStatus.NONE; break;
            case 1: status = BTBatteryStatus.BT_BATTERY_0; break;
            case 2: status = BTBatteryStatus.BT_BATTERY_1; break;
            case 3: status = BTBatteryStatus.BT_BATTERY_2; break;
            case 4: status = BTBatteryStatus.BT_BATTERY_3; break;
            case 5: status = BTBatteryStatus.BT_BATTERY_4; break;
            case 6: status = BTBatteryStatus.BT_BATTERY_5; break;
            default: break; 
        }
        return status; 
    }
}
