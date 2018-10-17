package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;

import android.content.Intent;
import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.ClimateACView;
import com.humaxdigital.automotive.statusbar.ui.ClimateBlowerTextView;
import com.humaxdigital.automotive.statusbar.ui.ClimateSeatView;
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
    private ClimateBlowerTextView mBlowerText;
    private String mBlowerState;
    private ClimateSeatView mSeatLeft;
    private int mSeatLeftState;
    private ClimateSeatView mSeatRight;
    private int mSeatRightState;
    private ClimateModeView mMode;
    private int mModeState;
    private ClimateTemperatureView mTemOut;
    private ClimateTemperatureView mTemOutDec;
    private String mTemOutState;
    private ClimateTemperatureView mTemIn;
    private ClimateTemperatureView mTemInDec;
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
                // todo : open climate full screen
                Intent intent = new Intent("android.car.intent.action.TOGGLE_HVAC_CONTROLS"); 
                if ( mContext != null ) mContext.sendBroadcast(intent);             }
        });

        mTemOut = mClimate.findViewById(R.id.img_climate_temperature_out_large);
        mTemOutDec = mClimate.findViewById(R.id.img_climate_temperature_out_small);
        if ( mTemOut != null && mTemOutDec != null ) {
            // get state
            mTemOutState = "27.5";
            if ( mTemOutState.contains(".") ) {
                String tem = mTemOutState.substring(0, mTemOutState.indexOf(".") );
                String dec = mTemOutState.substring(mTemOutState.indexOf("."), mTemOutState.length());
                mTemOut.update(tem);
                mTemOutDec.update(dec);
            } else {
                mTemOut.update(mTemOutState);
                mTemOutDec.update(".0");
            }
        }

        mSeatLeft = mClimate.findViewById(R.id.img_climate_seat_left);
        if ( mSeatLeft != null ) {
            Drawable d0 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_00, null);
            Drawable d1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_01, null);
            Drawable d2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_02, null);
            Drawable d3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_03, null);
            Drawable d4 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_04, null);
            Drawable d5 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_05, null);
            Drawable d6 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_06, null);

            if ( d0 != null && d1 != null && d2 != null && d3 != null && d4 != null && d5 != null && d6 != null ) {
                mSeatLeft.addIcon(0, d0);
                mSeatLeft.addIcon(1, d1);
                mSeatLeft.addIcon(2, d2);
                mSeatLeft.addIcon(3, d3);
                mSeatLeft.addIcon(4, d4);
                mSeatLeft.addIcon(5, d5);
                mSeatLeft.addIcon(6, d6);
                // get ac state
                mSeatLeftState = 5;
                mSeatLeft.update(mSeatLeftState);
            }
        }

        mMode = mClimate.findViewById(R.id.img_climate_mode);
        if ( mMode != null ) {
            Drawable enable = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car, null);
            Drawable disable = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_disable, null);
            if ( enable != null && disable != null ) {
                mMode.addIcon(1, enable);
                mMode.addIcon(2, disable);
                // get ac state
                mModeState = 1;
                mMode.update(mModeState);
            }
        }

        mAC = mClimate.findViewById(R.id.img_climate_ac);
        if ( mAC != null ) {
            Drawable d1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01, null);
            Drawable d2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02, null);
            Drawable d3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03, null);
            Drawable d4 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04, null);
            if ( d1 != null && d2 != null && d3 != null && d4 != null ) {
                mAC.addIcon(1, d1);
                mAC.addIcon(2, d2);
                mAC.addIcon(3, d3);
                mAC.addIcon(4, d4);
                // get ac state
                mACState = 1;
                mAC.update(mACState);
            }
        }

        mBlower = mClimate.findViewById(R.id.img_climate_blower);
        mBlowerText = mClimate.findViewById(R.id.img_climate_blower_text);
        if ( mBlower != null && mBlowerText != null ) {
            Drawable drawable = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind, null);
            if ( drawable != null ) mBlower.setIcon(drawable);
            // get ac state
            mBlowerState = "8";
            mBlowerText.update(mBlowerState);
        }

        mSeatRight = mClimate.findViewById(R.id.img_climate_seat_right);
        if ( mSeatRight != null ) {
            Drawable d0 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_00, null);
            Drawable d1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_01, null);
            Drawable d2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_02, null);
            Drawable d3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_03, null);
            Drawable d4 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_04, null);
            Drawable d5 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_05, null);
            Drawable d6 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_06, null);

            if ( d0 != null && d1 != null && d2 != null && d3 != null && d4 != null && d5 != null && d6 != null ) {
                mSeatRight.addIcon(0, d0);
                mSeatRight.addIcon(1, d1);
                mSeatRight.addIcon(2, d2);
                mSeatRight.addIcon(3, d3);
                mSeatRight.addIcon(4, d4);
                mSeatRight.addIcon(5, d5);
                mSeatRight.addIcon(6, d6);
                // get ac state
                mSeatRightState = 2;
                mSeatRight.update(mSeatRightState);
            }
        }

        mTemIn = mClimate.findViewById(R.id.img_climate_temperature_in_large);
        mTemInDec = mClimate.findViewById(R.id.img_climate_temperature_in_small);
        if ( mTemIn != null && mTemInDec != null ) {
            // get state
            mTemInState = "19";
            if ( mTemInState.contains(".") ) {
                String tem = mTemInState.substring(0, mTemInState.indexOf(".") );
                String dec = mTemInState.substring(mTemInState.indexOf("."), mTemInState.length());
                mTemIn.update(tem);
                mTemInDec.update(dec);
            } else {
                mTemIn.update(mTemInState);
                mTemInDec.update(".0");
            }
        }
    }
}
