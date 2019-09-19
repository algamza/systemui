package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;
import android.util.Log;

import android.car.CarNotConnectedException;
import android.car.VehicleAreaType;
import android.car.hardware.CarPropertyValue;
import android.extension.car.CarClusterManager;
import android.extension.car.CarPropertyFilter;

public class ClusterBrightnessImpl extends BaseImplement<Integer> {
    static final String TAG = "ClusterBrightnessImpl";
    private CarClusterManager mCarCluster; 
    public ClusterBrightnessImpl(Context context) {
        super(context);
    }

    @Override
    public Integer get() {
        int brightness = getClusterValue();
        Log.d(TAG, "get="+brightness);
        return brightness;
    }

    @Override
    public void set(Integer e) {
        Log.d(TAG, "set="+e);
        setClusterValue(e); 
    }

    public void fetchEx(CarExtensionClient client) {
        Log.d(TAG, "fetchEx");
        try {
            if ( client == null ) {
                if ( mCarCluster != null ) {
                    mCarCluster.unregisterCallback(mClusterCallback); 
                    mCarCluster = null;
                }

                return;
            }
            mCarCluster = client.getClusterManager();
            if ( mCarCluster == null ) return;
            CarPropertyFilter filter = new CarPropertyFilter();
            filter.addId(CarClusterManager.VENDOR_CANRX_CLU_DISP_BRIGHTNESS); 
            mCarCluster.registerCallback(mClusterCallback, filter); 
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car is not connected!", e);
        }
    }

    private int getClusterValue() {
        int value = 0; 
        if ( mCarCluster == null ) return value; 
        try {
            value = mCarCluster.getIntProperty(
                CarClusterManager.VENDOR_CANRX_CLU_DISP_BRIGHTNESS, 
                VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL);
            Log.d(TAG, "getClusterValue = pos: " + value);
        } catch (CarNotConnectedException e) {
            e.printStackTrace();
        }
        return value;
    }

    private void setClusterValue(int val) {
        if (mCarCluster != null) {
            try {
                mCarCluster.setIntProperty(
                    CarClusterManager.VENDOR_CANTX_CLU_DISP_BRIGHTNESS, 
                    VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL, val);
                Log.d(TAG, "setClusterValue = : " + val);
            } catch (CarNotConnectedException e) {
                e.printStackTrace();
            }
        }
    }

    private CarClusterManager.CarClusterEventCallback mClusterCallback = new CarClusterManager.CarClusterEventCallback() {
        @Override
        public void onChangeEvent(CarPropertyValue carPropertyValue) {
            if ( carPropertyValue == null || mListener == null ) return;
            if ( carPropertyValue.getPropertyId() == CarClusterManager.VENDOR_CANRX_CLU_DISP_BRIGHTNESS ) {
                int value = (Integer)carPropertyValue.getValue(); 
                Log.d(TAG, "onChangeEvent:VENDOR_CANRX_CLU_DISP_BRIGHTNESS="+value); 
                mListener.onChange(value); 
            }
        }

        @Override
        public void onErrorEvent(int i, int i1) {
        }
    };
}
