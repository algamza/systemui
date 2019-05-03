// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.statusbar.dev;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.app.TaskStackListener;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
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

import com.humaxdigital.automotive.systemui.R;

public class DevNavigationBar extends FrameLayout {
    private static final String TAG = DevNavigationBar.class.getSimpleName();

    private Context mContext;
    private DevCommands mDevCommands;
    private ContentResolver mContentResolver;
    private ActivityManager mActivityManager;

    private final Handler mRetrieveHandler = new Handler();
    private final Runnable mRetrieveRunnable = this::retrievePeriodicData;
    private ComponentName mTopActivity;
    private TextView mCurrentActivityTextView;
    private TextView mSavedActivityTextView;
    private TextView mStartTimeTextView;
    private TextView mUserSwitchTimeTextView;
    private CheckBox mCpuUsageCheckBox;
    private CheckBox mUsbDebuggingCheckBox;
    private CheckBox mDropCacheCheckBox;
    private CheckBox mUsbHubCheckBox;

    private long mStartTime;
    private long mUserSwitchTime;
    private long mBootCompletedTime;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_SWITCHED)) {
                mUserSwitchTime = SystemClock.uptimeMillis();
                mBootCompletedTime = 0;
            } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                mBootCompletedTime = SystemClock.uptimeMillis();
                updateBootCompletedTimeText();
            }
        }
    };

    public DevNavigationBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mContentResolver = context.getContentResolver();
        mActivityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mStartTime = mUserSwitchTime = SystemClock.uptimeMillis();

        addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                onAttached();
            }
            @Override
            public void onViewDetachedFromWindow(View v) {
                onDetached();
            }
        });

        final IntentFilter userSwitchIntentFiilter = new IntentFilter();
        userSwitchIntentFiilter.addAction(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiverAsUser(
                mBroadcastReceiver, UserHandle.ALL, userSwitchIntentFiilter,
                null, null);

        final IntentFilter bootCompletedIntentFiilter = new IntentFilter();
        bootCompletedIntentFiilter.addAction(Intent.ACTION_BOOT_COMPLETED);
        bootCompletedIntentFiilter.setPriority(-100000); // Same as DevBootReceiver
        mContext.registerReceiverAsUser(
                mBroadcastReceiver, UserHandle.ALL, bootCompletedIntentFiilter,
                android.Manifest.permission.RECEIVE_BOOT_COMPLETED, null);
    }

    protected void onFinishInflate () {
        mCurrentActivityTextView = (TextView) findViewById(R.id.txtCurrentActivity);
        mSavedActivityTextView = (TextView) findViewById(R.id.txtSavedActivity);
        mStartTimeTextView = (TextView) findViewById(R.id.txtStartTime);
        mUserSwitchTimeTextView = (TextView) findViewById(R.id.txtUserSwitchTime);

        mCpuUsageCheckBox = (CheckBox) findViewById(R.id.chkCpuUsage);
        mCpuUsageCheckBox.setOnClickListener(view -> { writeCpuUsageOptions(); });

        mUsbDebuggingCheckBox = (CheckBox) findViewById(R.id.chkUsbDebugging);
        mUsbDebuggingCheckBox.setOnClickListener(view -> { writeUsbDebuggingOptions(); });

        mDropCacheCheckBox = (CheckBox) findViewById(R.id.chkDropCache);
        mDropCacheCheckBox.setOnClickListener(view -> { writeDropCacheOptions(); });

        mUsbHubCheckBox = (CheckBox) findViewById(R.id.chkUsbHub);
        mUsbHubCheckBox.setOnClickListener(view -> { writeUsbHubOptions(); });

        findViewById(R.id.btnGoHome).setOnClickListener(view -> { goHome(); });
        findViewById(R.id.btnGoBack).setOnClickListener(view -> { goBack(); });
        findViewById(R.id.btnAppList).setOnClickListener(view -> { runAppList(); });
        findViewById(R.id.btnSettings).setOnClickListener(view -> { runSettings(); });
        findViewById(R.id.btnStop).setOnClickListener(view -> { stopTopActivity(); });
        findViewById(R.id.btnSave).setOnClickListener(view -> { saveCurrentActivity(); });
        findViewById(R.id.btnLoad).setOnClickListener(view -> { loadSavedActivity(); });
    }

    public void init(DevCommands devCommands) {
        mDevCommands = devCommands;
        resetBootCompletedTime();
        updateStartTimeText();
        updateCpuUsageOptions();
        updateUsbDebuggingOptions();
        writeCpuUsageOptions();
        updateDropCacheOptions();
        writeDropCacheOptions();
        updateUsbHubOptions();
        writeUsbHubOptions();
    }

    public void onAttached() {
        retrievePeriodicData();
        updateSavedActivityText();
    }

    public void onDetached() {
        mRetrieveHandler.removeCallbacks(mRetrieveRunnable);
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

    public long getBootCompletedTime() {
        return Settings.Global.getLong(mContext.getContentResolver(),
                "com.humaxdigital.automotive.systemui.statusbar.dev.BOOT_COMPLETED_TIME", 0);
    }

    public void resetBootCompletedTime() {
        Settings.Global.putLong(mContext.getContentResolver(),
                "com.humaxdigital.automotive.systemui.statusbar.dev.BOOT_COMPLETED_TIME", 0);
    }

    private void retrievePeriodicData() {
        mTopActivity = mActivityManager.getRunningTasks(1).get(0).topActivity;
        if (mCurrentActivityTextView != null) {
            mCurrentActivityTextView.setText(mTopActivity.flattenToShortString());
        }

        if (mBootCompletedTime == 0 && Process.myUserHandle().equals(UserHandle.SYSTEM)) {
            long bootCompletedTime = getBootCompletedTime();
            if (bootCompletedTime > mUserSwitchTime) {
                mBootCompletedTime = bootCompletedTime;
                updateBootCompletedTimeText();
            }
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

    private void updateStartTimeText() {
        if (mStartTimeTextView != null) {
            mStartTimeTextView.setText("A:" + toSecString(mStartTime));
        }
    }

    private void updateBootCompletedTimeText() {
        if (mStartTimeTextView != null) {
            long elapsed = mBootCompletedTime - mUserSwitchTime;
            mUserSwitchTimeTextView.setText("B:" + toSecString(elapsed));
        }
    }

    private String toSecString(long millis) {
        return String.format("%d.%d", millis / 1000, millis % 1000);
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

    private void updateCpuUsageOptions() {
        final boolean checked = (Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.SHOW_PROCESSES, 0) != 0);
        mCpuUsageCheckBox.setChecked(checked);
    }

    private void writeCpuUsageOptions() {
        boolean value = mCpuUsageCheckBox.isChecked();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SHOW_PROCESSES, value ? 1 : 0);
        Intent intent = new Intent(mContext, LoadAverageService.class);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        if (value) {
            mContext.startService(intent);
        } else {
            mContext.stopService(intent);
        }
    }

    private void updateUsbDebuggingOptions() {
        final boolean checked = (Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0) != 0);
        mUsbDebuggingCheckBox.setChecked(checked);
    }

    private void writeUsbDebuggingOptions() {
        boolean value = mUsbDebuggingCheckBox.isChecked();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, value ? 1 : 0);
    }

    private void updateDropCacheOptions() {
        final boolean enable = SystemProperties.getBoolean(
                "persist.vendor.humax.dropcache.enable", false);
        mDropCacheCheckBox.setChecked(enable);
    }

    private void writeDropCacheOptions() {
        boolean enable = mDropCacheCheckBox.isChecked();
        SystemProperties.set(
                "persist.vendor.humax.dropcache.enable", (enable) ? "true" : "false");
    }

    private void updateUsbHubOptions() {
        final boolean support = SystemProperties.getBoolean(
                "persist.vendor.humax.usbhub.support", false);
        mUsbHubCheckBox.setChecked(support);
    }

    private void writeUsbHubOptions() {
        boolean support = mUsbHubCheckBox.isChecked();
        SystemProperties.set(
                "persist.vendor.humax.usbhub.support", (support) ? "true" : "false");
    }
}
