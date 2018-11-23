package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;


public class SystemBLEController extends BaseController<Integer> {
    private enum BLEStatus { NONE, BLE_CONNECTED, BLE_CONNECTING, BLE_CONNECTION_FAIL }

    public SystemBLEController(Context context, DataStore store) {
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

    private BLEStatus convertToStatus(int mode) {
        BLEStatus status = BLEStatus.NONE; 
        // todo : check status 
        switch(mode) {
            case 0: status = BLEStatus.NONE; break;
            case 1: status = BLEStatus.BLE_CONNECTED; break;
            case 2: status = BLEStatus.BLE_CONNECTING; break;
            case 3: status = BLEStatus.BLE_CONNECTION_FAIL; break;
            default: break; 
        }
        return status; 
    }
}
