package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.os.Handler;

import com.humaxdigital.automotive.statusbar.R;

public class ClimateMenuTextDec extends LinearLayout {
    private Context mContext;
    private TextView mTextViewDec;
    private TextView mTextViewInt;
    private String mTextDec;
    private String mTextInt;
    private Handler mHandler; 

    public ClimateMenuTextDec(Context context) {
        super(context);
        mContext = context;
        if ( mContext != null ) 
            mHandler = new Handler(mContext.getMainLooper());
    }

    public ClimateMenuTextDec inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.climate_menu_text_decimal, this, true);
        mTextViewDec = (TextView)this.findViewById(R.id.text_dec);
        mTextViewInt = (TextView)this.findViewById(R.id.text_int);
        textRefresh();
        return this;
    }

    public ClimateMenuTextDec update(String textInt, String textDec) {
        if ( mContext == null ) return this; 

        mTextInt = textInt;
        mTextDec = textDec;
        if ( mHandler != null ) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    textRefresh();
                }
            }); 
        }
        
        return this;
    }

    private void textRefresh() {
        if ( mTextViewInt != null ) mTextViewInt.setText(mTextInt);
        if ( mTextViewDec != null ) mTextViewDec.setText(mTextDec);
    }
}


