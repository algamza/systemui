package com.humaxdigital.automotive.systemui.droplist.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.StateListDrawable;

import com.humaxdigital.automotive.systemui.R;

import java.util.HashMap;

public class MenuLayout extends LinearLayout {
    public interface MenuListener {
        boolean onClick();
        boolean onLongClick();
    }

    public interface MenuTouchListener {
        boolean onTouch(MotionEvent event);
    }

    private enum ButtonState {
        ENABLE,
        PRESS,
        DISABLE
    }

    private MenuListener mListener;
    private MenuTouchListener mTouchListener; 
    private Context mContext;

    private Drawable mResIcon = null;
    private HashMap<Integer,Drawable> mToggleIcons = new HashMap<>();
    private int mStatus = 0;
    private HashMap<ButtonState,Drawable> mButtonIcon = new HashMap<>();
    private boolean mEnable = true;
    private HashMap<Integer,String> mTexts = new HashMap<>();
    private String mText;
    private boolean mSupportLongClick = true;
    private boolean mSupportClick = true;  
    private boolean mSupportTouch = false; 
    private ImageView mIconBG;
    private ImageView mIcon; 
    private TextView mViewText;
    

    public MenuLayout(Context context) {
        super(context);
        mContext = context;
    }

    public MenuLayout inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.menu, this, true);

        mIconBG = (ImageView)this.findViewById(R.id.img_icon_bg);
        mIcon = (ImageView)this.findViewById(R.id.img_icon);
        mViewText = (TextView)this.findViewById(R.id.menu_text);

        if ( mIconBG == null || mIcon == null || mViewText == null ) return this;

        if ( mSupportClick ) mIconBG.setOnClickListener(mOnClickListener);
        if ( mSupportLongClick ) mIconBG.setOnLongClickListener(mOnLongClickListener);
        if ( mSupportTouch ) mIconBG.setOnTouchListener(mOnTouchListener);
        enableBG(true);

        if ( mResIcon != null ) {
            mIcon.setImageDrawable(mResIcon);
        } else if ( mButtonIcon.size() > 0 ) {
            mIcon.setImageDrawable(mButtonIcon.get(ButtonState.ENABLE));
        } else if ( mToggleIcons.size() > 0 ) {
            mIcon.setImageDrawable(mToggleIcons.get(mStatus));
        }
  
        if ( !mTexts.isEmpty() ) mViewText.setText(mTexts.get(mStatus));
        else mViewText.setText(mText);
        return this;
    }

    private void enableBG(boolean enable) {
        if ( mIconBG == null ) return;
        if ( enable ) {
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_pressed},
                getResources().getDrawable(R.drawable.dr_btn_p));
            states.addState(new int[] { android.R.attr.state_enabled},
                    getResources().getDrawable(R.drawable.dr_btn_n));
            mIconBG.setImageDrawable(states);
        } else {
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_pressed},
                getResources().getDrawable(R.drawable.dr_btn_p));
            states.addState(new int[] { android.R.attr.state_enabled},
                    getResources().getDrawable(R.drawable.dr_btn_d));
            mIconBG.setImageDrawable(states); 
        }
    }

    public MenuLayout setListener(MenuListener listener) {
        if ( mIconBG != null ) {
            if ( mSupportClick ) mIconBG.setOnClickListener(null); 
            if ( mSupportLongClick ) mIconBG.setOnLongClickListener(null);
            enableBG(false); 
        }
        if ( listener == null ) return this; 

        mListener = listener;
        if ( mIconBG != null ) {
            enableBG(true); 
            if ( mSupportClick ) mIconBG.setOnClickListener(mOnClickListener);
            if ( mSupportLongClick ) mIconBG.setOnLongClickListener(mOnLongClickListener);
        }
        return this;
    }

    public MenuLayout setTouchListener(MenuTouchListener listener) {
        mTouchListener = listener; 
        return this;
    }

    public MenuLayout addIcon(int status, Drawable icon) {
        mToggleIcons.put(status, icon);
        return this;
    }

    public MenuLayout addIconText(int status, Drawable icon, String text) {
        mToggleIcons.put(status, icon);
        mTexts.put(status, text); 
        return this;
    }

    public MenuLayout addIcon(Drawable enable, Drawable press, Drawable disable) {
        if ( enable != null ) mButtonIcon.put(ButtonState.ENABLE, enable);
        if ( press != null ) mButtonIcon.put(ButtonState.PRESS, press);
        if ( disable != null ) mButtonIcon.put(ButtonState.DISABLE, disable);
        return this;
    }

    public MenuLayout addIcon(Drawable icon) {
        mResIcon = icon;
        return this;
    }

    public MenuLayout setText(String text) {
        mText = text;
        return this;
    }

    public void updateText(String text) {
        mText = text; 
        mViewText.setText(mText); 
    }

    public void updateStatusText(int status, String text) {
        mTexts.put(status, text);
        if ( !mTexts.isEmpty() ) mViewText.setText(mTexts.get(mStatus));
    }

    public void enableText() {
        if ( mViewText != null ) mViewText.setAlpha(1.0f); 
    }

    public void disableText() {
        if ( mViewText != null ) mViewText.setAlpha(0.2f); 
    }

    public int getStatus() {
        return mStatus;
    }

    public boolean isEnable() {
        return mEnable;
    }

    public void updateState(int status) {
        Drawable drawable = mToggleIcons.get(status);
        if ( drawable == null ) return;
        mIcon.setImageDrawable(drawable);
        if ( !mTexts.isEmpty() ) 
            mViewText.setText(mTexts.get(status));
        mStatus = status;
    }

    public void updateEnable(boolean enable) {
        if ( enable ) {
            mIcon.setImageDrawable(mButtonIcon.get(ButtonState.ENABLE)); 
            enableBG(true); 
        }
        else {
            mIcon.setImageDrawable(mButtonIcon.get(ButtonState.DISABLE)); 
            enableBG(false); 
        }
        mEnable = enable;
    }

    public MenuLayout setSupportLongClick(boolean support) {
        mSupportLongClick = support; 
        return this;
    }

    public MenuLayout setSupportClick(boolean support) {
        mSupportClick = support; 
        return this;
    }

    public MenuLayout setSupportTouch(boolean support) {
        mSupportTouch = support; 
        return this; 
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if ( mListener != null ) mListener.onClick();
        }
    };

    private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if ( mListener == null )  return false;
            if ( mListener.onLongClick() ) {
                return true;
            }
            return false;
        }
    };

    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if ( mTouchListener == null ) return false;
            if ( mTouchListener.onTouch(event) ) return true;
            return false;
        }
    };
}
