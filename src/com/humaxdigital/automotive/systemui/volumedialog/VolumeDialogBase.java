
package com.humaxdigital.automotive.systemui.volumedialog;

import android.content.Context;
import android.view.View;

public abstract class VolumeDialogBase {
    public void init(Context context) {}
    public void deinit() {}
    public void open() {}
    public void close() {}
    public View getView() { return null; }
}