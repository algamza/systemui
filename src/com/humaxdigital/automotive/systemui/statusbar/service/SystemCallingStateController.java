package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.extension.car.CarTMSManager;

import android.telephony.TelephonyManager;

import android.util.Log;

public class SystemCallingStateController extends BaseController<Boolean> {
    private final String TAG = "SystemCallingStateController"; 
    private TelephonyManager mTelephony = null;
    private TMSClient mTMSClient = null;
   
    public SystemCallingStateController(Context context, DataStore store) {
        super(context, store);
        Log.d(TAG, "SystemCallingStateController"); 
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        Log.d(TAG, "connect"); 
        mTelephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mContext.registerReceiverAsUser(mBroadcastReceiver, 
            UserHandle.ALL, filter, null, null);
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect"); 
        mTMSClient = null; 
        mTelephony = null;
    }

    @Override
    public Boolean get() {
        boolean isCalling = isCalling();
        Log.d(TAG, "get="+isCalling); 
        return isCalling; 
    }

    public void fetchTMSClient(TMSClient tms) {
        mTMSClient = tms; 
        if ( mTMSClient != null ) 
            mTMSClient.registerCallback(mTMSCallback); 
    }

    private boolean isCalling() {
        if ( mTelephony == null ) return false; 
        int state = mTelephony.getCallState(); 
        switch(state) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
            case TelephonyManager.CALL_STATE_RINGING: {
                return true; 
            }
            case TelephonyManager.CALL_STATE_IDLE: break; 
        }
        return false; 
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch(action) {
                case TelephonyManager.ACTION_PHONE_STATE_CHANGED: {
                    String phone_state = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
                    Log.d(TAG, "ACTION_PHONE_STATE_CHANGED="+phone_state);
                    if ( phone_state == null ) break;
                    for ( Listener listener : mListeners ) 
                        listener.onEvent(isCalling());
                    break; 
                }
            }
        }
    };

    private final TMSClient.TMSCallback mTMSCallback = new TMSClient.TMSCallback() {
        @Override
        public void onEmergencyCall(boolean on) {
            Log.d(TAG, "onEmergencyCall="+on); 
            for ( Listener listener : mListeners ) 
                listener.onEvent(on);
        }
        @Override
        public void onBluelinkCall(boolean on) {
            Log.d(TAG, "onBluelinkCall="+on); 
            for ( Listener listener : mListeners ) 
                listener.onEvent(on);
        }
    }; 
}
