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

import android.util.Log;

import com.humaxdigital.automotive.statusbar.R;

import com.humaxdigital.automotive.statusbar.ProductConfig;
import com.humaxdigital.automotive.statusbar.ui.ClimateMenuImg;
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

    private IStatusBarService mService; 
    private Context mContext;
    private Resources mRes;
    private View mClimate;
    private Handler mHandler; 

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

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DU2 ) {
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mTempDR);
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mFanDirection);
        } else {
            mClimateViews.add(mTempDR);
            mClimateViews.add(mSeatDR);
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mFanDirection);
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
        } catch( RemoteException e ) {
            e.printStackTrace();
        }


        if ( mTempDR != null ) updateTemp(mTempDR, mTempDRState); 
        if ( mSeatDR != null ) mSeatDR.update(mSeatDRState.ordinal()); 
        if ( mAC != null ) mAC.update(mACState.ordinal()); 
        if ( mIntake != null ) mIntake.update(mIntakeState.ordinal()); 
        if ( mFanSpeed != null ) {
            if ( mFanSpeedState == FanSpeedState.STEPOFF ) {
                mFanSpeed.update(0, true, String.valueOf(FanSpeedState.STEP0.ordinal()-1)); 
            } else {
                mFanSpeed.update(0, false, String.valueOf(mFanSpeedState.ordinal()-1));
            }
        }
        if ( mFanDirection != null ) mFanDirection.update(mFanDirectionState.ordinal()); 
        if ( mSeatPS != null ) mSeatPS.update(mSeatPSState.ordinal()); 
        if ( mTempPS != null ) updateTemp(mTempPS, mTempPSState); 
    }

    private void updateTemp(ClimateMenuTextDec view, float temp) {
        // todo : need to check temperatture range ( old and new ) 
        // VENDOR_CANRX_HVAC_TEMPERATURE_NEW_RANGE
        if ( temp < 17.0f ) {
            view.update("LO", ""); 
            return; 
        } 

        if ( temp > 27.0f ) {
            view.update("HI", ""); 
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
        
        mTempDR.updateDisable(disable);
        mSeatDR.updateDisable(disable);
        mAC.updateDisable(disable);
        mIntake.updateDisable(disable);
        mFanSpeed.updateDisable(disable);
        mFanDirection.updateDisable(disable);
        mSeatPS.updateDisable(disable);
        mTempPS.updateDisable(disable);
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
                    if ( mFanSpeedState == FanSpeedState.STEPOFF ) {
                        mFanSpeed.update(0, true, String.valueOf(FanSpeedState.STEP0.ordinal()-1)); 
                    } else {
                        mFanSpeed.update(0, false, String.valueOf(mFanSpeedState.ordinal()-1));
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
