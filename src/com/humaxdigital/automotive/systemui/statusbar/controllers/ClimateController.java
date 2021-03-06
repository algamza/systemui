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

import java.util.Objects; 

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.common.util.ProductConfig;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuImg;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuTextDec;
import com.humaxdigital.automotive.systemui.statusbar.ui.ClimateMenuTextImg;

import com.humaxdigital.automotive.systemui.statusbar.service.StatusBarClimate;

public class ClimateController implements StatusBarClimate.StatusBarClimateCallback {
    private static final String TAG = "ClimateController"; 
    private static final String PACKAGE_NAME = "com.humaxdigital.automotive.systemui"; 

    private enum SeatState { 
        HEATER3(0), HEATER2(1), HEATER1(2), NONE(3), COOLER1(4), COOLER2(5), COOLER3(6); 
        private final int state; 
        SeatState(int state) { this.state = state; }
        public int state() { return state; }
    }
    private enum SeatOption { 
        HEAT_ONLY_2STEP(0), HEAT_ONLY_3STEP(1), VENT_ONLY_2STEP(2), 
        VENT_ONLY_3STEP(3), HEAT_VENT_2STEP(4), HEAT_VENT_3STEP(5), INVALID(6);
        private final int state; 
        SeatOption(int state) { this.state = state; }
        public int state() { return state; }
    }
    private enum FanDirectionState { 
        FACE(0), FLOOR_FACE(1), FLOOR(2), FLOOR_DEFROST(3), DEFROST(4), OFF(5);
        private final int state; 
        FanDirectionState(int state) { this.state = state; }
        public int state() { return state; }
        public int next() { return state+1; }
    }
    private enum FanSpeedState { 
        STEPOFF(0), STEP0(1), STEP1(2), STEP2(3), STEP3(4), 
        STEP4(5), STEP5(6), STEP6(7), STEP7(8), STEP8(9);
        private final int state; 
        FanSpeedState(int state) { this.state = state; }
        public int state() { return state; }
        public int speed() { return state-1; }
    }
    private enum ACState { 
        ON(0), OFF(1);
        private final int state; 
        ACState(int state) { this.state = state; }
        public int state() { return state; }
    }
    private enum IntakeState {         
        ON(0), OFF(1);
        private final int state; 
        IntakeState(int state) { this.state = state;}
        public int state() { return state; } 
    };
    private enum AirCleaning { 
        ON(0), OFF(1);
        private final int state; 
        AirCleaning(int state) { this.state = state;}
        public int state() { return state; } 
    }; 
    private enum SyncState { 
        ON(0), OFF(1);
        private final int state; 
        SyncState(int state) { this.state = state;}
        public int state() { return state; } 
    }; 
    private enum FrontDefogState { 
        ON(0), OFF(1);
        private final int state; 
        FrontDefogState(int state) { this.state = state;}
        public int state() { return state; } 
    }; 
    private enum ClimateType { 
        NONE(0), DEFAULT(1), NO_SEAT(2);  
        private final int state; 
        ClimateType(int state) { this.state = state;}
        public int state() { return state; } 
    }; 

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

    private ClimateMenuImg mSync; 
    private SyncState mSyncState = SyncState.OFF; 

    private FrontDefogState mFrontDefogState = FrontDefogState.OFF; 

    private Boolean mModeOff = false; 
    private Boolean mIGNOn = true; 
    private Boolean mIsOperateOn = false; 
    private Boolean mIsDisable = true; 

    private ContentResolver mContentResolver;
    private ContentObserver mClimateObserver;
    private final String CLIMATE_TYPE_KEY = "com.humaxdigital.automotive.systemui.statusbar.ClimateType";
    private boolean mIsViewInit = false; 

    private View mClimatePanel; 

    private final List<View> mClimateViews = new ArrayList<>();

    public ClimateController(Context context, View view) {
        Log.d(TAG, "ClimateController()"); 
        mContext = Objects.requireNonNull(context);
        mClimate = Objects.requireNonNull(view);
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
        Log.d(TAG, "init()"); 
        mService = Objects.requireNonNull(service); 
        mService.registerClimateCallback(this); 
        if ( mService.isInitialized() ) update(); 
    }

    public void deinit() {
        if ( mService != null ) 
            mService.unregisterClimateCallback(this); 
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
        if ( mSync != null ) mSync.setOnClickListener(mClimateSyncOnClick); 
        if ( ProductConfig.getModel() == ProductConfig.MODEL.CN7C ) {
            if ( mSeatDR != null ) mSeatDR.setOnClickListener(mSeatHeaterDROnClick); 
            if ( mSeatPS != null ) mSeatPS.setOnClickListener(mSeatHeaterPSOnClick); 
        } else {
            if ( mSeatDR != null ) mSeatDR.setOnClickListener(mClimateOnClickWidhoutFanOn); 
            if ( mSeatPS != null ) mSeatPS.setOnClickListener(mClimateOnClickWidhoutFanOn); 
        }
    }
    
