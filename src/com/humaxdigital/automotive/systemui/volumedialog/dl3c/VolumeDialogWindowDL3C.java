
package com.humaxdigital.automotive.systemui.volumedialog.dl3c;


import android.app.Dialog;

import android.content.Context;
import android.content.DialogInterface;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

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
import java.util.Objects; 

import com.humaxdigital.automotive.systemui.R; 
import com.humaxdigital.automotive.systemui.volumedialog.VolumeDialogWindowBase; 

public class VolumeDialogWindowDL3C extends VolumeDialogWindowBase {
    private final String TAG = "VolumeDialogWindowDL3C"; 
    private Context mContext; 
    private VolumeDialogUI mDialog;
    private View mPanel;
    private Window mWindow;
    private boolean mShowing;
    private final int MOVE_TIME_MS = 300;
    private final int SHOWING_TIME_MS = 3000;
    private final DialogHandler mHandler = new DialogHandler();
    private Timer mTimer;
    private TimerTask mHideTask;

    @Override
    public void init(Context context) {
        mContext = Objects.requireNonNull(context); 
        mShowing = false;
        mDialog = new VolumeDialogUI(mContext);
        mWindow = mDialog.getWindow();

        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        mWindow.setType(WindowManager.LayoutParams.TYPE_DISPLAY_OVERLAY);
        final WindowManager.LayoutParams lp = mWindow.getAttributes();
        lp.packageName = mContext.getPackageName();
        lp.format = PixelFormat.TRANSLUCENT;
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.width = (int)mContext.getResources().getDimension(R.dimen.volume_dialog_width_dl3c);
        lp.height = (int)mContext.getResources().getDimension(R.dimen.volume_dialog_height_dl3c);
        lp.windowAnimations = -1;

        mWindow.setAttributes(lp);
        mWindow.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT);

        mDialog.setCanceledOnTouchOutside(true);
        mDialog.setContentView(R.layout.volume_dialog_dl3c);
        mDialog.setOnShowListener(mShowListener);

        mPanel = mDialog.findViewById(R.id.volume_panel);
        int height = 720; 
        mPanel.setTranslationY(height);

        if ( mPanel != null ) 
            mPanel.setOnClickListener(mOnClickListener);

        mTimer = new Timer();
    }

    @Override
    public void deinit() {

    }

    @Override
    public void open() {
        openDialog(); 
    }
    
    @Override
    public void close(boolean force) {
        closeDialog(force);
    }

    @Override
    public View getView() {
        return mPanel; 
    }
    
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
        mPanel.setTranslationY(0);
        mPanel.setAlpha(1);
        mPanel.animate()
                .alpha(0)
                .translationY(mPanel.getHeight())
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

    private final class VolumeDialogUI extends Dialog implements DialogInterface {
        public VolumeDialogUI(Context context) {
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

    private void closeDialog(boolean force) {
        if ( force ) mHandler.obtainMessage(DialogHandler.DISMISS, 0).sendToTarget();
        else closeDialog();
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
            mPanel.setTranslationY(mPanel.getHeight());
            mPanel.setAlpha(0);
            mPanel.animate()
                    .alpha(1)
                    .translationY(0)
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