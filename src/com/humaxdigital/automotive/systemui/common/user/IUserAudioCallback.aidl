package com.humaxdigital.automotive.systemui.common.user;

oneway interface IUserAudioCallback {
    void onBluetoothMicMuteChanged(boolean mute); 
    void onNavigationChanged(boolean mute); 
    void onMasterMuteChanged(boolean mute); 
}