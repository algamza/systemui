// Copyright (c) 2018 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.statusbar;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.ServiceConnection;
import android.content.BroadcastReceiver;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserManager;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.Gravity;
import android.widget.ImageView;

import com.humaxdigital.automotive.systemui.SystemUIBase;
import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.statusbar.controllers.ControllerManager;
import com.humaxdigital.automotive.systemui.statusbar.controllers.ControllerManagerBase;
import com.humaxdigital.automotive.systemui.statusbar.controllers.dl3c.ControllerManagerDL3C;
import com.humaxdigital.automotive.systemui.statusbar.dev.DevCommandsProxy;
import com.humaxdigital.automotive.systemui.statusbar.dev.DevModeController;
import com.humaxdigital.automotive.systemui.statusbar.dev.DevNavigationBar;
import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarService;
import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarDev;
import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarService;
import com.humaxdigital.automotive.systemui.util.OSDPopup; 

import com.humaxdigital.automotive.systemui.util.ProductConfig;

import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

public class StatusBar implements SystemUIBase {
    private static final String TAG = "StatusBar";
    private WindowManager mWindowManager;
    private Context mContext = null; 
    private ViewGroup mNavBarWindow;
    private ViewGroup mStatusBarWindow;
    private View mDropListTouchWindow; 
    private View mNavBarView;
    private View mStatusBarView; 
    private DevNavigationBar mDevNavView;
    private DevModeController mDevModeController;
    private ControllerManagerBase mControllerManager; 
    private StatusBarService mStatusBarService;
    private boolean mUseSystemGestures;
    private boolean mCollapseDesired = false;
    private int mDropListTouchHeight = 0; 
    private int mDropListTouchWidth = 0; 
    private int mStatusBarHeight = 0; 
    private int mTouchDownY = 0; 
    private int mTouchDownValue = 0;
    private Boolean mTouchValid = false; 
    private int mTouchOffset = 15; 
    private final String OPEN_DROPLIST = "com.humaxdigital.automotive.systemui.droplist.action.OPEN_DROPLIST"; 

    @Override
    public void onCreate(Context context) {
        Log.d(TAG, "onCreate");
        mContext = context; 
        if ( mContext == null ) return;
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mUseSystemGestures = mContext.getResources().getBoolean(R.bool.config_useSystemGestures);
        startStatusBarService(mContext);
        /*
        if ( ProductConfig.getModel() == ProductConfig.MODEL.DL3C ) {
            createStatusBarWindow();
            createDL3CDropListTouchWindow();
        } else { 
        */
            createNaviBarWindow();
            createDropListTouchWindow();
        //}

        if (mUseSystemGestures) {
            registerSystemGestureReceiver();
        }

        // Defers creating developers view not to interfere loading of statusbar
        new Handler().postDelayed(() -> {
            createDevelopersWindow();
        }, 1000);
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mUseSystemGestures) {
            unregisterSystemGestureReceiver();
        }

        if (mNavBarWindow != null) {
            mNavBarWindow.removeAllViews();
            mWindowManager.removeViewImmediate(mNavBarWindow);
            mNavBarWindow = null;
        }

        if (mDropListTouchWindow != null) {
            ((ViewGroup)mDropListTouchWindow).removeAllViews();
            mWindowManager.removeViewImmediate(mDropListTouchWindow);
            mDropListTouchWindow = null;
        }

        if (mStatusBarWindow != null) {
            mStatusBarWindow.removeAllViews();
            mWindowManager.removeViewImmediate(mStatusBarWindow);
            mStatusBarWindow = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if ( mContext == null ) return;
        mContext.getResources().updateConfiguration(newConfig, null);
        if ( mControllerManager != null ) mControllerManager.configurationChange(newConfig);
    }

