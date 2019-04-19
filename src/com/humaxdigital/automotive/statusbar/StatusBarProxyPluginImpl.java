// Copyright (c) 2018 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.statusbar;

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
import android.os.RemoteException;
import android.os.UserManager;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.Gravity;

import com.humaxdigital.automotive.statusbar.controllers.ControllerManager;
import com.humaxdigital.automotive.statusbar.dev.DevCommandsProxy;
import com.humaxdigital.automotive.statusbar.dev.DevNavigationBar;
import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.IStatusBarDev;
import com.humaxdigital.automotive.statusbar.service.StatusBarService;

import com.humaxdigital.automotive.statusbar.droplist.DropListUIService;
import com.humaxdigital.automotive.statusbar.volumedialog.VolumeDialogService; 


import static android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;

public class StatusBarProxyPluginImpl extends Service {
    private static final String TAG = "StatusBarProxyPluginImpl";
    private final IBinder mBinder = new LocalBinder();
    private WindowManager mWindowManager;
    private ViewGroup mNavBarWindow;
    private View mStatusBarWindow; 
    private View mNavBarView;
    private View mDevNavView;
    private ControllerManager mControllerManager; 
    private IStatusBarService mStatusBarService;
    private boolean mCollapseDesired = false;
    private int mStatusBarHeight = 0; 
    private int mTouchDownY = 0; 
    private int mTouchUpY = 0; 
    private int mTouchDownValue = 0;
    private Boolean mTouchValid = false; 
    private final int TOUCH_OFFSET = 16; 
    private final String OPEN_DROPLIST = "com.humaxdigital.automotive.statusbar.droplist.action.OPEN_DROPLIST"; 

    public class LocalBinder extends Binder {
        StatusBarProxyPluginImpl getService() {
            return StatusBarProxyPluginImpl.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        startStatusBarService(this);
        startDropListService(this);
        startVolumeDialogService(this);
        
        mStatusBarWindow = (View)View.inflate(this, R.layout.status_bar_overlay, null);
        mStatusBarWindow.setOnTouchListener(mStatusBarTouchListener);
        String package_name = getPackageName(); 
        int id_statusbar_height = getResources().getIdentifier("statusbar_height", "integer",  package_name);
        int id_down_y = getResources().getIdentifier("statusbar_touch_down_y", "integer",  package_name);
        int id_up_y = getResources().getIdentifier("statusbar_touch_up_y", "integer",  package_name);
        if ( id_down_y > 0 ) mTouchDownY = getResources().getInteger(id_down_y); 
        if ( id_up_y > 0 ) mTouchUpY = getResources().getInteger(id_up_y);
        if ( id_statusbar_height > 0 ) mStatusBarHeight = getResources().getInteger(id_statusbar_height);

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

        mNavBarWindow = (ViewGroup) View.inflate(this, R.layout.nav_bar_window, null);
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

        mControllerManager = new ControllerManager(this, mNavBarView); 

        setContentBarView(mNavBarView);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mNavBarWindow != null) {
            mNavBarWindow.removeAllViews();
            mWindowManager.removeViewImmediate(mNavBarWindow);
            mNavBarWindow = null;
        }

        if (mStatusBarWindow != null) {
            ((ViewGroup)mStatusBarWindow).removeAllViews();
            mWindowManager.removeViewImmediate(mStatusBarWindow);
            mStatusBarWindow = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // keep it alive.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public View inflateNavBarView() {
        final View view = View.inflate(this, R.layout.navi_overlay, null);
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
        final DevNavigationBar devNavBarView = (DevNavigationBar)
                View.inflate(this, R.layout.dev_nav_bar, null);
        devNavBarView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setContentBarView(mNavBarView);
                return true;
            }
        });

        devNavBarView.init(new DevCommandsProxy(this) {
            @Override
            public Bundle invokeDevCommand(String command, Bundle args) {
                if (mStatusBarService != null) {
                    try {
                        IStatusBarDev dev = mStatusBarService.getStatusBarDev(); 
                        if ( dev != null ) return dev.invokeDevCommand(command, args);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                return new Bundle();
            }
        });

        return devNavBarView;
    }

    public void setContentBarView(View view) {
        mNavBarWindow.removeAllViews();
        mNavBarWindow.addView(view);
    }

    private void startStatusBarService(Context context){
        if ( context == null ) return; 
        Intent intent = new Intent(context, StatusBarService.class);
        context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE); 
    }

    private void startDropListService(Context context){
        if ( context == null ) return; 
        Intent intent = new Intent(context, DropListUIService.class);
        context.startService(intent);
    }

    private void startVolumeDialogService(Context context){
        if ( context == null ) return; 
        Intent intent = new Intent(context, VolumeDialogService.class);
        context.startService(intent);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service == null) {
                return;
            }

            mStatusBarService = IStatusBarService.Stub.asInterface(service); 
            mControllerManager.init(mStatusBarService); 
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            updateUIController(() -> {
                mControllerManager.deinit();
            });

            mStatusBarService = null;
        }
    };

    private final View.OnTouchListener mStatusBarTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int y = (int)event.getY(); 
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    //Log.d(TAG, "ACTION_DOWN:mTouchDownY="+mTouchDownY+", y="+y+", mTouchValid="+mTouchValid); 
                    if ( mTouchDownY > y ) {
                        mTouchValid = true; 
                        mTouchDownValue = y; 
                    }
                    break; 
                }
                case MotionEvent.ACTION_UP: {
                    //Log.d(TAG, "ACTION_UP:mTouchDownY="+mTouchDownY+", y="+y+", mTouchValid="+mTouchValid); 
                    if ( mTouchValid ) {
                        mTouchValid = false; 
                        if ( (y - mTouchDownValue) > TOUCH_OFFSET ) {
                            openDroplist();
                        }
                    }
                    break; 
                }
                case MotionEvent.ACTION_MOVE: {
                    //Log.d(TAG, "ACTION_MOVE:mTouchDownY="+mTouchDownY+", y="+y+", mTouchValid="+mTouchValid); 
                    if ( mTouchValid ) {
                        if ( (y - mTouchDownValue) > TOUCH_OFFSET ) {
                            mTouchValid = false; 
                            openDroplist();
                        }
                    }
                    break;
                }
                default: break; 
            }
            return false;
        }
    }; 

    private void updateUIController(Runnable r) {
        if (mControllerManager == null) return;
        Handler handler = new Handler(getMainLooper()); 
        handler.post(r); 
    }

    private void openDroplist() {
        Log.d(TAG, "openDroplist"); 
        Intent intent = new Intent(OPEN_DROPLIST); 
        sendBroadcast(intent);
    }
}
