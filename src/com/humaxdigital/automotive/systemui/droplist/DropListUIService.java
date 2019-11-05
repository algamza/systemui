package com.humaxdigital.automotive.systemui.droplist;

import android.app.Dialog;
import android.app.Service;
import android.app.Dialog;
import android.app.IActivityManager; 
import android.app.ActivityManager; 
import android.app.TaskStackListener; 

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.Settings;
import android.net.Uri;

import android.os.Bundle;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.UserHandle;
import android.os.RemoteException;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.Window;
import android.view.MotionEvent;
import android.view.ViewGroup;

import android.graphics.PixelFormat;

import android.util.DisplayMetrics;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import android.extension.car.settings.CarExtraSettings;

import com.humaxdigital.automotive.systemui.SystemUIBase;
import com.humaxdigital.automotive.systemui.common.util.ProductConfig;
import com.humaxdigital.automotive.systemui.droplist.controllers.ControllerManager;

import com.humaxdigital.automotive.systemui.R; 
import com.humaxdigital.automotive.systemui.common.CONSTANTS; 

public class DropListUIService implements SystemUIBase {
    private static final String TAG = "DropListUIService";

    private final List<View> mAddedViews = new ArrayList<>();

    private WindowManager mWindowManager;

    private final int DROP_MOVE_TIME_MS = 200;
    private View mPanelBody;
    private Window mWindow;

    private DropListDialog mDialog; 
    private boolean mShowing;
    private boolean mStartedDismiss = false; 
    private final DialogHandler mHandler = new DialogHandler();

    private int mScreenBottom;
    private int mScreenWidth;
    private int mNavBarHeight;

    private NotiDialog mNotiDialog; 
    private View mNotiPanel; 
    private int mNotiDialogHeight; 
    private final int NOTI_MOVE_TIME_MS = 500;
    private final int NOTI_TIME_OUT = 3000;

    private SystemControl mSystemController;
    private ControllerManager mControllerManager;
    private ContentResolver mContentResolver = null;
    private ContentObserver mUserAgreeObserver = null; 

    private BroadcastReceiver mReceiver; 

    private IActivityManager mActivityService = null;

    private int mTouchDownValue = 0;
    private Boolean mTouchValid = false; 
    private final int TOUCH_OFFSET = -100; 

    private Context mContext; 

    @Override
    public void onCreate(Context context) {
        Log.d(TAG, "create"); 
        mContext = context; 
        if ( mContext == null ) return;
        mActivityService = ActivityManager.getService();
        try {
            mActivityService.registerTaskStackListener(mTaskStackListener); 
        } catch( RemoteException e ) {
            Log.e(TAG, "error:"+e);
        } 
       
        mWindowManager = (WindowManager)mContext.getSystemService(mContext.WINDOW_SERVICE);

        DisplayMetrics metrics;
        metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(metrics);
        mScreenBottom = metrics.heightPixels;
        mScreenWidth = metrics.widthPixels;

        Resources res = mContext.getResources();
        int identifier = res.getIdentifier("navigation_bar_height_car_mode", "dimen", "android");   
        mNavBarHeight = (identifier > 0) ? res.getDimensionPixelSize(identifier) : 0;

        mDialog = new DropListDialog(mContext);
        mWindow = mDialog.getWindow();

        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);

        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND
            | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    //| WindowManager.LayoutParams.FLAG_DIM_BEHIND
                    //| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        mWindow.setType(WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY);

        final WindowManager.LayoutParams lp = mWindow.getAttributes();
        lp.format = PixelFormat.TRANSLUCENT;
        lp.packageName = mContext.getPackageName();
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.x = 0;
        lp.y = 0;
        lp.width = mScreenWidth;
        lp.height = mScreenBottom - mNavBarHeight;
        lp.windowAnimations = -1;

        mWindow.setAttributes(lp);
        mWindow.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        

        mDialog.setCanceledOnTouchOutside(true);
        

        mDialog.setContentView(R.layout.panel_main);
        
        mDialog.setOnShowListener(mShowListener);
 
        mPanelBody = mDialog.findViewById(R.id.panel);
        mPanelBody.setTranslationY(0);
        //mPanelBody.setOnTouchListener(mDropListTouchListener);
        mShowing = false;

        mControllerManager = new ControllerManager(mContext, mPanelBody);
        mControllerManager.setListener(mPanelListener);

