package com.humaxdigital.automotive.systemui.common.receiver;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.provider.Settings;
import android.view.KeyEvent;

import com.humaxdigital.automotive.systemui.common.CONSTANTS;

public class GlobalKeyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_GLOBAL_BUTTON.equals(intent.getAction())) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP){
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    Settings.Global.putInt(context.getContentResolver(), CONSTANTS.GLOBAL_KEY_VOLUME_UP, CONSTANTS.GLOBAL_KEY_VOLUME_UP_ON);
                } else if(event.getAction() == KeyEvent.ACTION_UP){
                    Settings.Global.putInt(context.getContentResolver(), CONSTANTS.GLOBAL_KEY_VOLUME_UP, CONSTANTS.GLOBAL_KEY_VOLUME_UP_OFF);
                }
            } else if ( event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN ) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    Settings.Global.putInt(context.getContentResolver(), CONSTANTS.GLOBAL_KEY_VOLUME_DOWN, CONSTANTS.GLOBAL_KEY_VOLUME_DOWN_ON);
                } else if(event.getAction() == KeyEvent.ACTION_UP){
                    Settings.Global.putInt(context.getContentResolver(), CONSTANTS.GLOBAL_KEY_VOLUME_DOWN, CONSTANTS.GLOBAL_KEY_VOLUME_DOWN_OFF);
                }
            }
        }
    }
}
