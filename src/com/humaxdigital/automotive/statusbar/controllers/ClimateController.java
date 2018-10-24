package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.os.RemoteException;

import com.humaxdigital.automotive.statusbar.R;

import com.humaxdigital.automotive.statusbar.ui.ClimateText;
import com.humaxdigital.automotive.statusbar.ui.ClimateView;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.IClimateCallback; 

public class ClimateController implements BaseController {
    private enum SeatState { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
    private enum IntakeState { FRE, REC }
    private enum ClimateModeState { MODE1, MODE2, MODE3, MODE4 }
    private enum BlowerSpeed { STEP1, STEP2, STEP3, STEP4, STEP5, STEP6, STEP7, STEP8 };

    private IStatusBarService mService; 
    private Context mContext;
    private Resources mRes;
    private View mClimate;

    private ClimateText mTempDRIntText;
    private ClimateText mTempDRDecText;
    private float mTempDR = 0.0f;
    private ClimateView mSeatDRView;
    private SeatState mSeatDRState = SeatState.NONE;
    private ClimateView mIntakeView;
    private IntakeState mIntakeState = IntakeState.FRE;
    private ClimateView mClimateModeView;
    private ClimateModeState mClimateModeState = ClimateModeState.MODE1;
    private ClimateView mBlowerView;
    private ClimateText mBlowerText;
    private BlowerSpeed mBlowerSpeed = BlowerSpeed.STEP1;
    private ClimateView mSeatPSView;
    private SeatState mSeatPSState = SeatState.NONE;
    private ClimateText mTempPSIntText;
    private ClimateText mTempPSDecText;
    private float mTempPS = 0.0f;


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
    }

    @Override
    public void deinit() {
        try {
            if ( mService != null ) mService.unregisterClimateCallback(mClimateCallback); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {

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
        if ( mClimate == null ) return;
        mClimate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openClimateSetting(); 
            }
        });

        mTempDRIntText = mClimate.findViewById(R.id.climate_temp_dr_int);
        mTempDRDecText = mClimate.findViewById(R.id.climate_temp_dr_dec);
        if ( mTempDRIntText != null && mTempDRDecText != null ) {
            try {
                if ( mService != null ) mTempDR = mService.getDRTemperature();
            } catch( RemoteException e ) {
                e.printStackTrace();
            }

            updateUITemp(mTempDRIntText, mTempDRDecText, mTempDR);
        }

