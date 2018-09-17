package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.UserProfileView;

public class UserProfileController {
    private Context mContext;
    private UserProfileView mUserProfileView;
    private Drawable mUserImage;
    public UserProfileController(Context context, View view) {
        mContext = context;
        initView(view);
    }
    private void initView(View view) {
        if ( view == null ) return;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open user profile
            }
        });
        mUserProfileView = view.findViewById(R.id.img_useprofile);
        if ( mUserProfileView != null )
        {
            // getuser prifile
            mUserImage = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.ic_user_profile, null);
            if ( mUserImage != null ) mUserProfileView.setImageDrawable(mUserImage);
        }
    }
}
