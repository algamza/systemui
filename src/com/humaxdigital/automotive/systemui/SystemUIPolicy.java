// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui;

import android.content.ContentResolver;
import android.content.Context;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.IWindowManager;
import android.util.Log;

public class SystemUIPolicy {
    private static final String TAG = "SystemUIService";

    private static final String DEFAULT_IMMERSIVE_MODE =
            "immersive.preconfirms=*";

    private static final float DEFAULT_WINDOW_ANIMATION_SCALE = 0.5f;
    private static final float DEFAULT_TRANSITION_ANIMATION_SCALE = 0.5f;
    private static final float DEFAULT_ANIMATOR_DURATION_SCALE = 0.5f;

    public static void applyPolicies(Context context) {
        applyImmersiveModePolicy(context);
        applyDefaultAnimationScales(context);
    }

    private static void applyImmersiveModePolicy(Context context) {
        final ContentResolver ContentResolver = context.getContentResolver();

        // Set immersive control policy string
        // See. frameworks/base/services/core/java/com/android/server/policy/PolicyControl.java
        Settings.Global.putString(ContentResolver,
                Settings.Global.POLICY_CONTROL, DEFAULT_IMMERSIVE_MODE);
    }

    private static void applyDefaultAnimationScales(Context context) {
        final IWindowManager wm = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            wm.setAnimationScales(
                new float[] {
                    DEFAULT_WINDOW_ANIMATION_SCALE,
                    DEFAULT_TRANSITION_ANIMATION_SCALE,
                    DEFAULT_ANIMATOR_DURATION_SCALE
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
