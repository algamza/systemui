package com.humaxdigital.automotive.systemui.notificationui;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import java.util.HashMap;

import com.humaxdigital.automotive.systemui.R;

public class NotificationUI extends LinearLayout {
    private enum Flag {
        TITLE,
        TEXT,
        ICON,
        DOUBLE_LINE
    }

    private Context mContext;
    private TextView mTitle;
    private TextView mBody;
    private ImageView mIcon;
    private ImageView mLine;
    private HashMap<Flag, Boolean> mFlags = new HashMap<>();

    private String mDataTitle = ""; 
    private String mDataText = ""; 
    private Icon mDataIcon = null;

    public NotificationUI(Context context) {
        super(context);
        mContext = context;
        initFlags();
    }

    private void initFlags() {
        mFlags.put(Flag.TITLE, false);
        mFlags.put(Flag.TEXT, false);
        mFlags.put(Flag.ICON, false);
        mFlags.put(Flag.DOUBLE_LINE, false);
    }

    public void inflate() {
        if ( mContext == null ) return;
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if ( mFlags.get(Flag.TITLE) ) {
            if ( mFlags.get(Flag.DOUBLE_LINE) ) 
                inflater.inflate(R.layout.notification_left_double_line, this, true);
            else 
                inflater.inflate(R.layout.notification_left, this, true);
        } else {
            if ( mFlags.get(Flag.DOUBLE_LINE) ) 
                inflater.inflate(R.layout.notification_double_line, this, true);
            else 
                inflater.inflate(R.layout.notification, this, true);
        }
        
        mTitle = (TextView)this.findViewById(R.id.title);
        mBody = (TextView)this.findViewById(R.id.body);
        mIcon = (ImageView)this.findViewById(R.id.icon);
        mLine = (ImageView)this.findViewById(R.id.line);

        if ( mTitle == null || mBody == null || mIcon == null || mLine == null ) return;
       
        if ( mFlags.get(Flag.TITLE) ) {
            mTitle.setVisibility(View.VISIBLE); 
            mLine.setVisibility(View.VISIBLE); 
            mTitle.setText(mDataTitle);
        } else {
            mTitle.setVisibility(View.GONE);
            mLine.setVisibility(View.GONE);
        }
        
        if ( mFlags.get(Flag.TEXT) ) {
            mBody.setVisibility(View.VISIBLE); 
            mBody.setText(mDataText);
        } else {
            mBody.setVisibility(View.GONE);
        }

        if ( mFlags.get(Flag.ICON) ) {
            mIcon.setVisibility(View.VISIBLE); 
            mIcon.setImageIcon(mDataIcon);
        } else {
            mIcon.setVisibility(View.GONE); 
        }
    }

    public NotificationUI setTitle(String title) {
        if ( title == null || title.equals("") ) return this;
        mDataTitle = title; 
        mFlags.put(Flag.TITLE, true);
        return this;
    }

    public NotificationUI setBody(String body) {
        if ( body == null || body.equals("") ) return this;
        mDataText = body; 
        mFlags.put(Flag.TEXT, true);
        if ( mDataText.contains("\n") )
            mFlags.put(Flag.DOUBLE_LINE, true);
        return this;
    }

    public NotificationUI setIcon(Icon icon) {
        if ( icon == null ) return this;
        mDataIcon = icon; 
        mFlags.put(Flag.ICON, true);
        return this;
    }
}
