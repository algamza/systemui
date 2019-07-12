package com.humaxdigital.automotive.systemui.statusbar.controllers.dl3c;

import android.os.Handler;
import android.os.UserHandle;

import android.content.Context;
import android.content.Intent;

import android.view.View;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.app.Instrumentation;

import android.util.Log; 

import com.humaxdigital.automotive.systemui.statusbar.ui.FrameLayoutButton; 
import com.humaxdigital.automotive.systemui.R;

public class ButtonController {
    private static String TAG = "ButtonController"; 
    private View mParentView; 
    private Context mContext;
    private FrameLayoutButton mBtnBack; 
    private ImageView mIconBack; 
    private FrameLayoutButton mBtnHome; 
    private ImageView mIconHome; 
    private Handler mHandler; 

    public ButtonController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view;
        mHandler = new Handler(mContext.getMainLooper());
    }

    public void init() {
        mBtnBack = mParentView.findViewById(R.id.btn_back);
        mBtnHome = mParentView.findViewById(R.id.btn_home);
        mIconBack = mParentView.findViewById(R.id.icon_back);
        mIconHome = mParentView.findViewById(R.id.icon_home); 

        if ( mBtnBack == null || mBtnHome == null ) return; 

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBack(); 
            }
        });
        mBtnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goHome(); 
            }
        });
    }

    public void deinit() {
        mParentView = null;
        mContext = null;
        mBtnBack = null;
        mBtnHome = null;
        mIconBack = null;
        mIconHome = null;
        mHandler = null;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private void goHome() {
        Log.d(TAG, "goHome"); 
        if ( mContext == null ) return;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    private void goBack() {
        Log.d(TAG, "goBack"); 
        injectKeyEvent(KeyEvent.KEYCODE_BACK);
    }

    private void injectKeyEvent(int keyCode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Instrumentation instrumentation = new Instrumentation();
                instrumentation.sendKeyDownUpSync(keyCode);
            }
        }).start();
    }
}
