
package com.humaxdigital.automotive.systemui.volumedialog;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;

public abstract class VolumeControllerBase {
    protected ArrayList<VolumeChangeListener> mListener = new ArrayList<>();

    public interface VolumeChangeListener {
        enum Event {
            VOLUME_UP,
            VOLUME_DOWN
        }
        enum Type {
            UNKNOWN, 
            RADIO_FM, 
            RADIO_AM, 
            USB, 
            ONLINE_MUSIC, 
            BT_AUDIO, 
            BT_PHONE_RING, 
            BT_PHONE_CALL, 
            CARLIFE_MEDIA,
            CARLIFE_NAVI, 
            CARLIFE_TTS, 
            BAIDU_MEDIA, 
            BAIDU_ALERT, 
            BAIDU_VR_TTS, 
            BAIDU_NAVI, 
            EMERGENCY_CALL, 
            ADVISOR_CALL,
            BEEP, 
            WELCOME_SOUND, 
            SETUP_GUIDE
        }
        void onVolumeUp(Type type, int max, int value);
        void onVolumeDown(Type type, int max, int value);
        void onMuteChanged(Type type, boolean mute);
    }

    public void init(Context context, View view) {}
    public void deinit() {}
    public void registVolumeListener(VolumeChangeListener listener) {
        if ( listener == null ) return;
        mListener.add(listener);
    }
    public void unregistVolumeListener(VolumeChangeListener listener) {
        if ( listener == null ) return;
        mListener.remove(listener);
    }
    public void fetch(VolumeControlService service) { }
}