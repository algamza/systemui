// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.common.util;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;

import java.util.Collection;
import java.util.List;

public class ActivityMonitor {
    private static final String TAG = ActivityMonitor.class.getSimpleName();

    private Context mContext;
    private ActivityManager mActivityManager;
    private IActivityManager mActivityManagerService;
    private ProcessObserver mProcessObserver;
    private TaskListener mTaskListener;
    private ActivityMonitorHandler mHandler;
    private ActivityChangeListener mActivityChangeListener;
    private final ArraySet<ActivityChangeListener> mListeners = new ArraySet<>();
    private ComponentName mTopActivity;

    public interface ActivityChangeListener {
        /**
         * Notify change of activity.
         * @param topActivity Component name for what is currently on top.
         */
        void onActivityChanged(ComponentName topActivity);
    }

    public ActivityMonitor(Context context) {
        if ( context == null ) return;
        mContext = context;
        mProcessObserver = new ProcessObserver();
        mTaskListener = new TaskListener();
        mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        mActivityManagerService = ActivityManager.getService();
    }

    public ActivityMonitor init() {
        if ( mContext == null ) return this;
        mHandler = new ActivityMonitorHandler(mContext.getMainLooper());
        try {
            mActivityManagerService.registerProcessObserver(mProcessObserver);
            mActivityManagerService.registerTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        updateTopActivity();
        return this;
    }

    public ActivityMonitor release() {
        try {
            mActivityManagerService.unregisterProcessObserver(mProcessObserver);
            mActivityManagerService.unregisterTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return this;
    }

    public ComponentName getTopActivity() {
        ComponentName top;
        synchronized (this) {
            top = mTopActivity;
        }
        return top;
    }

    public ActivityMonitor registerListener(ActivityChangeListener listener) {
        if ( listener == null ) return this;
        synchronized (this) {
            mListeners.add(listener);
        }
        return this;
    }

    public ActivityMonitor unregisterListener(ActivityChangeListener listener) {
        if ( listener == null ) return this;
        synchronized (this) {
            mListeners.remove(listener);
        }
        return this;
    }

    private void updateTopActivity() {
        Collection<ActivityChangeListener> listeners;
        ComponentName oldTopActivity;
        synchronized (this) {
            listeners = new ArraySet<>(mListeners);
            oldTopActivity = mTopActivity;
            List<ActivityManager.RunningTaskInfo> runningTaskInfoList =
                    mActivityManager.getRunningTasks(1);
            if (runningTaskInfoList.size() > 0) {
                mTopActivity = runningTaskInfoList.get(0).topActivity;
            }
        }
        if (!mTopActivity.equals(oldTopActivity)) {
            for (ActivityChangeListener l : listeners) {
                l.onActivityChanged(mTopActivity);
            }
        }
    }

    private class ProcessObserver extends IProcessObserver.Stub {
        @Override
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            mHandler.requestUpdatingTask();
        }

        @Override
        public void onProcessDied(int pid, int uid) {
            mHandler.requestUpdatingTask();
        }
    }

    private class TaskListener extends TaskStackListener {
        @Override
        public void onTaskStackChanged() {
            mHandler.requestUpdatingTask();
        }
    }

    private class ActivityMonitorHandler extends Handler {
        private static final int MSG_UPDATE_TASKS = 0;

        private ActivityMonitorHandler(Looper looper) {
            super(looper);
        }

        private void requestUpdatingTask() {
            Message msg = obtainMessage(MSG_UPDATE_TASKS);
            sendMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_TASKS:
                    updateTopActivity();
                    break;
            }
        }
    }
}
