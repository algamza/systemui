package com.humaxdigital.automotive.systemui.statusbar.service;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects; 

public class BaseController<E> {
    protected final List<Listener> mListeners = new ArrayList<>();
    protected DataStore mDataStore;
    protected Context mContext;

    public interface Listener<E> {
        void onEvent(E e);
    }

    public BaseController(Context context, DataStore store) {
        mContext = Objects.requireNonNull(context);
        mDataStore = Objects.requireNonNull(store);
    }
    
    public void addListener(Listener listener) { 
        mListeners.add(Objects.requireNonNull(listener)); 
    }
    
    public void removeListener(Listener listener) { 
        mListeners.remove(Objects.requireNonNull(listener)); 
    }

    protected void connect() {}
    protected void disconnect() {}
    protected void fetch() {}
    protected E get() { return null;  }
    protected void set(E e) {}
}
