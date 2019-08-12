package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Handler;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import android.provider.Settings;
import android.extension.car.settings.CarExtraSettings;
import android.util.Log; 

import com.humaxdigital.automotive.systemui.R;

import com.humaxdigital.automotive.systemui.util.ProductConfig;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuImg;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuImgTimeout;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuTextDec;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuTextImg;

import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarClimate;

public class ClimateController {
    static final String TAG = "ClimateController"; 
    static final String PACKAGE_NAME = "com.humaxdigital.automotive.systemui"; 

    private enum SeatState { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
    private enum FanDirectionState { FACE, FLOOR_FACE, FLOOR, FLOOR_DEFROST, DEFROST, OFF }
    private enum FanSpeedState { STEPOFF, STEP0, STEP1, STEP2, STEP3, STEP4, STEP5, STEP6, STEP7, STEP8 };
    private enum ACState { ON, OFF };
    private enum IntakeState { ON, OFF };
    private enum AirCleaning { ON, OFF }; 
    private enum FrontDefogState { ON, OFF }; 

    private StatusBarClimate mService; 
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

    private ClimateMenuImg mAirCleaning; 
    private AirCleaning mAirCleaningState = AirCleaning.OFF; 

    private FrontDefogState mFrontDefogState = FrontDefogState.OFF; 

    private Boolean mModeOff = false; 
    private Boolean mIGNOn = true; 
    private Boolean mIsOperateOn = false; 
    private Boolean mIsDisable = true; 

    private final List<View> mClimateViews = new ArrayList<>();

    public ClimateController(Context context, View view) {
        if ( view == null || context == null ) return;
        mContext = context;
        mClimate = view;
        mRes = mContext.getResources();
        mHandler = new Handler(mContext.getMainLooper());
        initView();
    }

    public void init(StatusBarClimate service) {
        mService = service; 
        if ( mService != null ) {
            mService.registerClimateCallback(mClimateCallback); 
            if ( mService.isInitialized() ) update(); 
        }
    }

    public void deinit() {
        if ( mService != null ) mService.unregisterClimateCallback(mClimateCallback); 
    }

    private void openClimateSetting() {
        if ( mService == null ) return; 
        mService.openClimateSetting();
    }

