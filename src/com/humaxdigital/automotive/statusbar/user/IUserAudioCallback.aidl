package com.humaxdigital.automotive.statusbar.user;

oneway interface IUserAudioCallback {
    void onAudioMuteChanged(boolean mute); 
    void onBluetoothMicMuteChanged(boolean mute); 
    void onNavigationChanged(boolean mute); 
}