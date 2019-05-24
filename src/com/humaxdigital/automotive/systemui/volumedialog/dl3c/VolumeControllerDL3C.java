package com.humaxdigital.automotive.systemui.volumedialog.dl3c;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.FrameLayout;

import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.humaxdigital.automotive.systemui.R; 

import com.humaxdigital.automotive.systemui.volumedialog.VolumeControllerBase; 
import com.humaxdigital.automotive.systemui.volumedialog.VolumeControlService; 
import com.humaxdigital.automotive.systemui.volumedialog.VolumeUtil; 

public class VolumeControllerDL3C extends VolumeControllerBase {
    private static final String TAG = "VolumeControllerDL3C"; 

    private Context mContext;
    private View mView;

    private TextView mTextVolumeName; 
    private ImageView mImgVolumeIcon; 
    private TextView mTextVolumeValue; 
    private Map<Integer, ImageView> mVolumeSels = new HashMap<>();
    private final int UI_SEL_MAX = 45; 
    private int mIdSellOn = 0; 
    private int mIdSellOff = 0;
    
    private VolumeControlService mController; 
    private int mCurrentVolume = 0; 
    private int mCurrentVolumeMax = 0; 
    private boolean mCurrentMute = false;
    private VolumeUtil.Type mCurrentVolumeType; 

    public VolumeControllerDL3C() { }

    @Override
    public void init(Context context, View view) {
        Log.d(TAG, "init"); 
        mContext = context;
        mView = view;
        initView();
        if ( mContext == null ) return; 
    }

    @Override
    public void deinit() {}

    @Override
    public void fetch(VolumeControlService service) { 
        Log.d(TAG, "fetch"); 
        mController = service; 
        if ( mController == null ) return; 
        mController.registerCallback(mServiceCallback); 
        updateCurrentVolume();
        updateVolumeUI();
    }

    private void updateCurrentVolume() { 
        mCurrentVolumeType = mController.getCurrentVolumeType(); 
        mCurrentVolumeMax = mController.getVolumeMax(mCurrentVolumeType); 
        mCurrentVolume = mController.getVolume(mCurrentVolumeType); 
        mCurrentMute = mController.getCurrentMute(); 
        Log.d(TAG, "updateCurrentVolume:type="+mCurrentVolumeType+", volume="+mCurrentVolume+", mute="+mCurrentMute);
    }

    private void updateVolumeUI() {
        Log.d(TAG, "updateVolumeUI"); 
        if ( mTextVolumeValue != null ) 
            mTextVolumeValue.setText(String.format("%d", mCurrentVolume));
        if ( mTextVolumeName != null ) 
            mTextVolumeName.setText(convertToString(mCurrentVolumeType)); 
        for ( int i = 1; i <= mCurrentVolume; i++ ) 
            mVolumeSels.get(i).setImageResource(mIdSellOn); 
        for ( int i = mCurrentVolume+1; i <= UI_SEL_MAX; i++ ) 
            mVolumeSels.get(i).setImageResource(mIdSellOff); 
    }

    private VolumeControlService.VolumeCallback mServiceCallback = 
        new VolumeControlService.VolumeCallback() {
        @Override
        public void onVolumeChanged(VolumeUtil.Type type, int max, int val) {
            Log.d(TAG, "onVolumeChanged"); 
            updateCurrentVolume();
            updateVolumeUI();
            for ( VolumeChangeListener listener : mListener ) {
                listener.onVolumeDown(convertToType(mCurrentVolumeType), mCurrentVolumeMax, mCurrentVolume);
            }
        }

        @Override
        public void onMuteChanged(VolumeUtil.Type type, int max, int volume, boolean mute) {
            Log.d(TAG, "onMuteChanged"); 
            updateCurrentVolume();
            updateVolumeUI();
            for ( VolumeChangeListener listener : mListener ) {
                listener.onMuteChanged(convertToType(type), mute);
            }
        }
    };

