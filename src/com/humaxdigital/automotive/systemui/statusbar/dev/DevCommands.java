// Copyright (c) 2019 HUMAX Co., Ltd. All rights reserved.
// Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

package com.humaxdigital.automotive.systemui.statusbar.dev;

public interface DevCommands {
    void forceStopPackage(String packageName, int userId);
    String getPreferenceString(String key, String defValue);
    void putPreferenceString(String key, String value);
    String execShellCommand(String commandLine);
}
