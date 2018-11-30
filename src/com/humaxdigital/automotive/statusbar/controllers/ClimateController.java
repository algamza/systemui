package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.os.RemoteException;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import com.humaxdigital.automotive.statusbar.R;

import com.humaxdigital.automotive.statusbar.ui.ClimateMenuImg;
import com.humaxdigital.automotive.statusbar.ui.ClimateMenuTextDec;
import com.humaxdigital.automotive.statusbar.ui.ClimateMenuTextImg;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.IClimateCallback; 

public class ClimateController implements BaseController {
    static final String TAG = "ClimateController"; 
    static final String PACKAGE_NAME = "com.humaxdigital.automotive.statusbar"; 

    private enum SeatState { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
    private enum FanDirectionState { FACE, FLOOR, FLOOR_FACE, FLOOR_DEFROST }
    private enum FanSpeedState { STEPOFF, STEP0, STEP1, STEP2, STEP3, STEP4, STEP5, STEP6, STEP7, STEP8 };
    private enum ACState { ON, OFF };
    private enum IntakeState { ON, OFF };

    private IStatusBarService mService; 
    private Context mContext;
    private Resources mRes;
    private View mClimate;

    private ClimateMenuTextDec mTempDR;
    private float mTempDRState = 0.0f;
    private ClimateMenuTextDec mTempPS;
    private float mTempPSState = 0.0f;

    private ClimateMenuImg mSeatDR;
    private SeatState mSeatDRState = SeatState.NONE;
    private ClimateMenuImg mSeatPS;
    private SeatState mSeatPSState = SeatState.NONE;

    private ClimateMenuImg mIntake;
    private IntakeState mIntakeState = IntakeState.OFF;
    private ClimateMenuImg mAC;
    private ACState mACState = ACState.OFF;
    private ClimateMenuImg mFanDirection;
    private FanDirectionState mFanDirectionState = FanDirectionState.FACE;

    private ClimateMenuTextImg mFanSpeed;
    private FanSpeedState mFanSpeedState = FanSpeedState.STEPOFF;

    private final List<View> mClimateViews = new ArrayList<>();


    public ClimateController(Context context, View view) {
        if ( view == null || context == null ) return;
        mContext = context;
        mClimate = view;
        mRes = mContext.getResources();
    }

    @Override
    public void init(IStatusBarService service) {
        mService = service; 
        try {
            if ( mService != null ) mService.registerClimateCallback(mClimateCallback); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }

        initView();
        update(); 
    }

