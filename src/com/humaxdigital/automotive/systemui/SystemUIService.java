// Copyright (c) 2018 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.humaxdigital.automotive.systemui.statusbar.StatusBar;
import com.humaxdigital.automotive.systemui.droplist.DropListUIService;
import com.humaxdigital.automotive.systemui.volumedialog.VolumeDialogService; 
import com.humaxdigital.automotive.systemui.wallpaper.WallpaperService; 

public class SystemUIService extends Service {
    private static final String TAG = "SystemUIService";
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        SystemUIPolicy.applyPolicies(this);

        startWallpaperService(this);
        startStatusBarService(this);
        startDropListService(this);
        startVolumeDialogService(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // keep it alive.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startStatusBarService(Context context){
        if ( context == null ) return; 
        Intent intent = new Intent(context, StatusBar.class);
        context.startService(intent);
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

    private void startWallpaperService(Context context){
        if ( context == null ) return; 
        Intent intent = new Intent(context, WallpaperService.class);
        context.startService(intent);
    }
}
