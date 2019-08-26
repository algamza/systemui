
package com.humaxdigital.automotive.systemui;

import android.content.Context;

public interface SystemUIBase {
    public SystemUIBase create(Context context); 
    public void destroy(); 
}