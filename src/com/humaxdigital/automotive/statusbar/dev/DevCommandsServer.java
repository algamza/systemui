// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.statusbar.dev;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class DevCommandsServer implements DevCommands {
    private static final String TAG = "DevCommandsServer";
    private Context mContext;

    public DevCommandsServer(Context context) {
        mContext = context;
    }

    public Bundle invokeDevCommand(String command, Bundle args) {
        Bundle ret = new Bundle();
        switch(command) {
            case "forceStopPackage": {
                final String packageName = args.getCharSequence("packageName").toString();
                final int userId = args.getInt("userId");
                forceStopPackage(packageName, userId);
                break;
            }

            case "getPreferenceString": {
                final String key = args.getCharSequence("key").toString();
                final String defValue = args.getCharSequence("defValue").toString();
                final String value = getPreferenceString(key, defValue);
                ret.putCharSequence("return", value);
                break;
            }

            case "putPreferenceString": {
                final String key = args.getCharSequence("key").toString();
                final String value = args.getCharSequence("value").toString();
                putPreferenceString(key, value);
                break;
            }
        }
        return ret;
    }

    public void forceStopPackage(String packageName, int userId) {
        ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        am.forceStopPackageAsUser(packageName, userId);
    }

    public String getPreferenceString(String key, String defValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sharedPref.getString(key, defValue);
    }

    public void putPreferenceString(String key, String value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

}
