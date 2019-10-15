package com.humaxdigital.automotive.systemui.volumedialog;

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

public class VolumeController extends VolumeControllerBase {
    private static final String TAG = "VolumeController"; 

    private enum AudioStateIcon {
        AUDIO,
        AUDIO_MUTE,
        BT_AUDIO,
        BT_AUDIO_MUTE,
        BT_PHONE,
        BT_PHONE_MUTE,
        NAVI,
        NAVI_MUTE,
        VR,
        VR_MUTE,
        RINGTONE, 
        RINGTONE_MUTE,
        CARLIFE_AUDIO,
        CARLIFE_AUDIO_MUTE,
        CARLIFE_PHONE,
        CARLIFE_PHONE_MUTE,
        CARLIFE_NAVI,
        CARLIFE_NAVI_MUTE,
        CARLIFE_VR,
        CARLIFE_VR_MUTE,
        CARLIFE_RINGTONE,
        CARLIFE_RINGTONE_MUTE
    }
    private Context mContext;
    private View mView;
    private ImageView mImgPlusN;
    private ImageView mImgPlusS;
    private ImageView mImgMinusN;
    private ImageView mImgMinusS;
    private ImageView mImgVolume;
    private TextView mTextVolume;
    private ProgressBar mProgress;
    private FrameLayout mPlus;
    private FrameLayout mMinus;
    private final int VOLUME_SELECT_TIME = 100;
    private final int VOLUME_LOOP_TIMEOUT = 100; 
    
    private final int PROGRESS_UI_STEP_MAX = 45;
    private int PROGRESS_STEP_MAX = 170;
    private final String MUTE_VALUE_TEXT = "0"; 
    private boolean mVolumeMute = false;
    private UpdateHandler mHandler = new UpdateHandler();

    private Handler mUIHandler; 

    private Timer mTimer = new Timer();
    private TimerTask mVolumeUpTask = null;
    private TimerTask mVolumeDownTask = null;
    private TimerTask mVolumeUpLoopTask = null;
    private TimerTask mVolumeDownLoopTask = null;
    
    private VolumeControlService mController; 
    private int mCurrentVolume = 0; 
    private int mCurrentVolumeMax = 0; 
    private VolumeUtil.Type mCurrentVolumeType; 
    private boolean mIsVolumeUp = false; 

    private Map<Integer, Integer> mVolumeTypeImages = new HashMap<>();

    public VolumeController() { }

