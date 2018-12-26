package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.RemoteException;
import android.os.Handler;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import com.humaxdigital.automotive.statusbar.R;

import com.humaxdigital.automotive.statusbar.ProductConfig;
import com.humaxdigital.automotive.statusbar.ui.ClimateMenuImg;
import com.humaxdigital.automotive.statusbar.ui.ClimateMenuImgTimeout;
import com.humaxdigital.automotive.statusbar.ui.ClimateMenuTextDec;
import com.humaxdigital.automotive.statusbar.ui.ClimateMenuTextImg;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.IClimateCallback; 

public class ClimateController implements BaseController {
    static final String TAG = "ClimateController"; 
    static final String PACKAGE_NAME = "com.humaxdigital.automotive.statusbar"; 

    private enum SeatState { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
    private enum FanDirectionState { FACE, FLOOR, FLOOR_FACE, FLOOR_DEFROST, DEFROST }
    private enum FanSpeedState { STEPOFF, STEP0, STEP1, STEP2, STEP3, STEP4, STEP5, STEP6, STEP7, STEP8 };
    private enum ACState { ON, OFF };
    private enum IntakeState { ON, OFF };
    private enum AirCleaning { ON, OFF, GREEN, RED }; 

    private IStatusBarService mService; 
    private Context mContext;
    private Resources mRes;
    private View mClimate;
    private Handler mHandler; 

    private ClimateMenuTextDec mTempDR;
    private float mTempDRState = 0.0f;
    private ClimateMenuTextDec mTempPS;
    private float mTempPSState = 0.0f;
    private Boolean mTempOn = true; 

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

    private ClimateMenuImgTimeout mAirCleaning; 
    private AirCleaning mAirCleaningState = AirCleaning.OFF; 

    private Boolean mIGNOn = false; 

    private final List<View> mClimateViews = new ArrayList<>();


