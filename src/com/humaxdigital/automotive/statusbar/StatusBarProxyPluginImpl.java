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
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import com.android.systemui.plugins.StatusBarProxyPlugin;
import com.android.systemui.plugins.annotations.Requires;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.IStatusBarCallback; 

@Requires(target = StatusBarProxyPlugin.class, version = StatusBarProxyPlugin.VERSION)
public class StatusBarProxyPluginImpl implements StatusBarProxyPlugin {
    private static final String TAG = "StatusBarProxyPluginImpl";
    private Context mSysUiContext;
    private Context mPluginContext;
    private WindowManager mWindowManager;
    private View mNavBarView;
    private ControllerManager mControllerManager; 
    IStatusBarService mStatusBarService;
    private boolean mCollapseDesired = false;

    @Override
    public void onCreate(Context sysuiContext, Context pluginContext) {
        mSysUiContext = sysuiContext;
        mPluginContext = pluginContext;
        mWindowManager = (WindowManager) mPluginContext.getSystemService(Context.WINDOW_SERVICE);

        startStatusBarService(mPluginContext); 

        mNavBarView = (ViewGroup) View.inflate(mPluginContext, R.layout.navi_overlay, null);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.setTitle("HmxStatusBar");
        lp.windowAnimations = 0;

        mWindowManager.addView(mNavBarView, lp);

        mControllerManager = new ControllerManager(mPluginContext, mNavBarView); 
    }

    @Override
    public void onDestroy() {
        if (mNavBarView != null) {
            mWindowManager.removeViewImmediate(mNavBarView);
            mNavBarView = null;
        }
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

        @Override
        public void onUpdated() throws RemoteException {
            updateUIController(() -> {
                mControllerManager.update(); 
            }); 
        }
    };

    private void updateUIController(Runnable r) {
        if ( mPluginContext == null || mControllerManager == null ) return;
        Handler handler = new Handler(mPluginContext.getMainLooper()); 
        handler.post(r); 
    }
}
