// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.statusbar.dev;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DevModeController {
    private static final String TAG = DevModeController.class.getSimpleName();

    private static final long ALIVE_TIME = 1500;
    private static final long MAX_DISTANCE_PIXELS = 120;

    private Context mContext;
    private View mNormalView;
    private View mDevView;
    private OnViewChangeListener mOnViewChangeListener;
    private Handler mHandler = new Handler();

    private int mCurrentStep = 0;
    private int mTouchAnchorX = 0;
    private int mTouchAnchorY = 0;
    private long mLastEventTime = 0;

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            initViews();
        }
    };

    public DevModeController(Context context, View normalView, View devView) {
        mContext = context;

        mNormalView = normalView;
        mDevView = devView;

        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
                false, mSettingsObserver);

        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(DevUtils.KEEP_UNLOCKED),
                false, mSettingsObserver);

        initViews();
    }

    public void setOnViewChangeListener(OnViewChangeListener l) {
        mOnViewChangeListener = l;
    }

    private void initViews() {
        if (mNormalView != null) {
            boolean devEnabled = DevUtils.isDevelopmentSettingsEnabled(mContext);
            if (devEnabled) {
                mNormalView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (processTouchEvent(v, event) && mDevView != null) {
                            dispatchOnViewChange(mDevView);
                        }
                        return false;
                    }
                });
            } else {
                mNormalView.setOnTouchListener(null);
            }

            boolean allowLongClick = true;
            allowLongClick &= devEnabled;
            allowLongClick &= DevUtils.isKeepingDevUnlocked(mContext);

            if (allowLongClick) {
                mNormalView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (DevUtils.isDevelopmentSettingsEnabled(mContext)) {
                            dispatchOnViewChange(mDevView);
                        }
                        return false;
                    }
                });
            } else {
                mNormalView.setOnLongClickListener(null);
            }
        }

        if (mDevView != null) {
            mDevView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    dispatchOnViewChange(mNormalView);
                    return false;
                }
            });
        }
    }

    private boolean processTouchEvent(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (stepPattern((int)event.getX(), (int)event.getY(), v.getWidth())) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return false;
    }

    private boolean stepPattern(int x, int y, int width) {
        Log.d(TAG, "stepPattern:x="+x+", y="+y+", width="+width); 
        long eventTime = SystemClock.uptimeMillis();
        if (Math.abs(eventTime - mLastEventTime) > ALIVE_TIME) {
            mCurrentStep = 0;
        }

        mLastEventTime = eventTime;

        final int range = mCurrentStep / 3;
        int areaLeft, areaRight;

        if ((mCurrentStep % 3) == 0) {
            mTouchAnchorX = x;
            mTouchAnchorY = y;
        }

        if ((range % 2) == 0) { // even
            areaLeft = 0;
            areaRight = width / 2;
        } else { // odd
            areaLeft = width / 2;
            areaRight = width;
        }

        if ((x < areaLeft || x >= areaRight) ||
                (Math.abs(mTouchAnchorX - x) > MAX_DISTANCE_PIXELS) ||
                (Math.abs(mTouchAnchorY - y) > MAX_DISTANCE_PIXELS)) {
            mCurrentStep = 0;
            return false;
        }

        if (mCurrentStep++ >= 8) {
            mCurrentStep = 0;
            return true;
        }

        return false;
    }

    private void dispatchOnViewChange(View v) {
        if (v != null && mOnViewChangeListener != null) {
            mHandler.post(() -> mOnViewChangeListener.onViewChange(v));
        }
    }

    public interface OnViewChangeListener {
        /**
         * Called when the controller recognized specific pattern and changed
         * current active view.
         *
         * @param v The view the controller selected the current view as.
         * @return True if the listener has consumed the event, false otherwise.
         */
        boolean onViewChange(View v);
    }
}
