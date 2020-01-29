package com.humaxdigital.automotive.systemui.droplist.impl;

import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.telephony.TelephonyManager;

import android.util.Log;

public class CallingImpl extends BaseImplement<Boolean> {
    private static final String TAG = "CallingImpl"; 
    private TelephonyManager mTelephony = null;

    public CallingImpl(Context context) {
        super(context);
    }

    @Override
    public void create() {
        mTelephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mContext.registerReceiverAsUser(mBroadcastReceiver, 
            UserHandle.ALL, filter, null, null);
    }

    @Override
    public void destroy() {
    }

    @Override
    public Boolean get() {
        boolean isCalling = isCalling();
        Log.d(TAG, "get="+isCalling); 
        return isCalling; 
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
}
