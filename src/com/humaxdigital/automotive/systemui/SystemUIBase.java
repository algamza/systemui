
package com.humaxdigital.automotive.systemui;

import android.content.Context;
import android.content.res.Configuration;

public interface SystemUIBase {
    public void onCreate(Context context); 
    public void onDestroy(); 
    public void onConfigurationChanged(Configuration newConfig); 
}