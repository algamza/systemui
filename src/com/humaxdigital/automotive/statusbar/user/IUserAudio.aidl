package com.humaxdigital.automotive.statusbar.user;

import com.humaxdigital.automotive.statusbar.user.IUserAudioCallback;

interface IUserAudio {
    void registCallback(IUserAudioCallback callback);
    void unregistCallback(IUserAudioCallback callback);
    boolean isAudioMute();
    boolean isBluetoothMicMute();
    boolean isNavigationMute();
}