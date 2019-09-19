package com.humaxdigital.automotive.systemui.common.util;

import android.os.Build; 
import android.util.Log;

public class ProductConfig {
    private static final String TAG = "ProductConfig"; 

    public enum MODEL { DN8C, DU2, CN7C, DL3C }
    public enum FEATURE { AV, AVC, AVN, AVNT }
    
    static public MODEL getModel() {
        MODEL model = MODEL.DN8C; 
        String name = Build.MODEL; 
        
        String[] array = name.split("-");
        if ( array.length >= 2 ) {
            if ( array[0].contains("BHDN") ) model = MODEL.DN8C; 
            else if ( array[0].contains("BHDU") ) model = MODEL.DU2; 
            else if ( array[0].contains("BHCN") ) model = MODEL.CN7C; 
            else if ( array[0].contains("DYDL") ) model = MODEL.DL3C; 
        }
        Log.d(TAG, "name="+name+", model="+model); 
        return model; 
    }

    static public FEATURE getFeature() {
        FEATURE feature = FEATURE.AV; 
        MODEL model = MODEL.DN8C; 
        String name = Build.MODEL; 
        String[] array = name.split("-");
        if ( array.length >= 2 ) {
            if ( array[1].contains("T") ) feature = FEATURE.AVNT; 
            else if ( array[1].contains("L") ) feature = FEATURE.AVC; 
            else if ( array[1].contains("N") ) feature = FEATURE.AVN; 
            else if ( array[1].contains("C") ) {}
        }
        Log.d(TAG, "name="+name+", feature="+feature); 
        return feature; 
    }
}