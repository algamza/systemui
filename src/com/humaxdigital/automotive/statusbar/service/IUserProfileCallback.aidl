package com.humaxdigital.automotive.statusbar.service;

import com.humaxdigital.automotive.statusbar.service.BitmapParcelable;

oneway interface IUserProfileCallback {
    void onUserChanged(in BitmapParcelable bitmap); 
}