    private VolumeChangeListener.Type convertToType(VolumeUtil.Type type) {
        VolumeChangeListener.Type ret = VolumeChangeListener.Type.UNKNOWN; 
        switch(type) {
            case UNKNOWN: ret = VolumeChangeListener.Type.UNKNOWN; break;  
            case RADIO_FM: ret = VolumeChangeListener.Type.RADIO_FM; break;  
            case RADIO_AM: ret = VolumeChangeListener.Type.RADIO_AM; break;  
            case USB: ret = VolumeChangeListener.Type.USB; break;  
            case ONLINE_MUSIC: ret = VolumeChangeListener.Type.ONLINE_MUSIC; break;  
            case BT_AUDIO: ret = VolumeChangeListener.Type.BT_AUDIO; break;  
            case BT_PHONE_RING: ret = VolumeChangeListener.Type.BT_PHONE_RING; break;  
            case BT_PHONE_CALL: ret = VolumeChangeListener.Type.BT_PHONE_CALL; break;  
            case CARLIFE_MEDIA: ret = VolumeChangeListener.Type.CARLIFE_MEDIA; break;  
            case CARLIFE_NAVI: ret = VolumeChangeListener.Type.CARLIFE_NAVI; break;  
            case CARLIFE_TTS: ret = VolumeChangeListener.Type.CARLIFE_TTS; break;  
            case BAIDU_MEDIA: ret = VolumeChangeListener.Type.BAIDU_MEDIA; break;  
            case BAIDU_ALERT: ret = VolumeChangeListener.Type.BAIDU_ALERT; break;  
            case BAIDU_VR_TTS: ret = VolumeChangeListener.Type.BAIDU_VR_TTS; break;  
            case BAIDU_NAVI: ret = VolumeChangeListener.Type.BAIDU_NAVI; break;  
            case EMERGENCY_CALL: ret = VolumeChangeListener.Type.EMERGENCY_CALL; break;  
            case ADVISOR_CALL: ret = VolumeChangeListener.Type.ADVISOR_CALL; break;  
            case BEEP: ret = VolumeChangeListener.Type.BEEP; break;  
            case WELCOME_SOUND: ret = VolumeChangeListener.Type.WELCOME_SOUND; break;  
            case SETUP_GUIDE: ret = VolumeChangeListener.Type.SETUP_GUIDE; break;  
            default: break; 
        }
        return ret; 
    }

    private String convertToString(VolumeUtil.Type type) {
        String name = "Bluetooth Audio"; 
        switch(type) {
            case UNKNOWN: name = "Unknown"; break;  
            case RADIO_FM: name = "FM Radio"; break;  
            case RADIO_AM: name = "AM Radio"; break;  
            case USB: name = "USB Music"; break;  
            case ONLINE_MUSIC: name = "Online Music"; break;  
            case BT_AUDIO: name = "Bluetooth Audio"; break;  
            case BT_PHONE_RING: name = "Phone Ring"; break;  
            case BT_PHONE_CALL: name = "Phone Call"; break;  
            case CARLIFE_MEDIA: name = "Carlife Media"; break;  
            case CARLIFE_NAVI: name = "Carlife Navigation"; break;  
            case CARLIFE_TTS: name = "Carlife Voice"; break;  
            case BAIDU_MEDIA: name = "Baidu Media"; break;  
            case BAIDU_ALERT: name = "Baidu Alert"; break;  
            case BAIDU_VR_TTS: name = "Baidu VR"; break;  
            case BAIDU_NAVI: name = "Baidu Navigation"; break;  
            case EMERGENCY_CALL: name = "Emergency Call"; break;  
            case ADVISOR_CALL: name = "Advisor Call"; break;  
            case BEEP: name = "Beep Sound"; break;  
            case WELCOME_SOUND: name = "Welcome Sound"; break;  
            case SETUP_GUIDE: name = "Setup Guide"; break;  
        }
        return name; 
    }

    private void initView() {
        if ( mView == null || mContext == null ) return;
        Log.d(TAG, "initView"); 

        mTextVolumeName = mView.findViewById(R.id.text_volume_type);
        mImgVolumeIcon = mView.findViewById(R.id.img_volume_icon);
        mTextVolumeValue = mView.findViewById(R.id.text_volume);

        for ( int i = 1; i <= UI_SEL_MAX; i++ ) {
            int resid = mContext.getResources().getIdentifier("img_sel_"+i, "id", mContext.getPackageName());
            if ( resid < 0 ) continue;
            mVolumeSels.put(i, (ImageView)mView.findViewById(resid)); 
        }

        mIdSellOff = R.drawable.ho_bg_volume_nor; 
        mIdSellOn = R.drawable.ho_bg_volume_sel; 
    }
}
