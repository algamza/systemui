package com.humaxdigital.automotive.systemui.statusbar.user;

oneway interface IUserAudioCallback {
    void onAudioMuteChanged(boolean mute); 
    void onBluetoothMicMuteChanged(boolean mute); 
    void onNavigationChanged(boolean mute); 
}