    private void createStatusBarWindow() {
        if ( mWindowManager == null || mContext == null ) return;
        mStatusBarWindow = (ViewGroup) View.inflate(mContext, R.layout.status_bar_window, null);
        mStatusBarWindow.setOnTouchListener(mStatusBarTouchListener);
        String package_name = mContext.getPackageName(); 
        int id_down_y = mContext.getResources().getIdentifier("statusbar_touch_down_y_dl3c", "integer",  package_name);
        int id_touch_offset= mContext.getResources().getIdentifier("statusbar_touch_offset_dl3c", "integer",  package_name);
        if ( id_down_y > 0 ) mTouchDownY = mContext.getResources().getInteger(id_down_y); 
        if ( id_touch_offset > 0 ) mTouchOffset = mContext.getResources().getInteger(id_touch_offset);

        int height = mContext.getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
        Log.d(TAG, "height="+height); 
        int id_statusbar_height = mContext.getResources().getIdentifier("statusbar_height", "integer",  mContext.getPackageName());
        if ( id_statusbar_height > 0 ) mStatusBarHeight = mContext.getResources().getInteger(id_statusbar_height);
        if ( mStatusBarWindow == null ) return;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, mStatusBarHeight,
                WindowManager.LayoutParams.TYPE_STATUS_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.setTitle("HmxStatusBar");
        lp.windowAnimations = 0; 
        lp.gravity = Gravity.TOP;

        mWindowManager.addView(mStatusBarWindow, lp);

        mStatusBarView = inflateStatusBarView();
        setContentBarView(mStatusBarView);
    }

    private void createDL3CDropListTouchWindow() {
        if ( mWindowManager == null || mContext == null ) return;

        if (mUseSystemGestures) {
            // Don't need the fake invisible top window to trigger the droplist
            // as long as using system gestures.
            return;
        }

        mDropListTouchWindow = (View)View.inflate(mContext, R.layout.status_bar_overlay, null);
        
        mDropListTouchWindow.setOnTouchListener(mStatusBarTouchListener);
        String package_name = mContext.getPackageName(); 
        int id_droplist_touch_height = mContext.getResources().getIdentifier("droplist_touch_height_dl3c", "integer",  package_name);
        int id_droplist_touch_width = mContext.getResources().getIdentifier("droplist_touch_width_dl3c", "integer",  package_name);
        int id_down_y = mContext.getResources().getIdentifier("statusbar_touch_down_y", "integer",  package_name);
        int id_touch_offset = mContext.getResources().getIdentifier("statusbar_touch_offset", "integer",  package_name);
        if ( id_down_y > 0 ) mTouchDownY = mContext.getResources().getInteger(id_down_y); 
        if ( id_droplist_touch_height > 0 ) mDropListTouchHeight = mContext.getResources().getInteger(id_droplist_touch_height);
        if ( id_droplist_touch_width > 0 ) mDropListTouchWidth = mContext.getResources().getInteger(id_droplist_touch_width);
        if ( id_touch_offset > 0 ) mTouchOffset = mContext.getResources().getInteger(id_touch_offset);

        WindowManager.LayoutParams slp = new WindowManager.LayoutParams(
            mDropListTouchWidth,
            mDropListTouchHeight,
            WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT);
        slp.token = new Binder();
        slp.gravity = Gravity.TOP|Gravity.RIGHT;
        slp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        slp.setTitle("HmxSystemUI");
        slp.packageName = package_name;
        slp.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

        mWindowManager.addView(mDropListTouchWindow, slp);
    }

    private void createDropListTouchWindow() {
        if ( mWindowManager == null || mContext == null ) return;

        if (mUseSystemGestures) {
            // Don't need the fake invisible top window to trigger the droplist
            // as long as using system gestures.
            return;
        }

        mDropListTouchWindow = (View)View.inflate(mContext, R.layout.status_bar_overlay, null);
        
        mDropListTouchWindow.setOnTouchListener(mStatusBarTouchListener);
        String package_name = mContext.getPackageName(); 
        int id_droplist_touch_height = mContext.getResources().getIdentifier("droplist_touch_height", "integer",  package_name);
        int id_droplist_touch_width = mContext.getResources().getIdentifier("droplist_touch_width", "integer",  package_name);
        int id_down_y = mContext.getResources().getIdentifier("statusbar_touch_down_y", "integer",  package_name);
        int id_touch_offset = mContext.getResources().getIdentifier("statusbar_touch_offset", "integer",  package_name);
        if ( id_down_y > 0 ) mTouchDownY = mContext.getResources().getInteger(id_down_y); 
        if ( id_droplist_touch_height > 0 ) mDropListTouchHeight = mContext.getResources().getInteger(id_droplist_touch_height);
        if ( id_droplist_touch_width > 0 ) mDropListTouchWidth = mContext.getResources().getInteger(id_droplist_touch_width);
        if ( id_touch_offset > 0 ) mTouchOffset = mContext.getResources().getInteger(id_touch_offset);

        WindowManager.LayoutParams slp = new WindowManager.LayoutParams(
            mDropListTouchWidth,
            mDropListTouchHeight,
            WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT);
        slp.token = new Binder();
        slp.gravity = Gravity.TOP|Gravity.LEFT;
        slp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        slp.setTitle("HmxSystemUI");
        slp.packageName = package_name;
        slp.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

        mWindowManager.addView(mDropListTouchWindow, slp);
    }

