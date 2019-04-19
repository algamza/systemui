package com.humaxdigital.automotive.statusbar.droplist.impl;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

public class DisplayImpl extends BaseImplement<Boolean> {
    private static final String ACTION_OPEN_SCREEN_SAVER = 
        "com.humaxdigital.dn8c.ACTION_SS_SCREEN_OFF";

    public DisplayImpl(Context context) {
        super(context);
    }
    
    @Override
    public void set(Boolean e) {
        if ( mContext == null ) return; 
        if ( e == false ) {
            Intent intent = new Intent(ACTION_OPEN_SCREEN_SAVER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }
}
