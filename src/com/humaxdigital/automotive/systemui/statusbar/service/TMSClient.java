package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.UserHandle;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;
import android.car.hardware.CarPropertyValue;
import android.car.CarNotConnectedException;
import android.extension.car.CarTMSManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Objects; 

import com.humaxdigital.automotive.systemui.common.CONSTANTS; 

public class TMSClient {
    private static final String TAG = "TMSClient";
    private CarTMSManager mTMSManager = null;
    private List<TMSCallback> mListeners = new ArrayList<>(); 
    private Context mContext; 
    private ConnectionStatus mCurrentConnectionStatus = ConnectionStatus.DISCONNECTED; 
    private ActiveStatus mCurrentActiveStatus = ActiveStatus.DEACTIVE; 
    private int mCurrentSignalLevel = 0; 
    private CallingStatus mCurrentCallingStatus = CallingStatus.CALL_DISCONNECTED; 
    private DataUsingStatus mCurrentDataUsingStatus = DataUsingStatus.DATA_NO_PACKET; 
    private LocationSharingStatus mCurrentLocationSharingStatus = LocationSharingStatus.LOCATION_SHARING_CANCEL; 

    private Timer mTimer = new Timer();
    private TimerTask mNetworkDisconnectTask = null; 
    private boolean isWaitingNetworkDisconnection = false; 
    private final int MAINTAIN_TIME_MS = 40000;

    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTED
    }

    public enum ActiveStatus {
        ACTIVE,
        DEACTIVE
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

    public interface TMSCallback {
        public void onConnectionChanged(ConnectionStatus connection);
        default public void onSignalLevelChanged(int level) {};
        default public void onCallingStatusChanged(CallingStatus status) {};
        default public void onDataUsingChanged(DataUsingStatus status) {}
        default public void onLocationSharingChanged(LocationSharingStatus status) {};
        default public void onEmergencyCall(boolean on) {};
        default public void onBluelinkCall(boolean on) {};
    }

    public TMSClient(Context context) {
        mContext = Objects.requireNonNull(context); 
        createBroadcastReceiver();
    }

    public void connect() {
    }

    public void disconnect() {
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
        mListeners.add(Objects.requireNonNull(callback)); 
    }

    public void unregisterCallback(TMSCallback callback) {
        mListeners.remove(Objects.requireNonNull(callback));
    }

    public ConnectionStatus getConnectionStatus() {
        Log.d(TAG, "getConnectionStatus="+mCurrentConnectionStatus);
        return mCurrentConnectionStatus; 
    }

    public ActiveStatus getActiveStatus() {
        Log.d(TAG, "getActiveStatus="+mCurrentActiveStatus);
        return mCurrentActiveStatus; 
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

    private void updateDataStatus(ConnectionStatus status) {
        Log.d(TAG, "updateDataStatus="+status+" current="+mCurrentConnectionStatus
            +", wait="+isWaitingNetworkDisconnection);
        if ( status == ConnectionStatus.DISCONNECTED 
            && mCurrentConnectionStatus != ConnectionStatus.DISCONNECTED ) {
            if ( isWaitingNetworkDisconnection ) return;
            mNetworkDisconnectTask = new TimerTask() {
                @Override
                public void run() {
                    isWaitingNetworkDisconnection = false;
                    mNetworkDisconnectTask = null; 
                    mCurrentConnectionStatus = ConnectionStatus.DISCONNECTED;
                    for ( TMSCallback callback : mListeners ) 
                        callback.onConnectionChanged(mCurrentConnectionStatus);
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

        if ( status != mCurrentConnectionStatus ) {
            mCurrentConnectionStatus = status;
            for ( TMSCallback callback : mListeners ) 
                callback.onConnectionChanged(mCurrentConnectionStatus);
        }
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
                    
                    updateDataStatus(status); 

                    if ( rssiSignal != mCurrentSignalLevel ) {
                        mCurrentSignalLevel = rssiSignal; 
                        for ( TMSCallback callback : mListeners ) 
                            callback.onSignalLevelChanged(mCurrentSignalLevel); 
                    }
                    if ( active == 0 ) 
                        mCurrentActiveStatus = ActiveStatus.DEACTIVE; 
                    else 
                        mCurrentActiveStatus = ActiveStatus.ACTIVE; 

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

                    Log.d(TAG, "APP_TMS_UPDATE_CALL_STATUS:call="+requestDataCallStatusType);

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

    private void createBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter(); 
        filter.addAction(CONSTANTS.ACTION_LOCATION_SHARING_COUNT); 
        mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, null);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch(action) {
                case CONSTANTS.ACTION_LOCATION_SHARING_COUNT: {
                    int count = intent.getExtras().getInt("lsc_count");
                    Log.d(TAG, "CONSTANTS.ACTION_LOCATION_SHARING_COUNT="+count); 
                    if (count > 0) {
                        LocationSharingStatus status = LocationSharingStatus.LOCATION_SHARING; 
                        if ( mCurrentLocationSharingStatus != status ) {
                            mCurrentLocationSharingStatus = status; 
                            for ( TMSCallback callback : mListeners ) 
                                callback.onLocationSharingChanged(status); 
                        }
                    } else {
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
    };
}
