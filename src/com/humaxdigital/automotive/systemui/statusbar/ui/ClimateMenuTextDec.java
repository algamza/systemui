package com.humaxdigital.automotive.systemui.statusbar.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects; 

import com.humaxdigital.automotive.systemui.R;

public class ClimateMenuTextDec extends LinearLayout {
    private Context mContext;
    private TextView mTextViewDec;
    private TextView mTextViewInt;
    private String mTextDec;
    private String mTextInt;
    private View mOffView;
    private View mTempView;
    private TextView mTextView; 
    private Boolean mTextVisibility = false; 

    public ClimateMenuTextDec(Context context) {
        super(context);
        mContext = Objects.requireNonNull(context);
    }

    public ClimateMenuTextDec inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.climate_menu_text_decimal, this, true);
        mTextViewDec = (TextView)this.findViewById(R.id.text_dec);
        mTextViewInt = (TextView)this.findViewById(R.id.text_int);
        mOffView = (View)this.findViewById(R.id.text_off);
        mTempView = (View)this.findViewById(R.id.text_temp);
        mTextView = (TextView)this.findViewById(R.id.text); 
        textRefresh();
        return this;
    }

    public ClimateMenuTextDec update(String textInt, String textDec) {
        updateTextVisibility(false); 
        mTextInt = textInt;
        mTextDec = textDec;
        textRefresh();
        return this;
    }

    public ClimateMenuTextDec update(String text) { 
        updateTextVisibility(true); 
        if ( mTextView != null ) mTextView.setText(text);
        textRefresh();
        return this;  
    }

    public ClimateMenuTextDec updateDisable(boolean disable) {
        if ( mTextViewDec == null || 
            mTextViewInt == null ||
            mTextView == null ) return this; 
        if ( disable ) {
            mTextViewDec.setTextColor(mContext.getResources().getColor(R.color.climateTextDis)); 
            mTextViewInt.setTextColor(mContext.getResources().getColor(R.color.climateTextDis)); 
            mTextView.setTextColor(mContext.getResources().getColor(R.color.climateTextDis)); 
        } else {
            mTextViewDec.setTextColor(mContext.getResources().getColor(R.color.ClimateTextNor)); 
            mTextViewInt.setTextColor(mContext.getResources().getColor(R.color.ClimateTextNor)); 
            mTextView.setTextColor(mContext.getResources().getColor(R.color.ClimateTextNor));  
        }

        return this; 
    }

    private void updateTextVisibility(boolean on) {
        if ( mTextVisibility == on ) return; 
        mTextVisibility = on; 
        if ( on ) {
            mTempView.setVisibility(View.INVISIBLE);
            mOffView.setVisibility(View.VISIBLE);
        } else {
            mTempView.setVisibility(View.VISIBLE);
            mOffView.setVisibility(View.INVISIBLE);
        }
    }

    private void textRefresh() {
        if ( mTextViewInt != null ) mTextViewInt.setText(mTextInt);
        if ( mTextViewDec != null ) mTextViewDec.setText(mTextDec);
    }
}