    public ClimateController(Context context, View view) {
        if ( view == null || context == null ) return;
        mContext = context;
        mClimate = view;
        mRes = mContext.getResources();
        mHandler = new Handler(mContext.getMainLooper());
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
                if ( mIGNOn ) return; 
                openClimateSetting(); 
            }
        });

        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if ( inflater == null ) return; 
        View climate = null; 
        if ( ProductConfig.getModel() == ProductConfig.MODEL.DU2 ) 
            climate = inflater.inflate(R.layout.du2_climate, null); 
         else if ( ProductConfig.getModel() == ProductConfig.MODEL.DN8C ) 
            climate = inflater.inflate(R.layout.dn8c_climate, null); 
         else 
         climate = inflater.inflate(R.layout.dn8c_climate, null); 
        
        if ( climate == null ) return; 
        ((ViewGroup)mClimate).addView(climate); 

        mTempDR = new ClimateMenuTextDec(mContext).inflate(); 
        mSeatDR = new ClimateMenuImg(mContext)
            .addIcon(SeatState.NONE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_00, null))
            .addIcon(SeatState.COOLER1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_01, null))
            .addIcon(SeatState.COOLER2.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_02, null))
            .addIcon(SeatState.COOLER3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_03, null))
            .addIcon(SeatState.HEATER1.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_04, null))
            .addIcon(SeatState.HEATER2.ordinal(),  ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_05, null))
            .addIcon(SeatState.HEATER3.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_06, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_dis, null))
            .inflate(); 
        mAC = new ClimateMenuImg(mContext)
            .addIcon(ACState.ON.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_ac_on, null))
            .addIcon(ACState.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_ac_off, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_ac_dis, null))
            .inflate();
        mAC.setOnClickListener(mClimateACOnClick); 
        mIntake = new ClimateMenuImg(mContext)
            .addIcon(IntakeState.ON.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_on, null))
            .addIcon(IntakeState.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_off, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_dis, null))
            .inflate();
        mIntake.setOnClickListener(mClimateIntakeOnClick); 
        mFanSpeed = new ClimateMenuTextImg(mContext)
            .addIcon(0, ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_n, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_dis, null))
            .inflate(); 
        mFanDirection = new ClimateMenuImg(mContext)
            .addIcon(FanDirectionState.FACE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01, null))
            .addIcon(FanDirectionState.FLOOR.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02, null))
            .addIcon(FanDirectionState.FLOOR_FACE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03, null))
            .addIcon(FanDirectionState.FLOOR_DEFROST.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04, null))
            .addIcon(FanDirectionState.DEFROST.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_defog, null))
            .addDisableIcon(FanDirectionState.FACE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR_FACE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR_DEFROST.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04_dis, null))
            .addDisableIcon(FanDirectionState.DEFROST.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_defog_dis, null))
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
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_dis, null))
            .inflate(); 
        mTempPS = new ClimateMenuTextDec(mContext).inflate(); 

        int red_timeout = mContext.getResources().getIdentifier("climate_aircleaning_red_timeout", "integer", PACKAGE_NAME); 
        int green_timeout = mContext.getResources().getIdentifier("climate_aircleaning_green_timeout", "integer", PACKAGE_NAME); 
        mAirCleaning = new ClimateMenuImgTimeout(mContext)
            .addIcon(AirCleaning.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_off, null), 0)
            .addIcon(AirCleaning.RED.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_on_01, null), 
                red_timeout > 0 ? mContext.getResources().getInteger(red_timeout):0)
            .addIcon(AirCleaning.GREEN.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_on_02, null), 
                green_timeout > 0 ? mContext.getResources().getInteger(green_timeout):0)
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_dis, null))
            .registTimeoutListener(mClimateAirCleaningImgTimeout)
            .inflate(); 
        mAirCleaning.setOnClickListener(mClimateAirCleaningOnClick); 

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DU2 ) {
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mTempDR);
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mFanDirection);
            mClimateViews.add(mAirCleaning);
        } else {
            mClimateViews.add(mTempDR);
            mClimateViews.add(mSeatDR);
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mFanDirection);
            mClimateViews.add(mAirCleaning); 
            mClimateViews.add(mSeatPS);
            mClimateViews.add(mTempPS);
        }

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
            mAirCleaningState = AirCleaning.values()[mService.getAirCleaningState()]; 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }


        if ( mTempDR != null ) updateTemp(mTempDR, mTempDRState); 
        if ( mSeatDR != null ) mSeatDR.update(mSeatDRState.ordinal()); 
        if ( mAC != null ) mAC.update(mACState.ordinal()); 
        if ( mIntake != null ) mIntake.update(mIntakeState.ordinal()); 
        if ( mFanDirection != null ) mFanDirection.update(mFanDirectionState.ordinal()); 
        if ( mSeatPS != null ) mSeatPS.update(mSeatPSState.ordinal()); 
        if ( mTempPS != null ) updateTemp(mTempPS, mTempPSState); 
        if ( mFanSpeed != null ) {
            if ( mFanSpeedState == FanSpeedState.STEPOFF || 
                    mFanSpeedState == FanSpeedState.STEP0) {
                mFanSpeed.update(0, true, String.valueOf(FanSpeedState.STEP0.ordinal()-1)); 
                updateTempOn(false); 
            } else {
                mFanSpeed.update(0, false, String.valueOf(mFanSpeedState.ordinal()-1));
                updateTempOn(true); 
            }
        }
        if ( mAirCleaning != null ) {
            if ( mAirCleaningState == AirCleaning.ON ) {
                mAirCleaningState = AirCleaning.RED; 
            }
            mAirCleaning.update(mAirCleaningState.ordinal()); 
        }
    }

    private void updateTempOn(boolean on) {
        if ( mTempOn == on ) return; 
        mTempOn = on; 
        if ( mTempDR != null ) updateTemp(mTempDR, mTempDRState); 
        if ( mTempPS != null ) updateTemp(mTempPS, mTempPSState); 
    }

    private void updateTemp(ClimateMenuTextDec view, float temp) { 
        if ( mContext == null ) return; 

        if ( !mTempOn ) {
            view.update(mContext.getResources().getString(R.string.temp_off)); 
            return; 
        }
        // todo : need to check temperatture range ( old and new ) 
        // VENDOR_CANRX_HVAC_TEMPERATURE_NEW_RANGE
        if ( temp < 17.0f ) {
            view.update(mContext.getResources().getString(R.string.temp_lo)); 
            return; 
        } 

        if ( temp > 27.0f ) {
            view.update(mContext.getResources().getString(R.string.temp_hi)); 
            return; 
        }

        String state = String.valueOf(temp);
        if ( state.contains(".") ) {
            String tem = state.substring(0, state.indexOf(".") );
            String dec = state.substring(state.indexOf("."), state.length());
            view.update(tem, dec); 
        } else 
            view.update(state, ".0"); 
    }

    private void updateIGOnChange(boolean on) {
        boolean disable = on; 
        
        if ( mTempDR != null ) mTempDR.updateDisable(disable);
        if ( mSeatDR != null ) mSeatDR.updateDisable(disable);
        if ( mAC != null ) mAC.updateDisable(disable);
        if ( mIntake != null ) mIntake.updateDisable(disable);
        if ( mFanSpeed != null ) mFanSpeed.updateDisable(disable);
        if ( mFanDirection != null ) mFanDirection.updateDisable(disable);
        if ( mSeatPS != null ) mSeatPS.updateDisable(disable);
        if ( mTempPS != null ) mTempPS.updateDisable(disable);
        if ( mAirCleaning != null ) mAirCleaning.updateDisable(disable); 
    } 

    private View.OnClickListener mClimateACOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mAC == null || mIGNOn ) return; 
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
            if ( mIntake == null || mIGNOn ) return; 
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
            if ( mFanDirection == null || mIGNOn ) return; 
            /*
            try {
                if ( mService != null ) {
                    if ( FanDirectionState.values()[mService.getFanDirection()] == 
                        FanDirectionState.DEFROST ) {
                        return; 
                    }
                }
            } catch( RemoteException e ) {
                e.printStackTrace();
            }
            */

            int next = mFanDirectionState.ordinal() + 1;
            if ( next >= (FanDirectionState.values().length-1) ) 
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

    private final View.OnClickListener mClimateAirCleaningOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mAirCleaning == null || mIGNOn ) return; 
            if ( mAirCleaningState == AirCleaning.ON ||
                mAirCleaningState == AirCleaning.RED || 
                mAirCleaningState == AirCleaning.GREEN ) {
                mAirCleaningState = AirCleaning.OFF; 
                mAirCleaning.update(mAirCleaningState.ordinal());
            } else if ( mAirCleaningState == AirCleaning.OFF ) {
                mAirCleaningState = AirCleaning.RED; 
                mAirCleaning.update(mAirCleaningState.ordinal());
            }

            try {
                if ( mService != null ) {
                    if ( mAirCleaningState == AirCleaning.RED ) {
                        mService.setAirCleaningState(AirCleaning.ON.ordinal());
                    } else if ( mAirCleaningState == AirCleaning.OFF ) {
                        mService.setAirCleaningState(mAirCleaningState.ordinal());
                    }
                }
            } catch( RemoteException e ) {
                e.printStackTrace();
            }
        }
    }; 

    private final ClimateMenuImgTimeout.ClimateDrawableTimout mClimateAirCleaningImgTimeout = 
        new ClimateMenuImgTimeout.ClimateDrawableTimout() {
        @Override
        public void onDrawableTimout(int status) {
            if ( mAirCleaning == null ) return; 
            if ( status == AirCleaning.RED.ordinal() ) {
                mAirCleaningState = AirCleaning.GREEN; 
            } else if ( status == AirCleaning.GREEN.ordinal() ) {
                mAirCleaningState = AirCleaning.OFF; 
                try {
                    if ( mService != null ) {
                        mService.setAirCleaningState(mAirCleaningState.ordinal());
                    }
                } catch( RemoteException e ) {
                    e.printStackTrace();
                }
            }
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAirCleaning.update(mAirCleaningState.ordinal()); 
                }
            }); 
        }
    }; 

    private final IClimateCallback.Stub mClimateCallback = new IClimateCallback.Stub() {
        public void onDRTemperatureChanged(float temp) throws RemoteException {
            if ( mTempDR == null ) return; 
            mTempDRState = temp;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateTemp(mTempDR, mTempDRState); 
                }
            }); 
        }
        public void onDRSeatStatusChanged(int status) throws RemoteException {
            if ( mSeatDR == null ) return;
            mSeatDRState = SeatState.values()[status]; 
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mSeatDR.update(mSeatDRState.ordinal());
                }
            }); 
        }
        public void onAirCirculationChanged(boolean isOn) throws RemoteException {
            if ( mIntake == null ) return; 
            mIntakeState = isOn?IntakeState.ON:IntakeState.OFF;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mIntake.update(mIntakeState.ordinal());
                }
            }); 
        }
        public void onAirConditionerChanged(boolean isOn) throws RemoteException {
            if ( mAC == null ) return; 
            mACState = isOn?ACState.ON:ACState.OFF;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAC.update(mACState.ordinal());
                }
            });    
        }
        public void onAirCleaningChanged(int status) throws RemoteException {
            if ( mAirCleaning == null ) return; 
            mAirCleaningState = AirCleaning.values()[status]; 
            if ( mAirCleaningState == AirCleaning.ON ) mAirCleaningState = AirCleaning.RED; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAirCleaning.update(mAirCleaningState.ordinal()); 
                }
            }); 
        }
        public void onFanDirectionChanged(int direction) throws RemoteException {
            if ( mFanDirection == null ) return; 
            mFanDirectionState = FanDirectionState.values()[direction]; 
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mFanDirection.update(mFanDirectionState.ordinal()); 
                }
            }); 
        }
        public void onBlowerSpeedChanged(int status) throws RemoteException {
            if ( mFanSpeed == null ) return; 
            mFanSpeedState = FanSpeedState.values()[status]; 
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ( (mFanSpeedState == FanSpeedState.STEPOFF) || 
                        mFanSpeedState == FanSpeedState.STEP0) {
                        mFanSpeed.update(0, true, String.valueOf(FanSpeedState.STEP0.ordinal()-1)); 
                        updateTempOn(false); 
                    } else {
                        mFanSpeed.update(0, false, String.valueOf(mFanSpeedState.ordinal()-1));
                        updateTempOn(true); 
                    } 
                }
            }); 
        }
        public void onPSSeatStatusChanged(int status) throws RemoteException {
            if ( mSeatPS == null ) return; 
            mSeatPSState = SeatState.values()[status]; 
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mSeatPS.update(mSeatPSState.ordinal()); 
                }
            }); 
        }
        public void onPSTemperatureChanged(float temp) throws RemoteException {
            if ( mTempPS == null ) return; 
            mTempPSState = temp;
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateTemp(mTempPS, mTempPSState); 
                }
            }); 
        }

        public void onIGNOnChanged(boolean on) throws RemoteException {
            if ( mHandler == null ) return; 

            mIGNOn = on; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateIGOnChange(mIGNOn); 
                }
            }); 
        }
    };
}
