package com.humaxdigital.automotive.statusbar.service;

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
        
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        int val = mDataStore.getBLEState();
        Log.d(TAG, "get="+val); 
        return val; 
    }

    public static String byteArrayToHex(byte[] paramArrayOfByte)
    {
        if (paramArrayOfByte == null) {
            return "null";
        }
        StringBuilder localStringBuilder = new StringBuilder(paramArrayOfByte.length * 2);
        int j = paramArrayOfByte.length;
        int i = 0;
        while (i < j)
        {
            localStringBuilder.append(String.format("%02x ", new Object[] { Byte.valueOf(paramArrayOfByte[i]) }));
            i += 1;
            if( (i != 0) && (i%16 == 0) ) {
                localStringBuilder.append("\n");
            }
        }
        return localStringBuilder.toString();
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
                    
                    if ( id == CarBLEManager.APP_BLE_SEND_CONNECTION_STATUS ) {

                        int dataLength = ((data[4] & 0xff) << 24) 
                                        | ((data[5] & 0xff) << 16) 
                                        | ((data[6] & 0xff) << 8) 
                                        | (data[7] & 0xff);
                        if ( dataLength < 0 ) break; 
                        byte[] eventData = new byte[dataLength];
                        System.arraycopy(data, 8, eventData, 0, dataLength);
                        
                        Log.d(TAG, "eventdata:length="+dataLength+", data="+eventData[0]); 
                        if ( dataLength != 1 ) break;
                        
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
                    } else if ( id == CarBLEManager.APP_BLE_SEND_ERROR_FOR_CALIBRATION ) {
                        if ( mDataStore.shouldPropagateBLEStatusUpdate(BLEStatus.CONNECTION_FAIL.ordinal()) ) {
                            for ( Listener listener : mListeners ) 
                                listener.onEvent(BLEStatus.CONNECTION_FAIL.ordinal());
                        }
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
