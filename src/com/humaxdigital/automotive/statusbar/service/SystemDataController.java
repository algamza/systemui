package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

public class SystemDataController extends BaseController<Integer> {
    private enum DataStatus { NONE, DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
    private ConnectivityManager mConnectivity;
    private TelephonyManager mTelephony;

    public SystemDataController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        mConnectivity = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        if ( mTelephony != null ) mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    @Override
    public void disconnect() {
        if ( mTelephony != null ) mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void fetch() {
        if ( mConnectivity == null || mTelephony == null ) return;

        boolean isAvailable = mConnectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isAvailable();
        boolean isConnected = mConnectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
        if ( isAvailable && isConnected ) {
            int networkType = mTelephony.getNetworkType();
            // todo : save Data store
        }
    }

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (state != null) {
                switch(state.getDataNetworkType()) {

                }
            }
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {

        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            switch(state) {
                case TelephonyManager.DATA_DISCONNECTED: break;
                case TelephonyManager.DATA_CONNECTED: break;
            }
        }
    };

    @Override
    public Integer get() {
        // todo : getData store

        //convertToStatus()
        return 1; 
    }

    private DataStatus convertToStatus(int type) {
        DataStatus status = DataStatus.NONE; 
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            {
                status = DataStatus.DATA_E_NO;
                break;
            }
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            {
                status = DataStatus.DATA_E_NO;
                break;
            }
            case TelephonyManager.NETWORK_TYPE_LTE:
            {
                status = DataStatus.DATA_4G_NO;
                break;
            }
            default: break;
        }
        return status; 
    }
}