        Intent bindIntent = new Intent(mContext, SystemControl.class);
        if ( !mContext.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE) )
        {
            Log.e(TAG, "Failed to connect to SystemControl");
        }

        final IntentFilter filter = new IntentFilter(); 
        filter.addAction(CONSTANTS.OPEN_DROPLIST); 
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                openDropList(); 
            }
        };
        mContext.registerReceiverAsUser(mReceiver, UserHandle.ALL, filter, null, null);

        mContentResolver = mContext.getContentResolver(); 
        mUserAgreeObserver = createUserAgreeObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT), 
            false, mUserAgreeObserver, UserHandle.USER_CURRENT); 
        if ( !isUserAgreement() ) startNotiAnimation();
    }

    @Override
    public void onDestroy() {
        if ( mReceiver != null ) {
            if ( mContext != null ) 
                mContext.unregisterReceiver(mReceiver);
        }

        for (View view : mAddedViews) {
            mWindowManager.removeView(view);
        }
        mAddedViews.clear();

        if ( mSystemController != null ) {
            if ( mContext != null ) 
                mContext.unbindService(mServiceConnection);
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mSystemController = ((SystemControl.LocalBinder)iBinder).getService();
            final Context context = mContext;

            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    mControllerManager.fetch(mSystemController);
                }
            };

            if ( mSystemController != null ) {
                mSystemController.requestRefresh(r, new Handler(context.getMainLooper()));
            }

            mSystemController.registerCallback(mSystemCallback); 
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSystemController = null;
            mControllerManager.fetch(null);
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if ( mContext == null ) return;
        mContext.getResources().updateConfiguration(newConfig, null);
        if ( mControllerManager != null ) mControllerManager.configurationChange(mContext);
        Log.d(TAG, "onConfigurationChanged=mPanelBody"); 
    }

    private void openDropList() {
        Log.d(TAG, "openDropList"); 
        if ( mShowing ) return; 
        mHandler.obtainMessage(DialogHandler.SHOW, 0).sendToTarget();
    }

    private void closeDropList() {
        Log.d(TAG, "closeDropList");
        if ( mDialog != null && mDialog.isShowing() ) {
            if ( !mShowing ) return; 
            mHandler.obtainMessage(DialogHandler.DISMISS, 0).sendToTarget();
        }
    }

    private ControllerManager.Listener mPanelListener = new ControllerManager.Listener() {
        @Override
        public void onCloseDropList() {
            closeDropList();
        }
    };

    private void showH() {
        mHandler.removeMessages(DialogHandler.SHOW);
        mHandler.removeMessages(DialogHandler.DISMISS);
        mDialog.show();
    }

    private void dismissH() {
        mHandler.removeMessages(DialogHandler.DISMISS);
        mHandler.removeMessages(DialogHandler.SHOW);

        if ( mStartedDismiss ) return;
        mStartedDismiss = true; 

        if ( mControllerManager != null ) mControllerManager.clear(); 

        mPanelBody.animate().cancel();
        mPanelBody.setTranslationY(0);
        mPanelBody.setAlpha(1);
        mPanelBody.animate()
                .alpha(0)
                .translationY(-mScreenBottom+mNavBarHeight)
                .setDuration(DROP_MOVE_TIME_MS)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mDialog.dismiss();
                                mShowing = false; 
                                mStartedDismiss = false;
                            }
                        }, DROP_MOVE_TIME_MS/2);
                    }
                })
                .start();
    }

    private final class DialogHandler extends Handler {
        private static final int SHOW = 1;
        private static final int DISMISS = 2;

        public DialogHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case SHOW: showH(); break;
                case DISMISS: dismissH(); break;
                default: break;
            }
        }
    }

    private final class DropListDialog extends Dialog implements DialogInterface {
        public DropListDialog(Context context) {
            super(context, R.style.Theme_D1NoTitleDim); 
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            int y = (int)ev.getY(); 
            switch(ev.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    mTouchValid = true; 
                    mTouchDownValue = y; 
                    break; 
                }
                case MotionEvent.ACTION_UP: {
                    if ( mTouchValid ) {
                        mTouchValid = false; 
                        if ( (y - mTouchDownValue) < TOUCH_OFFSET ) {
                            closeDropList();
                        }
                    }
                    break; 
                }
                case MotionEvent.ACTION_MOVE: {
                    if ( mTouchValid ) {
                        if ( (y - mTouchDownValue) < TOUCH_OFFSET ) {
                            mTouchValid = false; 
                            closeDropList();
                        }
                    }
                    break;
                }
                default: break; 
            }
            return super.dispatchTouchEvent(ev);
        }

        @Override
        protected void onStart() {
            Log.d(TAG, "onStart");
            super.setCanceledOnTouchOutside(true);
            super.onStart();
        }

        @Override
        protected void onStop() {
            Log.d(TAG, "onStop");
            super.onStop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }
    }

    private Dialog.OnShowListener mShowListener = new Dialog.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialog) {
            Log.d(TAG, "onShow");
            if ( mPanelBody == null ) return;
            mPanelBody.setTranslationY(-mScreenBottom+mNavBarHeight);
            mPanelBody.setAlpha(0);
            mPanelBody.animate()
                    .alpha(1)
                    .translationY(0)
                    .setDuration(DROP_MOVE_TIME_MS)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mShowing = true;
                                }
                            }, DROP_MOVE_TIME_MS/2);
                        }
                    })
                    .start();
        }
    };

    private void startNotiAnimation() {
        Log.d(TAG, "startNotiAnimation");
        if ( mContext == null ) return;
        mNotiDialog = new NotiDialog(mContext);
        if ( mNotiDialog == null ) return;
        Window window = mNotiDialog.getWindow();
        if ( window == null ) return;
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND
        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setType(WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY);
        final WindowManager.LayoutParams lp = window.getAttributes();
        lp.packageName = mContext.getPackageName();
        lp.format = PixelFormat.TRANSLUCENT;
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        if ( mScreenWidth != 0 ) lp.width = mScreenWidth;
        else lp.width = (int)mContext.getResources().getDimension(R.dimen.noti_width);
        mNotiDialogHeight = (int)mContext.getResources().getDimension(R.dimen.noti_height);
        lp.height = mNotiDialogHeight; 
        lp.windowAnimations = -1;

        window.setAttributes(lp);
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT);

        mNotiDialog.setContentView(R.layout.panel_noti);
        mNotiDialog.setOnShowListener(mNotiShowListener);

        mNotiPanel = mNotiDialog.findViewById(R.id.panel_noti);
        if ( mNotiPanel != null ) mNotiPanel.setTranslationY(-mNotiDialogHeight);
        
        mNotiDialog.show();
        ///panel.setTranslationX(-mPanel.getWidth());
        Handler handler = new Handler(); 
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if ( mNotiDialog == null ) return;
                mNotiPanel.animate().cancel();
                mNotiPanel.animate()
                        .alpha(0)
                        .translationY(-mNotiDialogHeight)
                        .setDuration(NOTI_MOVE_TIME_MS)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                if ( mNotiDialog != null ) mNotiDialog.dismiss();
                            }
                        })
                        .start();
            }
        }, NOTI_TIME_OUT); 
    }

    private boolean isUserAgreement() {
        if ( mContext == null ) return false; 
        int is_agreement = Settings.Global.getInt(mContext.getContentResolver(), 
            CarExtraSettings.Global.USERPROFILE_IS_AGREEMENT_SCREEN_OUTPUT,
            CarExtraSettings.Global.FALSE);   
        if ( is_agreement == CarExtraSettings.Global.FALSE ) return false; 
        else return true;
    }

    private ContentObserver createUserAgreeObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if ( !isUserAgreement() ) startNotiAnimation();
            }
        };
        return observer; 
    }

    private Dialog.OnShowListener mNotiShowListener = new Dialog.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialog) {
            Log.d(TAG, "onShow");
            if ( mNotiPanel == null ) return;
            mNotiPanel.setTranslationY(-mNotiDialogHeight);
            mNotiPanel.setAlpha(0);
            mNotiPanel.animate()
                    .alpha(1)
                    .translationY(0)
                    .setDuration(NOTI_MOVE_TIME_MS)
                    .start();
        }
    };
    
    private final class NotiDialog extends Dialog {
        public NotiDialog(Context context) {
            super(context, R.style.Theme_D1NoTitleDim);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            return super.dispatchTouchEvent(ev);
        }

        @Override
        protected void onStart() {
            super.onStart();
        }

        @Override
        protected void onStop() {
            super.onStop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }
    }

    private final TaskStackListener mTaskStackListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            if ( mDialog != null && mDialog.isShowing() ) {
                Log.d(TAG, "onTaskStackChanged");
                closeDropList();
            }
        }
    };

    private SystemControl.SystemCallback mSystemCallback = 
        new SystemControl.SystemCallback() {
        @Override
        public void onVRStateChanged(boolean on) {
            Log.d(TAG, "onVRStateChanged="+on);
            if ( !on ) return;
            closeDropList();
        }
        @Override
        public void onPowerOnChanged(boolean on) {
            Log.d(TAG, "onPowerOnChanged="+on);
            if ( on ) return;
            closeDropList();
        }

        @Override
        public void onEmergencyModeChanged(boolean enable) {
            closeDropList();
        }

        @Override
        public void onBluelinkCallModeChanged(boolean enable) {
            closeDropList();
        }

        @Override
        public void onImmobilizationModeChanged(boolean enable) {
            closeDropList();
        }

        @Override
        public void onSlowdownModeChanged(boolean enable) {
            closeDropList();
        }
    };

}
