package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mConnectivityListener, filter);

        if ( mTelephony != null ) 
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    @Override
    public void disconnect() {
        if ( mTelephony != null ) 
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void fetch() {
        if ( mConnectivity == null || mTelephony == null || mDataStore == null ) return;

        final NetworkInfo netInfo = mConnectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (netInfo == null)  return;

        final boolean isAvailable = netInfo.isAvailable();
        final boolean isConnected = netInfo.isConnectedOrConnecting();
        int type = mTelephony.getNetworkType();
        // todo : Check status when wifi is connected
        if ( isAvailable && isConnected ) {
            boolean using = usingData(mTelephony.getDataActivity()); 
            mDataStore.setNetworkData(type, using); 
        }
        else {
            mDataStore.setNetworkData(type, false); 
        }
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        int type = mDataStore.getNetworkDataType(); 
        boolean using = mDataStore.getNetworkDataUsing(); 
        return convertToStatus(type, using).ordinal(); 
    }

    private boolean usingData(int direction) {
        boolean using = false;
        switch(direction) {
            case TelephonyManager.DATA_ACTIVITY_IN:
            case TelephonyManager.DATA_ACTIVITY_OUT: 
            case TelephonyManager.DATA_ACTIVITY_INOUT: 
                {
                    using = true;
                    break;
                }
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
            case TelephonyManager.DATA_ACTIVITY_NONE: 
            default: break;
        }
        return using; 
    }

    private DataStatus convertToStatus(int type, boolean using) {
        DataStatus status = DataStatus.NONE; 
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            {
                if ( using ) status = DataStatus.DATA_E;
                else status = DataStatus.DATA_E_NO;
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
                if ( using ) status = DataStatus.DATA_E;
                else status = DataStatus.DATA_E_NO;
                break;
            }
            case TelephonyManager.NETWORK_TYPE_LTE:
            {
                if ( using ) status = DataStatus.DATA_4G;
                else status = DataStatus.DATA_4G_NO;
                break;
            }
            default: break;
        }
        return status; 
    }

    private final BroadcastReceiver mConnectivityListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mConnectivity == null || mTelephony == null || mDataStore == null ) return;

            final NetworkInfo netInfo = mConnectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (netInfo == null)  return;

            final boolean isAvailable = netInfo.isAvailable();
            final boolean isConnected = netInfo.isConnectedOrConnecting();
            int type = mTelephony.getNetworkType();
            // todo : Check status when wifi is connected
            if ( isAvailable && isConnected ) {
                boolean using = usingData(mTelephony.getDataActivity()); 
                if ( mDataStore.shouldPropagateNetworkDataUpdate(type, using) ) {
                    for ( Listener<Integer> listener : mListeners ) 
                        listener.onEvent(convertToStatus(type, using).ordinal());
                }
            }
            else {
                if ( mDataStore.shouldPropagateNetworkDataUpdate(type, false) ) {
                    for ( Listener<Integer> listener : mListeners ) 
                        listener.onEvent(convertToStatus(type, false).ordinal());
                }
            }
        }
    };

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataActivity(int direction) {
            if ( mTelephony == null ) return;
            int type = mTelephony.getNetworkType();
            boolean using = usingData(mTelephony.getDataActivity()); 
            boolean shouldPropagate = mDataStore.shouldPropagateNetworkDataUpdate(type, using);
            if ( shouldPropagate ) {
                for ( Listener<Integer> listener : mListeners ) 
                    listener.onEvent(convertToStatus(type, using).ordinal());
            }
        }
    };

}
