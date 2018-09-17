package com.humaxdigital.automotive.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.provider.Settings;
import android.view.View;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.DateView;

import java.util.Date;
import java.util.Locale;

public class DateController {
    private Context mContext;
    private DateView mDateVew;

    public DateController(Context context, View view) {
        mContext = context;
        initView(view);
        initClock();
    }
    private void initView(View view) {
        if ( view == null ) return;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( view == null ) return;
                view.getContext().startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
            }
        });
        mDateVew = view.findViewById(R.id.text_date);

        // test code
        updateClockUI("19:20 PM");
    }

    private void updateClockUI(String time) {
        if ( mDateVew != null ) mDateVew.setText(time);
    }

    private void initClock() {
        if ( mContext == null) return;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
