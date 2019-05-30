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

import com.humaxdigital.automotive.systemui.R;

import java.util.HashMap;

public class MenuLayout extends LinearLayout {
    public interface MenuListener {
        boolean onClick();
        boolean onLongClick();
    }

    private enum ButtonState {
        ENABLE,
        PRESS,
        DISABLE
    }

    private MenuListener mListener;
    private Context mContext;

    private Drawable mIcon = null;
    private HashMap<Integer,Drawable> mToggleIcons = new HashMap<>();
    private int mStatus = 0;
    private HashMap<ButtonState,Drawable> mButtonIcon = new HashMap<>();
    private boolean mEnable = true;
    private HashMap<Integer,String> mTexts = new HashMap<>();
    private String mText;
    private FrameLayout mViewEnable;
    private FrameLayout mViewPress;
    private FrameLayout mViewDisable;
    private ImageView mImgEnable;
    private ImageView mImgPress;
    private ImageView mImgDisable;
    private TextView mViewText;

    public MenuLayout(Context context) {
        super(context);
        mContext = context;
    }

    public MenuLayout inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.menu, this, true);

        mViewEnable = (FrameLayout)this.findViewById(R.id.menu_n);
        mViewDisable = (FrameLayout)this.findViewById(R.id.menu_d);
        mViewPress = (FrameLayout)this.findViewById(R.id.menu_p);
        mImgEnable = (ImageView)this.findViewById(R.id.img_n);
        mImgPress = (ImageView)this.findViewById(R.id.img_p);
        mImgDisable = (ImageView)this.findViewById(R.id.img_d);
        mViewText = (TextView)this.findViewById(R.id.menu_text);

        if ( mViewEnable == null || mViewDisable == null ||
                mViewPress == null || mImgEnable == null ||
                mImgPress == null || mImgDisable == null ||
                mViewText == null ) return this;

        if ( mIcon != null ) {
            mImgEnable.setImageDrawable(mIcon);
        } else if ( mButtonIcon.size() > 0 ) {
            mImgEnable.setImageDrawable(mButtonIcon.get(ButtonState.ENABLE));
            mImgDisable.setImageDrawable(mButtonIcon.get(ButtonState.DISABLE));
            mImgPress.setImageDrawable(mButtonIcon.get(ButtonState.PRESS));
        } else if ( mToggleIcons.size() > 0 ) {
            mImgEnable.setImageDrawable(mToggleIcons.get(mStatus));
        }
        buttonEnable();
        if ( !mTexts.isEmpty() ) mViewText.setText(mTexts.get(mStatus));
        else mViewText.setText(mText);
        return this;
    }

    public MenuLayout setListener(MenuListener listener) {
        if ( mListener != null ) return this;
        mListener = listener;
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( mListener != null ) mListener.onClick();
            }
        });

        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if ( mListener == null )  return false;
                if ( mListener.onLongClick() ) {
                    updateButtonVisible();
                    return true;
                }
                return false;
            }
        });

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        if ( mButtonIcon.size() > 0 ) buttonPress();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        updateButtonVisible(); 
                        break;
                    }
                }
                return false;
            }
        });
        return this;
    }

    private void updateButtonVisible() {
        if ( mButtonIcon.size() > 0 ) {
            if ( mEnable ) buttonEnable();
            else buttonDisable();
        }
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
        mIcon = icon;
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
        mImgEnable.setImageDrawable(drawable);
        if ( !mTexts.isEmpty() ) 
            mViewText.setText(mTexts.get(status));
        mStatus = status;
    }

    public void updateEnable(boolean enable) {
        if ( enable ) buttonEnable();
        else buttonDisable();
        mEnable = enable;
    }

    private void buttonEnable() {
        mViewEnable.setVisibility(View.VISIBLE);
        mViewDisable.setVisibility(View.INVISIBLE);
        mViewPress.setVisibility(View.INVISIBLE);
    }

    private void buttonPress() {
        mViewEnable.setVisibility(View.INVISIBLE);
        mViewDisable.setVisibility(View.INVISIBLE);
        mViewPress.setVisibility(View.VISIBLE);
    }

    private void buttonDisable() {
        mViewEnable.setVisibility(View.INVISIBLE);
        mViewDisable.setVisibility(View.VISIBLE);
        mViewPress.setVisibility(View.INVISIBLE);
    }
}