    private void removeOnClick() {
        if ( mClimate != null ) mClimate.setOnClickListener(null);
        if ( mAC != null ) mAC.setOnClickListener(null); 
        if ( mIntake != null ) mIntake.setOnClickListener(null); 
        if ( mFanDirection != null ) mFanDirection.setOnClickListener(null); 
        if ( mAirCleaning != null ) mAirCleaning.setOnClickListener(null); 
        if ( mSync != null ) mSync.setOnClickListener(null); 
    }

    private void initView() {
        Log.d(TAG, "initView()"); 
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
        } else if ( ProductConfig.getModel() == ProductConfig.MODEL.NPPE ) {
            mClimatePanel = inflater.inflate(R.layout.climate_no_seat, null); 
        } else {
            mClimatePanel = inflater.inflate(R.layout.climate, null); 
        }

        if ( mClimatePanel == null ) return; 
        ((ViewGroup)mClimate).addView(mClimatePanel); 

        mTempDR = new ClimateMenuTextDec(mContext).inflate(); 
        mSeatDR = new ClimateMenuImg(mContext)
            .addIcon(SeatState.NONE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_00, null))
            .addIcon(SeatState.COOLER1.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_01, null))
            .addIcon(SeatState.COOLER2.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_02, null))
            .addIcon(SeatState.COOLER3.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_03, null))
            .addIcon(SeatState.HEATER1.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_04, null))
            .addIcon(SeatState.HEATER2.state(),  ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_05, null))
            .addIcon(SeatState.HEATER3.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_06, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_left_dis, null))
            .inflate(); 
        mAC = new ClimateMenuImg(mContext)
            .addIcon(ACState.ON.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_ac_on, null))
            .addIcon(ACState.OFF.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_ac_off, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_ac_dis, null))
            .inflate();
        mIntake = new ClimateMenuImg(mContext)
            .addIcon(IntakeState.OFF.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_off, null))
            .addIcon(IntakeState.ON.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_on, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_car_dis, null))
            .inflate();
        mFanSpeed = new ClimateMenuTextImg(mContext)
            .addIcon(0, ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_n, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_dis, null))
            .inflate(); 
        mFanDirection = new ClimateMenuImg(mContext)
            .addIcon(FanDirectionState.FACE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01, null))
            .addIcon(FanDirectionState.FLOOR.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02, null))
            .addIcon(FanDirectionState.FLOOR_FACE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03, null))
            .addIcon(FanDirectionState.FLOOR_DEFROST.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04, null))
            .addIcon(FanDirectionState.DEFROST.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_defog, null))
            .addIcon(FanDirectionState.OFF.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_00_off, null))
            .addDisableIcon(FanDirectionState.FACE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_01_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_02_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR_FACE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_03_dis, null))
            .addDisableIcon(FanDirectionState.FLOOR_DEFROST.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_04_dis, null))
            .addDisableIcon(FanDirectionState.DEFROST.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_defog_dis, null))
            .addDisableIcon(FanDirectionState.OFF.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_wind_00_off_d, null))
            .inflate();
        mSeatPS = new ClimateMenuImg(mContext)
            .addIcon(SeatState.NONE.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_00, null))
            .addIcon(SeatState.COOLER1.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_01, null))
            .addIcon(SeatState.COOLER2.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_02, null))
            .addIcon(SeatState.COOLER3.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_03, null))
            .addIcon(SeatState.HEATER1.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_04, null))
            .addIcon(SeatState.HEATER2.state(),  ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_05, null))
            .addIcon(SeatState.HEATER3.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_06, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_seat_right_dis, null))
            .inflate(); 
        mTempPS = new ClimateMenuTextDec(mContext).inflate(); 

        mAirCleaning = new ClimateMenuImg(mContext)
            .addIcon(AirCleaning.OFF.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_off, null))
            .addIcon(AirCleaning.ON.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_on_02, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_aircleaning_dis, null))
            .inflate();

        mSync = new ClimateMenuImg(mContext)
            .addIcon(SyncState.OFF.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_sync_off, null))
            .addIcon(SyncState.ON.state(), ResourcesCompat.getDrawable(mRes, R.drawable.co_status_sync_on, null))
            .addDisableIcon(ResourcesCompat.getDrawable(mRes, R.drawable.co_status_sync_dis, null))
            .inflate();  

        if ( ProductConfig.getModel() == ProductConfig.MODEL.DU2 
            || ProductConfig.getModel() == ProductConfig.MODEL.DU2EV ) {
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
            mClimateViews.add(mSync); 
            mClimateViews.add(mTempPS);
        }
        else if ( ProductConfig.getModel() == ProductConfig.MODEL.NPPE ) {
            mClimateViews.add(mAC);
            mClimateViews.add(mIntake);
            mClimateViews.add(mTempDR);
            mClimateViews.add(mFanSpeed);
            mClimateViews.add(mFanDirection);
            mClimateViews.add(mAirCleaning); 
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
        
        Log.d(TAG, "update()"); 

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
        mSyncState = mService.getSyncState() ? SyncState.ON:SyncState.OFF; 
        mModeOff = mService.isModeOff(); 
        mFrontDefogState = FrontDefogState.values()[mService.getFrontDefogState()]; 

        if ( mTempDR != null ) updateTemp(mTempDR, mTempDRState); 
        if ( mSeatDR != null ) mSeatDR.update(mSeatDRState.state()); 
        if ( mAC != null ) updateAC();
        if ( mIntake != null ) mIntake.update(mIntakeState.state()); 
        if ( mFanDirection != null ) updateFanDirection();
        if ( mSeatPS != null ) mSeatPS.update(mSeatPSState.state()); 
        if ( mTempPS != null ) updateTemp(mTempPS, mTempPSState); 
        if ( mAirCleaning != null ) mAirCleaning.update(mAirCleaningState.state()); 
        if ( mSync != null ) mSync.update(mSyncState.state()); 
        if ( mFanSpeed != null ) {
            if ( isClimateOff() ) updateClimate(false); 
            else {
                updateFanSpeed(mFanSpeedState);
                updateClimate(true); 
            }
        }
        
        updateIGOnChange(mIGNOn); 
        updateOperateOnChange(mIsOperateOn); 
        updateModeOff(mModeOff); 
    }

    private boolean isClimateOff() {
        return mFanSpeedState == FanSpeedState.STEPOFF ? true:false; 
    }

    private void updateClimate(boolean on) {
        Log.d(TAG, "updateClimate="+on);
        if ( on ) updateFanSpeed(mFanSpeedState); 
        else updateFanSpeed(FanSpeedState.STEP0); 
        updateTempOn(on); 
        updateModeOff(!on);
        updateAC();
    }

    private void updateAC() {
        Log.d(TAG, "updateAC()"); 
        if ( mAC == null ) return;
        if ( isClimateOff() ) mAC.update(ACState.OFF.state()); 
        else mAC.update(mACState.state()); 
    }

    private void updateFanDirection() {
        if ( mFanDirection == null ) return;
        Log.d(TAG, "updateFanDirection="+mFanDirectionState); 
        if ( mModeOff || isClimateOff() ) {
            mFanDirection.update(FanDirectionState.OFF.state()); 
        } else {
            if ( mFrontDefogState == FrontDefogState.ON ) 
                mFanDirection.update(FanDirectionState.DEFROST.state()); 
            else 
                mFanDirection.update(mFanDirectionState.state()); 
        }
    }

    private void updateFanSpeed(FanSpeedState state) {
        if ( mFanSpeed == null ) return;
        Log.d(TAG, "updateFanSpeed="+state); 
        mFanSpeed.update(0, String.valueOf(state.speed()));
    }

    private void fanOn() {
        Log.d(TAG, "fanOn()"); 
        if ( (mFanSpeed != null) 
            && ( isClimateOff() || (mFanSpeedState == FanSpeedState.STEP0) ) ) {
            if ( mService != null )
                mService.setBlowerSpeed(FanSpeedState.STEP1.state());
        }
    }

    private ClimateType getClimateType() {
        ClimateType _type = ClimateType.NO_SEAT; 
        if ( mContentResolver == null ) return _type;
        int type = Settings.Global.getInt(mContentResolver, CLIMATE_TYPE_KEY, 2);
        _type = ClimateType.values()[type]; 
        Log.d(TAG, "getClimateType="+_type); 
        return _type; 
    }

    private void setClimateType(ClimateType type) {
        if ( mContentResolver == null ) return;
        Log.d(TAG, "setClimateType="+type); 
        Settings.Global.putInt(mContentResolver, CLIMATE_TYPE_KEY, type.state()); 
    }

    private void updateClimateType() {
        if ( mService == null ) ; 

        int ps_seat_op = mService.getPSSeatOption(); 
        int dr_seat_op = mService.getDRSeatOption();
        
        Log.d(TAG, "updateClimateType : ps-option="+ps_seat_op+", dr-option="+dr_seat_op); 

        if ( SeatOption.values()[ps_seat_op] == SeatOption.INVALID 
            || SeatOption.values()[dr_seat_op] == SeatOption.INVALID ) {
            // setClimateType(ClimateType.NO_SEAT); 
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
        if ( !mTempOn && isClimateOff() ) {
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
        if ( mSync != null ) mSync.updateDisable(disable);
    } 

    private View.OnClickListener mClimateOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if ( !mIGNOn || mIsOperateOn || mIsDisable ) return; 
            fanOn(); 
            openClimateSetting();
        }
    }; 

    private View.OnClickListener mClimateOnClickWidhoutFanOn = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if ( !mIGNOn || mIsOperateOn || mIsDisable ) return; 
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
            
            int next = 0; 

            if ( !isClimateOff() ) next = mFanDirectionState.next();
            else next = mFanDirectionState.state();

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
                    mService.setAirCleaningState(AirCleaning.ON.state());
                } else {
                    mService.setAirCleaningState(AirCleaning.OFF.state());
                }
            }
        }
    }; 

    private final View.OnClickListener mClimateSyncOnClick = new View.OnClickListener() { 
        @Override
        public void onClick(View v) {
            if ( mSync == null || !mIGNOn || mIsOperateOn ) return; 
            if ( mService != null ) {
                if ( mSyncState == SyncState.OFF ) {
                    mService.setSyncState(true);
                } else {
                    mService.setSyncState(false);
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
            mService.setDRSeatStatus(next.state());
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
            mService.setPSSeatStatus(next.state());
        }
    }; 


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
        Log.d(TAG, "onDRTemperatureChanged="+temp);
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
        Log.d(TAG, "onDRSeatStatusChanged="+status);
        mSeatDRState = SeatState.values()[status]; 
        if ( mHandler == null ) return; 
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSeatDR.update(mSeatDRState.state());
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
        Log.d(TAG, "onAirCirculationChanged="+isOn);
        mIntakeState = isOn?IntakeState.ON:IntakeState.OFF;
        if ( mHandler == null ) return; 
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mIntake.update(mIntakeState.state());
            }
        }); 
    }
    @Override
    public void onAirConditionerChanged(boolean isOn) {
        if ( mAC == null ) return; 
        Log.d(TAG, "onAirConditionerChanged="+isOn);
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
        Log.d(TAG, "onAirCleaningChanged="+status);
        mAirCleaningState = AirCleaning.values()[status]; 
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if ( mAirCleaning != null ) 
                    mAirCleaning.update(mAirCleaningState.state()); 
            }
        }); 
    }
    @Override
    public void onSyncChanged(boolean sync) {
        if ( mSync == null ) return; 
        Log.d(TAG, "onSyncChanged="+sync);
        mSyncState = sync ? SyncState.ON:SyncState.OFF; 
        
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if ( mSync != null ) 
                    mSync.update(mSyncState.state()); 
            }
        }); 
    }
    @Override
    public void onFanDirectionChanged(int direction) {
        if ( mFanDirection == null ) return; 
        Log.d(TAG, "onFanDirectionChanged="+direction);
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
        Log.d(TAG, "onBlowerSpeedChanged="+status);
        mFanSpeedState = FanSpeedState.values()[status]; 
        if ( mHandler == null ) return; 
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if ( isClimateOff() ) updateClimate(false); 
                else {
                    updateFanSpeed(mFanSpeedState);
                    updateClimate(true);
                }
            }
        }); 
    }
    @Override
    public void onPSSeatStatusChanged(int status) {
        Log.d(TAG, "onPSSeatStatusChanged="+status);
        if ( mSeatPS == null ) return; 
        mSeatPSState = SeatState.values()[status]; 
        if ( mHandler == null ) return; 
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSeatPS.update(mSeatPSState.state()); 
            }
        }); 
    }
    @Override
    public void onPSSeatOptionChanged(int option) {
        Log.d(TAG, "onPSSeatStatusChanged="+option);
        updateClimateType();
    }

    @Override
    public void onPSTemperatureChanged(float temp) {
        Log.d(TAG, "onPSTemperatureChanged="+temp);
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
        Log.d(TAG, "onFrontDefogStatusChanged="+state);
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
        Log.d(TAG, "onModeOffChanged="+off);
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
        Log.d(TAG, "onIGNOnChanged="+on);
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
        Log.d(TAG, "onOperateOnChanged="+on);
        if ( mHandler == null ) return; 

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateOperateOnChange(on); 
            }
        }); 
    }
}
