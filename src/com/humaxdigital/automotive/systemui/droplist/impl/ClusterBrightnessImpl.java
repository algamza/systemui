package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.util.Log;

import com.humaxdigital.automotive.systemui.user.IUserAudio;

public class ClusterBrightnessImpl extends BaseImplement<Integer> {
    static final String TAG = "ClusterBrightnessImpl"; 
    private IUserAudio mUserAudio = null;
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

    public void fetch(IUserAudio audio) {
        mUserAudio = audio;
    }
}
