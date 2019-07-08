package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.extension.car.CarBLEManager;

import android.util.Log;
public class SystemBLEController extends BaseController<Integer> {
    private static final String TAG = "SystemBLEController";
    private enum BLEStatus { NONE, CONNECTED, CONNECTING, CONNECTION_FAIL }
    private CarBLEManager mManager;

    public SystemBLEController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
    }

    @Override
    public void disconnect() {
        try {
            mManager.unregisterCallback(mBLECallback);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }
    }

    public void fetch(CarBLEManager manager) {
        if ( manager == null ) return; 
        mManager = manager; 
        if ( mDataStore == null ) return; 
        try {
            mManager.registerCallback(mBLECallback);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!");
        }

        // todo : need to get status
        BLEStatus state = BLEStatus.NONE;  
        Log.d(TAG, "fetch="+state); 
        mDataStore.setBLEState(state.ordinal());
        cmdRequest(CarBLEManager.CMD_BLE_REQ_CONNECTION_STATUS, 0, null);
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        int val = mDataStore.getBLEState();
        Log.d(TAG, "get="+val); 
        return val; 
    }

    private void cmdRequest(int eventId, int dataLength, byte[] data)
    {
        if (  dataLength < 0 ) return;

        int i = 0;
        byte[] fullData = new byte[8+dataLength];

        // 0. clear
        for(i = 0; i < fullData.length ; i++)
            fullData[i] = 0x00;

        // 1. Event ID
        fullData[0] = (byte)((eventId >> 24) & 0x000000FF);
        fullData[1] = (byte)((eventId >> 16) & 0x000000FF);
        fullData[2] = (byte)((eventId >> 8) & 0x000000FF);
        fullData[3] = (byte)(eventId & 0x000000FF);

        if(dataLength > 0 || data != null) {
            // 2. Data Length
            fullData[4] = (byte)(((dataLength) >> 24) & 0x000000FF);
            fullData[5] = (byte)(((dataLength) >> 16) & 0x000000FF);
            fullData[6] = (byte)(((dataLength) >> 8) & 0x000000FF);
            fullData[7] = (byte)((dataLength) & 0x000000FF);

            // 3. Data
            System.arraycopy(data, 0, fullData, 8, dataLength);
        }

        try
        {
            //=================
            //setProperty : MPU -> MCU
            //=================
            if ( mManager != null ) mManager.setProperty(byte[].class, CarBLEManager.VENDOR_BLE_CMD, 0, fullData);
        } catch (CarNotConnectedException e) {
            Log.e("BLE_TEST", "Failed to set VENDOR_BLE_CMD", e);
        }
    }

    private final CarBLEManager.CarBLEEventCallback mBLECallback = 
        new CarBLEManager.CarBLEEventCallback () {
        public void onChangeEvent(final CarPropertyValue value) {
            int zones = value.getAreaId();
            switch(value.getPropertyId()) {
                case CarBLEManager.VENDOR_BLE_EVENT: {
                    byte[] data = (byte[])value.getValue();
                    if ( data.length < 4 ) break; 
                    int id = ((data[0] & 0xff) << 24) 
                        | ((data[1] & 0xff) << 16) 
                        | ((data[2] & 0xff) << 8) 
                        | (data[3] & 0xff);
                    
                    Log.d(TAG, "VENDOR_BLE_EVENT="+id); 
                    
                    if ( id == CarBLEManager.EVT_BLE_CONNECTION_STATUS ) {

                        int dataLength = ((data[4] & 0xff) << 24) 
                                        | ((data[5] & 0xff) << 16) 
                                        | ((data[6] & 0xff) << 8) 
                                        | (data[7] & 0xff);
                        if ( dataLength < 0 ) break; 
                        byte[] eventData = new byte[dataLength];
                        System.arraycopy(data, 8, eventData, 0, dataLength);
                        
                        Log.d(TAG, "eventdata:length="+dataLength+", data="+eventData[0]); 

                        if ( eventData[0] == 0 ) {
                            if ( mDataStore.shouldPropagateBLEStatusUpdate(BLEStatus.NONE.ordinal()) ) {
                                for ( Listener listener : mListeners ) 
                                    listener.onEvent(BLEStatus.NONE.ordinal());
                            }
                        } else if ( eventData[0] == 1 ) {
                            if ( mDataStore.shouldPropagateBLEStatusUpdate(BLEStatus.CONNECTING.ordinal()) ) {
                                for ( Listener listener : mListeners ) 
                                    listener.onEvent(BLEStatus.CONNECTING.ordinal());
                            }
                        } else if ( eventData[0] == 2 ) {
                            if ( mDataStore.shouldPropagateBLEStatusUpdate(BLEStatus.CONNECTED.ordinal()) ) {
                                for ( Listener listener : mListeners ) 
                                    listener.onEvent(BLEStatus.CONNECTED.ordinal());
                            }
                        }
                    } else if ( id == CarBLEManager.EVT_BLE_ERROR_FOR_CALIBRATION ) {
                        // INFO : BLEStatus.CONNECTION_FAIL 
                        if ( mDataStore.shouldPropagateBLEStatusUpdate(BLEStatus.NONE.ordinal()) ) {
                            for ( Listener listener : mListeners ) 
                                listener.onEvent(BLEStatus.NONE.ordinal());
                        }
                        
                    } else if ( id == CarBLEManager.EVT_BLE_CONNECTED_PHONE_INFO ) {
                        int dataLength = ((data[4] & 0xff) << 24) 
                                        | ((data[5] & 0xff) << 16) 
                                        | ((data[6] & 0xff) << 8) 
                                        | (data[7] & 0xff);
                        if ( dataLength < 0 ) break; 
                        byte[] eventData = new byte[dataLength];
                        System.arraycopy(data, 8, eventData, 0, dataLength);
                        
                        Log.d(TAG, "eventdata:length="+dataLength+", data="+eventData[0]); 

                        BLEStatus state = BLEStatus.NONE;  
                        if ((eventData[0] & 1) > 0) {
                            state = BLEStatus.CONNECTED; 
                        }
                        Log.d(TAG, "fetch="+state); 
                        mDataStore.setBLEState(state.ordinal());
                    }
                    break; 
                } 
                default: break; 
            }
        }
        @Override
        public void onErrorEvent(final int propertyId, final int zone) {
        }
    }; 
}
