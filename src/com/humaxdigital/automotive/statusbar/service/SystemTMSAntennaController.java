package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

public class SystemTMSAntennaController extends BaseController<Integer> {
    private static final String TAG = "SystemTMSAntennaController";

    private enum AntennaStatus { NONE, TMS_ANTENNA_NO, TMS_ANTENNA_0, TMS_ANTENNA_1, 
        TMS_ANTENNA_2, TMS_ANTENNA_3, TMS_ANTENNA_4, TMS_ANTENNA_5 }

    public SystemTMSAntennaController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void fetch() {
    }

    @Override
    public Integer get() {
        int val = 0; 
        Log.d(TAG, "get="+val); 
        return val; 
    }

    private AntennaStatus convertToTMUAntennaLevel(int level) {
        AntennaStatus status = AntennaStatus.TMS_ANTENNA_NO; 
        // todo : check status 
        if ( level < 0 ) status = AntennaStatus.TMS_ANTENNA_0; 
        else if ( level > 0 && level <= 1 ) status = AntennaStatus.TMS_ANTENNA_1; 
        else if ( level > 1 && level <= 2 ) status = AntennaStatus.TMS_ANTENNA_2; 
        else if ( level > 2 && level <= 3 ) status = AntennaStatus.TMS_ANTENNA_3; 
        else if ( level > 3 && level <= 4 ) status = AntennaStatus.TMS_ANTENNA_4; 
        else if ( level > 4 ) status = AntennaStatus.TMS_ANTENNA_5; 

        return status; 
    }
}
