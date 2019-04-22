package com.humaxdigital.automotive.systemui.statusbar.service;
import android.util.Log;

public class ClimateUtils {
    private static final String TAG = "ClimateUtils";
    static public boolean isValidTemperatureHex(int hex) {
        if ( (hex >= 0x02 && hex <= 0x24) 
            || hex == 0x01 
            || hex == 0x00 
            || hex == 0xfe ) return true;
        return false; 
    }
    static public float temperatureHexToCelsius(int hex) {
        float phy = hex*0.5f + 14; 
        Log.d(TAG, "temperatureHexToCelsius="+phy); 
        return phy; 
    }
    
    static public float temperatureHexToFahrenheit(int hex) {
        float phy = hex*1.0f + 56; 
        Log.d(TAG, "temperatureHexToFahrenheit="+phy); 
        return phy; 
    }
}