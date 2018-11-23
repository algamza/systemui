package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;


public class SystemWirelessChargeController extends BaseController<Integer> {
    private enum WirelessChargeStatus { NONE, CHARGED, CHARGING, ERROR }

    public SystemWirelessChargeController(Context context, DataStore store) {
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

    private WirelessChargeStatus convertToStatus(int mode) {
        WirelessChargeStatus status = WirelessChargeStatus.NONE; 
        // todo : check status 
        switch(mode) {
            case 0: status = WirelessChargeStatus.NONE; break;
            case 1: status = WirelessChargeStatus.CHARGED; break;
            case 2: status = WirelessChargeStatus.CHARGING; break;
            case 3: status = WirelessChargeStatus.ERROR; break;
            default: break; 
        }
        return status; 
    }
}
