package com.humaxdigital.automotive.systemui.volumedialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, VolumeDialogService.class);
        context.startService(service);
    }
}
