package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.util.Log;
import android.car.hardware.CarPropertyValue;
import android.car.CarNotConnectedException;
import android.extension.car.CarTMSManager;

import java.util.ArrayList;
import java.util.List;

public class TMSClient {
    private static final String TAG = "TMSClient";
    private CarTMSManager mTMSManager = null;
    private List<TMSCallback> mListeners = new ArrayList<>(); 
    private Context mContext; 
    private ConnectionStatus mCurrentConnectionStatus = ConnectionStatus.DISCONNECTED; 
    private int mCurrentSignalLevel = 0; 
    private CallingStatus mCurrentCallingStatus = CallingStatus.CALL_DISCONNECTED; 
    private DataUsingStatus mCurrentDataUsingStatus = DataUsingStatus.DATA_NO_PACKET; 
    private LocationSharingStatus mCurrentLocationSharingStatus = LocationSharingStatus.LOCATION_SHARING_CANCEL; 

    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTED
    }

    public enum CallingStatus {
        CALL_DISCONNECTED,
        CALL_CONNECTED,
        CALL_RINGING,
        CALL_TIMEOUT,
        NO_SERIVCE
    }

    public enum DataUsingStatus {
        DATA_NO_PACKET,
        DATA_USING_PACKET
    }

    public enum LocationSharingStatus {
        LOCATION_SHARING_CANCEL,
        LOCATION_SHARING
    }

    public static abstract class TMSCallback {
        public void onConnectionChanged(ConnectionStatus connection) {};
        public void onSignalLevelChanged(int level) {};
        public void onCallingStatusChanged(CallingStatus status) {};
        public void onDataUsingChanged(DataUsingStatus status) {}
        public void onLocationSharingChanged(LocationSharingStatus status) {};
        public void onEmergencyCall(boolean on) {};
        public void onBluelinkCall(boolean on) {};
    }

    public TMSClient(Context context) {
        if ( context == null ) return; 
        mContext = context; 
    }

    public void connect() {
        if ( mContext == null ) return;
    }

    public void disconnect() {
        if ( mContext == null ) return; 
    }

    public void fetch(CarTMSManager mgr) {
        if ( mgr == null ) return;
        mTMSManager = mgr;
        try {
            mTMSManager.registerCallback(mTMSMgrCallback);
            mTMSManager.registerCallback(mTMSEventListener);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }
    }

    public void registerCallback(TMSCallback callback) {
        if ( callback == null ) return; 
        mListeners.add(callback); 
    }

    public void unregisterCallback(TMSCallback callback) {
        if ( callback == null ) return; 
        mListeners.remove(callback);
    }

    public ConnectionStatus getConnectionStatus() {
        Log.d(TAG, "getConnectionStatus="+mCurrentConnectionStatus);
        return mCurrentConnectionStatus; 
    }

    public int getSignalLevel() {
        Log.d(TAG, "getSignalLevel="+mCurrentSignalLevel); 
        // min = 0, max = 7
        return mCurrentSignalLevel;
    }

    public CallingStatus getCallingStatus() {
        Log.d(TAG, "getCallingStatus="+mCurrentCallingStatus); 
        return mCurrentCallingStatus; 
    }

    public DataUsingStatus getDataUsingStatus() {
        Log.d(TAG, "DataUsingStatus="+mCurrentDataUsingStatus); 
        return mCurrentDataUsingStatus; 
    }

    public LocationSharingStatus getLocationSharingStatus() {
        Log.d(TAG, "getLocationSharingStatus="+mCurrentLocationSharingStatus); 
        return mCurrentLocationSharingStatus; 
    }

    private final CarTMSManager.CarTMSEventCallback mTMSMgrCallback =
        new CarTMSManager.CarTMSEventCallback () {
        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            int zones = value.getAreaId();
            if ( value.getPropertyId() != CarTMSManager.VENDOR_TMS_EVENT ) return;

            int eventId = 0;
            int dataLength = 0;
            byte[] fullData = (byte[])value.getValue();
            eventId = ((fullData[0] & 0xff) << 24) 
                    | ((fullData[1] & 0xff) << 16) 
                    | ((fullData[2] & 0xff) << 8) 
                    | (fullData[3] & 0xff);
            dataLength = ((fullData[4] & 0xff) << 24) 
                    | ((fullData[5] & 0xff) << 16) 
                    | ((fullData[6] & 0xff) << 8) 
                    | (fullData[7] & 0xff);

            byte[] eventData = {0}; 
            if ( dataLength > 0 ) {
                eventData = new byte[dataLength];
                System.arraycopy(fullData, 8, eventData, 0, dataLength);
            }

            switch(eventId) {
                case CarTMSManager.APP_TMS_MODEM_LIVE_SIGNAL_1SEC: {
                    int active = eventData[0];
                    int rssiSignal = eventData[1];
                    int networkStatus = eventData[2];
                    ConnectionStatus status = ConnectionStatus.DISCONNECTED; 
                    if( networkStatus == 1 || networkStatus == 5 ) 
                        status = ConnectionStatus.CONNECTED; 
                    else 
                        status = ConnectionStatus.DISCONNECTED; 
                    if ( status != mCurrentConnectionStatus ) {
                        mCurrentConnectionStatus = status;
                        for ( TMSCallback callback : mListeners ) 
                            callback.onConnectionChanged(status);
                    }
                    if ( rssiSignal != mCurrentSignalLevel ) {
                        mCurrentSignalLevel = rssiSignal; 
                        for ( TMSCallback callback : mListeners ) 
                            callback.onSignalLevelChanged(mCurrentSignalLevel); 
                    }

                    Log.d(TAG, "APP_TMS_MODEM_LIVE_SIGNAL_1SEC:active="+active
                        +", signal="+rssiSignal+", stauts="+networkStatus);

                    break;
                }
                case CarTMSManager.APP_TMS_UPDATE_CALL_STATUS: {
                    int requestDataCallStatusType       = eventData[0]; // -> Call status
                    int requestDataPacketStatusType     = eventData[2]; // -> Packet status

                    CallingStatus calling_status = CallingStatus.CALL_DISCONNECTED; 
                    switch (requestDataCallStatusType) {
                        case 0: calling_status = CallingStatus.CALL_DISCONNECTED; break;
                        case 2: calling_status = CallingStatus.CALL_CONNECTED; break; 
                        case 4: calling_status = CallingStatus.CALL_RINGING; break;
                        case 5: calling_status = CallingStatus.CALL_DISCONNECTED; break;
                        case 6: calling_status = CallingStatus.NO_SERIVCE; break; 
                        case 8: calling_status = CallingStatus.CALL_TIMEOUT; break; 
                    }
                    
                    if ( mCurrentCallingStatus != calling_status ) {
                        mCurrentCallingStatus = calling_status; 
                        for ( TMSCallback callback : mListeners ) 
                            callback.onCallingStatusChanged(calling_status);
                    }
                    
                    // this is only call data 
                    /*
                    DataUsingStatus data_status = DataUsingStatus.DATA_NO_PACKET; 
                    switch(requestDataPacketStatusType) {
                        case 0: data_status = DataUsingStatus.DATA_NO_PACKET; break; 
                        case 1: data_status = DataUsingStatus.DATA_USING_PACKET; break; 
                    }

                    if ( mCurrentDataUsingStatus != data_status ) {
                        mCurrentDataUsingStatus = data_status; 
                        for ( TMSCallback callback : mListeners ) 
                            callback.onDataUsingChanged(data_status); 
                    }
                    */

                    Log.d(TAG, "APP_TMS_UPDATE_CALL_STATUS:call="+requestDataCallStatusType);

                    break;
                }
                case CarTMSManager.APP_TMS_RES_LOCATIONSHARING: {
                    int result = eventData[0];
                    if (result == 0x01) {
                        LocationSharingStatus status = LocationSharingStatus.LOCATION_SHARING; 
                        if ( mCurrentLocationSharingStatus != status ) {
                            mCurrentLocationSharingStatus = status; 
                            for ( TMSCallback callback : mListeners ) 
                                callback.onLocationSharingChanged(status); 
                        }
                    } 
                    break;
                }
                case CarTMSManager.APP_TMS_RES_LOCATIONSHARING_CANCEL: {
                    int result = eventData[0];
                    if (result == 0x01) {
                        LocationSharingStatus status = LocationSharingStatus.LOCATION_SHARING_CANCEL; 
                        if ( mCurrentLocationSharingStatus != status ) {
                            mCurrentLocationSharingStatus = status; 
                            for ( TMSCallback callback : mListeners ) 
                                callback.onLocationSharingChanged(status); 
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
            Log.w(TAG, "error : id = "+propertyId+", zone="+zone);
        }
    };
    private CarTMSManager.CarTMSEventListener mTMSEventListener = new CarTMSManager.CarTMSEventListener(){
        @Override
        public void onEmergencyMode(boolean enabled) {
            for ( TMSCallback callback : mListeners ) 
                callback.onEmergencyCall(enabled); 
        }
        @Override
        public void onBluelinkCallMode(boolean enabled) {
            for ( TMSCallback callback : mListeners ) 
                callback.onBluelinkCall(enabled); 
        }
    };
}
