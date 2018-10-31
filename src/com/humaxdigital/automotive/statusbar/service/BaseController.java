package com.humaxdigital.automotive.statusbar.service;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class BaseController<E> {
    protected final List<Listener> mListeners = new ArrayList<>();
    protected DataStore mDataStore;
    protected Context mContext;

    public interface Listener<E> {
        void onEvent(E e);
    }

    public BaseController(Context context, DataStore store) {
        mContext = context;
        mDataStore = store;
    }
    
    public void addListener(Listener listener) { 
        if ( listener != null ) mListeners.add(listener); 
    }
    
    public void removeListener(Listener listener) { 
        if( listener != null ) mListeners.remove(listener); 
    }

    protected void connect() {}
    protected void disconnect() {}
    protected void fetch() {}
    protected E get() { return null;  }
    protected void set(E e) {}
}
