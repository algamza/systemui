package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.os.Bundle;

import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.extension.car.CarTMSManager;

import android.util.Log; 
import java.util.Arrays;

public class SystemLocationController extends BaseController<Integer> implements TMSClient.TMSCallback {
    private static final String TAG = "SystemLocationController";
    private TMSClient mTMSClient = null; 
    private LocationStatus mCurrentStatus = LocationStatus.NONE; 
    
    public enum LocationStatus { 
        NONE(0), LOCATION_SHARING(1);
        private final int state; 
        LocationStatus(int state) { this.state = state;}
        public int state() { return state; } 
    }

    public SystemLocationController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
        if ( mTMSClient != null ) 
            mTMSClient.unregisterCallback(this);
    }

    public void fetchTMSClient(TMSClient tms) {
        mTMSClient = tms; 
        if ( mTMSClient != null ) 
            mTMSClient.registerCallback(this);
        mCurrentStatus = getCurrentStatus(); 
    }

    @Override
    public Integer get() {
        mCurrentStatus = getCurrentStatus(); 
        Log.d(TAG, "get="+mCurrentStatus); 
        return mCurrentStatus.state(); 
    }

    private LocationStatus getCurrentStatus() {
        LocationStatus status = LocationStatus.NONE;
        if ( mTMSClient == null 
            || mTMSClient.getConnectionStatus() == TMSClient.ConnectionStatus.DISCONNECTED ) 
            return status;
        switch(mTMSClient.getLocationSharingStatus()) {
            case LOCATION_SHARING: status = LocationStatus.LOCATION_SHARING; break;
            case LOCATION_SHARING_CANCEL: status = LocationStatus.NONE; break;
        }
        return status;
    }

    private void broadcastChangeEvent() {
        LocationStatus status = getCurrentStatus();
        if ( mCurrentStatus == status ) return;
        mCurrentStatus = status;
        for ( Listener listener : mListeners ) 
            listener.onEvent(mCurrentStatus.state());
    }

    @Override
    public void onConnectionChanged(TMSClient.ConnectionStatus connection) {
        Log.d(TAG, "onConnectionChanged="+connection); 
        broadcastChangeEvent();
    }

    @Override
    public void onActivationChanged(TMSClient.ActiveStatus active) {
        Log.d(TAG, "onActivationChanged="+active); 
        broadcastChangeEvent();
    }

    @Override
    public void onLocationSharingChanged(TMSClient.LocationSharingStatus status) {
        Log.d(TAG, "onLocationSharingChanged="+status); 
        broadcastChangeEvent();
    }
}
