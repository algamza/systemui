// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.statusbar.dev;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.app.TaskStackListener;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.humaxdigital.automotive.statusbar.R;

public class DevNavigationBar extends FrameLayout {
    private static final String TAG = DevNavigationBar.class.getSimpleName();

    private Context mContext;
    private DevCommands mDevCommands;
    private ContentResolver mContentResolver;
    private ActivityManager mActivityManager;

    private final Handler mRetrieveHandler = new Handler();
    private final Runnable mRetrieveRunnable = this::retrieveTopActivity;
    private ComponentName mTopActivity;
    private TextView mCurrentActivityTextView;
    private TextView mSavedActivityTextView;

    public DevNavigationBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mContentResolver = context.getContentResolver();
        mActivityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);

        addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                retrieveTopActivity();
                updateSavedActivityText();
            }
            @Override
            public void onViewDetachedFromWindow(View v) {
                mRetrieveHandler.removeCallbacks(mRetrieveRunnable);
            }
        });
    }

    public void setDevCommands(DevCommands devCommands) {
        mDevCommands = devCommands;
    }

    protected void onFinishInflate () {
        mCurrentActivityTextView = (TextView) findViewById(R.id.txtCurrentActivity);
        mSavedActivityTextView = (TextView) findViewById(R.id.txtSavedActivity);

        findViewById(R.id.btnGoHome).setOnClickListener(view -> { goHome(); });
        findViewById(R.id.btnGoBack).setOnClickListener(view -> { goBack(); });
        findViewById(R.id.btnAppList).setOnClickListener(view -> { runAppList(); });
        findViewById(R.id.btnSettings).setOnClickListener(view -> { runSettings(); });
        findViewById(R.id.btnStop).setOnClickListener(view -> { stopTopActivity(); });
        findViewById(R.id.btnSave).setOnClickListener(view -> { saveCurrentActivity(); });
        findViewById(R.id.btnLoad).setOnClickListener(view -> { loadSavedActivity(); });
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

    public void stopTopActivity() {
        if (mDevCommands != null && mTopActivity != null) {
            mDevCommands.forceStopPackage(mTopActivity.getPackageName(), UserHandle.USER_CURRENT);
        }
    }

    public void saveCurrentActivity() {
        if (mTopActivity == null || mDevCommands == null)
            return;
        mDevCommands.putPreferenceString("dev_saved_actvity", mTopActivity.flattenToShortString());
        updateSavedActivityText();
    }

    public void loadSavedActivity() {
        if (mDevCommands == null)
            return;
        String savedActivity = mDevCommands.getPreferenceString("dev_saved_actvity", "");
        if (!savedActivity.isEmpty()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.setComponent(ComponentName.unflattenFromString(savedActivity));
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    private void retrieveTopActivity() {
        mTopActivity = mActivityManager.getRunningTasks(1).get(0).topActivity;
        if (mCurrentActivityTextView != null) {
            mCurrentActivityTextView.setText(mTopActivity.flattenToShortString());
        }
        mRetrieveHandler.postDelayed(mRetrieveRunnable, 1000);
    }

    private void updateSavedActivityText() {
        if (mDevCommands != null) {
            String savedActivity = mDevCommands.getPreferenceString("dev_saved_actvity", "");
            if (mSavedActivityTextView != null) {
                mSavedActivityTextView.setText(savedActivity);
            }
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
