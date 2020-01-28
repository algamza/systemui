
package com.humaxdigital.automotive.systemui.volumedialog;

import android.content.Context;
import android.view.View;

import java.util.ArrayList;
import java.util.Objects; 

public abstract class VolumeDialogWindowBase {
    protected ArrayList<DialogListener> mListener = new ArrayList<>();
    public interface DialogListener {
        void onShow(boolean show);
    }
    public void init(Context context) {}
    public void deinit() {}
    public void open() {}
    public void close(boolean force) {}
    public void registDialogListener(DialogListener listener) {
        mListener.add(Objects.requireNonNull(listener));
    }
    public void unregistDialogListener(DialogListener listener) {
        mListener.remove(Objects.requireNonNull(listener));
    }
    public View getView() { return null; }
}