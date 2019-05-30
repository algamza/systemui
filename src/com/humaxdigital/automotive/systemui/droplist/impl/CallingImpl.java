package com.humaxdigital.automotive.systemui.droplist.impl;

import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.extension.car.CarTMSManager;

import android.telephony.TelephonyManager;

import android.util.Log;

public class CallingImpl extends BaseImplement<Boolean> {
    private static final String TAG = "CallingImpl"; 
    private TelephonyManager mTelephony = null;
    private CarExtensionClient mCarClient = null;
    private CarTMSManager mCarTMSManager = null;

    public CallingImpl(Context context) {
        super(context);
    }

    @Override
    public void create() {
        if ( mContext == null ) return;
        mTelephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mContext.registerReceiverAsUser(mBroadcastReceiver, 
            UserHandle.ALL, filter, null, null);
    }

    @Override
    public void destroy() {
        fetchEx(null);
    }

    @Override
    public Boolean get() {
        boolean isCalling = isCalling();
        Log.d(TAG, "get="+isCalling); 
        return isCalling; 
    }

    public void fetchEx(CarExtensionClient client) {
        Log.d(TAG, "fetchEx");
        mCarClient = client; 
      
        if ( client == null ) {
            if ( mCarTMSManager != null ) {
                mCarTMSManager.unregisterCallback(mTMSEventListener);
                mCarTMSManager = null;
            }

            return;
        }
        mCarTMSManager = mCarClient.getTMSManager();
        if ( mCarTMSManager == null ) return;
        mCarTMSManager.registerCallback(mTMSEventListener);

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
                    if ( mListener != null ) 
                        mListener.onChange(isCalling());
                    break; 
                }
            }
        }
    };

    private CarTMSManager.CarTMSEventListener mTMSEventListener = new CarTMSManager.CarTMSEventListener(){
        @Override
        public void onEmergencyMode(boolean enabled) {
            if ( mListener != null ) 
                mListener.onChange(enabled);
        }
        @Override
        public void onBluelinkCallMode(boolean enabled) {
            if ( mListener != null ) 
                mListener.onChange(enabled);
        }
    };
}
