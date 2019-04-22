package com.humaxdigital.automotive.systemui.droplist.user;

import com.humaxdigital.automotive.systemui.droplist.user.IUserAudioCallback;

interface IUserAudio {
    void registCallback(IUserAudioCallback callback);
    void unregistCallback(IUserAudioCallback callback);
    boolean isMasterMute(); 
    void setMasterMute(boolean mute); 
}