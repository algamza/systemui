// Copyright (c) 2018 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

import com.humaxdigital.automotive.systemui.statusbar.StatusBar;
import com.humaxdigital.automotive.systemui.droplist.DropListUIService;
import com.humaxdigital.automotive.systemui.volumedialog.VolumeDialog; 
import com.humaxdigital.automotive.systemui.wallpaper.WallpaperService; 

import java.util.ArrayList;
import java.util.List;

public class SystemUIService extends Service {
    private static final String TAG = "SystemUIService";
    private final List<SystemUIBase> mSystemUIs = new ArrayList<>();
    
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        SystemUIPolicy.applyPolicies(this);

        //mSystemUIs.add(new WallpaperService()); 
        mSystemUIs.add(new StatusBar()); 
        mSystemUIs.add(new DropListUIService()); 
        //mSystemUIs.add(new VolumeDialog()); 

        for ( SystemUIBase ui : mSystemUIs ) ui.onCreate(this);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        for ( SystemUIBase ui : mSystemUIs ) ui.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        for ( SystemUIBase ui : mSystemUIs ) ui.onConfigurationChanged(newConfig);
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
}
