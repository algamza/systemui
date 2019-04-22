package com.humaxdigital.automotive.systemui.droplist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent droplistUiService = new Intent(context, DropListUIService.class);
        context.startService(droplistUiService);
    }
}
