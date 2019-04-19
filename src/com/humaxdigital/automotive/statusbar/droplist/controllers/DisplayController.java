package com.humaxdigital.automotive.statusbar.droplist.controllers;

import android.view.View;

import android.content.Context;
import android.content.res.Resources;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.droplist.SystemControl;
import com.humaxdigital.automotive.statusbar.droplist.ui.MenuLayout;

public class DisplayController implements BaseController {
    private MenuLayout mView;
    private SystemControl mSystem;  
    private Listener mListener; 

    @Override
    public BaseController init(View view) {
        mView = (MenuLayout)view;
        mView.setListener(mMenuCallback);
        return this;
    }

    @Override
    public void fetch(SystemControl system) {
        if ( system == null ) return; 
        mSystem = system; 
    }

    @Override
    public View getView() {
        return mView;
    }

    @Override
    public BaseController setListener(Listener listener) { 
        mListener = listener; 
        return this; 
    }

    @Override
    public void refresh(Context context) {
        if ( context == null || mView == null ) return;
        Resources res = context.getResources();
        mView.updateText(res.getString(R.string.STR_DISPLAY_OFF_01_ID));
    }

    private MenuLayout.MenuListener mMenuCallback = new MenuLayout.MenuListener() {
        @Override
        public boolean onClick() {
            if ( mSystem != null ) mSystem.displayOff(); 
            return true; 
        }
        
        @Override
        public boolean onLongClick() {
            return false; 
        }
    }; 
}
