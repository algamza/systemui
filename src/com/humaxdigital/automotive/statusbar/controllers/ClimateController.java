package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.ClimateACView;
import com.humaxdigital.automotive.statusbar.ui.ClimateAirView;
import com.humaxdigital.automotive.statusbar.ui.ClimateBlowerView;
import com.humaxdigital.automotive.statusbar.ui.ClimateModeView;
import com.humaxdigital.automotive.statusbar.ui.ClimateTemperatureView;

public class ClimateController {
    private Context mContext;
    private Resources mRes;
    private View mClimate;
    private ClimateACView mAC;
    private int mACState;
    private ClimateBlowerView mBlower;
    private String mBlowerState;
    private ClimateAirView mCooler;
    private int mCoolerState;
    private ClimateAirView mHeater;
    private int mHeaterState;
    private ClimateModeView mMode;
    private int mModeState;
    private ClimateTemperatureView mTemOut;
    private String mTemOutState;
    private ClimateTemperatureView mTemIn;
    private String mTemInState;

    public ClimateController(Context context, View view) {
        if ( view == null ) return;
        mContext = context;
        if ( mContext != null ) mRes = mContext.getResources();
        mClimate = view;
        initView();
    }
    private void initView() {
        if ( mClimate == null ) return;
        mClimate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open Climate full Screen
                Log.d("TEST", "###### Open Climate Application");
            }
        });

        mAC = mClimate.findViewById(R.id.img_climate_ac);
        if ( mAC != null ) {
            Drawable drawable = ResourcesCompat.getDrawable(mRes, R.drawable.ic_climate_air, null);
            if ( drawable != null ) {
                mAC.setIcon(1, drawable);
                // get ac state
                mACState = 1;
                mAC.update(mACState);
            }
        }
        mBlower = mClimate.findViewById(R.id.img_climate_blower);
        if ( mBlower != null ) {
            Drawable drawable = ResourcesCompat.getDrawable(mRes, R.drawable.ic_climate_blower, null);
            if ( drawable != null ) {
                mBlower.setIcon(drawable);
                // get ac state
                mBlowerState = "8";
                mBlower.update(mBlowerState);
            }
        }
        mCooler = mClimate.findViewById(R.id.img_climate_cooler);
        if ( mCooler != null ) {
            Drawable drawable = ResourcesCompat.getDrawable(mRes, R.drawable.ic_climate_cooler, null);
            if ( drawable != null ) {
                mCooler.setIcon(1, drawable);
                // get ac state
                mCoolerState = 1;
                mCooler.update(mCoolerState);
            }
        }
        mHeater = mClimate.findViewById(R.id.img_climate_heater);
        if ( mHeater != null ) {
            Drawable drawable = ResourcesCompat.getDrawable(mRes, R.drawable.ic_climate_heater, null);
            if ( drawable != null ) {
                mHeater.setIcon(1, drawable);
                // get ac state
                mHeaterState = 1;
                mHeater.update(mHeaterState);
            }
        }
        mMode = mClimate.findViewById(R.id.img_climate_mode);
        if ( mMode != null ) {
            Drawable drawable = ResourcesCompat.getDrawable(mRes, R.drawable.ic_climate_mode, null);
            if ( drawable != null ) {
                mMode.setIcon(1, drawable);
                // get ac state
                mModeState = 1;
                mMode.update(mModeState);
            }
        }
        mTemOut = mClimate.findViewById(R.id.img_climate_temperature_out);
        if ( mTemOut != null ) {
            // get state
            mTemOutState = "27.5";
            mTemOut.update(mTemOutState);
        }
        mTemIn = mClimate.findViewById(R.id.img_climate_temperature_in);
        if ( mTemIn != null ) {
            // get state
            mTemInState = "19.0";
            mTemIn.update(mTemInState);
        }
    }
}
