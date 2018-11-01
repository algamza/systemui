package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;


public class SystemBTCallController extends BaseController<Integer> {
    private enum BTCallStatus { NONE, STREAMING_CONNECTED, 
        HANDS_FREE_CONNECTED, HF_FREE_STREAMING_CONNECTED, 
        CALL_HISTORY_DOWNLOADING, CONTACTS_HISTORY_DOWNLOADING, 
        TMU_CALLING, BT_CALLING, BT_PHONE_MIC_MUTE }

    public SystemBTCallController(Context context, DataStore store) {
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

    private BTCallStatus convertToStatus(int mode) {
        BTCallStatus status = BTCallStatus.NONE; 
        // todo : check status 
        switch(mode) {
            case 0: status = BTCallStatus.NONE; break;
            case 1: status = BTCallStatus.STREAMING_CONNECTED; break;
            case 2: status = BTCallStatus.HANDS_FREE_CONNECTED; break;
            case 3: status = BTCallStatus.HF_FREE_STREAMING_CONNECTED; break;
            case 4: status = BTCallStatus.CALL_HISTORY_DOWNLOADING; break;
            case 5: status = BTCallStatus.CONTACTS_HISTORY_DOWNLOADING; break;
            case 6: status = BTCallStatus.TMU_CALLING; break;
            case 7: status = BTCallStatus.BT_CALLING; break;
            case 8: status = BTCallStatus.BT_PHONE_MIC_MUTE; break;
            default: break; 
        }
        return status; 
    }
}
