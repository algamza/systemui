package com.humaxdigital.automotive.systemui.user;

import android.os.RemoteException;
import android.os.Bundle; 
import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile; 
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothDevice; 

import java.util.ArrayList;
import java.util.HashMap; 
import java.util.Map;
import java.util.List;
import android.util.Log;

public class UserBluetooth extends IUserBluetooth.Stub {
    private final String TAG = "UserBluetooth"; 

    public enum Profiles { HEADSET, A2DP_SINK }

    private int[] mBTDeviceProfiles = { 
        BluetoothProfile.HEADSET_CLIENT, 
        BluetoothProfile.A2DP_SINK }; 

    private final PerUserService mService;
    private List<IUserBluetoothCallback> mListeners = new ArrayList<>(); 
    private Context mContext = null; 
    private BluetoothAdapter mBluetoothAdapter = null;
    private Map<Integer, BluetoothProfile> mCurrentProxy = new HashMap<>(); 
    private int mContactsDownloadingState = 0; 
    //private final String CALL_STATUS = "com.humaxdigital.automotive.btphone.response_call_status"; 

    //private int mCurrentCallingState = 0; 
    private int mCurrentContactsDownloadState = 0;
    private int mCurrentCallHistoryDownloadState = 0;
    private int mCurrentBatteryLevel = 0; 
    private int mCurrentAntennaLevel = 0;
    private boolean mCurrentHeadsetConnected = false;
    private boolean mCurrentA2dpConnected = false; 

    public UserBluetooth(PerUserService service) {
        Log.d(TAG, "UserBluetooth");
        mService = service; 
        if ( service == null ) return;
        mContext = mService.getApplicationContext(); 
        if ( mContext == null ) return;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connect();
    }

    public void destroy() {
        disconnect();
        mListeners.clear();
        mBluetoothAdapter = null;
        mContext = null;
    }
    
    @Override
    public void registCallback(IUserBluetoothCallback callback) throws RemoteException {
        Log.d(TAG, "registCallback");
        mListeners.add(callback);
    }

    @Override
    public void unregistCallback(IUserBluetoothCallback callback) throws RemoteException {
        Log.d(TAG, "unregistCallback");
        mListeners.remove(callback); 
    }

    @Override
    public int getBatteryLevel() throws RemoteException {
        Log.d(TAG, "getBatteryLevel="+mCurrentBatteryLevel); 
        return mCurrentBatteryLevel; 
    }

    @Override
    public int getAntennaLevel() throws RemoteException {
        Log.d(TAG, "getAntennaLevel="+mCurrentAntennaLevel); 
        return mCurrentAntennaLevel; 
    }
/*
    @Override
    public int getBluetoothCallingState() throws RemoteException {
        Log.d(TAG, "getBluetoothCallingState="+mCurrentCallingState); 
        return mCurrentCallingState; 
    }
*/
    @Override
    public int getContactsDownloadState() throws RemoteException {
        Log.d(TAG, "getContactsDownloadState="+mCurrentContactsDownloadState); 
        return mCurrentContactsDownloadState; 
    }

    @Override
    public int getCallHistoryDownloadState() throws RemoteException {
        Log.d(TAG, "getCallHistoryDownloadState="+mCurrentCallHistoryDownloadState); 
        return mCurrentCallHistoryDownloadState; 
    }

    @Override
    public boolean isHeadsetDeviceConnected() throws RemoteException {
        Log.d(TAG, "isHeadsetDeviceConnected="+mCurrentHeadsetConnected); 
        return mCurrentHeadsetConnected; 
    }

    @Override
    public boolean isA2dpDeviceConnected() throws RemoteException {
        Log.d(TAG, "isA2dpDeviceConnected="+mCurrentA2dpConnected); 
        return mCurrentA2dpConnected; 
    }

    @Override
    public boolean isBluetoothEnabled() throws RemoteException {
        if ( mBluetoothAdapter == null ) return false;
        boolean enable = mBluetoothAdapter.isEnabled();
        Log.d(TAG, "isEnabled="+enable);
        return enable;
    }

    @Override
    public boolean isEnabled() throws RemoteException {
        
        if ( mBluetoothAdapter == null ) return false;
        boolean enable = mBluetoothAdapter.isEnabled();
        Log.d(TAG, "isEnabled="+enable);
        return enable;
    }

    @Override
    public void setBluetoothEnable(boolean enable) throws RemoteException {
        if ( mBluetoothAdapter == null ) return;
        Log.d(TAG, "setBluetoothEnable="+enable);
        if ( enable ) mBluetoothAdapter.enable();
        else mBluetoothAdapter.disable();
    }
    
