package com.humaxdigital.automotive.systemui.volumedialog;

public class VolumeUtil {
    public enum Type {
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
    
    static public Type convertToType(int mode) {
        Type type = Type.UNKNOWN;
        switch (mode) {
        case 0: type = Type.UNKNOWN; break;
        case 1: type = Type.RADIO_FM; break; 
        case 2: type = Type.RADIO_AM; break; 
        case 3: type = Type.USB; break; 
        case 4: type = Type.ONLINE_MUSIC; break; 
        case 5: type = Type.BT_AUDIO; break; 
        case 6: type = Type.BT_PHONE_RING; break; 
        case 7: type = Type.BT_PHONE_CALL; break; 
        case 8: type = Type.CARLIFE_MEDIA; break; 
        case 9: type = Type.CARLIFE_NAVI; break; 
        case 10: type = Type.CARLIFE_TTS; break; 
        case 11: type = Type.BAIDU_MEDIA; break; 
        case 12: type = Type.BAIDU_ALERT; break; 
        case 13: type = Type.BAIDU_VR_TTS; break; 
        case 14: type = Type.BAIDU_NAVI; break; 
        case 15: type = Type.EMERGENCY_CALL; break; 
        case 16: type = Type.ADVISOR_CALL; break; 
        case 17: type = Type.BEEP; break; 
        case 18: type = Type.WELCOME_SOUND; break; 
        case 19: type = Type.SETUP_GUIDE; break; 
        default: break;
        }
        return type;
    }
    
    static public int convertToMode(Type type) {
        int mode = 0; 
        switch (type) {
            case UNKNOWN: mode = 0; break;
            case RADIO_FM: mode = 1; break; 
            case RADIO_AM: mode = 2; break; 
            case USB: mode = 3; break; 
            case ONLINE_MUSIC: mode = 4; break; 
            case BT_AUDIO: mode = 5; break; 
            case BT_PHONE_RING: mode = 6; break; 
            case BT_PHONE_CALL: mode = 7; break; 
            case CARLIFE_MEDIA: mode = 8; break; 
            case CARLIFE_NAVI: mode = 9; break; 
            case CARLIFE_TTS: mode = 10; break; 
            case BAIDU_MEDIA: mode = 11; break; 
            case BAIDU_ALERT: mode = 12; break; 
            case BAIDU_VR_TTS: mode = 13; break; 
            case BAIDU_NAVI: mode = 14; break; 
            case EMERGENCY_CALL: mode = 15; break; 
            case ADVISOR_CALL: mode = 16; break; 
            case BEEP: mode = 17; break; 
            case WELCOME_SOUND: mode = 18; break; 
            case SETUP_GUIDE: mode = 19; break; 
            default: break;
        }
        return mode;
    }
}

 