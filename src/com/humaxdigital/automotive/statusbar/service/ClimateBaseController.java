package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.car.hardware.hvac.CarHvacManager;
import android.extension.car.CarHvacManagerEx;

public class ClimateBaseController<E> extends BaseController<E> {
    protected CarHvacManager mManager; 
    public ClimateBaseController(Context context, DataStore store, CarHvacManagerEx manager) {
        super(context, store);
        mManager = manager; 
    }
    protected Boolean update(E e) { return false; }
}
