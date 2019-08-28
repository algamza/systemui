package com.humaxdigital.automotive.systemui.notificationui;

import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.RemoteViews;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;
import android.service.notification.StatusBarNotification;
import android.graphics.drawable.Icon;
import android.app.Notification;
import android.app.PendingIntent; 
import android.graphics.drawable.Drawable; 
import android.content.res.XmlResourceParser;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.humaxdigital.automotive.systemui.R;

public class NotificationUiService extends Service {
    private final String TAG = "NotificationUiService";
    private NotificationDialog mDialog;
    private Window mWindow;
    private View mPanel;
    private boolean mShowing;
    private final int MOVE_TIME_MS = 200;
    private final long SHOWING_TIME_MS = 5000;
    private final DialogHandler mHandler = new DialogHandler();
    private ArrayList<NotificationUI> mNotificationUIs = new ArrayList<>();
    private NotificationUI mCurrentNotificationUI;
    private PendingIntent mCurrentNotificationIntent; 
    private Timer mTimer = new Timer();
    private TimerTask mHideTask;
    private long mCurrentShowingTimeMS = 0; 
    private ArrayList<String> mWhiteList = new ArrayList<>(); 
    private ArrayList<String> mBlackList = new ArrayList<>(); 
    private String mCurrentKey = ""; 
    private String mCurrentTitle = ""; 

