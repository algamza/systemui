package com.humaxdigital.automotive.systemui.volumedialog;

import android.app.Service;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;

import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.graphics.PixelFormat;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import com.humaxdigital.automotive.systemui.R; 

public class VolumeDialogService extends Service {
    private final String TAG = "VolumeDialogService"; 

    private VolumeDialogBase mDialog; 
    private VolumeControllerBase mController; 
    private VolumeControlService mVolumeService;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate"); 
        mDialog = new VolumeDialog();
        mDialog.init(this); 

        mController = new VolumeController(); 
        mController.init(this, mDialog.getView()); 
        mController.registVolumeListener(mVolumeListener);

        startVolumeControlService(); 
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy"); 
        if ( mController != null ) {
            mController.deinit(); 
            mController.unregistVolumeListener(mVolumeListener); 
            mController = null; 
        }
        if ( mDialog != null ) mDialog.deinit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind"); 
        return null;
    }

    private void startVolumeControlService() {
        Log.d(TAG, "startVolumeControlService");
        Intent bindIntent = new Intent(this, VolumeControlService.class);
        if ( !bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE) ) {
            Log.e(TAG, "Failed to connect to VolumeControlService");
        }
    }

    private VolumeControllerBase.VolumeChangeListener mVolumeListener = 
        new VolumeControllerBase.VolumeChangeListener() {
        @Override
        public void onVolumeUp(VolumeControllerBase.VolumeChangeListener.Type type, int max, int value) {
            if ( mDialog != null ) mDialog.open();
        }

        @Override
        public void onVolumeDown(VolumeControllerBase.VolumeChangeListener.Type type, int max, int value) {
            if ( mDialog != null ) mDialog.open();
        }

        @Override
        public void onMuteChanged(VolumeControllerBase.VolumeChangeListener.Type type, boolean mute) {
            if ( mDialog != null ) mDialog.open();
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected");
            mVolumeService = ((VolumeControlService.LocalBinder)iBinder).getService();
            final Context context = VolumeDialogService.this;
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    if ( mController != null ) mController.fetch(mVolumeService);
                }
            };

            if ( mVolumeService != null ) {
                mVolumeService.requestRefresh(r, new Handler(context.getMainLooper()));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            mVolumeService = null;
            if ( mController != null ) mController.fetch(null);
        }
    };
}