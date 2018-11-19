package com.humaxdigital.automotive.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.view.View;
import android.os.RemoteException;
import android.graphics.Bitmap;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.UserProfileView;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.IUserProfileCallback; 
import com.humaxdigital.automotive.statusbar.service.BitmapParcelable; 

public class UserProfileController implements BaseController {
    private Context mContext;
    private View mParentView; 
    private UserProfileView mUserProfileView;
    private IStatusBarService mService; 

    public UserProfileController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view; 
    }
    
    @Override
    public void init(IStatusBarService service) {
        mService = service; 
        try {
            if ( mService != null ) mService.registerUserProfileCallback(mUserProfileCallback);
        } catch( RemoteException e ) {
            e.printStackTrace();
        } 
        initView();
    }

    @Override
    public void deinit() {
        try {
            if ( mService != null ) mService.unregisterUserProfileCallback(mUserProfileCallback);
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    private void initView() {
        if ( mParentView == null ) return;
        mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mService == null ) return;
                try {
                    mService.openUserProfileSetting(); 
                } catch( RemoteException e ) {
                    e.printStackTrace();
                }
            }
        });

        mUserProfileView = mParentView.findViewById(R.id.img_useprofile);
        if ( mUserProfileView != null ) mUserProfileView.setImageBitmap(getUserBitmap()); 
    }

    private Bitmap getUserBitmap() {
        if ( mService == null ) return null; 
        Bitmap img = null; 
        try {
            img = mService.getUserProfileImage().getBitmap();
        } catch( RemoteException e ) {
            e.printStackTrace();
            return null; 
        }
        return img; 
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private final IUserProfileCallback.Stub mUserProfileCallback = new IUserProfileCallback.Stub() {
        public void onUserChanged(BitmapParcelable data) throws RemoteException {
           if ( mUserProfileView != null ) mUserProfileView.setImageBitmap(getUserBitmap()); 
        }
    };
}
