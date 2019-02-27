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
import android.net.TrafficStats;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class SystemDataController extends BaseController<Integer> {
    private static final String TAG = "SystemDataController";
    private enum DataStatus { NONE, DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
    private ConnectivityManager mConnectivity = null;
    private TelephonyManager mTelephony = null;
    private Timer mTimer = new Timer();
    private TimerTask mNetworkCheckTask = null;
    private final int LOOP_TIME = 500;
    private long mTotalRx = 0; 
    private long mTotalTx = 0;
    private boolean mUsing = false; 
    private boolean mIsNetworkAvaliable = false; 
    private boolean mIsNetworkConnected = false; 
    private boolean mIsAvaliableActivityType = false; 
    private int mDataType = 0;
    private DataStatus mDataStatus = DataStatus.NONE;

    public SystemDataController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect");
        mConnectivity = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mConnectivityListener, filter);

        if ( mTelephony != null ) 
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        
        mTotalRx = TrafficStats.getMobileRxBytes();
        mTotalTx = TrafficStats.getMobileTxBytes();
        mNetworkCheckTask = new TimerTask() {
            @Override
            public void run() {
                long rx = TrafficStats.getMobileRxBytes();
                long tx = TrafficStats.getMobileTxBytes();
                //Log.d(TAG, "r_old="+mTotalRx+", rx="+rx+" ("+(rx-mTotalRx)+"), t_old="+mTotalTx+", tx="+tx+" ("+(tx-mTotalTx)+")"); 
                if ( (mTotalRx - rx) != 0 || (mTotalTx - tx) != 0 ) {
                    if ( !mUsing ) {
                        mUsing = true; 
                        Log.d(TAG, "r_old="+mTotalRx+", rx="+rx+" ("+(rx-mTotalRx)+"), t_old="+mTotalTx+", tx="+tx+" ("+(tx-mTotalTx)+")"); 
                        DataStatus status = convertToStatus(mIsNetworkAvaliable 
                            && mIsNetworkConnected, mDataType, mUsing); // && mIsAvaliableActivityType, mDataType, mUsing); 
                        if ( mDataStatus != status ) {
                            mDataStatus = status; 
                            for ( Listener<Integer> listener : mListeners ) 
                                listener.onEvent(mDataStatus.ordinal());
                        }
                    } 
                } else {
                    if ( mUsing ) {
                        mUsing = false; 
                        Log.d(TAG, "r_old="+mTotalRx+", rx="+rx+" ("+(rx-mTotalRx)+"), t_old="+mTotalTx+", tx="+tx+" ("+(tx-mTotalTx)+")"); 
                        DataStatus status = convertToStatus(mIsNetworkAvaliable 
                            && mIsNetworkConnected, mDataType, mUsing);// && mIsAvaliableActivityType, mDataType, mUsing); 
                        if ( mDataStatus != status ) {
                            mDataStatus = status; 
                            for ( Listener<Integer> listener : mListeners ) 
                                listener.onEvent(mDataStatus.ordinal());
                        }
                    }
                }
                mTotalRx = rx; 
                mTotalTx = tx; 
            }
        };

        mTimer.schedule(mNetworkCheckTask, 0, LOOP_TIME);
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect");
        if ( mNetworkCheckTask != null ) {
            mNetworkCheckTask.cancel();
            mTimer.purge();
            mNetworkCheckTask =  null;
        }

        if ( mTelephony != null ) 
            mTelephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void fetch() {
        if ( mConnectivity == null || mTelephony == null ) return;

        final NetworkInfo netInfo = mConnectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (netInfo == null)  return;

        mIsNetworkAvaliable = netInfo.isAvailable();
        mIsNetworkConnected = netInfo.isConnectedOrConnecting();
        mIsAvaliableActivityType = isAvaliableData(mTelephony.getDataActivity()); 
        mDataType = mTelephony.getNetworkType();
        mDataStatus = convertToStatus(mIsNetworkAvaliable 
            && mIsNetworkConnected, mDataType, mUsing); // && mIsAvaliableActivityType, mDataType, mUsing); 

        Log.d(TAG, "fetch:avaliable="+mIsNetworkAvaliable+", connected="+mIsNetworkConnected
            +", avaliable data="+mIsAvaliableActivityType+", type="+mDataType+", status="+mDataStatus);
    }

    @Override
    public Integer get() {
        return mDataStatus.ordinal(); 
    }

    private boolean isAvaliableData(int direction) {
        boolean avaliable = false;
        switch(direction) {
            case TelephonyManager.DATA_ACTIVITY_IN:
            case TelephonyManager.DATA_ACTIVITY_OUT: 
            case TelephonyManager.DATA_ACTIVITY_INOUT: 
                {
                    avaliable = true;
                    break;
                }
            case TelephonyManager.DATA_ACTIVITY_DORMANT:
            case TelephonyManager.DATA_ACTIVITY_NONE: 
            default: break;
        }
        Log.d(TAG, "isAvaliableData="+avaliable+", direction="+direction);
        return avaliable; 
    }

    private DataStatus convertToStatus(boolean on, int type, boolean using) {
        DataStatus status = DataStatus.NONE; 
        if ( !on ) return status;
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GSM:
            {
                if ( using ) status = DataStatus.DATA_E;
                else status = DataStatus.DATA_E_NO;
                break;
            }
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            {
                if ( using ) status = DataStatus.DATA_E;
                else status = DataStatus.DATA_E_NO;
                break;
            }
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            {
                if ( using ) status = DataStatus.DATA_4G;
                else status = DataStatus.DATA_4G_NO;
                break;
            }
            default: break;
        }
        Log.d(TAG, "convertToStatus:type="+type+", using="+using+", state="+status);
        return status; 
    }

    private final BroadcastReceiver mConnectivityListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mConnectivity == null || mTelephony == null ) return;
            final NetworkInfo netInfo = mConnectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (netInfo == null)  return;

            mIsNetworkAvaliable = netInfo.isAvailable();
            mIsNetworkConnected = netInfo.isConnectedOrConnecting();
            mIsAvaliableActivityType = isAvaliableData(mTelephony.getDataActivity()); 
            mDataType = mTelephony.getNetworkType();
            Log.d(TAG, "onReceive=CONNECTIVITY_ACTION, available="+mIsNetworkAvaliable
                +", connected="+mIsNetworkConnected+", type="+mDataType);
            // todo : Check status when wifi is connected
            DataStatus status = convertToStatus(mIsNetworkAvaliable 
                && mIsNetworkConnected, mDataType, mUsing); // && mIsAvaliableActivityType, mDataType, mUsing); 
            if ( status != mDataStatus ) {
                mDataStatus = status;
                for ( Listener<Integer> listener : mListeners ) 
                    listener.onEvent(mDataStatus.ordinal());
            }
        }
    };

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataActivity(int direction) {
            if ( mTelephony == null ) return;
            mDataType = mTelephony.getNetworkType();
            mIsAvaliableActivityType = isAvaliableData(mTelephony.getDataActivity()); 
            DataStatus status = convertToStatus(mIsNetworkAvaliable 
                && mIsNetworkConnected, mDataType, mUsing); // && mIsAvaliableActivityType, mDataType, mUsing); 
            if ( status != mDataStatus ) {
                mDataStatus = status;
                for ( Listener<Integer> listener : mListeners ) 
                    listener.onEvent(mDataStatus.ordinal());
            }
        }
    };
}
