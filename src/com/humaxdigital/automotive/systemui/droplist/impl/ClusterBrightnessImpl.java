package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.util.Log;


public class ClusterBrightnessImpl extends BaseImplement<Integer> {
    static final String TAG = "ClusterBrightnessImpl"; 
    public ClusterBrightnessImpl(Context context) {
        super(context);
    }

    @Override
    public Integer get() {
        int brightness = 0;
        return brightness;
    }

    @Override
    public void set(Integer e) {
        if ( mContext == null ) return;
        Log.d(TAG, "set="+e);
    }
}
