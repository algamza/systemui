package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;


public class SystemAntennaController extends BaseController<Integer> {
    private enum AntennaStatus { NONE, BT_ANTENNA_NO, BT_ANTENNA_0, 
        BT_ANTENNA_1, BT_ANTENNA_2, BT_ANTENNA_3, BT_ANTENNA_4, 
        BT_ANTENNA_5, TMU_ANTENNA_NO, TMU_ANTENNA_0, TMU_ANTENNA_1, 
        TMU_ANTENNA_2, TMU_ANTENNA_3, TMU_ANTENNA_4, TMU_ANTENNA_5}

    public SystemAntennaController(Context context, DataStore store) {
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

    private AntennaStatus convertToStatus(int mode) {
        AntennaStatus status = AntennaStatus.NONE; 
        // todo : check status 
        switch(mode) {
            case 0: status = AntennaStatus.NONE; break;
            case 1: status = AntennaStatus.BT_ANTENNA_NO; break;
            case 2: status = AntennaStatus.BT_ANTENNA_0; break;
            case 3: status = AntennaStatus.BT_ANTENNA_1; break;
            case 4: status = AntennaStatus.BT_ANTENNA_2; break;
            case 5: status = AntennaStatus.BT_ANTENNA_3; break;
            case 6: status = AntennaStatus.BT_ANTENNA_4; break;
            case 7: status = AntennaStatus.BT_ANTENNA_5; break;
            case 8: status = AntennaStatus.TMU_ANTENNA_NO; break;
            case 9: status = AntennaStatus.TMU_ANTENNA_0; break;
            case 10: status = AntennaStatus.TMU_ANTENNA_1; break;
            case 11: status = AntennaStatus.TMU_ANTENNA_2; break;
            case 12: status = AntennaStatus.TMU_ANTENNA_3; break;
            case 13: status = AntennaStatus.TMU_ANTENNA_4; break;
            case 14: status = AntennaStatus.TMU_ANTENNA_5; break;
            default: break; 
        }
        return status; 
    }
}
