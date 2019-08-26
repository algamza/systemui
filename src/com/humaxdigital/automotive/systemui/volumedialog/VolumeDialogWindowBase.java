
package com.humaxdigital.automotive.systemui.volumedialog;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;

public abstract class VolumeDialogWindowBase {
    protected ArrayList<DialogListener> mListener = new ArrayList<>();
    public interface DialogListener {
        void onShow(boolean show);
    }
    public void init(Context context) {}
    public void deinit() {}
    public void open() {}
    public void close() {}
    public void registDialogListener(DialogListener listener) {
        if ( listener == null ) return;
        mListener.add(listener);
    }
    public void unregistDialogListener(DialogListener listener) {
        if ( listener == null ) return;
        mListener.remove(listener);
    }
    public View getView() { return null; }
}