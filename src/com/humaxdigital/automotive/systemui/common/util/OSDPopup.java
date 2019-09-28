package com.humaxdigital.automotive.systemui.common.util;

import android.content.Context;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class OSDPopup {
    private static final String TAG = "OSDPopup"; 
    
    static public void send(Context context, String text) {
        if ( context == null || text == null ) return;
        String channelid =  "SystemUIPopup";
        NotificationChannel channel = 
            new NotificationChannel(channelid,  "SystemUIPopup", NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = 
            (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
        Notification noti = new Notification.Builder(context, channelid)
            .setContentText(text)
            .setSmallIcon(-1)
            .build();
        notificationManager.notify(0, noti);
    }
}