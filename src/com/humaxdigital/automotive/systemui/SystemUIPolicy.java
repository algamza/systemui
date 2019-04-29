// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

public class SystemUIPolicy {
    private static final String TAG = "SystemUIService";

    private static final String DEFAULT_IMMERSIVE_MODE =
            "immersive.preconfirms=*";

    public static void applyPolicies(Context context) {
        applyImmersiveModePolicy(context);
    }

    private static void applyImmersiveModePolicy(Context context) {
        final ContentResolver ContentResolver = context.getContentResolver();

        // Set immersive control policy string
        // See. frameworks/base/services/core/java/com/android/server/policy/PolicyControl.java
        Settings.Global.putString(ContentResolver,
                Settings.Global.POLICY_CONTROL, DEFAULT_IMMERSIVE_MODE);
    }
}
