package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.Bundle;

import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.extension.car.CarTMSManager;

import android.util.Log; 
import java.util.Arrays;

public class SystemLocationController extends BaseController<Integer> {
    private static final String TAG = "SystemLocationController";
    enum LocationStatus { NONE, LOCATION_SHARING }
    private CarTMSManager mManager; 

    public SystemLocationController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
    }

    @Override
    public void disconnect() {

    }

    public void fetch(CarTMSManager manager) {
        if ( manager == null ) return; 
        mManager = manager; 
        try {
            mManager.registerCallback(mTMSCallback);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        int val = mDataStore.getLocationSharingState(); 
        Log.d(TAG, "get="+val); 
        return val; 
    }

    private final CarTMSManager.CarTMSEventCallback mTMSCallback = 
        new CarTMSManager.CarTMSEventCallback() {
            
        @Override
        public void onChangeEvent(final CarPropertyValue value) {
            int zones = value.getAreaId();
            if ( value.getPropertyId() != CarTMSManager.VENDOR_TMS_EVENT ) return; 
           
            int i = 0;
            int eventId = 0;
            int dataLength = 0;
            byte[] fullData = {0};
            fullData = (byte[])value.getValue();

            eventId = ((fullData[0] & 0xff) << 24) | ((fullData[1] & 0xff) << 16) | ((fullData[2] & 0xff) << 8) | (fullData[3] & 0xff);

            switch(eventId) {
                case CarTMSManager.APP_TMS_RES_LOCATIONSHARING: 
                    updateStatus(LocationStatus.LOCATION_SHARING); 
                    break; 
                case CarTMSManager.APP_TMS_RES_LOCATIONSHARING_CANCEL: 
                    updateStatus(LocationStatus.NONE); 
                    break; 
                default: break; 
            }
        }

        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
            Log.w(TAG, "tms Error in TmsTestFragment :  propertyId="+propertyId+", zone=0x"+zone);
        }
    }; 

    private void updateStatus(LocationStatus status) {
        if ( mDataStore == null ) return; 
        if ( mDataStore.shouldPropagateLocationSharingStatusUpdate(status.ordinal()) ) {
            for ( Listener listener : mListeners ) 
                listener.onEvent(status.ordinal());
        }
    }

    private byte[] command(int id) { 
        byte[] data = new byte[8];
        Arrays.fill(data, (byte)0); 

        data[0] = (byte)((id >> 24) & 0x000000FF);
        data[1] = (byte)((id >> 16) & 0x000000FF);
        data[2] = (byte)((id >> 8) & 0x000000FF);
        data[3] = (byte)(id & 0x000000FF);
        return data; 
    }
}
