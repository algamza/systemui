package com.humaxdigital.automotive.statusbar.volumedialog;

import android.car.CarNotConnectedException;
import android.extension.car.CarAudioManagerEx;
import android.extension.car.log.VcrmEventLog; 

import android.util.Log;
import java.util.HashMap;

public class ScenarioVCRMLog {
    private final String TAG = "ScenarioVCRMLog"; 
    private enum LogType {
        MEDIA, 
        BT_STREAMING, 
        AUX, 
        BT_HF, 
        INCOMMING_RING, 
        PHONE_MIC, 
        NAVIGATION,
        CARPLAY_NAVI,
        CARPLAY_MEDIA,
        CARPLAY_SIRI, 
        CARPLAY_PHONE,
        CARPLAY_PHONE_BELL,
        AUTO_NAVI,
        AUTO_MEDIA,
        MIRRORLINK_NAVI,
        MIRRORLINK_MEDIA,
        CARLIFE_NAVI,
        CARLIFE_MEDIA,
        BEEP
    }

    private CarAudioManagerEx mCarAudioManagerEx = null;
    private HashMap<LogType,Integer> mLogVolume = new HashMap<>();

    public ScenarioVCRMLog() {
    }

    public void destroy() {
        mCarAudioManagerEx = null;
        mLogVolume.clear();
    }

    public ScenarioVCRMLog fetch(CarAudioManagerEx mgr) {
        mCarAudioManagerEx = mgr; 
        return this;
    }

    public void updateLog(int mode, int volume) {
        boolean isVolumeChanged = false; 
        switch(mode) {
            case 0: 
                break; 
            case 1: 
            case 2:  
            case 3: 
            case 11:
            case 4: {
                if (  mLogVolume.get(LogType.MEDIA) != volume ) isVolumeChanged = true;
                mLogVolume.put(LogType.MEDIA, volume); 
                break;
            }
            case 5: {
                if ( mLogVolume.get(LogType.BT_STREAMING) != volume ) isVolumeChanged = true;
                mLogVolume.put(LogType.BT_STREAMING, volume); 
                break;
            }
            case 6: {
                if ( mLogVolume.get(LogType.INCOMMING_RING) != volume ) isVolumeChanged = true;
                mLogVolume.put(LogType.INCOMMING_RING, volume); 
                break;
            }
            case 7: {
                if ( mLogVolume.get(LogType.BT_HF) != volume ) isVolumeChanged = true;
                mLogVolume.put(LogType.BT_HF, volume); 
                break;
            }
            case 8: {
                if ( mLogVolume.get(LogType.CARLIFE_MEDIA) != volume ) isVolumeChanged = true;
                mLogVolume.put(LogType.CARLIFE_MEDIA, volume); 
                break;
            }
            case 9: {
                if ( mLogVolume.get(LogType.CARLIFE_NAVI) != volume ) isVolumeChanged = true;
                mLogVolume.put(LogType.CARLIFE_NAVI, volume); 
                break;
            }
            case 10: 
                break;
            case 12: 
                break;
            case 13: 
                break;
            case 14: {
                if ( mLogVolume.get(LogType.NAVIGATION) != volume ) isVolumeChanged = true;
                mLogVolume.put(LogType.NAVIGATION, volume); 
                break;
            } 
            case 15: 
                break;
            case 16: 
                break; 
            case 17: {
                if ( mLogVolume.get(LogType.BEEP) != volume ) isVolumeChanged = true;
                mLogVolume.put(LogType.BEEP, volume); 
                break;
            }
            case 18: 
                break; 
            case 19: 
                break; 
        }
        Log.d(TAG, "updateLog="+isVolumeChanged); 
        if ( isVolumeChanged ) sendLog();
    }

    public void updateLogAll() {
        Log.d(TAG, "updateLogAll"); 
        if ( mCarAudioManagerEx == null ) return;

        for (int index = 0; LogType.values().length > index; ++index ) {
            int volume = 0; 
            LogType type = LogType.values()[index]; 
            try {
                int mode = convertToMode(type); 
                if ( mode != 0 ) volume = mCarAudioManagerEx.getVolumeForLas(mode); 
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Failed to get current volume", e);
            }
            mLogVolume.put(type, volume); 
        }

        sendLog();
    }

    private void sendLog() {
        String value = ""; 
        for (int index = 0; LogType.values().length > index; ++index ) {
            int volume = mLogVolume.get(LogType.values()[index]);
            value+= (volume+"\t"); 
        }
        Log.d(TAG, "sendLog="+value); 
        VcrmEventLog.writeStandardLog(VcrmEventLog.Standard.ID_STD_5, value); 
    }

    private int convertToMode(LogType type) {
        int mode = 0; 
        switch(type) {
            case MEDIA: 
                mode = 4; break; 
            case BT_STREAMING: 
                mode = 5; break; 
            case BT_HF: 
                mode = 7; break; 
            case NAVIGATION: 
                mode = 14; break;
            case CARLIFE_NAVI:
                mode = 9; break;
            case CARLIFE_MEDIA:
                mode = 8; break;
            case BEEP: 
                mode = 17; break; 
            case INCOMMING_RING:
                mode = 6; break;
            // TODO : need to check 
            case PHONE_MIC:
            case CARPLAY_SIRI: 
            case CARPLAY_MEDIA: 
            case CARPLAY_NAVI: 
            case AUTO_NAVI: 
            case AUTO_MEDIA:
            case AUX: 
            case CARPLAY_PHONE: 
            case CARPLAY_PHONE_BELL: 
            case MIRRORLINK_NAVI: 
            case MIRRORLINK_MEDIA: 
                break; 
        }
        return mode; 
    }
}