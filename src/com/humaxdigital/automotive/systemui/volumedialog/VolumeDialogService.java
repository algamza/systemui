package com.humaxdigital.automotive.systemui.volumedialog;

import android.app.Service;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.ContentResolver;

import android.provider.Settings;
import android.net.Uri;
import android.database.ContentObserver;

import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.os.UserHandle;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.graphics.PixelFormat;

import android.extension.car.settings.CarExtraSettings;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import com.humaxdigital.automotive.systemui.util.ProductConfig; 
import com.humaxdigital.automotive.systemui.R; 

import com.humaxdigital.automotive.systemui.volumedialog.dl3c.VolumeDialogDL3C; 
import com.humaxdigital.automotive.systemui.volumedialog.dl3c.VolumeControllerDL3C; 

public class VolumeDialogService extends Service {
    private final String TAG = "VolumeDialogService"; 

    private VolumeDialogBase mDialog; 
    private VolumeControllerBase mController; 
    private VolumeControlService mVolumeService;

    private ContentResolver mContentResolver = null;
    private ContentObserver mLastModeObserver = null;
    private int mLastMode = 0; 

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate"); 

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DL3C ) 
            mDialog = new VolumeDialogDL3C();
        else 
            mDialog = new VolumeDialog();
        mDialog.init(this); 
        mDialog.registDialogListener(mDialogListener); 

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DL3C ) 
            mController = new VolumeControllerDL3C(); 
        else
            mController = new VolumeController(); 

        mController.init(this, mDialog.getView()); 
        mController.registVolumeListener(mVolumeListener);

        startVolumeControlService(); 

        mContentResolver = this.getContentResolver();
        mLastModeObserver = createLastModeObserver();
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CarExtraSettings.Global.LAST_MEDIA_MODE), 
                false, mLastModeObserver, UserHandle.USER_CURRENT); 
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy"); 
        if ( mLastModeObserver != null && mContentResolver != null )  {
            mContentResolver.unregisterContentObserver(mLastModeObserver); 
        }

        if ( mController != null ) {
            mController.deinit(); 
            mController.unregistVolumeListener(mVolumeListener); 
            mController = null; 
        }
        if ( mDialog != null ) {
            mDialog.unregistDialogListener(mDialogListener); 
            mDialog.deinit();
        }
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

    private VolumeDialogBase.DialogListener mDialogListener = 
        new VolumeDialogBase.DialogListener() {
        @Override
        public void onShow(boolean show) {
            if ( mVolumeService != null ) mVolumeService.onShow(show); 
        }
    };

    private ContentObserver createLastModeObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                
                if ( mContentResolver == null ) return;
                int lastmode = Settings.Global.getInt(mContentResolver, 
                    CarExtraSettings.Global.LAST_MEDIA_MODE, 
                    CarExtraSettings.Global.LAST_MEDIA_MODE_DEFAULT);
                Log.d(TAG, "onChange:lastmode="+lastmode);
                if ( mLastMode == lastmode ) return;
                mLastMode = lastmode; 
                if ( mDialog != null ) mDialog.close();
            }
        };
        return observer; 
    }
}