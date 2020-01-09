package com.humaxdigital.automotive.systemui.common.util;

import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;

import android.util.Log;

import com.humaxdigital.automotive.systemui.common.CONSTANTS;

public class CommonMethod {
    private static final String TAG = "CommonMethod"; 

    static public void closeVR(Context context) {
        if ( context == null ) return;
        Log.d(TAG, "closeVR");
        Intent intent = new Intent(); 
        ComponentName name = new ComponentName(CONSTANTS.VR_PACKAGE_NAME, CONSTANTS.VR_RECEIVER_NAME);
        intent.setComponent(name);
        intent.setAction(CONSTANTS.VR_DISMISS_ACTION);
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }
}