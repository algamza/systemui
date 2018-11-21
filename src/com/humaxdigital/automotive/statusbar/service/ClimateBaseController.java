package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;
import android.extension.car.CarHvacManagerEx;

public class ClimateBaseController<E> extends BaseController<E> {
    protected CarHvacManagerEx mManager; 
    public ClimateBaseController(Context context, DataStore store) {
        super(context, store);
    }
    protected Boolean update(E e) { return false; }
    protected void fetch(CarHvacManagerEx manager) {
        super.fetch();
        mManager = manager;
    }
}