    @Override
    public void deinit() {
        try {
            if ( mService != null ) mService.unregisterClimateCallback(mClimateCallback); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    private void openClimateSetting() {
        if ( mService == null ) return; 
        try {
            mService.openClimateSetting();
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    private void initView() {
        if ( mClimate == null || mContext == null || mService == null ) return;
        mClimate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openClimateSetting(); 
            }
        });

        mTempDR = new ClimateMenuTextDec(mContext).inflate(); 
        mSeatDR = new ClimateMenuImg(mContext)
            .addIcon(SeatState.NONE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_00, null))
            .addIcon(SeatState.COOLER1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_01, null))
            .addIcon(SeatState.COOLER2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_02, null))
            .addIcon(SeatState.COOLER3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_03, null))
            .addIcon(SeatState.HEATER1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_04, null))
            .addIcon(SeatState.HEATER2.ordinal(),  ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_05, null))
            .addIcon(SeatState.HEATER3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_06, null))
            .inflate(); 
        mAC = new ClimateMenuImg(mContext)
            .addIcon(ACState.ON.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_ac_on, null))
            .addIcon(ACState.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_ac_off, null))
            .inflate();
        mAC.setOnClickListener(mClimateACOnClick); 
        mIntake = new ClimateMenuImg(mContext)
            .addIcon(IntakeState.ON.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_on, null))
            .addIcon(IntakeState.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_off, null))
            .inflate();
        mIntake.setOnClickListener(mClimateIntakeOnClick); 
        mFanSpeed = new ClimateMenuTextImg(mContext)
            .addIcon(0, ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_d, null))
            .addIcon(1, ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind, null))
            .inflate(); 
        mFanDirection = new ClimateMenuImg(mContext)
            .addIcon(FanDirectionState.FACE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01, null))
            .addIcon(FanDirectionState.FLOOR.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02, null))
            .addIcon(FanDirectionState.FLOOR_FACE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03, null))
            .addIcon(FanDirectionState.FLOOR_DEFROST.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04, null))
            .inflate();
        mFanDirection.setOnClickListener(mClimateFanDirectionOnClick); 
        mSeatPS = new ClimateMenuImg(mContext)
            .addIcon(SeatState.NONE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_00, null))
            .addIcon(SeatState.COOLER1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_01, null))
            .addIcon(SeatState.COOLER2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_02, null))
            .addIcon(SeatState.COOLER3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_03, null))
            .addIcon(SeatState.HEATER1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_04, null))
            .addIcon(SeatState.HEATER2.ordinal(),  ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_05, null))
            .addIcon(SeatState.HEATER3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_06, null))
            .inflate(); 
        mTempPS = new ClimateMenuTextDec(mContext).inflate(); 

        mClimateViews.add(mTempDR);
        mClimateViews.add(mSeatDR);
        mClimateViews.add(mAC);
        mClimateViews.add(mIntake);
        mClimateViews.add(mFanSpeed);
        mClimateViews.add(mFanDirection);
        mClimateViews.add(mSeatPS);
        mClimateViews.add(mTempPS);

        for ( int i = 0; i<mClimateViews.size(); i++ ) {
            int resid = mContext.getResources().getIdentifier("climate_menu_"+i, "id", PACKAGE_NAME);
            if ( resid < 0 ) continue;
            ((FrameLayout)mClimate.findViewById(resid)).addView(mClimateViews.get(i));
        }
    }

    private void update() {
        if ( mService == null ) return; 

        try {
            mTempDRState = mService.getDRTemperature();
            mSeatDRState = SeatState.values()[mService.getDRSeatStatus()]; 
            mACState = mService.getAirConditionerState() ? ACState.ON:ACState.OFF; 
            mIntakeState = mService.getAirCirculationState() ? IntakeState.ON:IntakeState.OFF; 
            mFanSpeedState = FanSpeedState.values()[mService.getBlowerSpeed()]; 
            mFanDirectionState = FanDirectionState.values()[mService.getFanDirection()]; 
            mSeatPSState = SeatState.values()[mService.getPSSeatStatus()]; 
            mTempPSState = mService.getPSTemperature();
        } catch( RemoteException e ) {
            e.printStackTrace();
        }


        if ( mTempDR != null ) updateTemp(mTempDR, mTempDRState); 
        if ( mSeatDR != null ) mSeatDR.update(mSeatDRState.ordinal()); 
        if ( mAC != null ) mAC.update(mACState.ordinal()); 
        if ( mIntake != null ) mIntake.update(mIntakeState.ordinal()); 
        if ( mFanSpeed != null ) {
            if ( mFanSpeedState == FanSpeedState.STEPOFF ) {
                mFanSpeed.update(0, String.valueOf(FanSpeedState.STEP0.ordinal()-1)); 
                mFanSpeed.setTextColor(mRes.getColor(R.color.colorClimateBlowerOff)); 
            } else {
                mFanSpeed.update(1, String.valueOf(mFanSpeedState.ordinal()-1));
                mFanSpeed.setTextColor(mRes.getColor(R.color.colorClimateText)); 
            }
        }
        if ( mFanDirection != null ) mFanDirection.update(mFanDirectionState.ordinal()); 
        if ( mSeatPS != null ) mSeatPS.update(mSeatPSState.ordinal()); 
        if ( mTempPS != null ) updateTemp(mTempPS, mTempPSState); 
    }

    private void updateTemp(ClimateMenuTextDec view, float temp) {
        String state = String.valueOf(temp);
        if ( state.contains(".") ) {
            String tem = state.substring(0, state.indexOf(".") );
            String dec = state.substring(state.indexOf("."), state.length());
            view.update(tem, dec); 
        } else 
            view.update(state, ".0"); 
        
    }

    private View.OnClickListener mClimateACOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mAC == null ) return; 
            if ( mACState == ACState.ON ) {
                mACState = ACState.OFF; 
                mAC.update(mACState.ordinal());
            } else {
                mACState = ACState.ON; 
                mAC.update(mACState.ordinal());
            }

            try {
                if ( mService != null ) 
                    mService.setAirConditionerState(mACState==ACState.ON?true:false);
            } catch( RemoteException e ) {
                e.printStackTrace();
            }
        }
    }; 

    private final View.OnClickListener mClimateIntakeOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mIntake == null ) return; 
            if ( mIntakeState == IntakeState.ON ) {
                mIntakeState = IntakeState.OFF; 
                mIntake.update(mIntakeState.ordinal());
            } else {
                mIntakeState = IntakeState.ON; 
                mIntake.update(mIntakeState.ordinal());
            }

            try {
                if ( mService != null ) 
                    mService.setAirCirculationState(mIntakeState==IntakeState.ON?true:false);
            } catch( RemoteException e ) {
                e.printStackTrace();
            }
        }
    }; 

    private final View.OnClickListener mClimateFanDirectionOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mFanDirection == null ) return; 
            int next = mFanDirectionState.ordinal() + 1;
            if ( next >= FanDirectionState.values().length ) 
                mFanDirectionState = FanDirectionState.values()[0];
            else 
                mFanDirectionState = FanDirectionState.values()[next];

            mFanDirection.update(mFanDirectionState.ordinal()); 

            try {
                if ( mService != null ) 
                    mService.setFanDirection(mFanDirectionState.ordinal());
            } catch( RemoteException e ) {
                e.printStackTrace();
            }
        }
    }; 

    private final IClimateCallback.Stub mClimateCallback = new IClimateCallback.Stub() {
        public void onDRTemperatureChanged(float temp) throws RemoteException {
            if ( mTempDR == null ) return; 
            mTempDRState = temp;
            updateTemp(mTempDR, mTempDRState); 
        }
        public void onDRSeatStatusChanged(int status) throws RemoteException {
            if ( mSeatDR == null ) return;
            mSeatDRState = SeatState.values()[status]; 
            mSeatDR.update(mSeatDRState.ordinal());
        }
        public void onAirCirculationChanged(boolean isOn) throws RemoteException {
            if ( mIntake == null ) return; 
            mIntakeState = isOn?IntakeState.ON:IntakeState.OFF;
            mIntake.update(mIntakeState.ordinal());
        }
        public void onAirConditionerChanged(boolean isOn) throws RemoteException {
            if ( mAC == null ) return; 
            mACState = isOn?ACState.ON:ACState.OFF;
            mAC.update(mACState.ordinal());
        }
        public void onFanDirectionChanged(int direction) throws RemoteException {
            if ( mFanDirection == null ) return; 
            mFanDirectionState = FanDirectionState.values()[direction]; 
            mFanDirection.update(mFanDirectionState.ordinal()); 
        }
        public void onBlowerSpeedChanged(int status) throws RemoteException {
            if ( mFanSpeed == null ) return; 
            mFanSpeedState = FanSpeedState.values()[status]; 
            if ( mFanSpeedState == FanSpeedState.STEPOFF ) {
                mFanSpeed.update(0, String.valueOf(FanSpeedState.STEP0.ordinal()-1)); 
                mFanSpeed.setTextColor(mRes.getColor(R.color.colorClimateBlowerOff)); 
            } else {
                mFanSpeed.update(1, String.valueOf(mFanSpeedState.ordinal()-1));
                mFanSpeed.setTextColor(mRes.getColor(R.color.colorClimateText)); 
            }  
        }
        public void onPSSeatStatusChanged(int status) throws RemoteException {
            if ( mSeatPS == null ) return; 
            mSeatPSState = SeatState.values()[status]; 
            mSeatPS.update(mSeatPSState.ordinal()); 
        }
        public void onPSTemperatureChanged(float temp) throws RemoteException {
            if ( mTempPS == null ) return; 
            mTempPSState = temp;
            updateTemp(mTempPS, mTempPSState); 
        }
    };
}