    private String mBlockKey = ""; 
    private boolean mIsBlockState = false; 

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        createWhiteList(); 
        createBlackList(); 
        initDialog();
        initBroadcaster();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(mRefreshListener);
        localBroadcastManager.unregisterReceiver(mStateListener);
        localBroadcastManager.unregisterReceiver(mCancelListener);
    }

    
    private void initBroadcaster() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(mRefreshListener, new IntentFilter(NotificationSubscriber.ACTION_REFRESH));
        localBroadcastManager.registerReceiver(mStateListener, new IntentFilter(NotificationSubscriber.ACTION_STATE_CHANGE));
        localBroadcastManager.registerReceiver(mCancelListener, new IntentFilter(NotificationSubscriber.ACTION_CANCEL));
    }
    private final BroadcastReceiver mRefreshListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive:ACTION_REFRESH");
            if ( updateUI(intent.getStringExtra(NotificationSubscriber.EXTRA_KEY)) ) 
                openDialog();
        }
    };
    private final BroadcastReceiver mStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive:ACTION_STATE_CHANGE");
            if ( updateUI(intent.getStringExtra(NotificationSubscriber.EXTRA_KEY)) ) 
                openDialog();
        }
    };

    private final BroadcastReceiver mCancelListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive:ACTION_CANCEL");
            String key = intent.getStringExtra(NotificationSubscriber.EXTRA_KEY); 
            if ( key != null && key.equals(mCurrentKey) ) {
                Log.d(TAG, "onReceive:ACTION_CANCEL:current key");
                closeDialog();
            } else {
                Log.d(TAG, "onReceive:ACTION_CANCEL:other key");
            }   
        }
    };

    private boolean isValidPackage(String name) {
        for ( String str : mWhiteList )
            if (str.equals(name)) return true; 
        return false; 
    }

    private void createWhiteList() {
        XmlResourceParser parser = getResources().getXml(R.xml.white_list);
        try {
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if ( event == XmlResourceParser.TEXT ) mWhiteList.add(parser.getText()); 
                event = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isValidTitle(String text) {
        if ( text == null ) return true;
        for ( String str : mBlackList )
            if (text.contains(str)) return false; 
        return true; 
    }

    private void createBlackList() {
        XmlResourceParser parser = getResources().getXml(R.xml.black_list);
        try {
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if ( event == XmlResourceParser.TEXT ) mBlackList.add(parser.getText()); 
                event = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean updateUI(String key) {
        if ( mPanel == null ) return false; 
        final List<StatusBarNotification> notifications = NotificationSubscriber.getNotifications();
        if ( notifications == null ) return false; 

        synchronized (notifications) {
            for (int i = 0; i < notifications.size(); i++) {
                StatusBarNotification notification = notifications.get(i); 
                if (notification.getKey().equals(key)) {
                    if ( !isValidPackage(notification.getPackageName()) ) return false;
                    Notification noti = notification.getNotification();
                    if ( noti == null ) return false; 
                    Icon icon = noti.getSmallIcon(); 
                    long duration = noti.getTimeoutAfter(); 
                    Bundle extras = noti.extras;
                    String title = extras.getString(Notification.EXTRA_TITLE);
                    CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
                    CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
                    RemoteViews remote_view = noti.contentView;
                    //RemoteViews remote_view = noti.bigContentView;
                    //RemoteViews remote_view = noti.headsUpContentView; 
                    Log.d(TAG, "packagename:"+notification.getPackageName()+", title:"+title+", text:"+text); 
                    if ( (remote_view == null) && (text == null || text.equals("")) && 
                        (title == null  || title.equals("")) ) return false; 
                    if ( (remote_view == null) && !isValidTitle(title) ) return false;
                    
                    NotificationUI ui = new NotificationUI(NotificationUiService.this); 
                    if ( remote_view != null ) ui.setRemoteViews(remote_view); 
                    if ( title != null && !title.equals("") ) ui.setTitle(title); 
                    else ui.setTitle(""); 
                    if ( text != null ) ui.setBody(text.toString()); 
                    else ui.setBody(""); 

                    if ( icon != null ) {
                        if ( icon.getType() == Icon.TYPE_RESOURCE ) {
                            if ( icon.getResId() == -1 ) ui.setIcon(null); 
                            else ui.setIcon(icon); 
                        } else ui.setIcon(icon); 
                        // FIXME: If icon is not set, an error occurs.
                        /*
                        Drawable drawable = icon.loadDrawable(this); 
                        if ( drawable.getIntrinsicHeight() == 1 && drawable.getIntrinsicWidth() == 1 ) {
                            ui.setIcon(null); 
                        } else ui.setIcon(icon); 
                        */
                    }
                    else ui.setIcon(null);
                    
                    ui.setOnClickListener(mOnClick); 
                    if ( noti.contentIntent != null ) { 
                        mCurrentNotificationIntent = noti.contentIntent; 
                    } else {
                        mCurrentNotificationIntent = null; 
                    }

                    if ( duration > 0 ) mCurrentShowingTimeMS = duration; 
                    else mCurrentShowingTimeMS = 0; 
                    
                    ui.inflate();

                    if ( isBlockOSD(key) ) return false; 
                    if ( isUpdateOSD(key, title, text) ) return true;
                    
                    if ( mCurrentNotificationUI != null ) {
                        ((ViewGroup)mPanel).addView(ui); 
                        ((ViewGroup)mPanel).removeView(mCurrentNotificationUI); 
                        mCurrentNotificationUI = ui; 
                    } else {
                        ((ViewGroup)mPanel).addView(ui); 
                        mCurrentNotificationUI = ui; 
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBlockOSD(String key) {
        boolean ret = false; 
        if ( mBlockKey.equals(key) ) {
            if ( mIsBlockState ) ret = true; 
        } else {
            mBlockKey = key; 
            mIsBlockState = false;
        }

        return ret;
    }

    private void updateBlock() {
        Log.d(TAG, "updateBlock:state="+mIsBlockState); 
        if ( mIsBlockState ) return;
        mIsBlockState = true; 
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "cancel block:state="+mIsBlockState); 
            mIsBlockState = false;
            mBlockKey = ""; 
        }, 1000);
    }

    private boolean isUpdateOSD(String key, String title, CharSequence text) {
        boolean ret = false;
        if ( mCurrentKey.equals(key) ) {
            Log.d(TAG, "isUpdateOSD equals(key)="+key); 
            if ( title != null && !title.equals("") && title.equals(mCurrentTitle) ) {
                if ( text != null ) {
                    if ( mCurrentNotificationUI != null ) 
                        mCurrentNotificationUI.updateBody(text.toString()); 
                    Log.d(TAG, "only update body"); 
                    ret = true;
                }
            }
        } else {
            mCurrentKey = key;
            ret = false;
        }
        mCurrentTitle = title; 
        return ret; 
    }

    private View.OnClickListener mOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if ( mCurrentNotificationIntent != null ) {
                synchronized(mCurrentNotificationIntent) {
                    try {
                        mCurrentNotificationIntent.send(); 
                    } catch (PendingIntent.CanceledException e) {
                        Log.d(TAG, "failed to send intent for " + e);
                    }
                }
            }
            Log.d(TAG, "onClick");
            updateBlock();
            closeDialog();
        }
    }; 

    private void dismissAll() {
        LocalBroadcastManager.getInstance(NotificationUiService.this).
            sendBroadcast(new Intent(NotificationSubscriber.ACTION_DISMISS)
            .putExtra(NotificationSubscriber.EXTRA_KEY, "*"));
    }

    private void initDialog() {
        Log.d(TAG, "initDialog");
        mShowing = false;
        mDialog = new NotificationDialog(this);
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
        lp.packageName = this.getPackageName();
        lp.format = PixelFormat.TRANSLUCENT;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.x = 0;
        lp.y = 0;
        lp.width = (int)getResources().getDimension(R.dimen.dialog_width);
        lp.height = (int)getResources().getDimension(R.dimen.dialog_height);
        lp.windowAnimations = -1;
        mWindow.setAttributes(lp);
        mWindow.setLayout(lp.width, lp.height);

        mDialog.setCanceledOnTouchOutside(true);
        mDialog.setContentView(R.layout.notification_dialog);
        mDialog.setOnShowListener(mShowListener);

        mPanel =  mDialog.findViewById(R.id.notification_dialog);
        mPanel.setTranslationX(-mPanel.getHeight());
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    public void closeDialog() {
        Log.d(TAG, "closeDialog");
        if ( !mShowing ) return;
        clearSpecialCase();
        mHandler.obtainMessage(DialogHandler.DISMISS, 0).sendToTarget();
    }

    private void clearSpecialCase() {
        mCurrentKey = ""; 
        mCurrentTitle = ""; 
    }

    public void openDialog() {
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
        if ( mCurrentShowingTimeMS != 0 ) mTimer.schedule(mHideTask, mCurrentShowingTimeMS);
        else mTimer.schedule(mHideTask, SHOWING_TIME_MS);

        if ( mShowing ) return;
        mHandler.obtainMessage(DialogHandler.SHOW, 0).sendToTarget();
    }

    private Dialog.OnShowListener mShowListener = new Dialog.OnShowListener() {
        @Override
        public void onShow(DialogInterface dialog) {
            Log.d(TAG, "onShow");
            if ( mPanel == null ) return;
            mPanel.setTranslationY(-mPanel.getHeight());
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

    private void showH() {
        Log.d(TAG, "showH");
        mHandler.removeMessages(DialogHandler.SHOW);
        mHandler.removeMessages(DialogHandler.DISMISS);
        Log.d(TAG, "show");
        mDialog.show();
    }

    private void dismissH() {
        Log.d(TAG, "dismissH");
        mShowing = false;
        
        mHandler.removeMessages(DialogHandler.DISMISS);
        mHandler.removeMessages(DialogHandler.SHOW);

        mPanel.animate().cancel();
        mPanel.setTranslationY(0);
        mPanel.setAlpha(1);
        mDialog.dismiss();
        /*
        mPanel.animate()
                .alpha(0)
                .translationY(-mPanel.getHeight())
                .setDuration(MOVE_TIME_MS/2)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "dismiss");
                                mDialog.dismiss();
                            }
                        }, MOVE_TIME_MS/4);
                    }
                })
                .start();
                */
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
                case DISMISS: if ( mShowing ) dismissH(); break;
                default: break;
            }
        }
    }

    private final class NotificationDialog extends Dialog implements DialogInterface {
        public NotificationDialog(Context context) {
            super(context, R.style.Theme_D1NoTitleDim);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            return super.dispatchTouchEvent(ev);
        }

        @Override
        protected void onStart() {
            super.setCanceledOnTouchOutside(true);
            super.onStart();
        }

        @Override
        protected void onStop() {
            super.onStop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if ( isShowing() ) {
                if ( event.getAction() == MotionEvent.ACTION_OUTSIDE ) {
                    return true;
                }
            }
            return false;
        }
    }
}
