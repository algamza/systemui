package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;


public class SystemMuteController extends BaseController<Integer> {
    private enum MuteStatus { NONE, AV_MUTE, NAV_MUTE, AV_NAV_MUTE }

    public SystemMuteController(Context context, DataStore store) {
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

    private MuteStatus convertToStatus(int mode) {
        MuteStatus status = MuteStatus.NONE; 
        // todo : check status 
        switch(mode) {
            case 0: status = MuteStatus.NONE; break;
            case 1: status = MuteStatus.AV_MUTE; break;
            case 2: status = MuteStatus.NAV_MUTE; break;
            case 3: status = MuteStatus.AV_NAV_MUTE; break;
            default: break; 
        }
        return status; 
    }
}
