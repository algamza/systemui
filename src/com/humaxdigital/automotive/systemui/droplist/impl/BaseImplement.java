package com.humaxdigital.automotive.systemui.droplist.impl;

import android.content.Context;

public class BaseImplement<E> {
    public interface Listener<E> {
        void onChange(E e);
    }

    protected Listener mListener;
    protected Context mContext;

    public BaseImplement(Context context) {
        mContext = context;
    }

    public BaseImplement setListener(Listener listener) {
        mListener = listener;
        return this; 
    }

    public void create() {}
    public void destroy() {}
    protected E get() { return null; }
    protected void set(E e) {}
}