    private void initView() {
        if ( mClimate == null || mContext == null ) return;
        mClimate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( !mIGNOn || mIsOperateOn || mIsDisable ) return; 
                fanOn(); 
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
        else if ( ProductConfig.getModel() == ProductConfig.MODEL.CN7C )
            climate = inflater.inflate(R.layout.cn7c_climate, null); 
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
            .addIcon(IntakeState.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_off, null))
            .addIcon(IntakeState.ON.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_on, null))
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
            .addIcon(FanDirectionState.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_00_off, null))
            .addDisableIcon(FanDirectionState.FACE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR_FACE.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR_DEFROST.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04_dis, null))
            .addDisableIcon(FanDirectionState.DEFROST.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_defog_dis, null))
            .addDisableIcon(FanDirectionState.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_00_off_d, null))
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

        mAirCleaning = new ClimateMenuImg(mContext)
            .addIcon(AirCleaning.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_off, null))
            .addIcon(AirCleaning.ON.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_on_02, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_dis, null))
            .inflate(); 
        mAirCleaning.setOnClickListener(mClimateAirCleaningOnClick); 

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DU2 ) {
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mTempDR);
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mFanDirection);
            mClimateViews.add(mAirCleaning);
        } 
        else if ( ProductConfig.getModel() == ProductConfig.MODEL.CN7C ) {
            mClimateViews.add(mSeatDR);
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mTempDR);
            mClimateViews.add(mAirCleaning); 
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mFanDirection);
            mClimateViews.add(mSeatPS);
        } 
        else {
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

        updateTempOn(false); 
        updateDisable(true);
    }

    private void fanOn() {
        if ( (mFanSpeed != null) 
            && ( (mFanSpeedState == FanSpeedState.STEPOFF) 
            || (mFanSpeedState == FanSpeedState.STEP0) ) ) {
            if ( mService != null )
                mService.setBlowerSpeed(FanSpeedState.STEP1.ordinal());
        }
    }

    private void update() {
        if ( mService == null ) return; 

        updateDisable(false);

        mIGNOn = mService.getIGNStatus() == 1 ? true:false;
        mIsOperateOn = mService.isOperateOn(); 
        mTempDRState = mService.getDRTemperature();
        mSeatDRState = SeatState.values()[mService.getDRSeatStatus()]; 
        mACState = mService.getAirConditionerState() ? ACState.ON:ACState.OFF; 
        mIntakeState = mService.getAirCirculationState() ? IntakeState.ON:IntakeState.OFF; 
        mFanSpeedState = FanSpeedState.values()[mService.getBlowerSpeed()]; 
        mFanDirectionState = FanDirectionState.values()[mService.getFanDirection()]; 
        mSeatPSState = SeatState.values()[mService.getPSSeatStatus()]; 
        mTempPSState = mService.getPSTemperature();
        mAirCleaningState = AirCleaning.values()[mService.getAirCleaningState()]; 
        mModeOff = mService.isModeOff(); 
        mFrontDefogState = FrontDefogState.values()[mService.getFrontDefogState()]; 

        if ( mTempDR != null ) updateTemp(mTempDR, mTempDRState); 
        if ( mSeatDR != null ) mSeatDR.update(mSeatDRState.ordinal()); 
        if ( mAC != null ) mAC.update(mACState.ordinal()); 
        if ( mIntake != null ) mIntake.update(mIntakeState.ordinal()); 
        if ( mFanDirection != null ) {
            if ( mFrontDefogState == FrontDefogState.ON ) {
                mFanDirection.update(FanDirectionState.DEFROST.ordinal()); 
            } else {
                mFanDirection.update(mFanDirectionState.ordinal());
            } 
        }
        if ( mSeatPS != null ) mSeatPS.update(mSeatPSState.ordinal()); 
        if ( mTempPS != null ) updateTemp(mTempPS, mTempPSState); 
        if ( mFanSpeed != null ) {
            if ( mFanSpeedState == FanSpeedState.STEPOFF ) {
                mFanSpeed.update(0, false, String.valueOf(FanSpeedState.STEP0.ordinal()-1)); 
                updateTempOn(false); 
            } else {
                mFanSpeed.update(0, false, String.valueOf(mFanSpeedState.ordinal()-1));
                updateTempOn(true); 
            }
        }
        if ( mAirCleaning != null ) mAirCleaning.update(mAirCleaningState.ordinal()); 
        
        updateIGOnChange(mIGNOn); 
        updateOperateOnChange(mIsOperateOn); 
        updateModeOff(mModeOff); 
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

    private void updateModeOff(boolean off) {
        Log.d(TAG, "updateModeOff="+off);
        if ( mIsOperateOn || !mIGNOn ) return;   
        if ( mModeOff == off ) return; 
        mModeOff = off; 
        if ( mFanDirection == null ) return;
        if ( mModeOff ) {
            mFanDirection.update(FanDirectionState.OFF.ordinal()); 
        } else {
            if ( mFrontDefogState == FrontDefogState.ON ) 
                mFanDirection.update(FanDirectionState.DEFROST.ordinal()); 
            else 
                mFanDirection.update(mFanDirectionState.ordinal()); 
        }
    }

    private void updateIGOnChange(boolean on) {
        Log.d(TAG, "updateIGOnChange="+on); 
        mIGNOn = on; 
        boolean disable = !mIGNOn; 
        if ( !disable && mIsOperateOn ) return;
        updateDisable(disable); 
    } 

    private void updateOperateOnChange(boolean on) {
        Log.d(TAG, "updateOperateOnChange="+on); 
        mIsOperateOn = on; 
        boolean disable = mIsOperateOn; 
        if ( !disable && !mIGNOn ) return;
        updateDisable(disable); 
    } 

    private void updateDisable(boolean disable) {
        mIsDisable = disable; 
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
            if ( mAC == null || !mIGNOn ) return; 
            if ( mACState == ACState.ON ) {
                //mACState = ACState.OFF; 
                //mAC.update(mACState.ordinal());
            } else {
                //mACState = ACState.ON; 
                //mAC.update(mACState.ordinal());
            }

            if ( mService != null ) 
                mService.setAirConditionerState(mACState==ACState.ON?false:true);

        }
    }; 

    private final View.OnClickListener mClimateIntakeOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mIntake == null || !mIGNOn ) return; 
            if ( mIntakeState == IntakeState.ON ) {
                //mIntakeState = IntakeState.OFF; 
                //mIntake.update(mIntakeState.ordinal());
            } else {
                //mIntakeState = IntakeState.ON; 
                //mIntake.update(mIntakeState.ordinal());
            }

            if ( mService != null ) 
                mService.setAirCirculationState(mIntakeState==IntakeState.ON?false:true);
        }
    }; 

    private final View.OnClickListener mClimateFanDirectionOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mFanDirection == null || !mIGNOn ) return; 

            int next = mFanDirectionState.ordinal() + 1;

            if ( next >= (FanDirectionState.values().length-2) ) {
                //mFanDirectionState = FanDirectionState.values()[0];
                next = 0; 
            }
            else {
                //mFanDirectionState = FanDirectionState.values()[next];
            }
                
            //mFanDirection.update(mFanDirectionState.ordinal()); 

            if ( mService != null ) 
                mService.setFanDirection(next);
        }
    }; 

    private final View.OnClickListener mClimateAirCleaningOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mAirCleaning == null || !mIGNOn || mIsOperateOn ) return; 
            if ( mService != null ) {
                if ( mAirCleaningState == AirCleaning.OFF ) {
                    //mAirCleaningState = AirCleaning.ON;
                    //mAirCleaning.update(mAirCleaningState.ordinal()); 
                    mService.setAirCleaningState(AirCleaning.ON.ordinal());
                } else {
                    //mAirCleaningState = AirCleaning.OFF;
                    //mAirCleaning.update(mAirCleaningState.ordinal()); 
                    mService.setAirCleaningState(AirCleaning.OFF.ordinal());
                }
            }
        }
    }; 

    private final StatusBarClimate.StatusBarClimateCallback mClimateCallback 
        = new StatusBarClimate.StatusBarClimateCallback() {
        @Override
        public void onInitialized() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    update(); 
                }
            }); 
        }
        @Override
        public void onDRTemperatureChanged(float temp) {
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
        @Override
        public void onDRSeatStatusChanged(int status) {
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
        @Override
        public void onAirCirculationChanged(boolean isOn) {
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
        @Override
        public void onAirConditionerChanged(boolean isOn) {
            if ( mAC == null ) return; 
            mACState = isOn?ACState.ON:ACState.OFF;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAC.update(mACState.ordinal());
                }
            });    
        }
        @Override
        public void onAirCleaningChanged(int status) {
            if ( mAirCleaning == null ) return; 
            mAirCleaningState = AirCleaning.values()[status]; 
            
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ( mAirCleaning != null ) 
                        mAirCleaning.update(mAirCleaningState.ordinal()); 
                }
            }); 
        }
        @Override
        public void onFanDirectionChanged(int direction) {
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
        @Override
        public void onBlowerSpeedChanged(int status) {
            if ( mFanSpeed == null ) return; 
            mFanSpeedState = FanSpeedState.values()[status]; 
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ( mFanSpeedState == FanSpeedState.STEPOFF ) {
                        mFanSpeed.update(0, false, String.valueOf(FanSpeedState.STEP0.ordinal()-1)); 
                        updateTempOn(false); 
                    } else {
                        mFanSpeed.update(0, false, String.valueOf(mFanSpeedState.ordinal()-1));
                        updateTempOn(true); 
                    } 
                }
            }); 
        }
        @Override
        public void onPSSeatStatusChanged(int status) {
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
        public void onPSTemperatureChanged(float temp) {
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

        public void onFrontDefogStatusChanged(int state) {
            mFrontDefogState = FrontDefogState.values()[state]; 
            if ( mHandler == null ) return; 
            if ( mFrontDefogState == FrontDefogState.ON ) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFanDirection.update(FanDirectionState.DEFROST.ordinal()); 
                    }
                }); 
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFanDirection.update(mFanDirectionState.ordinal()); 
                    }
                }); 
            }
        }

        public void onModeOffChanged(boolean off) {
            if ( mHandler == null ) return; 

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateModeOff(off); 
                }
            }); 
        }

        public void onIGNOnChanged(boolean on) {
            if ( mHandler == null ) return; 

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateIGOnChange(on); 
                }
            }); 
        }

        public void onOperateOnChanged(boolean on) {
            if ( mHandler == null ) return; 

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateOperateOnChange(on); 
                }
            }); 
        }

        public void onRearCameraOn(boolean on) {
            // Not implement
        }
    };
}