    private void createNaviBarWindow() {
        if ( mWindowManager == null || mContext == null ) return;
        mNavBarWindow = (ViewGroup) View.inflate(mContext, R.layout.nav_bar_window, null);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.setTitle("HmxNavigationBar");
        lp.windowAnimations = 0; 

        mWindowManager.addView(mNavBarWindow, lp);

        mNavBarView = inflateNavBarView();
        setContentBarView(mNavBarView);
    }

    private void createDevelopersWindow() {
        mDevNavView = (DevNavigationBar) View.inflate(mContext, R.layout.dev_nav_bar, null);
        mDevNavView.init(new DevCommandsProxy(mContext) {
            @Override
            public Bundle invokeDevCommand(String command, Bundle args) {
                if (mStatusBarService != null) {
                    StatusBarDev dev = mStatusBarService.getStatusBarDev();
                    if ( dev != null ) return dev.invokeDevCommand(command, args);
                }
                return new Bundle();
            }
        });

        View normalView;
        //if (ProductConfig.getModel() == ProductConfig.MODEL.DL3C) {
        //    normalView = mStatusBarView;
        //} else {
            normalView = mNavBarView;
        //}

        mDevModeController = new DevModeController(mContext, normalView, mDevNavView);
        mDevModeController.setOnViewChangeListener(new DevModeController.OnViewChangeListener() {
            @Override
            public boolean onViewChange(View v) {
                setContentBarView(v);
                return true;
            }
        });
    }

    public View inflateNavBarView() {
        if ( mContext == null ) return null;
        View view = null; 
        if ( ProductConfig.getModel() == ProductConfig.MODEL.DU2 ) 
            view = View.inflate(mContext, R.layout.du2_navi_overlay, null);
        else if ( ProductConfig.getModel() == ProductConfig.MODEL.DN8C ) 
            view = View.inflate(mContext, R.layout.dn8c_navi_overlay, null);
        else if ( ProductConfig.getModel() == ProductConfig.MODEL.CN7C ) 
            view = View.inflate(mContext, R.layout.du2_navi_overlay, null);
        else 
            view = View.inflate(mContext, R.layout.dn8c_navi_overlay, null);
        return view;
    }

    public View inflateStatusBarView() {
        if ( mContext == null ) return null; 
        View view = View.inflate(mContext, R.layout.dl3c_statusbar, null);
        return view;
    }

    public View inflateDevNavBarView() {
        if ( mContext == null ) return null;
        final DevNavigationBar devNavBarView = (DevNavigationBar)
                View.inflate(mContext, R.layout.dev_nav_bar, null);

        devNavBarView.init(new DevCommandsProxy(mContext) {
            @Override
            public Bundle invokeDevCommand(String command, Bundle args) {
                if (mStatusBarService != null) {
                    StatusBarDev dev = mStatusBarService.getStatusBarDev(); 
                    if ( dev != null ) return dev.invokeDevCommand(command, args);
                }
                return new Bundle();
            }
        });

        return devNavBarView;
    }

    public void setContentBarView(View view) {
        //if ( ProductConfig.getModel() == ProductConfig.MODEL.DL3C ) {
        //    if ( mStatusBarWindow == null ) return;
        //    mStatusBarWindow.removeAllViews();
        //    mStatusBarWindow.addView(view); 
        //} else {
            if ( mNavBarWindow == null ) return;
            mNavBarWindow.removeAllViews();
            mNavBarWindow.addView(view);
        //}
    }

