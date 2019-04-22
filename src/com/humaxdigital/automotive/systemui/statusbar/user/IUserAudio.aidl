package com.humaxdigital.automotive.systemui.statusbar.user;

import com.humaxdigital.automotive.systemui.statusbar.user.IUserAudioCallback;

interface IUserAudio {
    void registCallback(IUserAudioCallback callback);
    void unregistCallback(IUserAudioCallback callback);
    boolean isAudioMute();
    boolean isBluetoothMicMute();
    boolean isNavigationMute();
}