package com.humaxdigital.automotive.systemui.statusbar.controllers;

import android.content.Context;
import android.content.res.Resources;
import android.content.ContentResolver;
import android.provider.Settings;
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
import android.database.ContentObserver;
import android.net.Uri;
import android.os.UserHandle;
import android.extension.car.settings.CarExtraSettings;
import android.util.Log; 

import com.humaxdigital.automotive.systemui.R;

import com.humaxdigital.automotive.systemui.common.util.ProductConfig;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuImg;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuImgTimeout;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuTextDec;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuTextImg;

import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarClimate;

public class ClimateController {
    private static final String TAG = "ClimateController"; 
    private static final String PACKAGE_NAME = "com.humaxdigital.automotive.systemui"; 

    private enum SeatState { HEATER3, HEATER2, HEATER1, NONE, COOLER1, COOLER2, COOLER3 }
    private enum SeatOption { HEAT_ONLY_2STEP, HEAT_ONLY_3STEP, VENT_ONLY_2STEP, VENT_ONLY_3STEP, HEAT_VENT_2STEP, HEAT_VENT_3STEP, INVALID }
    private enum FanDirectionState { FACE, FLOOR_FACE, FLOOR, FLOOR_DEFROST, DEFROST, OFF }
    private enum FanSpeedState { STEPOFF, STEP0, STEP1, STEP2, STEP3, STEP4, STEP5, STEP6, STEP7, STEP8 };
    private enum ACState { ON, OFF };
    private enum IntakeState { ON, OFF };
    private enum AirCleaning { ON, OFF }; 
    private enum FrontDefogState { ON, OFF }; 
    private enum ClimateType { NONE, DEFAULT, NO_SEAT }; 

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

    private ClimateMenuImg mSeatDR = null;
    private SeatState mSeatDRState = SeatState.NONE;
    private ClimateMenuImg mSeatPS = null;
    private SeatState mSeatPSState = SeatState.NONE;
    private ArrayList<SeatState> mSeatHeatToggle = new ArrayList<>(); 

    private SeatOption mSeatDROption = SeatOption.INVALID; 
    private SeatOption mSeatPSOption = SeatOption.INVALID; 

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
    private Boolean mClimateOn = false; 

    private ContentResolver mContentResolver;
    private ContentObserver mClimateObserver;
    private final String CLIMATE_TYPE_KEY = "com.humaxdigital.automotive.systemui.statusbar.ClimateType";
    private boolean mIsViewInit = false; 

    private View mClimatePanel; 

    private final List<View> mClimateViews = new ArrayList<>();