    @Override
    public void init(Context context, View view) {
        Log.d(TAG, "VolumeController"); 
        mContext = context;
        mView = view;
        int id_progress_max = mContext.getResources().getIdentifier("progress_max", "integer",  mContext.getPackageName());
        if ( id_progress_max > 0 ) PROGRESS_STEP_MAX = mContext.getResources().getInteger(id_progress_max);
        createViews();
        initViews();
        if ( mContext == null ) return; 
        mUIHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    public void deinit() {}

    @Override
    public void fetch(VolumeControlService service) { 
        Log.d(TAG, "fetch"); 
        mController = service; 
        if ( mController == null ) return; 
        mController.registerCallback(mServiceCallback); 
        mCurrentVolumeType = mController.getCurrentVolumeType(); 
        mCurrentVolumeMax = mController.getVolumeMax(mCurrentVolumeType); 
        mCurrentVolume = mController.getVolume(mCurrentVolumeType); 
        if ( mImgVolume != null ) 
            mImgVolume.setImageResource(convertToVolumeIcon(isVolumeMute(), mCurrentVolumeType)); 
        if ( mTextVolume != null ) 
            mTextVolume.setText(convertToStep(mCurrentVolumeMax, mCurrentVolume));
        if ( mProgress != null ) 
            mProgress.setProgress(convertToProgressValue(mCurrentVolumeMax, mCurrentVolume));
    }

    private void updateMuteState() {
        if ( mController == null ) return;
        mVolumeMute = mController.getCurrentMute(); 
    }

    private VolumeControlService.VolumeCallback mServiceCallback = 
        new VolumeControlService.VolumeCallback() {
        @Override
        public void onVolumeChanged(VolumeUtil.Type type, int max, int val) {
            if ( mUIHandler == null ) return; 
            updateMuteState();
            mCurrentVolumeType = type; 
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                   mImgVolume.setImageResource(convertToVolumeIcon(isVolumeMute(), mCurrentVolumeType)); 
                }
            }); 
            mCurrentVolumeMax = max; 
            if ( mCurrentVolume > val ) { 
                mCurrentVolume = val; 
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        volumeNoUI(mCurrentVolumeType, mCurrentVolumeMax, mCurrentVolume);
                        for ( VolumeChangeListener listener : mListener ) {
                            listener.onVolumeDown(convertToType(mCurrentVolumeType), mCurrentVolumeMax, mCurrentVolume);
                        }
                    }
                }); 
            } 
            else {
                mCurrentVolume = val; 
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        volumeNoUI(mCurrentVolumeType, mCurrentVolumeMax, mCurrentVolume);
                        for ( VolumeChangeListener listener : mListener ) {
                            listener.onVolumeUp(convertToType(mCurrentVolumeType), mCurrentVolumeMax, mCurrentVolume);
                        }
                    }
                }); 
            }
        }

        @Override
        public void onMuteChanged(VolumeUtil.Type type, int max, int volume, boolean mute) {
            updateMuteState();
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ( mImgVolume == null || mTextVolume == null || mProgress == null ) return;
                    mImgVolume.setImageResource(convertToVolumeIcon(mute, type)); 
                    if ( mute ) {
                        mTextVolume.setText(MUTE_VALUE_TEXT);
                        mProgress.setProgress(convertToProgressValue(max, 0));
                    }
                    else {
                        mTextVolume.setText(convertToStep(max, volume));
                        mProgress.setProgress(convertToProgressValue(max, volume));
                    }
                }
            }); 
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

    private void createViews() {
        if ( mView == null ) return;
        Log.d(TAG, "createViews"); 
        mImgPlusN = mView.findViewById(R.id.img_plus_n);
        mImgPlusS = mView.findViewById(R.id.img_plus_s);
        mImgMinusN = mView.findViewById(R.id.img_minus_n);
        mImgMinusS = mView.findViewById(R.id.img_minus_s);
        mImgVolume = mView.findViewById(R.id.img_volume);
        if ( mImgVolume != null ) mImgVolume.setImageResource(R.drawable.co_ic_volume);
        mTextVolume = mView.findViewById(R.id.text_volume);
        mProgress = mView.findViewById(R.id.progress_volume);
        mPlus = mView.findViewById(R.id.icon_plus);
        mMinus = mView.findViewById(R.id.icon_minus);

        mVolumeTypeImages.put(AudioStateIcon.AUDIO.ordinal(), R.drawable.co_ic_volume); 
        mVolumeTypeImages.put(AudioStateIcon.AUDIO_MUTE.ordinal(), R.drawable.co_ic_volume_mute); 
        mVolumeTypeImages.put(AudioStateIcon.BT_AUDIO.ordinal(), R.drawable.co_ic_btaudio); 
        mVolumeTypeImages.put(AudioStateIcon.BT_AUDIO_MUTE.ordinal(), R.drawable.co_ic_btaudio_mute); 
        mVolumeTypeImages.put(AudioStateIcon.BT_PHONE.ordinal(), R.drawable.co_ic_btphone); 
        mVolumeTypeImages.put(AudioStateIcon.BT_PHONE_MUTE.ordinal(), R.drawable.co_ic_btphone_mute); 
        mVolumeTypeImages.put(AudioStateIcon.NAVI.ordinal(), R.drawable.co_ic_navi); 
        mVolumeTypeImages.put(AudioStateIcon.NAVI_MUTE.ordinal(), R.drawable.co_ic_navi_mute); 
        mVolumeTypeImages.put(AudioStateIcon.VR.ordinal(), R.drawable.co_ic_vr); 
        mVolumeTypeImages.put(AudioStateIcon.VR_MUTE.ordinal(), R.drawable.co_ic_vr_mute); 
        mVolumeTypeImages.put(AudioStateIcon.RINGTONE.ordinal(), R.drawable.co_ic_ringtone); 
        mVolumeTypeImages.put(AudioStateIcon.RINGTONE_MUTE.ordinal(), R.drawable.co_ic_ringtone_mute); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_AUDIO.ordinal(), R.drawable.co_ic_cp_volume); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_AUDIO_MUTE.ordinal(), R.drawable.co_ic_cp_volume_mute); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_PHONE.ordinal(), R.drawable.co_ic_cp_phone); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_PHONE_MUTE.ordinal(), R.drawable.co_ic_cp_phone_mute); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_NAVI.ordinal(), R.drawable.co_ic_cp_navi); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_NAVI_MUTE.ordinal(), R.drawable.co_ic_cp_navi_mute); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_VR.ordinal(), R.drawable.co_ic_cp_vr); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_VR_MUTE.ordinal(), R.drawable.co_ic_cp_vr_mute); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_RINGTONE.ordinal(), R.drawable.co_ic_cp_ringtone); 
        mVolumeTypeImages.put(AudioStateIcon.CARLIFE_RINGTONE_MUTE.ordinal(), R.drawable.co_ic_cp_ringtone_mute); 
    }

    private void initViews() {
        Log.d(TAG, "initViews"); 
        if ( mImgPlusN != null ) mImgPlusN.setVisibility(View.VISIBLE);
        if ( mImgPlusS != null ) mImgPlusS.setVisibility(View.INVISIBLE);
        if ( mImgMinusN != null ) mImgMinusN.setVisibility(View.VISIBLE);
        if ( mImgMinusS != null ) mImgMinusS.setVisibility(View.INVISIBLE);
        if ( mImgVolume != null ) mImgVolume.setVisibility(View.VISIBLE);
        if ( mTextVolume != null ) mTextVolume.setText("10");
        if ( mProgress != null ) mProgress.setProgress(20);
        if ( mPlus != null ) {
            mPlus.setOnClickListener(mOnClickListener);
            mPlus.setOnLongClickListener(mOnLongClickListener);
            mPlus.setOnTouchListener(mOnTouchListener);
        }
        if ( mMinus != null ) {
            mMinus.setOnClickListener(mOnClickListener);
            mMinus.setOnLongClickListener(mOnLongClickListener);
            mMinus.setOnTouchListener(mOnTouchListener);
        }
    }

    private void volumeNoUI(VolumeUtil.Type type, int max, int value) {
        Log.d(TAG, "volumeUpNoUI:type="+type+", max="+max+", value="+value); 
        mImgVolume.setImageResource(convertToVolumeIcon(isVolumeMute(), mCurrentVolumeType)); 
        if ( mProgress != null ) mProgress.setProgress(convertToProgressValue(max, value));
        if ( mTextVolume != null ) mTextVolume.setText(convertToStep(max, value));
    }

    private void volumeUp(VolumeUtil.Type type, int max, int value) {
        Log.d(TAG, "volumeUp:type="+type+", max="+max+", value="+value); 
        if ( mVolumeUpTask != null ) {
            mVolumeUpTask.cancel();
            mTimer.purge();
            mVolumeUpTask =  null;
        }
        if ( mVolumeDownTask != null ) {
            mVolumeDownTask.run();
        }

        mVolumeUpTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(UpdateHandler.PLUS_NORMAL_SHOW, 0).sendToTarget();
            }
        };
        mImgPlusS.setVisibility(View.VISIBLE);
        mImgPlusN.setVisibility(View.INVISIBLE);

        mTimer.schedule(mVolumeUpTask, VOLUME_SELECT_TIME);

        mImgVolume.setImageResource(convertToVolumeIcon(isVolumeMute(), mCurrentVolumeType)); 
        mProgress.setProgress(convertToProgressValue(max, value));
        mTextVolume.setText(convertToStep(max, value));
    }

    private void volumeDown(VolumeUtil.Type type, int max, int value) {
        Log.d(TAG, "volumeDown:type="+type+", max="+max+", value="+value); 
        if ( mVolumeDownTask != null ) {
            mVolumeDownTask.cancel();
            mTimer.purge();
            mVolumeDownTask =  null;
        }
        if ( mVolumeUpTask != null ) {
            mVolumeUpTask.run();
        }

        mVolumeDownTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(UpdateHandler.MINUS_NOMAL_SHOW, 0).sendToTarget();
            }
        };

        mImgMinusS.setVisibility(View.VISIBLE);
        mImgMinusN.setVisibility(View.INVISIBLE);

        mTimer.schedule(mVolumeDownTask, VOLUME_SELECT_TIME);

        mImgVolume.setImageResource(convertToVolumeIcon(isVolumeMute(), mCurrentVolumeType)); 
        mProgress.setProgress(convertToProgressValue(max, value));
        mTextVolume.setText(convertToStep(max, value));
    }

    private boolean isVolumeMute() {
        Log.d(TAG, "isVolumeMute="+mVolumeMute);
        return mVolumeMute;
    }

    private int convertToVolumeIcon(boolean mute, VolumeUtil.Type type) {
        Log.d(TAG, "convertToVolumeIcon : mute="+mute+", type="+type);
        int resId = mVolumeTypeImages.get(AudioStateIcon.AUDIO.ordinal()); 
        switch(type) {
            case UNKNOWN:
            case RADIO_FM:
            case RADIO_AM:
            case USB:
            case WELCOME_SOUND: 
            case SETUP_GUIDE:
            case BAIDU_MEDIA: 
            case ONLINE_MUSIC: {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.AUDIO.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.AUDIO_MUTE.ordinal()); 
                break;
            } 
            case BT_AUDIO: {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.BT_AUDIO.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.BT_AUDIO_MUTE.ordinal()); 
                break;
            } 
            case EMERGENCY_CALL: 
            case ADVISOR_CALL: 
            case BT_PHONE_CALL: {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.BT_PHONE.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.BT_PHONE_MUTE.ordinal()); 
                break;
            } 
            case BAIDU_NAVI: {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.NAVI.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.NAVI_MUTE.ordinal()); 
                break;
            } 
            case BAIDU_VR_TTS:  {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.VR.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.VR_MUTE.ordinal()); 
                break;
            } 
            case BT_PHONE_RING: {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.RINGTONE.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.RINGTONE_MUTE.ordinal()); 
                break;
            }
            case CARLIFE_MEDIA: {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.CARLIFE_AUDIO.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.CARLIFE_AUDIO_MUTE.ordinal()); 
                break;
            }
            case CARLIFE_TTS: {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.CARLIFE_VR.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.CARLIFE_VR_MUTE.ordinal()); 
                break;
            }
            case CARLIFE_NAVI: {
                if ( !mute ) resId = mVolumeTypeImages.get(AudioStateIcon.CARLIFE_NAVI.ordinal()); 
                else resId = mVolumeTypeImages.get(AudioStateIcon.CARLIFE_NAVI_MUTE.ordinal()); 
                break;
            }
            case BEEP:
            case BAIDU_ALERT: {
                break;
            }
            default: break;
        }
        return resId; 
    }

    private String convertToStep(int max, int val) {
        float current = (float)(PROGRESS_UI_STEP_MAX*val)/max;  
        String num = String.format("%d", Math.round(current));
        return num;        
    }

    private int convertToProgressValue(int max, int val) {
        float current = (float)(PROGRESS_UI_STEP_MAX*val)/max; 
        float progress = (float)(PROGRESS_STEP_MAX * current)/PROGRESS_UI_STEP_MAX; 
        return Math.round(progress); 
    }

    private final class UpdateHandler extends Handler {
        private static final int PLUS_SELECT_SHOW = 1;
        private static final int PLUS_NORMAL_SHOW = 2;
        private static final int MINUS_SELECT_SHOW = 3;
        private static final int MINUS_NOMAL_SHOW = 4;

        public UpdateHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case PLUS_SELECT_SHOW: updateUIPlusSelect(); break;
                case PLUS_NORMAL_SHOW: updateUIPlusNormal(); break;
                case MINUS_SELECT_SHOW: updateUIMinusSelect(); break;
                case MINUS_NOMAL_SHOW: updateUIMinusNormal(); break;
                default: break;
            }
        }
    }

    private void updateUIPlusSelect() {
        Log.d(TAG, "updateUIPlusSelect");
    }

    private void updateUIPlusNormal() {
        Log.d(TAG, "updateUIPlusNormal");
        mImgPlusS.setVisibility(View.INVISIBLE);
        mImgPlusN.setVisibility(View.VISIBLE);
    }

    private void updateUIMinusNormal() {
        Log.d(TAG, "updateUIMinusNormal");
        mImgMinusS.setVisibility(View.INVISIBLE);
        mImgMinusN.setVisibility(View.VISIBLE);
    }

    private void updateUIMinusSelect() {
        Log.d(TAG, "updateUIMinusSelect");
    }

    private void _volumeUp() {
        int up_volume = mCurrentVolume + 1; 
        if ( up_volume > PROGRESS_UI_STEP_MAX ) {
            up_volume = PROGRESS_UI_STEP_MAX; 
            return;
        }

        if ( !mController.setVolume(mCurrentVolumeType, up_volume) ) return;
        
        mUIHandler.post(new VolumeRunnable(0, up_volume)); 
    }

    private void _volumeDown() {
        int down_volume = mCurrentVolume - 1; 
        if ( down_volume < 0 ) {
            down_volume = 0; 
            return;
        }

        if ( !mController.setVolume(mCurrentVolumeType, down_volume) ) return;

        mUIHandler.post(new VolumeRunnable(1, down_volume)); 
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.icon_plus: {
                    _volumeUp(); 
                    break;
                }
                case R.id.icon_minus: {
                    _volumeDown(); 
                    break;
                } 
                default: break;
            }
        }
    };

    View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.icon_plus: {
                    if ( mVolumeUpLoopTask != null ) {
                        mVolumeUpLoopTask.cancel();
                        mTimer.purge();
                        mVolumeUpLoopTask =  null;
                    }

                    mVolumeUpLoopTask = new TimerTask() {
                        @Override
                        public void run() {
                            _volumeUp(); 
                        }
                    };

                    mTimer.schedule(mVolumeUpLoopTask, 0, VOLUME_LOOP_TIMEOUT);
                    break;
                }
                case R.id.icon_minus: {
                    if ( mVolumeDownLoopTask != null ) {
                        mVolumeDownLoopTask.cancel();
                        mTimer.purge();
                        mVolumeDownLoopTask =  null;
                    }

                    mVolumeDownLoopTask = new TimerTask() {
                        @Override
                        public void run() {
                            _volumeDown(); 
                        }
                    };

                    mTimer.schedule(mVolumeDownLoopTask, 0, VOLUME_LOOP_TIMEOUT);
                    break;
                }
                default: break;
            }
            return true;
        }
    };

    View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch( event.getAction() ) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    if ( mVolumeUpLoopTask != null ) {
                        mVolumeUpLoopTask.cancel();
                        mTimer.purge();
                        mVolumeUpLoopTask =  null;
                    }
                    if ( mVolumeDownLoopTask != null ) {
                        mVolumeDownLoopTask.cancel();
                        mTimer.purge();
                        mVolumeDownLoopTask =  null;
                    }
                }
            }
            return false;
        }
    };

    public class VolumeRunnable implements Runnable {
        private int mVolume = 0;
        private int mMsg = 0; 
        public VolumeRunnable(int msg, int volume) {
          this.mVolume = volume;
          this.mMsg = msg; 
        }
        @Override
        public void run() {
            switch(mMsg) {
                case 0: {
                    volumeUp(mCurrentVolumeType, mCurrentVolumeMax, mVolume); 
                    for ( VolumeChangeListener listener : mListener ) {
                        listener.onVolumeUp(convertToType(mCurrentVolumeType), mCurrentVolumeMax, mVolume);
                    }
                    break;
                }
                case 1: {
                    volumeDown(mCurrentVolumeType, mCurrentVolumeMax, mVolume); 
                    for ( VolumeChangeListener listener : mListener ) {
                        listener.onVolumeDown(convertToType(mCurrentVolumeType), mCurrentVolumeMax, mVolume);
                    }
                    break;
                }
            }
        }
    }
}
