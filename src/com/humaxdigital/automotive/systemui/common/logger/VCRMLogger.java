package com.humaxdigital.automotive.systemui.common.logger;

import android.extension.car.log.VcrmEventLog; 
import android.util.Log;

public class VCRMLogger {
    private static final String TAG = "VCRMLogger"; 

    private static VolumeType sLastVolumeType = VolumeType.UNKNOWN;
    private static int sLastVolumeLevel = -1;

    private VCRMLogger() {}
    public enum VolumeType {
        UNKNOWN, RADIO_FM, RADIO_AM, USB, ONLINE_MUSIC, BT_AUDIO, 
        BT_PHONE_RING, BT_PHONE_CALL, CARLIFE_MEDIA, CARLIFE_NAVI, 
        CARLIFE_TTS, BAIDU_MEDIA, BAIDU_ALERT, BAIDU_VR_TTS, BAIDU_NAVI, 
        EMERGENCY_CALL, ADVISOR_CALL, BEEP, WELCOME_SOUND, SETUP_GUIDE
    }
    public enum WirelessChargingState {
        OFF, CELLPHONE_ON_PAD, CHARGING, CHARGING_COMPLETE, CELLPHONE_REMINDER, CHARGING_ERROR
    }

    public static void changedTimeSetting(String time) {
        Log.d(TAG, "changedTimeSetting:time="+time); 
    }

    public static void changedWirelessCharging(WirelessChargingState state) {
        Log.d(TAG, "changedWirelessCharging="+state); 
        int value = 0; 
        switch(state) {
            case OFF:  value = 1; break; 
            case CELLPHONE_ON_PAD: value = 2; break; 
            case CHARGING: value = 3; break; 
            case CHARGING_COMPLETE: value = 4; break; 
            case CELLPHONE_REMINDER: value = 5; break; 
            case CHARGING_ERROR: value = 6; break; 
            default: return; 
        }
        VcrmEventLog.writeNewStandardLog(VcrmEventLog.NewVCRMStandard.ID_STD_VI_158, Integer.toString(value));
    }

    public static void changedScreen(String screen) {
        Log.d(TAG, "changedScreen:screen="+screen); 
        if ( screen == null ) return;
        VcrmEventLog.writeNewStandardLog(VcrmEventLog.NewVCRMStandard.ID_STD_UX_1, screen);
    }

    /*
        [ServiceID:1~39]
        1:BT Streaming,
        Aux,
        BT Hands Free,
        Incomming Ring,
        Phone MIC, (핸즈프리 통화 시 마이크 음량)
        Navigation 안내음,
        Navigation 경고음,
        CarPlay_Navi,
        CarPlay_Media,
        10:CarPlay_Siri,
        CarPlay_Phone,
        CarPlay_Phone_bell,
        Android Auto_Navi,
        Android Auto_Media,
        MirrorLink_Navi,
        MirrorLink_Media,
        CarLife_Navi,
        CarLife_Media,
        BEEP, (Soft Key 동작에 대한 반응 사운드)
        20:CD/DVD,
        USB Audio,
        USB Video,
        iPod,
        Voice Memo,
        FM,
        AM,
        SXM,
        ITM,
        DMB,
        30:CMMB,
        1Seg,
        Voice Recognition,
        SMS,
        Concierge Call,
        SOS Call,
        Driver Voice,
        CarLife VR,
        INFO, (TA음량)
        39:Alerts, (음성메모 시작/종료 알람음)
    */
    public static void changedVolume(VolumeType type, int level) {
        Log.d(TAG, "changedVolume:type="+type+", level="+level); 

        if (type == sLastVolumeType && level == sLastVolumeLevel) {
            return;
        }
        sLastVolumeType = type;
        sLastVolumeLevel = level;

        StringBuffer sb = new StringBuffer(); 
        switch(type) {
            //case UNKNOWN: break; 
            case RADIO_FM: sb.append(Integer.toString(25)); break;  
            case RADIO_AM: sb.append(Integer.toString(26)); break;  
            case USB: sb.append(Integer.toString(21)); break;  
            case ONLINE_MUSIC: sb.append(Integer.toString(14)); break;  
            case BT_AUDIO: sb.append(Integer.toString(1)); break; 
            case BT_PHONE_RING: sb.append(Integer.toString(4)); break; 
            case BT_PHONE_CALL: sb.append(Integer.toString(3)); break; 
            case CARLIFE_MEDIA: sb.append(Integer.toString(18)); break; 
            case CARLIFE_NAVI: sb.append(Integer.toString(17)); break; 
            case CARLIFE_TTS: sb.append(Integer.toString(37)); break; 
            //case BAIDU_MEDIA: break; 
            //case BAIDU_ALERT: break; 
            case BAIDU_VR_TTS: sb.append(Integer.toString(32)); break; 
            case BAIDU_NAVI: sb.append(Integer.toString(8)); break; 
            case EMERGENCY_CALL: sb.append(Integer.toString(35)); break; 
            case ADVISOR_CALL: sb.append(Integer.toString(34)); break; 
            case BEEP: sb.append(Integer.toString(19)); break; 
            //case WELCOME_SOUND: break;  
            //case SETUP_GUIDE: sb.append(Integer.toString(38)); break; 
            default: return; 
        }
        sb.append("\t"); 
        sb.append(Integer.toString(level)); 
        VcrmEventLog.writeNewStandardLog(VcrmEventLog.NewVCRMStandard.ID_STD_AV_11, sb.toString());
    }

    public static void triggerTimeSettingFromStatusBar() {
        // setting menu [1], Status Bar [2], home widget [3]
        VcrmEventLog.writeNewStandardLog(VcrmEventLog.NewVCRMStandard.ID_STD_SYS_10, Integer.toString(2));
    }

    public static void triggerThreeFigers() {
        Log.d(TAG, "triggerThreeFigers"); 
        VcrmEventLog.writeNewUtilCount(VcrmEventLog.NewVCRMUtilCount.ID_UTIL_SYS_11); 
    }

    public static void triggerFourFigers() {
        Log.d(TAG, "triggerFourFigers"); 
        VcrmEventLog.writeNewUtilCount(VcrmEventLog.NewVCRMUtilCount.ID_UTIL_SYS_12);
    }

    public static void triggerDropDown() {
        Log.d(TAG, "triggerDropDown"); 
        VcrmEventLog.writeNewUtilCount(VcrmEventLog.NewVCRMUtilCount.ID_UTIL_SYS_14);
    }
}