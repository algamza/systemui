package com.humaxdigital.automotive.statusbar.droplist.user;

import com.humaxdigital.automotive.statusbar.droplist.user.IUserAudioCallback;

interface IUserAudio {
    void registCallback(IUserAudioCallback callback);
    void unregistCallback(IUserAudioCallback callback);
    boolean isMasterMute(); 
    void setMasterMute(boolean mute); 
}