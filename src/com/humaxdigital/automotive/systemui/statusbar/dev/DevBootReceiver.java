// Copyright (c) 2018 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.statusbar.dev;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

public class DevBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            putBootCompletedTime(context, SystemClock.uptimeMillis());
        }
    }

    public void putBootCompletedTime(Context context, long time) {
        Settings.Global.putLong(context.getContentResolver(),
                "com.humaxdigital.automotive.systemui.statusbar.dev.BOOT_COMPLETED_TIME", time);
    }
}
