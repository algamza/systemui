// Copyright (c) 2018 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.statusbar;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.Gravity;

import com.android.systemui.plugins.StatusBarProxyPlugin;
import com.android.systemui.plugins.annotations.Requires;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.IStatusBarCallback; 

import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

@Requires(target = StatusBarProxyPlugin.class, version = StatusBarProxyPlugin.VERSION)
public class StatusBarProxyPluginImpl implements StatusBarProxyPlugin {
    private static final String TAG = "StatusBarProxyPluginImpl";
    private Context mSysUiContext;
    private Context mPluginContext;
    private WindowManager mWindowManager;
    private ViewGroup mNavBarWindow;
    private View mStatusBarWindow; 
    private View mNavBarView;
    private View mDevNavView;
    private ControllerManager mControllerManager; 
    IStatusBarService mStatusBarService;
    private boolean mCollapseDesired = false;
    private int mStatusBarHeight = 0; 
    private int mTouchDownY = 0; 
    private int mTouchUpY = 0; 
    private Boolean mTouchValid = false; 
    private final String OPEN_DROPLIST = "com.humaxdigital.automotive.droplist.action.OPEN_DROPLIST"; 

    @Override
    public void onCreate(Context sysuiContext, Context pluginContext) {
        if ( pluginContext == null ) return; 
        mSysUiContext = sysuiContext;
        mPluginContext = pluginContext;
        mWindowManager = (WindowManager) mPluginContext.getSystemService(Context.WINDOW_SERVICE);

        startStatusBarService(mPluginContext); 
        
        mStatusBarWindow = (View)View.inflate(mPluginContext, R.layout.status_bar_overlay, null);
        mStatusBarWindow.setOnTouchListener(mStatusBarTouchListener);
        String package_name = mPluginContext.getPackageName(); 
        int id_statusbar_height = mPluginContext.getResources().getIdentifier("statusbar_height", "integer",  package_name);
        int id_down_y = mPluginContext.getResources().getIdentifier("statusbar_touch_down_y", "integer",  package_name);
        int id_up_y = mPluginContext.getResources().getIdentifier("statusbar_touch_up_y", "integer",  package_name);
        if ( id_down_y > 0 ) mTouchDownY = mPluginContext.getResources().getInteger(id_down_y); 
        if ( id_up_y > 0 ) mTouchUpY = mPluginContext.getResources().getInteger(id_up_y);
        if ( id_statusbar_height > 0 ) mStatusBarHeight = mPluginContext.getResources().getInteger(id_statusbar_height);

        WindowManager.LayoutParams slp = new WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            mStatusBarHeight,
            WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT);
        slp.token = new Binder();
        slp.gravity = Gravity.TOP;
        slp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        slp.setTitle("HmxStatusBar");
        slp.packageName = package_name;
        slp.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

        mWindowManager.addView(mStatusBarWindow, slp);

        mNavBarWindow = (ViewGroup) View.inflate(mPluginContext, R.layout.nav_bar_window, null);
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
        mDevNavView = inflateDevNavBarView();

        mControllerManager = new ControllerManager(mPluginContext, mNavBarView); 

        setContentBarView(mNavBarView);
    }

    @Override
    public void onDestroy() {
        if (mNavBarWindow != null) {
            mWindowManager.removeViewImmediate(mNavBarWindow);
            mNavBarWindow = null;
        }
    }

    public View inflateNavBarView() {
        final View view = View.inflate(mPluginContext, R.layout.navi_overlay, null);
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setContentBarView(mDevNavView);
                return true;
            }
        });
        return view;
    }

    public View inflateDevNavBarView() {
        final View devNavBarView = View.inflate(mPluginContext, R.layout.dev_nav_bar, null);
        devNavBarView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setContentBarView(mNavBarView);
                return true;
            }
        });

        return devNavBarView;
    }

    public void setContentBarView(View view) {
        mNavBarWindow.removeAllViews();
        mNavBarWindow.addView(view);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void setCollapseDesired(boolean collapseDesired) {
        mCollapseDesired = collapseDesired;
    }

    @Override
    public boolean holdStatusBarOpen() {
        return false;
    }

    /* @Override */
    public void setSystemUiVisibility(int vis, int fullscreenStackVis,
            int dockedStackVis, int mask, Rect fullscreenStackBounds,
            Rect dockedStackBounds) {
    }

    private void startStatusBarService(Context context){
        if ( context == null ) return; 
        Intent intent = new Intent().setAction("com.humaxdigital.automotive.statusbar.service.StatusBarService");
        intent.setPackage("com.humaxdigital.automotive.statusbar"); 
        context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE); 
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service == null) {
                return;
            }

            mStatusBarService = IStatusBarService.Stub.asInterface(service); 
            
            try {
                mStatusBarService.registerStatusBarCallback(mBinderStatusBarCallback);
                if (mStatusBarService.isInitialized()) {
                    updateUIController(() -> {
                        mControllerManager.init(mStatusBarService); 
                    });
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            updateUIController(() -> {
                mControllerManager.deinit();
            });

            if (mStatusBarService != null) {
                try {
                    mStatusBarService.unregisterStatusBarCallback(mBinderStatusBarCallback);
                    mStatusBarService = null; 
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final IStatusBarCallback.Stub mBinderStatusBarCallback = new IStatusBarCallback.Stub() {
        @Override
        public void onInitialized() throws RemoteException {
            updateUIController(() -> {
                mControllerManager.init(mStatusBarService); 
            });
        }
    };

    private final View.OnTouchListener mStatusBarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int y = (int)event.getY(); 
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if ( mTouchDownY > y ) mTouchValid = true; 
                    break; 
                }
                case MotionEvent.ACTION_UP: {
                    if ( mTouchValid ) {
                        mTouchValid = false; 
                        if ( mTouchUpY < y ) openDroplist(); 
                    }
                    break; 
                }
                default: break; 
            }
            return false;
        }
    }; 

    private void updateUIController(Runnable r) {
        if ( mPluginContext == null || mControllerManager == null ) return;
        Handler handler = new Handler(mPluginContext.getMainLooper()); 
        handler.post(r); 
    }

    private void openDroplist() {
        if ( mPluginContext == null ) return; 
        Log.d(TAG, "openDroplist"); 
        Intent intent = new Intent(OPEN_DROPLIST); 
        mPluginContext.sendBroadcast(intent);
    }
}