    private void startStatusBarService(Context context){
        if ( context == null ) return; 
        Intent intent = new Intent(context, StatusBarService.class);
        context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE); 
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service == null) {
                return;
            }

            mStatusBarService = ((StatusBarService.StatusBarServiceBinder)service).getService();

            //if (ProductConfig.getModel() == ProductConfig.MODEL.DL3C) {
            //    mControllerManager = new ControllerManagerDL3C();
            //    mControllerManager.create(mContext, mStatusBarView);
            //} else {
                mControllerManager = new ControllerManager();
                mControllerManager.create(mContext, mNavBarView);
            //}
            mControllerManager.init(mStatusBarService); 
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            updateUIController(() -> {
                mControllerManager.deinit();
                mControllerManager = null;
            });

            mStatusBarService = null;
        }
    };

    public static final String ACTION_SYSTEM_GESTURE = "android.intent.action.SYSTEM_GESTURE";
    public static final String EXTRA_GESTURE = "gesture";

    private BroadcastReceiver mSystemGestureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_SYSTEM_GESTURE)) {
                String gesture = intent.getStringExtra(EXTRA_GESTURE);
                if ("swipe-from-top".equals(gesture) && !isSpecialCase()) {
                    openDroplist();
                }
            }
        }
    };

    private void registerSystemGestureReceiver() {
        if ( mContext == null ) return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SYSTEM_GESTURE);
        mContext.registerReceiver(mSystemGestureReceiver, intentFilter);
    }

    private void unregisterSystemGestureReceiver() {
        if ( mContext != null ) 
            mContext.unregisterReceiver(mSystemGestureReceiver);
    }

    private final View.OnTouchListener mStatusBarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int y = (int)event.getY(); 
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    Log.d(TAG, "ACTION_DOWN:mTouchDownY="+mTouchDownY+", y="+y+", mTouchValid="+mTouchValid); 
                    if ( mTouchDownY > y ) {
                        mTouchValid = true; 
                        mTouchDownValue = y; 
                    }
                    break; 
                }
                case MotionEvent.ACTION_UP: {
                    Log.d(TAG, "ACTION_UP:mTouchDownY="+mTouchDownY+", y="+y+", mTouchValid="+mTouchValid); 
                    if ( mTouchValid ) {
                        mTouchValid = false; 
                        if ( (y - mTouchDownValue) > mTouchOffset ) {
                            if ( !isSpecialCase() ) openDroplist();
                        }
                    }
                    break; 
                }
                case MotionEvent.ACTION_MOVE: {
                    Log.d(TAG, "ACTION_MOVE:mTouchDownY="+mTouchDownY+", y="+y+", mTouchValid="+mTouchValid); 
                    if ( mTouchValid ) {
                        if ( (y - mTouchDownValue) > mTouchOffset ) {
                            mTouchValid = false; 
                            if ( !isSpecialCase() ) openDroplist();
                        }
                    }
                    break;
                }
                default: break; 
            }
            return false;
        }
    }; 

    private boolean isSpecialCase() {
        if ( mStatusBarService == null || mContext == null ) return false; 
        if ( mStatusBarService.isUserAgreement() ) {
            Log.d(TAG, "is special case : user agreement"); 
            return true;
        }
        if ( mStatusBarService.isUserSwitching() ) {
            Log.d(TAG, "is special case : user switching"); 
            return true;
        }
        if ( mStatusBarService.isFrontCamera() ) {
            Log.d(TAG, "is special case : front camera"); 
            OSDPopup.send(mContext, mContext.getResources().getString(R.string.STR_MESG_18334_ID));
            return true; 
        }
        if ( mStatusBarService.isRearCamera() ) {
            Log.d(TAG, "is special case : rear camera"); 
            OSDPopup.send(mContext, mContext.getResources().getString(R.string.STR_MESG_18334_ID));
            return true;
        }
        if ( mStatusBarService.isPowerOff() ) {
            Log.d(TAG, "is special case : power off"); 
            return true;
        }
        if ( mStatusBarService.isEmergencyCall() ) {
            Log.d(TAG, "is special case : emergency call"); 
            return true;
        }
        if ( mStatusBarService.isBluelinkCall() ) {
            Log.d(TAG, "is special case : bluelink call"); 
            return true;
        }
        if ( mStatusBarService.isSVIOn() ) {
            Log.d(TAG, "is special case : svi on"); 
            OSDPopup.send(mContext, mContext.getResources().getString(R.string.STR_FEATURE_CURRENTLY_UNAVAILABLE_ID));
            return true;
        }
        if ( mStatusBarService.isSVSOn() ) {
            Log.d(TAG, "is special case : svs on"); 
            OSDPopup.send(mContext, mContext.getResources().getString(R.string.STR_FEATURE_CURRENTLY_UNAVAILABLE_ID));
            return true;
        }
        return false; 
    }

    private void updateUIController(Runnable r) {
        if ( mControllerManager == null || mContext == null ) return;
        Handler handler = new Handler(mContext.getMainLooper()); 
        handler.post(r); 
    }

    private void openDroplist() {
        Log.d(TAG, "openDroplist"); 
        if ( mContext == null ) return;
        Intent intent = new Intent(OPEN_DROPLIST); 
        mContext.sendBroadcast(intent);
    }
}
