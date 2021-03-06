package com.humaxdigital.automotive.systemui.droplist.controllers;

import android.os.Build; 

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.FrameLayout;
import android.view.MotionEvent;

import com.humaxdigital.automotive.systemui.R;
import com.humaxdigital.automotive.systemui.droplist.SystemControl;
import com.humaxdigital.automotive.systemui.droplist.ui.MenuLayout;
import com.humaxdigital.automotive.systemui.common.util.ProductConfig; 

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; 

public class ControllerManager {
    public interface  Listener {
        void onCloseDropList();
    }

    private static final String TAG = "ControllerManager"; 
    private static final String PACKAGE_NAME = "com.humaxdigital.automotive.systemui"; 

    private final List<BaseController> mControllers = new ArrayList<>();
    private BrightnessController mBrightnessController;
    private Context mContext;
    private View mPanel;
    private Listener mListener;
    private View mCloseButton;
    private View mCheckBoxView; 
    private boolean mCloseBtnPress = false;
    private View mCloseBtnN; 
    private View mCloseBtnP; 

    public ControllerManager(Context context, View view) {
        mContext = Objects.requireNonNull(context);
        mPanel = Objects.requireNonNull(view);
        init();
    }

    public void fetch(SystemControl system) {
        for ( BaseController controller : mControllers ) {
            controller.fetch(system);
        }

        mBrightnessController.fetch(system);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void configurationChange(Context context) {
        mContext = Objects.requireNonNull(context); 
        for ( BaseController controller : mControllers )
            controller.refresh(mContext);
    }

    public void clear() {
        if ( mBrightnessController != null ) mBrightnessController.clear();
    }

    private void init() {
        if ( mContext == null ) return;
        Resources res = mContext.getResources();
        // NOTE: removed wifi 
        /*
        if ( ProductConfig.getFeature() != ProductConfig.FEATURE.AVNT ) {
            mControllers.add(new WifiController()
                    .init(new MenuLayout(mContext)
                        .setText(res.getString(R.string.STR_WI_FI_08_ID))
                        .addIcon(res.getDrawable(R.drawable.dr_btn_wifi_n),
                            res.getDrawable(R.drawable.dr_btn_wifi_p),
                            res.getDrawable(R.drawable.dr_btn_wifi_d)))
                    .setListener(mControlListener));
        }*/
        mControllers.add(new BluetoothController()
                .init(new MenuLayout(mContext)
                    .setText(res.getString(R.string.STR_BLUETOOTH_06_ID))
                    .addIcon(res.getDrawable(R.drawable.dr_btn_bt_n),
                        res.getDrawable(R.drawable.dr_btn_bt_p),
                        res.getDrawable(R.drawable.dr_btn_bt_d)))
                .setListener(mControlListener));
        mControllers.add(new QuietModeController()
                .init(new MenuLayout(mContext)
                    .setText(res.getString(R.string.STR_QUIET_MODE_04_ID))
                    .addIcon(res.getDrawable(R.drawable.dr_btn_quiet_n),
                        res.getDrawable(R.drawable.dr_btn_quiet_p),
                        res.getDrawable(R.drawable.dr_btn_quiet_d)))
                .setListener(mControlListener));
        mControllers.add(new BeepController()
                .init(new MenuLayout(mContext)
                    .setText(res.getString(R.string.STR_BEEP_04_ID))
                    .addIcon(res.getDrawable(R.drawable.dr_btn_beep_n),
                        res.getDrawable(R.drawable.dr_btn_beep_p),
                        res.getDrawable(R.drawable.dr_btn_beep_d))
                    .setSupportLongClick(false)
                    .setSupportTouch(true))); 
        mControllers.add(new MuteController()
                .init(new MenuLayout(mContext)
                    .setText(res.getString(R.string.STR_MUTE_06_ID))
                    .addIcon(res.getDrawable(R.drawable.dr_btn_mute_n),
                        res.getDrawable(R.drawable.dr_btn_mute_p),
                        res.getDrawable(R.drawable.dr_btn_mute_d))
                    .setSupportLongClick(false))); 
        mControllers.add(new ModeController()
                .init(new MenuLayout(mContext)
                    .addIconText(ModeController.Mode.AUTOMATIC.mode(), 
                        res.getDrawable(R.drawable.dr_btn_auto_n),
                        res.getString(R.string.STR_AUTOMATIC_04_ID))
                    .addIconText(ModeController.Mode.DAYLIGHT.mode(), 
                        res.getDrawable(R.drawable.dr_btn_a_day_n),
                        res.getString(R.string.STR_DAYLIGHT_04_ID))
                    .addIconText(ModeController.Mode.NIGHT.mode(), 
                        res.getDrawable(R.drawable.dr_btn_a_night_n),
                        res.getString(R.string.STR_NIGHT_04_ID)))
                .setListener(mControlListener));
         // NOTE: removed wifi 
        //if ( ProductConfig.getFeature() == ProductConfig.FEATURE.AVNT ) {
            mControllers.add(new SetupController()
                    .init(new MenuLayout(mContext)
                        .setText(res.getString(R.string.STR_SETUP_06_ID))
                        .addIcon(res.getDrawable(R.drawable.dr_btn_set_n),
                            res.getDrawable(R.drawable.dr_btn_set_p),
                            res.getDrawable(R.drawable.dr_btn_set_d)))
                    .setListener(mControlListener));
        //}
        mControllers.add(new ThemeController()
                .init(new MenuLayout(mContext)
                    .addIconText(ThemeController.Theme.THEME1.theme(), 
                        res.getDrawable(R.drawable.dr_btn_theme1_n),
                        res.getString(R.string.STR_THEME1_ID))
                    .addIconText(ThemeController.Theme.THEME2.theme(), 
                        res.getDrawable(R.drawable.dr_btn_theme2_n),
                        res.getString(R.string.STR_THEME2_ID))
                    .addIconText(ThemeController.Theme.THEME3.theme(), 
                        res.getDrawable(R.drawable.dr_btn_theme3_n),
                        res.getString(R.string.STR_THEME3_ID)))
                .setListener(mControlListener));
        mControllers.add(new DisplayController()
                .init(new MenuLayout(mContext)
                    .setText(res.getString(R.string.STR_DISPLAY_OFF_01_ID))
                    .addIcon(res.getDrawable(R.drawable.dr_btn_display_n),
                        res.getDrawable(R.drawable.dr_btn_display_p),
                        res.getDrawable(R.drawable.dr_btn_display_d))
                    .setSupportLongClick(false))
                .setListener(mControlListener));

        for ( int i = 0; i<mControllers.size(); i++ ) {
            int resid = mContext.getResources().getIdentifier("menu_"+i,
                    "id", PACKAGE_NAME);
            ((FrameLayout)mPanel.findViewById(resid)).addView(((MenuLayout)mControllers.get(i).getView()).inflate());
        }

        mBrightnessController = new BrightnessController(mContext);
        mBrightnessController.init(mPanel);

        mControllers.add(mBrightnessController);

        mCloseButton = mPanel.findViewById(R.id.close);
        if ( mCloseButton != null ) {
            mCloseButton.setOnClickListener(mCloseListener);
            mCloseButton.setOnTouchListener(mTouchListener); 
        }

        mCloseBtnN = mPanel.findViewById(R.id.close_n); 
        mCloseBtnP = mPanel.findViewById(R.id.close_p); 
    }

    private final View.OnClickListener mCloseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "close button click");
            buttonRelease();
            if ( mListener != null ) mListener.onCloseDropList();
        }
    };

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    buttonPress();
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    buttonRelease();
                    break;
                }
            }
            return false;
        }
    }; 

    private final BaseController.Listener mControlListener = new BaseController.Listener() {
        @Override
        public void onClose() {
            if ( mListener != null ) mListener.onCloseDropList();
        }
    }; 

    private void buttonPress() {
        if ( mCloseBtnPress ) return;
        if ( mCloseBtnN == null || mCloseBtnP == null ) return;
        mCloseBtnPress = true;
        mCloseBtnN.setVisibility(View.INVISIBLE);
        mCloseBtnP.setVisibility(View.VISIBLE);
    }

    private void buttonRelease() {
        if ( !mCloseBtnPress ) return;
        if ( mCloseBtnN == null || mCloseBtnP == null ) return;
        mCloseBtnPress = false;
        mCloseBtnN.setVisibility(View.VISIBLE);
        mCloseBtnP.setVisibility(View.INVISIBLE);
    }
}
