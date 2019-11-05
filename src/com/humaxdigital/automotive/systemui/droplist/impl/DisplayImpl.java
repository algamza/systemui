package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import com.humaxdigital.automotive.systemui.common.CONSTANTS; 

public class DisplayImpl extends BaseImplement<Boolean> {

    public DisplayImpl(Context context) {
        super(context);
    }
    
    @Override
    public void set(Boolean e) {
        if ( mContext == null ) return; 
        if ( e == false ) {
            Intent intent = new Intent(CONSTANTS.ACTION_OPEN_SCREEN_SAVER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }
}
