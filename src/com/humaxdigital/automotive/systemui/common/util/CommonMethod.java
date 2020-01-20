package com.humaxdigital.automotive.systemui.common.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.util.List;

import com.humaxdigital.automotive.systemui.common.CONSTANTS;

public class CommonMethod {
    private static final String TAG = "CommonMethod"; 

    public static ComponentName getTopActivity(Context context) {
        if ( context == null ) return null;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (tasks.isEmpty()) return null;
        return tasks.get(0).topActivity;
    }

    public static boolean isDropListShown(Context context) {
        if ( context == null ) return false;
        int shown = Settings.Global.getInt(
                context.getContentResolver(), CONSTANTS.SETTINGS_DROPLIST, 0);
        return (shown != 0);
    }

    public static boolean isVRShown(Context context) {
        if ( context == null ) return false;
        int shown = Settings.Global.getInt(
                context.getContentResolver(), CONSTANTS.SETTINGS_VR, 0);
        return (shown != 0);
    }

    public static void closeVR(Context context) {
        if ( context == null ) return;
        Intent intent = new Intent(); 
        ComponentName name = new ComponentName(CONSTANTS.VR_PACKAGE_NAME, CONSTANTS.VR_RECEIVER_NAME);
        intent.setComponent(name);
        intent.setAction(CONSTANTS.VR_DISMISS_ACTION);
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    public static void powerOn(Context context) {
        if ( context == null ) return;
        Intent intent = new Intent(CONSTANTS.ACTION_POWER_OFF_MODE_EXIT); 
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public static void goHome(Context context) {
        if ( context == null ) return;
        goHome(context, null);
    }

    public static void goHome(Context context, Bundle extras) {
        if ( context == null ) return;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        if (extras != null) {
            intent.putExtras(extras);
        }
        context.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    public static void turnOffDisplay(Context context) {
        if ( context == null ) return;
        context.startActivity(new Intent(CONSTANTS.ACTION_DISPLAY_OFF));
    } 

    public static int getShowingHomePageOrNegative(Context context) {
        if ( context == null ) return 0;
        ComponentName topActivity = getTopActivity(context);
        if (topActivity == null)
            return -1;
        if (!CONSTANTS.HOME_ACTIVITY_NAME.equals(topActivity.flattenToShortString()))
            return -1;
        final ContentResolver contentResolver = context.getContentResolver();
        return Settings.Global.getInt(contentResolver, CONSTANTS.KEY_CURRENT_HOME_PAGE, -1);
    }
}