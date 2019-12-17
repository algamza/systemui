// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.statusbar.dev;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.extension.car.settings.CarExtraSettings;
import android.graphics.Color;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
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
import android.widget.Switch;
import android.widget.TextView;

import com.android.settingslib.development.SystemPropPoker;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.common.util.ActivityMonitor;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DevNavigationBar extends FrameLayout {
    private static final String TAG = DevNavigationBar.class.getSimpleName();

    private static final String ANR_DIR_PATH = "/data/anr";
    private static final String TOMBSTONES_DIR_PATH = "/data/tombstones";

    private Context mContext;
    private ActivityManager mActivityManager;
    private DevCommands mDevCommands;
    private ActivityMonitor mActivityMonitor;
    private ContentResolver mContentResolver;

    private final Handler mRetrieveHandler;
    private final Runnable mRetrieveErrorsRunnable = this::retrieveErrorCounts;
    private final Runnable mRetrieveThermalRunnable = this::retrieveThermalTemp;

    private ComponentName mTopActivity;
    private float mLastThermalTemp;

    private TextView mCurrentActivityTextView;
    private TextView mSavedActivityTextView;
    private TextView mSystemStateTextView;
    private TextView mErrorCountsTextView;
    private TextView mThermalTempTextView;

    private CheckBox mCpuUsageCheckBox;
    private CheckBox mUsbDebuggingCheckBox;
    private CheckBox mDropCacheCheckBox;
    private CheckBox mUsbHubCheckBox;
    private CheckBox mMapAutoCheckBox;

    private Switch mKeepUnlockedSwitch;
    private Switch mTrackTouchSwitch;
    private Switch mShowUpdatesSwitch;
    private Switch mDebugLayoutSwitch;

    private ActivityMonitor.ActivityChangeListener mActivityChangeListener =
            new ActivityMonitor.ActivityChangeListener() {
        @Override
        public void onActivityChanged(ComponentName topActivity) {
            mTopActivity = topActivity;
            updateTopActivity(topActivity);
        }
    };

    private ContentObserver mSystemStateSettingsObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri, int userId) {
            retrieveSystemStates();
        }
    };

    private FileObserver mAnrDirObserver =
            new FileObserver(ANR_DIR_PATH, FileObserver.CREATE|FileObserver.DELETE) {
        @Override
        public void onEvent(int event, String path) {
            mRetrieveHandler.post(mRetrieveErrorsRunnable);
        }
    };

    private FileObserver mTombstonesDirObserver =
            new FileObserver(TOMBSTONES_DIR_PATH, FileObserver.CREATE|FileObserver.DELETE) {
        @Override
        public void onEvent(int event, String path) {
            mRetrieveHandler.post(mRetrieveErrorsRunnable);
        }
    };

    public DevNavigationBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mContentResolver = context.getContentResolver();
        mActivityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mRetrieveHandler = new Handler(context.getMainLooper());

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
    }

    protected void onFinishInflate () {
        mCurrentActivityTextView = (TextView) findViewById(R.id.txtCurrentActivity);
        mSavedActivityTextView = (TextView) findViewById(R.id.txtSavedActivity);
        mSystemStateTextView = (TextView) findViewById(R.id.txtSystemState);
        mErrorCountsTextView = (TextView) findViewById(R.id.txtErrorCounts);
        mThermalTempTextView = (TextView) findViewById(R.id.txtThermalTemp);

        mCpuUsageCheckBox = (CheckBox) findViewById(R.id.chkCpuUsage);
        mCpuUsageCheckBox.setOnClickListener(view -> { writeCpuUsageOptions(); });

        mUsbDebuggingCheckBox = (CheckBox) findViewById(R.id.chkUsbDebugging);
        mUsbDebuggingCheckBox.setOnClickListener(view -> { writeUsbDebuggingOptions(); });

        mDropCacheCheckBox = (CheckBox) findViewById(R.id.chkDropCache);
        mDropCacheCheckBox.setOnClickListener(view -> { writeDropCacheOptions(); });

        mUsbHubCheckBox = (CheckBox) findViewById(R.id.chkUsbHub);
        mUsbHubCheckBox.setOnClickListener(view -> { writeUsbHubOptions(); });

        mMapAutoCheckBox = (CheckBox) findViewById(R.id.chkMapAuto);
        mMapAutoCheckBox.setOnClickListener(view -> { writeMapAutoOptions(); });

        mKeepUnlockedSwitch = (Switch) findViewById(R.id.swKeepUnlocked);
        mKeepUnlockedSwitch.setOnClickListener(view -> { writeKeepUnlockedOptions(); });

        mTrackTouchSwitch = (Switch) findViewById(R.id.swTrackTouch);
        mTrackTouchSwitch.setOnClickListener(view -> { writeTrackTouchOptions(); });

        mShowUpdatesSwitch = (Switch) findViewById(R.id.swShowUpdates);
        mShowUpdatesSwitch.setOnClickListener(view -> { writeShowUpdatesOption(); });

        mDebugLayoutSwitch = (Switch) findViewById(R.id.swDebugLayout);
        mDebugLayoutSwitch.setOnClickListener(view -> { writeDebugLayoutOptions(); });

        findViewById(R.id.btnGoHome).setOnClickListener(view -> { goHome(); });
        findViewById(R.id.btnGoBack).setOnClickListener(view -> { goBack(); });
        findViewById(R.id.btnAppList).setOnClickListener(view -> { runAppList(); });
        findViewById(R.id.btnSettings).setOnClickListener(view -> { runSettings(); });
        findViewById(R.id.btnUser0).setOnClickListener(view -> { DevUtils.setLocale(Locale.SIMPLIFIED_CHINESE); });
        findViewById(R.id.btnUser1).setOnClickListener(view -> { DevUtils.setLocale(Locale.US); });
        findViewById(R.id.btnUser2).setOnClickListener(view -> { DevUtils.setLocale(Locale.KOREA); });
        findViewById(R.id.btnStop).setOnClickListener(view -> { stopTopActivity(); });
        findViewById(R.id.btnSave).setOnClickListener(view -> { saveCurrentActivity(); });
        findViewById(R.id.btnLoad).setOnClickListener(view -> { loadSavedActivity(); });
    }

    public void init(DevCommands devCommands, ActivityMonitor activityMonitor) {
        mDevCommands = devCommands;
        mActivityMonitor = activityMonitor;

        updateCpuUsageOptions();
        updateUsbDebuggingOptions();
        writeCpuUsageOptions();
        updateDropCacheOptions();
        writeDropCacheOptions();
        updateUsbHubOptions();
        writeUsbHubOptions();
        updateMapAutoOptions();
        writeMapAutoOptions();
        updateKeepUnlockedOptions();
        updateTrackTouchOptions();
        updateShowUpdatesOption();
        updateDebugLayoutOptions();
    }

    public void onAttached() {
        mActivityMonitor.registerListener(mActivityChangeListener);

        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(CarExtraSettings.Global.POWER_STATE),
                false, mSystemStateSettingsObserver);
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(CarExtraSettings.Global.LAST_MEDIA_MODE),
                false, mSystemStateSettingsObserver);

        mAnrDirObserver.startWatching();
        mTombstonesDirObserver.startWatching();

        retrieveTopActivity();
        retrieveSystemStates();
        retrieveErrorCounts();
        retrieveThermalTemp();

        updateSavedActivityText();
    }

    public void onDetached() {
        mRetrieveHandler.removeCallbacksAndMessages(null);
        mAnrDirObserver.stopWatching();
        mTombstonesDirObserver.stopWatching();
        mContentResolver.unregisterContentObserver(mSystemStateSettingsObserver);
        mActivityMonitor.unregisterListener(mActivityChangeListener);
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
        final List<ActivityManager.RunningTaskInfo> runningTaskInfoList =
                mActivityManager.getRunningTasks(1);
        if (!runningTaskInfoList.isEmpty())
            mTopActivity = runningTaskInfoList.get(0).topActivity;
        else
            mTopActivity = null;

        if (mCurrentActivityTextView != null && mTopActivity != null) {
            updateTopActivity(mTopActivity);
        }
    }

    private void updateTopActivity(ComponentName topActivity) {
        mCurrentActivityTextView.setText(topActivity.flattenToShortString());
    }

    private void retrieveSystemStates() {
        int powerState = Settings.Global.getInt(
                mContentResolver, CarExtraSettings.Global.POWER_STATE, -1);
        int mediaMode = Settings.Global.getInt(
                mContentResolver, CarExtraSettings.Global.LAST_MEDIA_MODE, -1);

        String outText = String.format("S:%d,%d", powerState, mediaMode);
        if (!outText.equals(mSystemStateTextView.getText())) {
            mSystemStateTextView.setText(outText);
        }
    }

    private void retrieveErrorCounts() {
        try {
            int anrCnt = new File(ANR_DIR_PATH).list().length;
            int tbsCnt = new File(TOMBSTONES_DIR_PATH).list().length;

            String outText = String.format("E:%d,%d", anrCnt, tbsCnt);
            if (!outText.equals(mErrorCountsTextView.getText())) {
                mErrorCountsTextView.setText(outText);
            }
        } catch (Exception e) {
        }
    }

    private void retrieveThermalTemp() {
        final String cmdLine = "cat /sys/devices/virtual/thermal/thermal_zone0/temp";
        String outText = DevUtils.runShellScript(cmdLine).trim();
        float temp = 0.0f;
        try {
            int parsedInt = Integer.parseInt(outText);
            temp = parsedInt / 1000;
            temp /= 10.0f; // Making as a fake value
            if (Math.abs(temp - mLastThermalTemp) >= 0.1f) {
                mLastThermalTemp = temp;
                mThermalTempTextView.setText("T:" + String.format("%.1f", temp));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        // reschedule time to get thermal temperature
        mRetrieveHandler.postDelayed(mRetrieveThermalRunnable, 2000);
    }

    private void updateSavedActivityText() {
        if (mDevCommands != null) {
            String savedActivity = mDevCommands.getPreferenceString("dev_saved_actvity", "");
            if (mSavedActivityTextView != null) {
                mSavedActivityTextView.setText(savedActivity);
            }
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

    private void updateMapAutoOptions() {
        final boolean support = SystemProperties.getBoolean(
                "persist.vendor.humax.log.copy.mapauto", false);
        mMapAutoCheckBox.setChecked(support);
    }

    private void writeMapAutoOptions() {
        boolean support = mMapAutoCheckBox.isChecked();
        SystemProperties.set(
                "persist.vendor.humax.log.copy.mapauto", (support) ? "true" : "false");
    }

    private void updateKeepUnlockedOptions() {
        mKeepUnlockedSwitch.setChecked(DevUtils.isKeepingDevUnlocked(mContext));
    }

    private void writeKeepUnlockedOptions() {
        DevUtils.setKeepingDevUnlocked(mContext, mKeepUnlockedSwitch.isChecked());
    }

    private void updateTrackTouchOptions() {
        final boolean checked = (Settings.System.getInt(
                mContentResolver, Settings.System.POINTER_LOCATION, 0) != 0);
        mTrackTouchSwitch.setChecked(checked);
    }

    private void writeTrackTouchOptions() {
        Settings.System.putInt(mContentResolver,
                Settings.System.POINTER_LOCATION, mTrackTouchSwitch.isChecked() ? 1 : 0);
    }

    private void updateShowUpdatesOption() {
        // magic communication with surface flinger.
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(1010, data, reply, 0);
                @SuppressWarnings("unused")
                int showCpu = reply.readInt();
                @SuppressWarnings("unused")
                int enableGL = reply.readInt();
                int showUpdates = reply.readInt();
                @SuppressWarnings("unused")
                int showBackground = reply.readInt();
                int disableOverlays = reply.readInt();
                reply.recycle();
                data.recycle();

                mShowUpdatesSwitch.setChecked((showUpdates == 1));
            }
        } catch (RemoteException ex) {
            // ignore
        }
    }

    private void writeShowUpdatesOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                final int showUpdates = mShowUpdatesSwitch.isChecked() ? 1 : 0;
                data.writeInt(showUpdates);
                flinger.transact(1002, data, null, 0);
                data.recycle();

                updateShowUpdatesOption();
            }
        } catch (RemoteException ex) {
            // ignore
        }
    }

    private void updateDebugLayoutOptions() {
        mDebugLayoutSwitch.setChecked(
                SystemProperties.getBoolean(View.DEBUG_LAYOUT_PROPERTY, false));
    }

    private void writeDebugLayoutOptions() {
        SystemProperties.set(View.DEBUG_LAYOUT_PROPERTY,
                mDebugLayoutSwitch.isChecked() ? "true" : "false");
        SystemPropPoker.getInstance().poke();
    }
}
