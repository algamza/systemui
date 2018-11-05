package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.os.Bundle;

import android.location.LocationManager;
import android.location.LocationListener; 
import android.location.LocationProvider; 
import android.location.Location;

public class SystemLocationController extends BaseController<Integer> {
    enum LocationStatus { NONE, LOCATION_SHARING }
    private LocationManager mManager; 

    public SystemLocationController(Context context, DataStore store) {
        super(context, store);
    }

    @Override
    public void connect() {
        if ( mContext == null ) return;
        mManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        mManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
    }

    @Override
    public void disconnect() {
        if ( mManager != null ) mManager.removeUpdates(mLocationListener); 
    }

    @Override
    public void fetch() {
        if ( mManager == null || mDataStore == null ) return; 
        mDataStore.setLocationShareState(mManager.isLocationEnabled()); 
    }

    @Override
    public Integer get() {
        if ( mDataStore == null ) return 0; 
        if ( mDataStore.getLocationShareState() ) {
            return convertToStatus(1).ordinal(); 
        } else {
            return convertToStatus(0).ordinal(); 
        }
    }

    private LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {}
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if ( mManager == null || mDataStore == null ) return; 
            switch(status) {
                case LocationProvider.AVAILABLE:
                case LocationProvider.OUT_OF_SERVICE: 
                    {
                        boolean enable = mManager.isLocationEnabled(); 
                        boolean shouldPropagate = mDataStore.shouldPropagateLocationShareUpdate(enable);
                        if ( shouldPropagate ) {
                            for ( Listener<Integer> listener : mListeners ) {
                                if ( enable ) {
                                    listener.onEvent(convertToStatus(1).ordinal());
                                } else {
                                    listener.onEvent(convertToStatus(0).ordinal());
                                }
                            }
                        }
                        break; 
                    }
                default: break; 
            }
        }
    };

    private LocationStatus convertToStatus(int mode) {
        LocationStatus status = LocationStatus.NONE;
        switch(mode) {
            case 0: break;
            case 1: status = LocationStatus.LOCATION_SHARING;
            default: break;  
        }
        return status; 
    }
}
