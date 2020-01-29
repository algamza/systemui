// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.statusbar.dev;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.Objects; 

public class DevCommandsProxy implements DevCommands {
    private static final String TAG = "DevCommandsProxy";
    
    private Context mContext;

    public DevCommandsProxy(Context context) {
        mContext = Objects.requireNonNull(context);
    }

    public void forceStopPackage(String packageName, int userId) {
        Bundle args = new Bundle();
        args.putCharSequence("packageName", packageName);
        args.putInt("userId", userId);
        invokeDevCommand("forceStopPackage", args);
    }

    public String getPreferenceString(String key, String defValue) {
        Bundle args = new Bundle();
        args.putCharSequence("key", key);
        args.putCharSequence("defValue", defValue);
        Bundle ret = invokeDevCommand("getPreferenceString", args);
        return ret.getCharSequence("return", defValue).toString();
    }

    public void putPreferenceString(String key, String value) {
        Bundle args = new Bundle();
        args.putCharSequence("key", key);
        args.putCharSequence("value", value);
        invokeDevCommand("putPreferenceString", args);
    }

    public String execShellCommand(String commandLine) {
        Bundle args = new Bundle();
        args.putCharSequence("commandLine", commandLine);
        Bundle ret = invokeDevCommand("execShellCommand", args);
        return ret.getCharSequence("return", "").toString();
    }

    public Bundle invokeDevCommand(String command, Bundle args) {
        // Return empty bundle. Subclass should override.
        return new Bundle();
    }
}
