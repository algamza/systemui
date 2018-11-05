package com.humaxdigital.automotive.statusbar.service;

import android.graphics.BitmapFactory; 
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.ByteArrayOutputStream;

public class BitmapParcelable implements Parcelable {
    private Bitmap mData = null;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        byte[] buffer = bitmapToByteArray(mData);
        out.writeInt(buffer.length); 
        out.writeByteArray(buffer); 
    }

    public static final Parcelable.Creator<BitmapParcelable> CREATOR
            = new Parcelable.Creator<BitmapParcelable>() {
        public BitmapParcelable createFromParcel(Parcel in) {
            return new BitmapParcelable(in);
        }

        public BitmapParcelable[] newArray(int size) {
            return new BitmapParcelable[size];
        }
    };
    
    private BitmapParcelable(Parcel in) {
        int length = in.readInt(); 
        byte[] buffer = new byte[length]; 
        in.readByteArray(buffer); 
        mData = byteArrayToBitmap(buffer); 
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); 
        return stream.toByteArray(); 
    }

    private Bitmap byteArrayToBitmap(byte[] bytearray) {
        return BitmapFactory.decodeByteArray(bytearray, 0, bytearray.length); 
    }

    public Bitmap getBitmap() {
        return mData; 
    }

    public BitmapParcelable(Bitmap bitmap) {
        mData = bitmap; 
    }
}