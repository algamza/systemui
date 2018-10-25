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
    private enum ClimateModeState { FLOOR, FACE, FLOOR_FACE, FLOOR_DEFROST }
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
    private boolean mIntakeState = false;
    private ClimateView mClimateModeView;
    private ClimateModeState mClimateModeState = ClimateModeState.FLOOR;
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
            Drawable on = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car, null);
            Drawable off = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_disable, null);
            if ( on != null && off != null ) {
                mIntakeView.addIcon(1, on);
                mIntakeView.addIcon(0, off);
            }

            try {
                if ( mService != null ) mIntakeState = mService.getAirCirculationState();
            } catch( RemoteException e ) {
                e.printStackTrace();
            }

            mIntakeView.update(mIntakeState ? 1 : 0);
            mIntakeView.setListener(new ClimateView.ClickListener() {
                @Override
                public void onClicked(int state) {
                    if ( state == 0 ) {
                        mIntakeView.update(1);
                        mIntakeState = true;
                    } else {
                        mIntakeState = false;
                        mIntakeView.update(0);
                    }
                    
                    try {
                        if ( mService != null ) mService.setAirCirculationState(mIntakeState);
                    } catch( RemoteException e ) {
                        e.printStackTrace();
                    }
                }
            });
        }

        mClimateModeView = mClimate.findViewById(R.id.climate_mode);
        if ( mClimateModeView != null ) {
            Drawable floor = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01, null);
            Drawable face = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02, null);
            Drawable floor_face = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03, null);
            Drawable floor_defrost = ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04, null);
            if ( floor != null && face != null && floor_face != null && floor_defrost != null ) {
                mClimateModeView.addIcon(ClimateModeState.FLOOR.ordinal(), floor);
                mClimateModeView.addIcon(ClimateModeState.FACE.ordinal(), face);
                mClimateModeView.addIcon(ClimateModeState.FLOOR_FACE.ordinal(), floor_face);
                mClimateModeView.addIcon(ClimateModeState.FLOOR_DEFROST.ordinal(), floor_defrost);
                int direction = 0;
                try {
                    if ( mService != null ) direction = mService.getFanDirection();
                } catch( RemoteException e ) {
                    e.printStackTrace();
                }
                mClimateModeState = ClimateModeState.values()[direction];
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
            
            int status = 0; 
            try {
                if ( mService != null ) status = mService.getBlowerSpeed();
            } catch( RemoteException e ) {
                e.printStackTrace();
            }
            mBlowerSpeed = BlowerSpeed.values()[status]; 
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
        public void onAirCirculationChanged(boolean isOn) throws RemoteException {
            if ( mIntakeView == null ) return; 
            mIntakeState = isOn;
            mIntakeView.update(mIntakeState ? 1:0);
        }
        public void onFanDirectionChanged(int direction) throws RemoteException {
            if ( mClimateModeView == null ) return; 
            mClimateModeState = ClimateModeState.values()[direction];
            mClimateModeView.update(mClimateModeState.ordinal());
        }
        public void onBlowerSpeedChanged(int status) throws RemoteException {
            if ( mBlowerText == null ) return;
            mBlowerSpeed = BlowerSpeed.values()[status]; 
            mBlowerText.update(String.valueOf(mBlowerSpeed.ordinal()));
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
