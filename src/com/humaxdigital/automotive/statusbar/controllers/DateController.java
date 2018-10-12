package com.humaxdigital.automotive.statusbar.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.DateView;


public class DateController {
    private Context mContext;
    private DateView mDateVew;

    public DateController(Context context, View view) {
        mContext = context;
        if ( mContext == null ) return;

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
        updateClockUI(getCurrentTime());
    }

    private void updateClockUI(String time) {
        if ( mDateVew != null ) mDateVew.setText(time);
    }

    private void initClock() {
        if ( mContext == null ) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        mContext.registerReceiver(mDateTimeChangedReceiver, filter);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mContext.unregisterReceiver(mDateTimeChangedReceiver);
    }

    private final BroadcastReceiver mDateTimeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateClockUI(getCurrentTime());
        }
    };

    private String getCurrentTime() {
        DateFormat df = new SimpleDateFormat("h:mm a");
        return df.format(Calendar.getInstance().getTime());
    }
}
