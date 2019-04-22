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

    private VolumeDialog mDialog;
    private View mPanel;
    private Window mWindow;
    private boolean mShowing;
    private final int MOVE_TIME_MS = 300;
    private final int SHOWING_TIME_MS = 3000;
    private final DialogHandler mHandler = new DialogHandler();
    private VolumeController mController;
    private Timer mTimer;
    private TimerTask mHideTask;
    private VolumeControlService mVolumeService;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate"); 
        initDialog();
        startVolumeControlService(); 
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy"); 
        if ( mController != null ) {
            mController.unregistVolumeListener(mVolumeListener); 
            mController = null; 
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind"); 
        return null;
    }

    private void initDialog() {
        Log.d(TAG, "initDialog");
        mShowing = false;
        mDialog = new VolumeDialog(this);
        mWindow = mDialog.getWindow();

        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        mWindow.setType(WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY);
        final WindowManager.LayoutParams lp = mWindow.getAttributes();
        lp.packageName = this.getPackageName();
        lp.format = PixelFormat.TRANSLUCENT;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.width = (int)getResources().getDimension(R.dimen.volume_dialog_width);
        lp.height = (int)getResources().getDimension(R.dimen.volume_dialog_height);
        lp.windowAnimations = -1;

        mWindow.setAttributes(lp);
        mWindow.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT);

        mDialog.setCanceledOnTouchOutside(true);
        mDialog.setContentView(R.layout.volume_dialog);
        mDialog.setOnShowListener(mShowListener);

        mPanel = mDialog.findViewById(R.id.volume_panel);
        mPanel.setTranslationX(-mPanel.getWidth());

        if ( mPanel != null ) 
            mPanel.setOnClickListener(mOnClickListener);
        
        mController = new VolumeController(this, mPanel)
                .registVolumeListener(mVolumeListener);

        mTimer = new Timer();
    }

    private void startVolumeControlService() {
        Log.d(TAG, "startVolumeControlService");
        Intent bindIntent = new Intent(this, VolumeControlService.class);
        if ( !bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE) ) {
            Log.e(TAG, "Failed to connect to VolumeControlService");
        }
    }

    private VolumeController.VolumeChangeListener mVolumeListener = 
        new VolumeController.VolumeChangeListener() {
        @Override
        public void onVolumeUp(VolumeController.VolumeChangeListener.Type type, int max, int value) {
            openDialog();
        }

        @Override
        public void onVolumeDown(VolumeController.VolumeChangeListener.Type type, int max, int value) {
            openDialog();
        }

        @Override
        public void onMuteChanged(VolumeController.VolumeChangeListener.Type type, boolean mute) {
            openDialog();
        }
    };

    private void showH() {
        Log.d(TAG, "showH");
        mHandler.removeMessages(DialogHandler.SHOW);
        mHandler.removeMessages(DialogHandler.DISMISS);
        mDialog.show();
    }

    private void dismissH() {
        Log.d(TAG, "dismissH");
        mHandler.removeMessages(DialogHandler.DISMISS);
        mHandler.removeMessages(DialogHandler.SHOW);

        mPanel.animate().cancel();
        mPanel.setTranslationX(0);
        mPanel.setAlpha(1);
        mPanel.animate()
                .alpha(0)
                .translationX(-mPanel.getWidth())
                .setDuration(MOVE_TIME_MS/2)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mDialog.dismiss();
                                mShowing = false;
                            }
                        }, MOVE_TIME_MS/4);
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

    private final class VolumeDialog extends Dialog implements DialogInterface {
        public VolumeDialog(Context context) {
            super(context, R.style.Theme_D1NoTitleDim);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            // todo : reschedule timeout
            Log.d(TAG, "dispatchTouchEvent");
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
            if ( isShowing() ) {
                if ( event.getAction() == MotionEvent.ACTION_OUTSIDE ) {
                    Log.d(TAG, "onTouchEvent:MotionEvent.ACTION_OUTSIDE");
                    //closeDialog();
                    return true;
                }
            }
            return false;
        }
    }

    private void closeDialog() {
        Log.d(TAG, "closeDialog");
        if ( !mShowing ) return;
        mHandler.obtainMessage(DialogHandler.DISMISS, 0).sendToTarget();
    }

    private void openDialog() {
        Log.d(TAG, "openDialog");
        if ( mHideTask != null ) {
            if ( mHideTask.scheduledExecutionTime() > 0 ) {
                mHideTask.cancel();
                mTimer.purge();
                mHideTask =  null;
            }
        }
        mHideTask = new TimerTask() {
            @Override
            public void run() {
                closeDialog();
            }
        };
        mTimer.schedule(mHideTask, SHOWING_TIME_MS);

        if ( mShowing ) return;
        mHandler.obtainMessage(DialogHandler.SHOW, 0).sendToTarget();
    }

    private Dialog.OnShowListener mShowListener = new Dialog.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialog) {
            Log.d(TAG, "onShow");
            if ( mPanel == null ) return;
            mPanel.setTranslationX(-mPanel.getWidth());
            mPanel.setAlpha(0);
            mPanel.animate()
                    .alpha(1)
                    .translationX(0)
                    .setDuration(MOVE_TIME_MS)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mShowing = true;
                                }
                            }, MOVE_TIME_MS/2);
                        }
                    })
                    .start();
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
                    mController.fetch(mVolumeService);
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
            mController.fetch(null);
        }
    };

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Panel onClick");
            if ( v.getId() != R.id.volume_panel ) return;
            Log.d(TAG, "closeDialog");
            closeDialog();
        }
    };

}