    public ClimateController(Context context, View view) {
        if ( view == null || context == null ) return;
        mContext = context;
        mClimate = view;
        mRes = mContext.getResources();
        mHandler = new Handler(mContext.getMainLooper());
        mContentResolver = mContext.getContentResolver();
        mClimateObserver = createClimateObserver(); 
        mContentResolver.registerContentObserver(
            Settings.Global.getUriFor(CLIMATE_TYPE_KEY), 
            false, mClimateObserver, UserHandle.USER_CURRENT); 
        initToggleValue(); 
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

    private void initToggleValue() {
        mSeatHeatToggle.add(SeatState.NONE); 
        mSeatHeatToggle.add(SeatState.HEATER3); 
        mSeatHeatToggle.add(SeatState.HEATER2); 
        mSeatHeatToggle.add(SeatState.HEATER1); 
    }

    private ContentObserver createClimateObserver() {
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if ( mHandler == null ) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initView();
                        update(); 
                    }
                });
            }
        };
        return observer; 
    }

    private ClimateType getClimateType() {
        ClimateType _type = ClimateType.NONE; 
        if ( mContentResolver == null ) return _type;
        int type = 0; 
        try {
            type = Settings.Global.getInt(mContentResolver, CLIMATE_TYPE_KEY);
        } catch(Settings.SettingNotFoundException e) {
            Log.e(TAG, "error : " + e ); 
        }
        _type = ClimateType.values()[type]; 
        Log.d(TAG, "getClimateType="+_type); 
        return _type; 
    }

    private void openClimateSetting() {
        if ( mService == null ) return; 
        mService.openClimateSetting();
    }

    private void addOnClick() {
        if ( mClimate != null ) mClimate.setOnClickListener(mClimateOnClick);
        if ( mAC != null ) mAC.setOnClickListener(mClimateACOnClick); 
        if ( mIntake != null ) mIntake.setOnClickListener(mClimateIntakeOnClick); 
        if ( mFanDirection != null ) mFanDirection.setOnClickListener(mClimateFanDirectionOnClick); 
        if ( mAirCleaning != null ) mAirCleaning.setOnClickListener(mClimateAirCleaningOnClick); 
        if ( ProductConfig.getModel() == ProductConfig.MODEL.CN7C ) {
            if ( mSeatDR != null ) mSeatDR.setOnClickListener(mSeatHeaterDROnClick); 
            if ( mSeatPS != null ) mSeatPS.setOnClickListener(mSeatHeaterPSOnClick); 
        }
    }
    
    private void removeOnClick() {
        if ( mClimate != null ) mClimate.setOnClickListener(null);
        if ( mAC != null ) mAC.setOnClickListener(null); 
        if ( mIntake != null ) mIntake.setOnClickListener(null); 
        if ( mFanDirection != null ) mFanDirection.setOnClickListener(null); 
        if ( mAirCleaning != null ) mAirCleaning.setOnClickListener(null); 
    }

    private void initView() {
        if ( mClimate == null || mContext == null ) return;

        if ( mClimatePanel != null ) {
            removeOnClick();
            mClimateViews.clear();
            if ( mClimate != null ) 
                ((ViewGroup)mClimate).removeAllViews(); 
            mClimatePanel = null; 
        }

        ClimateType type = getClimateType(); 
        if ( type == ClimateType.NONE ) return;

        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if ( inflater == null ) return; 
        mIsViewInit = true;
        boolean support_seat = true; 
        if ( type == ClimateType.NO_SEAT ) support_seat = false; 

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DN8C 
            || ProductConfig.getModel() == ProductConfig.MODEL.CN7C ) {
            if ( support_seat ) mClimatePanel = inflater.inflate(R.layout.climate, null); 
            else mClimatePanel = inflater.inflate(R.layout.climate_no_seat, null); 
        } else {
            mClimatePanel = inflater.inflate(R.layout.climate, null); 
        }

        if ( mClimatePanel == null ) return; 
        ((ViewGroup)mClimate).addView(mClimatePanel); 

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
        mIntake = new ClimateMenuImg(mContext)
            .addIcon(IntakeState.OFF.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_off, null))
            .addIcon(IntakeState.ON.ordinal(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_on, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_dis, null))
            .inflate();
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

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DU2 ) {
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mTempDR);
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mFanDirection);
            mClimateViews.add(mAirCleaning);
        } 
        else if ( ProductConfig.getModel() == ProductConfig.MODEL.CN7C ) {
            if ( support_seat ) {
                mClimateViews.add(mSeatDR);
                mClimateViews.add(mAC);
                mClimateViews.add(mIntake);
                mClimateViews.add(mTempDR);
                mClimateViews.add(mFanSpeed);
                mClimateViews.add(mFanDirection);
                mClimateViews.add(mAirCleaning); 
                mClimateViews.add(mSeatPS);
            } else {
                mClimateViews.add(mAC);
                mClimateViews.add(mIntake);
                mClimateViews.add(mTempDR);
                mClimateViews.add(mFanSpeed);
                mClimateViews.add(mFanDirection);
                mClimateViews.add(mAirCleaning); 
            }
        } 
        else if ( ProductConfig.getModel() == ProductConfig.MODEL.DL3C ) {
            mClimateViews.add(mTempDR);
            mClimateViews.add(mFanDirection);
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mAirCleaning); 
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mTempPS);
        }
        else if ( ProductConfig.getModel() == ProductConfig.MODEL.DN8C ) {
            if ( support_seat ) {
                mClimateViews.add(mTempDR);
                mClimateViews.add(mSeatDR);
                mClimateViews.add(mAC);
                mClimateViews.add(mIntake);
                mClimateViews.add(mFanSpeed);
                mClimateViews.add(mFanDirection);
                mClimateViews.add(mAirCleaning); 
                mClimateViews.add(mSeatPS);
                mClimateViews.add(mTempPS);
            } else {
                mClimateViews.add(mTempDR);
                mClimateViews.add(mFanDirection);
                mClimateViews.add(mAC);
                mClimateViews.add(mIntake);
                mClimateViews.add(mAirCleaning); 
                mClimateViews.add(mFanSpeed);
                mClimateViews.add(mTempPS);
            }
        } else {
            mClimateViews.add(mTempDR);
            mClimateViews.add(mFanDirection);
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mAirCleaning); 
            mClimateViews.add(mFanSpeed);
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

    private void update() {
        if ( mService == null ) return; 

        updateClimateType();

        if ( !mIsViewInit ) {
            mIsViewInit = true; 
            initView(); 
        }

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
        if ( mAC != null ) updateAC();
        if ( mIntake != null ) mIntake.update(mIntakeState.ordinal()); 
        if ( mFanDirection != null ) updateFanDirection();
        if ( mSeatPS != null ) mSeatPS.update(mSeatPSState.ordinal()); 
        if ( mTempPS != null ) updateTemp(mTempPS, mTempPSState); 
        if ( mAirCleaning != null ) mAirCleaning.update(mAirCleaningState.ordinal()); 
        if ( mFanSpeed != null ) {
            if ( mFanSpeedState == FanSpeedState.STEPOFF ) climateOff();
            else {
                updateFanSpeed(mFanSpeedState);
                climateOn();
            }
        }
        
        updateIGOnChange(mIGNOn); 
        updateOperateOnChange(mIsOperateOn); 
        updateModeOff(mModeOff); 
    }

    private boolean isClimateOn() {
        Log.d(TAG, "isClimateOn="+mClimateOn); 
        return mClimateOn; 
    }

    private void climateOn() {
        Log.d(TAG, "climateOn="+mClimateOn); 
        mClimateOn = true; 
        updateFanSpeed(mFanSpeedState); 
        updateTempOn(true); 
        updateModeOff(false);
        updateAC();
    }

    private void climateOff() {
        Log.d(TAG, "climateOff="+mClimateOn); 
        mClimateOn = false; 
        updateFanSpeed(FanSpeedState.STEP0); 
        updateTempOn(false); 
        updateModeOff(true);
        updateAC();
    }

    private void updateAC() {
        Log.d(TAG, "updateAC:climateon="+mClimateOn); 
        if ( mAC == null ) return;
        if ( !mClimateOn ) mAC.update(ACState.OFF.ordinal()); 
        else mAC.update(mACState.ordinal()); 
    }

    private void updateFanDirection() {
        if ( mFanDirection == null ) return;
        if ( mModeOff || !mClimateOn ) {
            mFanDirection.update(FanDirectionState.OFF.ordinal()); 
        } else {
            if ( mFrontDefogState == FrontDefogState.ON ) 
                mFanDirection.update(FanDirectionState.DEFROST.ordinal()); 
            else 
                mFanDirection.update(mFanDirectionState.ordinal()); 
        }
    }

    private void updateFanSpeed(FanSpeedState state) {
        if ( mFanSpeed == null ) return;
        mFanSpeed.update(0, false, String.valueOf(state.ordinal()-1));
    }

    private void fanOn() {
        if ( (mFanSpeed != null) 
            && ( (mFanSpeedState == FanSpeedState.STEPOFF) 
            || (mFanSpeedState == FanSpeedState.STEP0) ) ) {
            if ( mService != null )
                mService.setBlowerSpeed(FanSpeedState.STEP1.ordinal());
        }
    }

    private void setClimateType(ClimateType type) {
        if ( mContentResolver == null ) return;
        Log.d(TAG, "setClimateType="+type); 
        Settings.Global.putInt(mContentResolver, CLIMATE_TYPE_KEY, type.ordinal()); 
    }

    private void updateClimateType() {
        if ( mService == null ) ; 

        int ps_seat_op = mService.getPSSeatOption(); 
        int dr_seat_op = mService.getDRSeatOption();
        
        Log.d(TAG, "updateClimateType : ps-option="+ps_seat_op+", dr-option="+dr_seat_op); 

        if ( SeatOption.values()[ps_seat_op] == SeatOption.INVALID 
            || SeatOption.values()[dr_seat_op] == SeatOption.INVALID ) {
            setClimateType(ClimateType.NO_SEAT); 
        } else {
            setClimateType(ClimateType.DEFAULT); 
        }
    }

    private void updateTempOn(boolean on) {
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
        if ( mIsOperateOn ) return;   
        mModeOff = off;     
        updateFanDirection();
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
        Log.d(TAG, "updateDisable="+disable); 

        if ( disable ) removeOnClick(); 
        else addOnClick(); 
        
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

    private View.OnClickListener mClimateOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if ( !mIGNOn || mIsOperateOn || mIsDisable ) return; 
            fanOn(); 
            openClimateSetting();
        }
    }; 

    private View.OnClickListener mClimateACOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mAC == null || !mIGNOn ) return; 

            if ( mService != null ) 
                mService.setAirConditionerState(mACState==ACState.ON?false:true);

        }
    }; 

    private final View.OnClickListener mClimateIntakeOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mIntake == null || !mIGNOn ) return; 
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
                next = 0; 
            }
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
                    mService.setAirCleaningState(AirCleaning.ON.ordinal());
                } else {
                    mService.setAirCleaningState(AirCleaning.OFF.ordinal());
                }
            }
        }
    }; 

    private final View.OnClickListener mSeatHeaterDROnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mService == null || !mIGNOn ) return; 
            SeatState next = mSeatHeatToggle.get(0); 
            boolean is_next = false; 
            for ( SeatState state : mSeatHeatToggle ) {
                if ( is_next ) {
                    next = state;
                    break; 
                } 
                if ( state == mSeatDRState ) is_next = true; 
   
            }
            mService.setDRSeatStatus(next.ordinal());
        }
    }; 

    private final View.OnClickListener mSeatHeaterPSOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mService == null || !mIGNOn ) return; 
            SeatState next = mSeatHeatToggle.get(0); 
            boolean is_next = false; 
            for ( SeatState state : mSeatHeatToggle ) {
                if ( is_next ) {
                    next = state;
                    break; 
                }
                if ( state == mSeatPSState ) is_next = true; 
            }
            mService.setPSSeatStatus(next.ordinal());
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
        public void onDRSeatOptionChanged(int option) {
            updateClimateType();
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
                    updateAC();
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
                    updateFanDirection();
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
                    if ( mFanSpeedState == FanSpeedState.STEPOFF ) climateOff();
                    else {
                        updateFanSpeed(mFanSpeedState);
                        climateOn();
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
        @Override
        public void onPSSeatOptionChanged(int option) {
            updateClimateType();
        }

        @Override
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

        @Override
        public void onFrontDefogStatusChanged(int state) {
            mFrontDefogState = FrontDefogState.values()[state]; 
            if ( mHandler == null ) return; 
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateFanDirection();
                }
            }); 
        }

        @Override
        public void onModeOffChanged(boolean off) {
            if ( mHandler == null ) return; 

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateModeOff(off); 
                }
            }); 
        }

        @Override
        public void onIGNOnChanged(boolean on) {
            if ( mHandler == null ) return; 

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateIGOnChange(on); 
                }
            }); 
        }

        @Override
        public void onOperateOnChanged(boolean on) {
            if ( mHandler == null ) return; 

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateOperateOnChange(on); 
                }
            }); 
        }

        @Override
        public void onRearCameraOn(boolean on) {
            // Not implement
        }
    };
}
