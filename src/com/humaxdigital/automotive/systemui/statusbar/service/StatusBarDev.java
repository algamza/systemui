package com.humaxdigital.automotive.systemui.statusbar.service;

import android.os.Bundle;
import android.os.RemoteException;
import android.content.Context;
import android.util.Log;
import com.humaxdigital.automotive.systemui.statusbar.dev.DevCommandsServer;

public class StatusBarDev extends IStatusBarDev.Stub {
    private static final String TAG = "StatusBarDev";
    private DevCommandsServer mDevCommandsServer = null;

    public StatusBarDev(Context context) {
        Log.d(TAG, "StatusBarDev");
        if ( context == null ) return;
        mDevCommandsServer = new DevCommandsServer(context);
    }

    public void destroy() {
        Log.d(TAG, "onDestroy");
        mDevCommandsServer = null;
    }

    @Override
    public Bundle invokeDevCommand(String command, Bundle args) throws RemoteException {
        Log.d(TAG, "invokeDevCommand");
        if ( mDevCommandsServer == null ) return null;
        return mDevCommandsServer.invokeDevCommand(command, args);
    }
}