        mSeatDRView = mClimate.findViewById(R.id.climate_seat_dr);
        if ( mSeatDRView != null ) {
            Drawable none = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_00, null);
            Drawable cooler1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_01, null);
            Drawable cooler2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_02, null);
            Drawable cooler3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_03, null);
            Drawable heater1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_04, null);
            Drawable heater2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_05, null);
            Drawable heater3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_06, null);
            if ( none != null && cooler1 != null && cooler2 != null &&
                    cooler3 != null && heater1 != null && heater2 != null && heater3 != null ) {
                mSeatDRView.addIcon(SeatState.NONE.ordinal(), none);
                mSeatDRView.addIcon(SeatState.COOLER1.ordinal(), cooler1);
                mSeatDRView.addIcon(SeatState.COOLER2.ordinal(), cooler2);
                mSeatDRView.addIcon(SeatState.COOLER3.ordinal(), cooler3);
                mSeatDRView.addIcon(SeatState.HEATER1.ordinal(), heater1);
                mSeatDRView.addIcon(SeatState.HEATER2.ordinal(), heater2);
                mSeatDRView.addIcon(SeatState.HEATER3.ordinal(), heater3);
            }

            int status = 0; 
            try {
                if ( mService != null ) status = mService.getDRSeatStatus();
            } catch( RemoteException e ) {
                e.printStackTrace();
            }
            mSeatDRState = SeatState.values()[status]; 
            mSeatDRView.update(mSeatDRState.ordinal());
        }

        mIntakeView = mClimate.findViewById(R.id.climate_intake);
        if ( mIntakeView != null ) {
            Drawable rec = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car, null);
            Drawable fre = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_disable, null);
            if ( rec != null && fre != null ) {
                mIntakeView.addIcon(IntakeState.FRE.ordinal(), fre);
                mIntakeView.addIcon(IntakeState.REC.ordinal(), rec);
            }

            // todo : get climate intake state
            mIntakeState = IntakeState.FRE;

            mIntakeView.update(mIntakeState.ordinal());
            mIntakeView.setListener(new ClimateView.ClickListener() {
                @Override
                public void onClicked(int state) {
                    // todo : changed climate intake mode
                    if ( state == IntakeState.FRE.ordinal() ) {
                        mIntakeState = IntakeState.REC;
                    } else if ( state == IntakeState.REC.ordinal() ) {
                        mIntakeState = IntakeState.FRE;
                    }
                    mIntakeView.update(mIntakeState.ordinal());
                }
            });
        }

        mClimateModeView = mClimate.findViewById(R.id.climate_mode);
        if ( mClimateModeView != null ) {
            Drawable mode1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01, null);
            Drawable mode2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02, null);
            Drawable mode3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03, null);
            Drawable mode4 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04, null);
            if ( mode1 != null && mode2 != null && mode3 != null && mode4 != null ) {
                mClimateModeView.addIcon(ClimateModeState.MODE1.ordinal(), mode1);
                mClimateModeView.addIcon(ClimateModeState.MODE2.ordinal(), mode2);
                mClimateModeView.addIcon(ClimateModeState.MODE3.ordinal(), mode3);
                mClimateModeView.addIcon(ClimateModeState.MODE4.ordinal(), mode4);

                // todo : get climate mode state
                mClimateModeState = ClimateModeState.MODE1;

                mClimateModeView.update(mClimateModeState.ordinal());
            }
        }

        mBlowerView = mClimate.findViewById(R.id.climate_blower_img);
        mBlowerText = mClimate.findViewById(R.id.climate_blower_text);
        if ( mBlowerView != null && mBlowerText != null ) {
            Drawable img = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind, null);
            if ( img != null ) {
                mBlowerView.addIcon(0, img);
                mBlowerView.update(0);
            }

            // todo : get climate blower speed
            mBlowerSpeed = BlowerSpeed.STEP8;

            mBlowerText.update(String.valueOf(mBlowerSpeed.ordinal()));
        }

        mSeatPSView = mClimate.findViewById(R.id.climate_seat_ps);
        if ( mSeatPSView != null ) {
            Drawable none = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_00, null);
            Drawable cooler1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_01, null);
            Drawable cooler2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_02, null);
            Drawable cooler3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_03, null);
            Drawable heater1 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_04, null);
            Drawable heater2 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_05, null);
            Drawable heater3 = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_06, null);
            if ( none != null && cooler1 != null && cooler2 != null &&
                    cooler3 != null && heater1 != null && heater2 != null && heater3 != null ) {
                mSeatPSView.addIcon(SeatState.NONE.ordinal(), none);
                mSeatPSView.addIcon(SeatState.COOLER1.ordinal(), cooler1);
                mSeatPSView.addIcon(SeatState.COOLER2.ordinal(), cooler2);
                mSeatPSView.addIcon(SeatState.COOLER3.ordinal(), cooler3);
                mSeatPSView.addIcon(SeatState.HEATER1.ordinal(), heater1);
                mSeatPSView.addIcon(SeatState.HEATER2.ordinal(), heater2);
                mSeatPSView.addIcon(SeatState.HEATER3.ordinal(), heater3);
            }

            int status = 0; 
            try {
                if ( mService != null ) status = mService.getPSSeatStatus();
            } catch( RemoteException e ) {
                e.printStackTrace();
            }
            
            mSeatPSState = SeatState.values()[status]; 
            mSeatPSView.update(mSeatPSState.ordinal());
        }

        mTempPSIntText = mClimate.findViewById(R.id.climate_temp_ps_int);
        mTempPSDecText = mClimate.findViewById(R.id.climate_temp_ps_dec);
        if ( mTempPSIntText != null && mTempPSDecText != null ) {
            try {
                if ( mService != null ) mTempPS = mService.getPSTemperature();
            } catch( RemoteException e ) {
                e.printStackTrace();
            }

            updateUITemp(mTempPSIntText, mTempPSDecText, mTempPS);
        }
    }

    private void updateUITemp(ClimateText tempInt, ClimateText tempDec, float temp) {
        String state = String.valueOf(temp);
        if ( state.contains(".") ) {
            String tem = state.substring(0, state.indexOf(".") );
            String dec = state.substring(state.indexOf("."), state.length());
            tempInt.update(tem);
            tempDec.update(dec);
        } else {
            tempInt.update(state);
            tempDec.update(".0");
        }
    }

    private final IClimateCallback.Stub mClimateCallback = new IClimateCallback.Stub() {
        public void onDRTemperatureChanged(float temp) throws RemoteException {
            mTempDR = temp;
            updateUITemp(mTempDRIntText, mTempDRDecText, mTempDR);
        }
        public void onDRSeatStatusChanged(int status) throws RemoteException {
            if ( mSeatDRView == null ) return;
            mSeatDRState = SeatState.values()[status]; 
            mSeatDRView.update(mSeatDRState.ordinal());
        }
        public void onIntakeStatusChanged(int status) throws RemoteException {
        }
        public void onClimateModeChanged(int status) throws RemoteException {
        }
        public void onBlowerSpeedChanged(int status) throws RemoteException {
        }
        public void onPSSeatStatusChanged(int status) throws RemoteException {
            if ( mSeatPSView == null ) return;
            mSeatPSState = SeatState.values()[status]; 
            mSeatPSView.update(mSeatPSState.ordinal());
        }
        public void onPSTemperatureChanged(float temp) throws RemoteException {
            mTempPS = temp;
            updateUITemp(mTempPSIntText, mTempPSDecText, mTempPS);
        }
    };
}
