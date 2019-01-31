package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.util.Log;

public class SystemDataController extends BaseController<Integer> {
    private static final String TAG = "SystemDataController";
    private enum DataStatus { NONE, DATA_4G, DATA_4G_NO, DATA_E, DATA_E_NO }
    private TMSClient mTMSClient = null;
    private DataStatus mCurrentStatus = DataStatus.NONE; 

    public SystemDataController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
        if ( mTMSClient != null ) 
            mTMSClient.unregisterCallback(mTMSCallback);
    }

    @Override
    public void fetch() {
    }

    public void fetch(TMSClient tms) {
        mTMSClient = tms; 
        mTMSClient.registerCallback(mTMSCallback);

        mCurrentStatus = getCurrentStatus();
    }

    @Override
    public Integer get() {
        mCurrentStatus = getCurrentStatus();
        Log.d(TAG, "get="+mCurrentStatus); 
        return mCurrentStatus.ordinal(); 
    }

    private DataStatus getCurrentStatus() {
        DataStatus status = DataStatus.NONE;
        if ( mTMSClient == null 
            || mTMSClient.getConnectionStatus() == TMSClient.ConnectionStatus.DISCONNECTED ) 
            return status;
        switch(mTMSClient.getDataUsingStatus()) {
            case DATA_NO_PACKET: status = DataStatus.DATA_4G; break;
            case DATA_USING_PACKET: status = DataStatus.DATA_4G_NO; break;
        }
        return status;
    }

    private void broadcastChangeEvent() {
        DataStatus status = getCurrentStatus();
        if ( mCurrentStatus == status ) return;
        mCurrentStatus = status;
        for ( Listener listener : mListeners ) 
            listener.onEvent(mCurrentStatus.ordinal());
    }

    private final TMSClient.TMSCallback mTMSCallback = new TMSClient.TMSCallback() {
        @Override
        public void onConnectionChanged(TMSClient.ConnectionStatus connection) {
            Log.d(TAG, "onConnectionChanged="+connection); 
            broadcastChangeEvent();
        }

        @Override
        public void onDataUsingChanged(TMSClient.DataUsingStatus status) {
            Log.d(TAG, "onDataUsingChanged="+status); 
            broadcastChangeEvent();
        }
    }; 
}
