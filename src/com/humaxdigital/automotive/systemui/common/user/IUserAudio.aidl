package com.humaxdigital.automotive.systemui.common.user;

import com.humaxdigital.automotive.systemui.common.user.IUserAudioCallback;

interface IUserAudio {
    void registCallback(IUserAudioCallback callback);
    void unregistCallback(IUserAudioCallback callback);
    boolean isBluetoothMicMute();
    boolean isNavigationMute();
    boolean isMasterMute(); 
    void setMasterMute(boolean mute); 
    void performClick();
}