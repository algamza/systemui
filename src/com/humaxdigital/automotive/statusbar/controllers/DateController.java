package com.humaxdigital.automotive.statusbar.controllers;

import android.content.Context;
import android.view.View;
import android.os.RemoteException;

import com.humaxdigital.automotive.statusbar.R;
import com.humaxdigital.automotive.statusbar.ui.DateView;

import com.humaxdigital.automotive.statusbar.service.IStatusBarService;
import com.humaxdigital.automotive.statusbar.service.IDateTimeCallback; 

public class DateController implements BaseController {
    private View mParentView; 
    private Context mContext;
    private DateView mDateVew;
    private DateView mDateNoonView;
    private IStatusBarService mService; 

    public DateController(Context context, View view) {
        if ( context == null || view == null ) return;
        mContext = context;
        mParentView = view;
    }

    @Override
    public void init(IStatusBarService service) {
        if ( service == null ) return;
        mService = service; 
        try {
            mService.registerDateTimeCallback(mDateTimeCallback); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
        initView();
    }

    @Override
    public void deinit() {
        if ( mService == null ) return;
        try {
            mService.unregisterDateTimeCallback(mDateTimeCallback); 
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
    }

    private void initView() {
        if ( mParentView == null || mService == null ) return;
        mParentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( mService == null ) return;
                try {
                    mService.openDateTimeSetting(); 
                } catch( RemoteException e ) {
                    e.printStackTrace();
                }
            }
        });

        mDateVew = mParentView.findViewById(R.id.text_date_time);
        mDateNoonView = mParentView.findViewById(R.id.text_date_noon);

        try {
            updateClockUI(mService.getDateTime());
        } catch( RemoteException e ) {
            e.printStackTrace();
        }
    }

    private void updateClockUI(String time) {
        if ( mDateVew == null || mDateNoonView == null ) return;
        String date = "";
        String noon = "";
        if ( time.contains("AM") ) {
            date = time.substring(0, time.indexOf("AM"));
            noon = "AM";
        }
        else if ( time.contains("PM") ) {
            date = time.substring(0, time.indexOf("PM"));
            noon = "PM";
        }
        mDateVew.setText(date);
        mDateNoonView.setText(noon);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private final IDateTimeCallback.Stub mDateTimeCallback = new IDateTimeCallback.Stub() {
        @Override
        public void onDateTimeChanged(String time) throws RemoteException {
            updateClockUI(time);
        }
    };
}
