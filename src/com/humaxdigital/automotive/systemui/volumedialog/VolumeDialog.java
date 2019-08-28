package com.humaxdigital.automotive.systemui.volumedialog;

import android.app.Service;
import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.res.Configuration;

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

import com.humaxdigital.automotive.systemui.SystemUIBase;

import com.humaxdigital.automotive.systemui.util.ProductConfig; 
import com.humaxdigital.automotive.systemui.R; 

import com.humaxdigital.automotive.systemui.volumedialog.dl3c.VolumeDialogWindowDL3C; 
import com.humaxdigital.automotive.systemui.volumedialog.dl3c.VolumeControllerDL3C; 

public class VolumeDialog implements SystemUIBase {
    private final String TAG = "VolumeDialog"; 

    private VolumeDialogWindowBase mDialog; 
    private VolumeControllerBase mController; 
    private VolumeControlService mVolumeService;

    private ContentResolver mContentResolver = null;
    private ContentObserver mLastModeObserver = null;
    private ContentObserver mPowerObserver = null;
    private int mLastMode = 0; 
    private Context mContext = null; 

    @Override
    public void onCreate(Context context) {
        Log.d(TAG, "create"); 
        mContext = context; 
        if ( context == null ) return;
        if ( ProductConfig.getModel() == ProductConfig.MODEL.DL3C ) 
            mDialog = new VolumeDialogWindowDL3C();
        else 
            mDialog = new VolumeDialogWindow();
        mDialog.init(mContext); 
        mDialog.registDialogListener(mDialogListener); 

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DL3C ) 
            mController = new VolumeControllerDL3C(); 
        else
            mController = new VolumeController(); 

        mController.init(mContext, mDialog.getView()); 
        mController.registVolumeListener(mVolumeListener);

        startVolumeControlService(); 

        mContentResolver = mContext.getContentResolver();
        mLastModeObserver = createLastModeObserver();
        mPowerObserver = createPowerObserver();
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CarExtraSettings.Global.LAST_MEDIA_MODE), 
                false, mLastModeObserver, UserHandle.USER_CURRENT); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CarExtraSettings.Global.POWER_STATE), 
                false, mPowerObserver, UserHandle.USER_CURRENT); 
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroy"); 
        if ( mLastModeObserver != null && mContentResolver != null && mPowerObserver != null )  {
            mContentResolver.unregisterContentObserver(mLastModeObserver); 
            mContentResolver.unregisterContentObserver(mPowerObserver); 
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
    public void onConfigurationChanged(Configuration newConfig) {
    }

    private void startVolumeControlService() {
        if ( mContext == null ) return;
        Log.d(TAG, "startVolumeControlService");
        Intent bindIntent = new Intent(mContext, VolumeControlService.class);
        if ( !mContext.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE) ) {
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
            final Context context = mContext;
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

    private VolumeDialogWindowBase.DialogListener mDialogListener = 
        new VolumeDialogWindowBase.DialogListener() {
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

    private ContentObserver createPowerObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if ( mContentResolver == null ) return;
                int power = Settings.Global.getInt(mContentResolver, 
                    CarExtraSettings.Global.POWER_STATE, 
                    CarExtraSettings.Global.POWER_STATE_NORMAL);
                Log.d(TAG, "onChange:power="+power);
                if ( power == CarExtraSettings.Global.POWER_STATE_POWER_OFF ) {
                    if ( mDialog != null ) mDialog.close();
                }
            }
        };
        return observer; 
    }
}