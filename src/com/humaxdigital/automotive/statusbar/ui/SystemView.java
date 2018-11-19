package com.humaxdigital.automotive.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.humaxdigital.automotive.statusbar.R;

import java.util.HashMap;

public class SystemView extends FrameLayout {
    private Context mContext;
    private int mStatus = 0;
    private HashMap<Integer,Drawable> mIcons = new HashMap<>();

    public SystemView(Context context) {
        super(context);
        mContext = context;
    }

    public SystemView inflate() {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.system_menu, this, true);
        ((ImageView)this.findViewById(R.id.system_menu)).setImageDrawable(mIcons.get(mStatus));
        return this;
    }

    public SystemView addIcon(int status, Drawable icon) {
        mIcons.put(status, icon);
        return this;
    }

    public SystemView update(int status) {
        Drawable drawable = mIcons.get(status);
        if ( drawable == null ) return null;
        mStatus = status;
        ((ImageView)this.findViewById(R.id.system_menu)).setImageDrawable(mIcons.get(mStatus));
        return this;
    }
}
