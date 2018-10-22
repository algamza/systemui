/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.humaxdigital.automotive.statusbar;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.ComponentName;
import android.os.Handler;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.widget.ImageButton;
import android.os.RemoteException;

import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;
import com.android.systemui.plugins.OverlayPlugin;
import com.android.systemui.plugins.annotations.Requires;

@Requires(target = OverlayPlugin.class, version = OverlayPlugin.VERSION)
public class StatusBarOverlayPlugin implements OverlayPlugin {
    private static final String TAG = "StatusBarOverlayPlugin";
    private Context mPluginContext;

    private View mStatusBarView;
    private View mNavBarView;
    private boolean mInputSetup;
    private boolean mCollapseDesired;
    private float mStatusBarHeight;
    private View mNavBarViewGroup;

    private ControllerManager mControllerManager; 

    IStatusBarService mStatusBarService;

    @Override
    public void onCreate(Context sysuiContext, Context pluginContext) {
        mPluginContext = pluginContext;
        startStatusBarService(mPluginContext); 
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mStatusBarView != null) {
            mStatusBarView.post(
                    () -> ((ViewGroup) mStatusBarView.getParent()).removeView(mStatusBarView));
        }
        if (mNavBarView != null) {
            mNavBarView.post(() -> ((ViewGroup) mNavBarView.getParent()).removeView(mNavBarView));
        }

        if ( mPluginContext != null ) mPluginContext.unbindService(mServiceConnection);
    }

    @Override
    public void setup(View statusBar, View navBar) {
        Log.d(TAG, "Setup");

        int id = mPluginContext.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        mStatusBarHeight = mPluginContext.getResources().getDimension(id);
        if (statusBar instanceof ViewGroup) {
            /*
            mStatusBarView = LayoutInflater.from(mPluginContext)
                    .inflate(R.layout.statusbar_overlay, (ViewGroup) statusBar, false);
            ((ViewGroup) statusBar).addView(mStatusBarView);
            */
        }

        if ( navBar instanceof ViewGroup ) {
            mNavBarViewGroup = ((ViewGroup)navBar).getChildAt(0);
            mNavBarView = LayoutInflater.from(mPluginContext).inflate(R.layout.navi_overlay, (ViewGroup)mNavBarViewGroup, false);
            if ( mNavBarView != null ) {
                mControllerManager = new ControllerManager(mPluginContext, mNavBarView); 

                // todo : test code : The listener should be cleared.
                mNavBarView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if ( mNavBarViewGroup != null && mNavBarView != null )
                            ((ViewGroup)mNavBarViewGroup).removeView(mNavBarView); 
                        return true;
                    }
                });
                /*
                boolean is_removed_all = false;
                while( !is_removed_all )
                {
                    View child = ((ViewGroup)navBarView).getChildAt(0); 
                    if ( child != null ) ((ViewGroup)navBarView).removeView(child); 
                    else is_removed_all = true;
                }
                */
                ((ViewGroup)mNavBarViewGroup).addView(mNavBarView);
            }
        }
    }

    @Override
    public void setCollapseDesired(boolean collapseDesired) {
        mCollapseDesired = collapseDesired;
    }

    @Override
    public boolean holdStatusBarOpen() {
        /*
        if (!mInputSetup) {
            mInputSetup = true;
            mStatusBarView.getViewTreeObserver().addOnComputeInternalInsetsListener(
                    onComputeInternalInsetsListener);
        }
        */
        return false;
    }

    private void startStatusBarService(Context context){
        if ( context == null ) return; 
        Intent intent = new Intent().setAction("com.humaxdigital.automotive.statusbar.StatusBarService");
        intent.setPackage("com.humaxdigital.automotive.statusbar"); 
        context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE); 
    }

    final OnComputeInternalInsetsListener onComputeInternalInsetsListener = inoutInfo -> {
        inoutInfo.setTouchableInsets(InternalInsetsInfo.TOUCHABLE_INSETS_REGION);
        if (mCollapseDesired) {
            inoutInfo.touchableRegion.set(new Rect(0, 0, 50000, (int) mStatusBarHeight));
        } else {
            inoutInfo.touchableRegion.set(new Rect(0, 0, 50000, 50000)); 
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            
            if ( service == null ) return;

            mStatusBarService = IStatusBarService.Stub.asInterface(service); 
            
            try {
                mStatusBarService.registerStatusBarCallback(mBinderStatusBarCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if ( mControllerManager != null ) mControllerManager.deinit();
            
            if ( mStatusBarService != null ) {
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
            
            if ( mControllerManager != null ) mControllerManager.init(mStatusBarService); 
        }

        @Override
        public void onUpdated() throws RemoteException {
            if ( mControllerManager != null ) mControllerManager.update(); 
        }
    };
}
