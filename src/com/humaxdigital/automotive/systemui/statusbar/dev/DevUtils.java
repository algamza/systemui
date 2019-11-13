// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.statusbar.dev;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import java.util.Locale;

public class DevUtils {
    private static final String TAG = DevUtils.class.getSimpleName();

    public static final String KEEP_UNLOCKED =
            "com.humaxdigital.automotive.systemui.statusbar.dev.KEEP_UNLOCKED";

    public static boolean isDevelopmentSettingsEnabled(Context context) {
        final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        final boolean settingEnabled = Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                Build.TYPE.equals("eng") ? 1 : 0) != 0;
        final boolean hasRestriction = um.hasUserRestriction(
                UserManager.DISALLOW_DEBUGGING_FEATURES);
        final boolean isAdminOrDemo = um.isAdminUser() || um.isDemoUser();
        return isAdminOrDemo && !hasRestriction && settingEnabled;
    }

    public static void setDevelopmentSettingsEnabled(Context context, boolean enable) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, enable ? 1 : 0);
    }

    public static boolean isKeepingDevUnlocked(Context context) {
        final int defValue = Build.TYPE.equals("eng") ? 1 : 0;
        final boolean value = (Settings.Global.getInt(
                context.getContentResolver(), KEEP_UNLOCKED, defValue) != 0);
        return value;
    }

    public static void setKeepingDevUnlocked(Context context, boolean keepingUnlocked) {
        Settings.Global.putInt(context.getContentResolver(),
                KEEP_UNLOCKED, keepingUnlocked ? 1 : 0);
    }

    public static void setLocale(Locale locale) {
        updateLocales(new LocaleList(locale));
    }

    public static void updateLocales(LocaleList locales) {
        try {
            final IActivityManager am = ActivityManager.getService();
            final Configuration config = am.getConfiguration();

            config.setLocales(locales);
            config.userSetLocale = true;

            am.updatePersistentConfiguration(config);

            // Trigger the dirty bit for the Settings Provider.
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static LocaleList getLocales() {
        try {
            return ActivityManager.getService()
                    .getConfiguration().getLocales();
        } catch (RemoteException e) {
            e.printStackTrace();
            return LocaleList.getDefault();
        }
    }
}
