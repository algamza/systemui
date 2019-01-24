// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.statusbar.dev;

import android.annotation.Nullable;
import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;

import com.humaxdigital.automotive.statusbar.R;

public class DevNavigationBar extends FrameLayout {
    private static final String TAG = DevNavigationBar.class.getSimpleName();

    private Context mContext;
    private ContentResolver mContentResolver;

    public DevNavigationBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    protected void onFinishInflate () {
        findViewById(R.id.btnGoHome).setOnClickListener(view -> { goHome(); });
        findViewById(R.id.btnGoBack).setOnClickListener(view -> { goBack(); });
        findViewById(R.id.btnAppList).setOnClickListener(view -> { runAppList(); });
        findViewById(R.id.btnSettings).setOnClickListener(view -> { runSettings(); });
        
        final int adbEnabled = Settings.Global.getInt(mContentResolver, Settings.Global.ADB_ENABLED, 0);
        final CheckBox chkAdbEnabled = (CheckBox) findViewById(R.id.chkAdbEnabled);
        chkAdbEnabled.setOnClickListener(view -> { toggleAdbEnabled(view); });
        chkAdbEnabled.setChecked(adbEnabled == 1);

        // TODO: Disabled because not verified
        chkAdbEnabled.setEnabled(false);
    }

    public void goHome() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    public void goBack() {
        injectKeyEvent(KeyEvent.KEYCODE_BACK);
    }

    public void runAppList() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.setClassName("com.humaxdigital.automotive.dn8clauncher",
                            "com.humaxdigital.automotive.dn8clauncher.AppListActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    public void runSettings() {
        Intent intent = new Intent();
        intent.setAction("android.settings.SETTINGS");
        mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    public void toggleAdbEnabled(View v) {
        int enabled = Settings.Global.getInt(mContentResolver, Settings.Global.ADB_ENABLED, 0);
        enabled = 1 - enabled;
        Settings.Global.putInt(mContentResolver, Settings.Global.ADB_ENABLED, enabled);
        if (v instanceof CheckBox) {
            CheckBox checkbox = (CheckBox) v;
            checkbox.setChecked(enabled == 1);
        }
    }

    private void injectKeyEvent(int keyCode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Instrumentation instrumentation = new Instrumentation();
                instrumentation.sendKeyDownUpSync(keyCode);
            }
        }).start();
    }
}