    private void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect"); 
        IntentFilter filter = new IntentFilter(); 
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); 
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED); 
        filter.addAction(BluetoothHeadsetClient.ACTION_AG_EVENT); 
        filter.addAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED); 
        filter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED); 
        // FIXME: it is bluetooth extension action. ( action_pbap_state ) 
        // please check. packages\apps\Bluetooth\src\com\android\bluetooth\pbapclient\PbapClientStateMachine.java 
        // intent.getIntExtra("state", 0);
        // PBAP_STATE_IDLE = 0;
        // PBAP_STATE_CONNECTING = 1;
        // PBAP_STATE_CONNECTED = 2;
        // PBAP_STATE_DOWNLOADING = 3;
        //filter.addAction("action_pbap_state"); 
        //filter.addAction(CALL_STATUS); 
        mContext.registerReceiver(mBTReceiver, filter);
        checkAllProfileConnection(); 
    }

    private void disconnect() {
        if ( mContext == null ) return; 
        Log.d(TAG, "disconnect"); 
        updateBluetootOn(false);
        for ( int key : mCurrentProxy.keySet() ) {
            if ( mBluetoothAdapter == null ) break; 
            mBluetoothAdapter.closeProfileProxy(key, mCurrentProxy.get(key)); 
        }
        mContext.unregisterReceiver(mBTReceiver); 
    }

    private int convertToProfile(Profiles profile) {
        int device_profile = 0; 
        switch(profile) {
            case HEADSET: device_profile = mBTDeviceProfiles[0]; break; 
            case A2DP_SINK: device_profile = mBTDeviceProfiles[1]; break; 
            default: break; 
        }
        return device_profile; 
    }

    private void checkAllProfileConnection() {
        for ( Profiles profile : Profiles.values() ) {
            checkProfileConnection(profile); 
        }
    }

    private void checkProfileConnection(Profiles profile) {
        if ( mBluetoothAdapter == null ) return; 

        int _profile = convertToProfile(profile); 
        if ( !mCurrentProxy.isEmpty() ) {
            BluetoothProfile _proxy = mCurrentProxy.get(_profile);
            if ( _proxy != null ) 
                mBluetoothAdapter.closeProfileProxy(_profile, _proxy); 
        }
        mBluetoothAdapter.getProfileProxy(mContext, mServiceListener, _profile);
    }

    BluetoothProfile.ServiceListener mServiceListener = 
        new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mCurrentProxy.put(profile, proxy); 

            switch(profile) {
                case BluetoothProfile.HEADSET_CLIENT: {
                    if ( proxy.getConnectedDevices().size() > 0 ) {
                        BluetoothHeadsetClient client = (BluetoothHeadsetClient)proxy;
                        try{
                            Bundle features = client.getCurrentAgEvents(proxy.getConnectedDevices().get(0));  
                            if ( features != null ) {
                                if ( BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL != null ) 
                                    mCurrentBatteryLevel = features.getInt(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL);
                                if ( BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH != null ) 
                                    mCurrentAntennaLevel = features.getInt(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH);
                            } else {
                                Log.d(TAG, "features are null"); 
                            }
                        } catch(Exception ex){
                            Log.d(TAG, "error : "+ex); 
                        }
                        updateConectionState(Profiles.HEADSET, 1); 
                    }
                    else updateConectionState(Profiles.HEADSET, 0); 
                    break; 
                }
                case BluetoothProfile.A2DP_SINK: {
                    if ( proxy.getConnectedDevices().size() > 0 ) 
                        updateConectionState(Profiles.A2DP_SINK, 1); 
                    else updateConectionState(Profiles.A2DP_SINK, 0); 
                    break; 
                }
                default: break; 
            }
        }
        @Override
        public void onServiceDisconnected(int profile) {
            switch(profile) {
                case BluetoothProfile.HEADSET_CLIENT: 
                    updateConectionState(Profiles.HEADSET, 0); 
                    break; 
                case BluetoothProfile.A2DP_SINK: 
                    updateConectionState(Profiles.A2DP_SINK, 0); 
                    break; 
                default: break; 
            }
        }
    };

    private void updateConectionState(Profiles profile, int state) {
        Log.d(TAG, "updateConectionState : profile="+profile+", state="+state);
        try {
            switch(profile) {
                case HEADSET: {
                    mCurrentHeadsetConnected = state==1?true:false; 
                    if ( !mCurrentHeadsetConnected ) {
                        //mCurrentCallingState = 0; 
                        mCurrentBatteryLevel = 0; 
                        mCurrentAntennaLevel = 0;
                        mCurrentContactsDownloadState = 0;
                        mCurrentCallHistoryDownloadState = 0;
                    }
                    for ( IUserBluetoothCallback callback : mListeners ) 
                        callback.onHeadsetConnectionStateChanged(state); 
                    break;
                }
                case A2DP_SINK: {
                    mCurrentA2dpConnected = state==1?true:false;
                    for ( IUserBluetoothCallback callback : mListeners ) 
                        callback.onA2dpConnectionStateChanged(state); 
                    break;
                }
            }
        } catch(RemoteException e) {
            Log.e(TAG, "error = " + e); 
        }
    }

    private void updateBluetootOn(boolean on) {
        if ( !on ) {
            //mCurrentCallingState = 0; 
            mCurrentContactsDownloadState = 0;
            mCurrentCallHistoryDownloadState = 0;
            mCurrentBatteryLevel = 0; 
            mCurrentAntennaLevel = 0;
            mCurrentHeadsetConnected = false;
            mCurrentA2dpConnected = false; 
        } 
        try {
            for ( IUserBluetoothCallback callback : mListeners ) 
                callback.onBluetoothEnableChanged(on?1:0); 
        } catch(RemoteException e) {
            Log.e(TAG, "error = " + e); 
        } 
    }

    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( mBluetoothAdapter == null || intent == null ) return;
            switch(intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF); 
                    if ( state == BluetoothAdapter.STATE_OFF ) updateBluetootOn(false);
                    else if ( state == BluetoothAdapter.STATE_ON ) updateBluetootOn(true);
                    Log.d(TAG, "ACTION_STATE_CHANGED:state="+state); 
                    break; 
                }
                // TODO : Because of timing issue ... 
                //case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED: 
                case BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED: {
                    // TODO: check the profile extra state 
                    /*
                    int STATE_DISCONNECTED = 0;
                    int STATE_CONNECTING = 1;
                    int STATE_CONNECTED = 2;
                    int STATE_DISCONNECTING = 3;
                    */
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1); 
                    Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED:Headset:state="+state); 
                    if ( state == 0 || state == 2 ) 
                        checkProfileConnection(Profiles.HEADSET); 
                    break;
                }
                case BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED: {
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1); 
                    Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED:A2DP:state="+state); 
                    if ( state == 0 || state == 2 ) 
                        checkProfileConnection(Profiles.A2DP_SINK); 
                    break; 
                }
                case BluetoothHeadsetClient.ACTION_AG_EVENT: {
                    Profiles profile = Profiles.HEADSET; 
                    try {
                        int battery_level = intent.getIntExtra(BluetoothHeadsetClient.EXTRA_BATTERY_LEVEL, -1);
                        if ( battery_level != -1 ) {
                            mCurrentBatteryLevel = battery_level; 
                            for ( IUserBluetoothCallback callback : mListeners ) 
                                callback.onBatteryStateChanged(battery_level);
                        }
                        int antenna_level = intent.getIntExtra(BluetoothHeadsetClient.EXTRA_NETWORK_SIGNAL_STRENGTH, -1);
                        if ( antenna_level != -1 ) {
                            mCurrentAntennaLevel = antenna_level; 
                            for ( IUserBluetoothCallback callback : mListeners ) 
                                callback.onAntennaStateChanged(antenna_level); 
                        }
                        Log.d(TAG, "ACTION_AG_EVENT:battery="+battery_level+", antenna="+antenna_level); 
                    } catch(RemoteException e) {
                        Log.e(TAG, "error = " + e); 
                    }
                    break; 
                }
                /*
                case "action_pbap_state": {
                    int state = intent.getIntExtra("state", 0);
                    Log.d(TAG, "action_pbap_state="+state); 
                    if ( mContactsDownloadingState == state ) break; 
                    try {
                        if ( state == 3 ) {
                            mContactsDownloadingState = state; 
                            mCurrentContactsDownloadState = 1; 
                            for ( IUserBluetoothCallback callback : mListeners ) 
                                callback.onContactsDownloadStateChanged(mCurrentContactsDownloadState); 
                        } else if ( state == 2 ) {
                            mContactsDownloadingState = state; 
                            mCurrentContactsDownloadState = 0; 
                            for ( IUserBluetoothCallback callback : mListeners ) 
                                callback.onContactsDownloadStateChanged(mCurrentContactsDownloadState); 
                        }
                    } catch(RemoteException e) {
                        Log.e(TAG, "error = " + e); 
                    }

                    break; 
                }
                */
                /*
                case CALL_STATUS: {
                    int state = intent.getIntExtra("status", 0);
                    Log.d(TAG, "received:com.humaxdigital.automotive.btphone.response_call_status:state="+state); 
                        // 0 : Idle
                        // 1 : Dialing
                        // 2 : Calling
                        // 3 : Outgoing
                    if ( state == 2 ) {
                        if ( !mCurrentHeadsetConnected ) return; 
                        if ( mCurrentCallingState == 1 ) return;
                        mCurrentCallingState = 1; 
                    }
                    else {
                        if ( mCurrentCallingState == 0 ) return;
                        mCurrentCallingState = 0; 
                    }
                    try {
                        for ( IUserBluetoothCallback callback : mListeners ) 
                            callback.onBluetoothCallingStateChanged(mCurrentCallingState); 
                    } catch(RemoteException e) {
                        Log.e(TAG, "error = " + e); 
                    }
                    break;
                }
                */
                default: break;
            }
        }
    };
}