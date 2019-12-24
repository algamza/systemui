package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.UserHandle;

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
    private TMSClient mTMSClient = null;
    private Timer mTimer = new Timer();
    private TimerTask mNetworkCheckTask = null;
    private TimerTask mNetworkDisconnectTask = null; 
    private boolean isWaitingNetworkDisconnection = false; 
    private final int LOOP_TIME = 500;
    private long mTotalRx = 0; 
    private long mTotalTx = 0;
    private boolean mUsing = false; 
    private boolean mIsNetworkAvaliable = false; 
    private boolean mIsNetworkConnected = false; 
    private boolean mIsAvaliableActivityType = false; 
    private int mDataType = 0;
    private DataStatus mDataStatus = DataStatus.NONE;
    private final int MAINTAIN_TIME_MS = 40000;

    public SystemDataController(Context context, DataStore store) {
        super(context, store);
        Log.d(TAG, "SystemDataController");
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect");
        mConnectivity = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(TelephonyManager.ACTION_PRECISE_DATA_CONNECTION_STATE_CHANGED); 
        mContext.registerReceiverAsUser(mConnectivityListener, UserHandle.ALL, filter, null, null);

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
                        updateDataStatus(status);
                    } 
                } else {
                    if ( mUsing ) {
                        mUsing = false; 
                        Log.d(TAG, "r_old="+mTotalRx+", rx="+rx+" ("+(rx-mTotalRx)+"), t_old="+mTotalTx+", tx="+tx+" ("+(tx-mTotalTx)+")"); 
                        DataStatus status = convertToStatus(mIsNetworkAvaliable 
                            && mIsNetworkConnected, mDataType, mUsing);// && mIsAvaliableActivityType, mDataType, mUsing); 
                        updateDataStatus(status); 
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

        mTMSClient = null;
    }

    @Override
    public void fetch() {
        if ( mConnectivity == null || mTelephony == null ) return;

        final NetworkInfo netInfo = mConnectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (netInfo == null)  return;

        mIsNetworkAvaliable = netInfo.isAvailable();
        mIsNetworkConnected = netInfo.isConnectedOrConnecting();
        mIsAvaliableActivityType = isAvaliableData(mTelephony.getDataActivity()); 
        mDataType = mTelephony.getDataNetworkType();
        mDataStatus = convertToStatus(mIsNetworkAvaliable 
            && mIsNetworkConnected, mDataType, mUsing); // && mIsAvaliableActivityType, mDataType, mUsing); 

        Log.d(TAG, "fetch:avaliable="+mIsNetworkAvaliable+", connected="+mIsNetworkConnected
            +", avaliable data="+mIsAvaliableActivityType+", type="+mDataType+", status="+mDataStatus);
            
        broadcastStatus(mDataStatus); 
    }

    public void fetchTMSClient(TMSClient tms) {
        if ( tms == null ) return; 
        Log.d(TAG, "fetchTMSClient"); 
        mTMSClient = tms; 
    }

    @Override
    public Integer get() {
        DataStatus status = mDataStatus; 
        if ( mTMSClient != null ) {
            if ( mTMSClient.getActiveStatus() == TMSClient.ActiveStatus.DEACTIVE ) 
                status = DataStatus.NONE; 
        }
        Log.d(TAG, "get="+status+", mDataStatus="+mDataStatus); 
        return status.ordinal(); 
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

    private void updateDataStatus(DataStatus status) {
        Log.d(TAG, "updateDataStatus="+status+" current="+mDataStatus+", wait="+isWaitingNetworkDisconnection);
        if ( status == DataStatus.NONE && mDataStatus != DataStatus.NONE ) {
            if ( isWaitingNetworkDisconnection ) return;
            mNetworkDisconnectTask = new TimerTask() {
                @Override
                public void run() {
                    isWaitingNetworkDisconnection = false;
                    mNetworkDisconnectTask = null; 
                    mDataStatus = DataStatus.NONE;
                    broadcastStatus(mDataStatus); 
                }
            };
            isWaitingNetworkDisconnection = true;
            mTimer.schedule(mNetworkDisconnectTask, MAINTAIN_TIME_MS);
            return;
        } else {
            if ( mNetworkDisconnectTask != null ) {
                if ( mNetworkDisconnectTask.scheduledExecutionTime() > 0 ) {
                    mNetworkDisconnectTask.cancel();
                    mTimer.purge();
                    mNetworkDisconnectTask =  null;
                    isWaitingNetworkDisconnection = false;
                }
            }
        }
        if ( status != mDataStatus ) {
            mDataStatus = status;
            broadcastStatus(mDataStatus); 
        }
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
            mDataType = mTelephony.getDataNetworkType();
            Log.d(TAG, "action="+intent.getAction()+", available="+mIsNetworkAvaliable
                +", connected="+mIsNetworkConnected+", type="+mDataType);
            // todo : Check status when wifi is connected
            DataStatus status = convertToStatus(mIsNetworkAvaliable 
                && mIsNetworkConnected, mDataType, mUsing); // && mIsAvaliableActivityType, mDataType, mUsing); 
            updateDataStatus(status);
        }
    };

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onDataActivity(int direction) {
            if ( mTelephony == null ) return;
            mDataType = mTelephony.getDataNetworkType();
            mIsAvaliableActivityType = isAvaliableData(mTelephony.getDataActivity()); 
            DataStatus status = convertToStatus(mIsNetworkAvaliable 
                && mIsNetworkConnected, mDataType, mUsing); // && mIsAvaliableActivityType, mDataType, mUsing); 
            updateDataStatus(status);
        }
    };

    private void broadcastStatus(DataStatus status) {
        DataStatus _status = status; 
        if ( mTMSClient != null ) {
            if ( mTMSClient.getActiveStatus() == TMSClient.ActiveStatus.DEACTIVE ) 
                _status = DataStatus.NONE; 
        }
        Log.d(TAG, "broadcastStatus="+_status+", status="+status); 

        for ( Listener<Integer> listener : mListeners ) 
            listener.onEvent(_status.ordinal());
    }
}
