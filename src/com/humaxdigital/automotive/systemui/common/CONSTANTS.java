package com.humaxdigital.automotive.systemui.common;

public class CONSTANTS {
    public static final String GLOBAL_KEY_VOLUME_UP ="com.humaxdigital.automotive.systemui.common.GLOBAL_KEY_VOLUME_UP";
    public static final int GLOBAL_KEY_VOLUME_UP_ON = 1;
    public static final int GLOBAL_KEY_VOLUME_UP_OFF = 0;
    public static final String GLOBAL_KEY_VOLUME_DOWN ="com.humaxdigital.automotive.systemui.common.GLOBAL_KEY_VOLUME_DOWN";
    public static final int GLOBAL_KEY_VOLUME_DOWN_ON = 1;
    public static final int GLOBAL_KEY_VOLUME_DOWN_OFF = 0;

    public static final String CHANGE_USER_ICON_EVENT = "com.humaxdigital.automotive.app.USERPROFILE.CHANGE_USER_ICON_EVENT";
    public static final String CHANGE_USER_GET_EXTRA_IMG = "BitmapImage"; 
    public static final String REQUEST_CURRENT_USER_ICON = "com.humaxdigital.dn8c.REQUEST_CURRENT_USER_ICON";

    public static final String ACTION_OPEN_WIFI_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_WIFI"; 
    public static final String ACTION_OPEN_BLUETOOTH_SETTING = "com.humaxdigital.dn8c.ACTION_BLUETOOTH_SETTINGS";  
    public static final String ACTION_OPEN_QUIET_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_SOUND_QUIET_MODE"; 
    public static final String ACTION_OPEN_AUTOMATIC_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_DISPLAY"; 
    public static final String ACTION_OPEN_THEME_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_ADVANCED_THEME_STYLE"; 
    public static final String ACTION_OPEN_SETUP = "com.humaxdigital.dn8c.ACTION_SETTINGS"; 
    public static final String ACTION_VOLUME_SETTINGS_STARTED = "com.humaxdigital.setup.ACTION_VOLUME_SETTINGS_STARTED";
    public static final String ACTION_VOLUME_SETTINGS_STOPPED = "com.humaxdigital.setup.ACTION_VOLUME_SETTINGS_STOPPED";
    public static final String ACTION_DEFAULT_SOUND_SETTINGS_STARTED = "com.humaxdigital.setup.ACTION_DEFAULT_SOUND_SETTINGS_STARTED"; 
    public static final String ACTION_DEFAULT_SOUND_SETTINGS_STOPPED = "com.humaxdigital.setup.ACTION_DEFAULT_SOUND_SETTINGS_STOPPED"; 
    public static final String ACTION_CAMERA_START = "com.humaxdigital.automotive.camera.ACTION_CAM_STARTED";
    public static final String ACTION_CAMERA_STOP = "com.humaxdigital.automotive.camera.ACTION_CAM_STOPED";
    public static final String ACTION_OPEN_DATE_SETTING = "com.humaxdigital.dn8c.ACTION_SETTINGS_CLOCK";
    public static final String ACTION_OPEN_USERPROFILE_SETTING = "com.humaxdigital.automotive.app.USERPROFILE";
    public static final String ACTION_LOCATION_SHARING_COUNT = "com.humaxdigital.automotive.bluelink.LSC_COUNT";
    public static final String ACTION_OPEN_SCREEN_SAVER = "com.humaxdigital.dn8c.ACTION_SS_SCREEN_OFF";
    public static final String ACTION_CARLIFE_STATE = "com.humaxdigital.automotive.carlife.CONNECTED"; 
    public static final String PBAP_STATE = "android.extension.car.PBAP_STATE"; 
    public static final String ACTION_OPEN_DROPLIST = "com.humaxdigital.automotive.systemui.droplist.action.OPEN_DROPLIST"; 
    public static final String ACTION_CLOSE_DROPLIST = "com.humaxdigital.automotive.systemui.droplist.action.CLOSE_DROPLIST";
    public static final String ACTION_CHANGE_MIC_MUTE = "com.humaxdigital.automotive.btphone.change_mute";
    public static final String ACTION_DISPLAY_OFF = "com.humaxdigital.dn8c.ACTION_SS_SCREEN_OFF";

    public static final String OPEN_HVAC_APP = "com.humaxdigital.automotive.climate.CLIMATE";
    public static final String VR_PACKAGE_NAME = "com.humaxdigital.automotive.baiduadapterservice";
    public static final String VR_RECEIVER_NAME = "com.humaxdigital.automotive.baiduadapterservice.duerosadapter.VRSpecialCaseReceiver";
    public static final String VR_DISMISS_ACTION = "com.humaxdigital.automotive.baiduadapterservice.VR_DISMISS_REQ";

    public static final String SETTINGS_DROPLIST = "droplist_shown";
    public static final String SETTINGS_VR = "vr_shown";
    public static final String BT_SYSTEM = "android.extension.car.BT_SYSTEM";

    // System gestures (defined in 'frameworks/base/.../policy/SystemGesturesDetector.java')
    public static final String ACTION_SYSTEM_GESTURE = "android.intent.action.SYSTEM_GESTURE";
    public static final String EXTRA_GESTURE = "gesture";
    public static final String EXTRA_FINGERS = "fingers";
    public static final String SYSTEM_GESTURE_SWIPE_FROM_TOP = "swipe-from-top";
    public static final String SYSTEM_GESTURE_HOLD_BY_FINGERS = "hold-by-fingers";

    public static final String HOME_ACTIVITY_NAME = "com.humaxdigital.automotive.dn8clauncher/.HomeActivity";
    public static final String SCREENSAVER_ACTIVITY_NAME = "com.humaxdigital.automotive.screensaver/.MainActivity";
    public static final String KEY_CURRENT_HOME_PAGE = "com.humaxdigital.dn8c.KEY_CURRENT_HOME_PAGE";
}
