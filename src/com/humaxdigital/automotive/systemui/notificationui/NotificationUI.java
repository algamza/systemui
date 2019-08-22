package com.humaxdigital.automotive.systemui.notificationui;

import android.content.Context;
import android.content.res.Resources.NotFoundException; 
import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RemoteViews;
import android.widget.FrameLayout;
import android.view.View;
import android.util.Log;
import java.util.HashMap;


import com.humaxdigital.automotive.systemui.R;

public class NotificationUI extends LinearLayout {
    private final String TAG = "NotificationUI";
    private enum Flag {
        TITLE,
        TEXT,
        ICON,
        DOUBLE_LINE,
        VIEWS
    }

    private Context mContext;
    private TextView mTitle;
    private TextView mBody;
    private ImageView mIcon;
    private ImageView mLine;
    private FrameLayout mViews;
    private HashMap<Flag, Boolean> mFlags = new HashMap<>();

    private String mDataTitle = ""; 
    private String mDataText = ""; 
    private Icon mDataIcon = null;
    private RemoteViews mDataRemoteViews = null; 

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
        mFlags.put(Flag.VIEWS, false); 
    }

    public void inflate() {
        if ( mContext == null ) return;
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if ( mFlags.get(Flag.VIEWS)) {
            inflater.inflate(R.layout.notification_remote_view, this, true);
        }
        else if ( mFlags.get(Flag.TITLE) ) {
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
       
        if ( mFlags.get(Flag.VIEWS) ) {
            mViews = (FrameLayout)this.findViewById(R.id.view_container); 
            if ( mViews == null ) return; 
            if ( mDataRemoteViews != null ) {
                try {
                    ViewGroup inf_view = (ViewGroup)mDataRemoteViews.apply(mContext, mViews); 
                    mViews.addView(inf_view, 0); 
                } catch(NotFoundException e) {
                    Log.e(TAG, "not found resource !!"); 
                }
            }
        } else {
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
    }

    public NotificationUI setTitle(String title) {
        if ( title == null || title.equals("") ) return this;
        Log.d(TAG, "setTitle="+title); 
        mDataTitle = title; 
        mFlags.put(Flag.TITLE, true);
        return this;
    }

    public NotificationUI setBody(String body) {
        if ( body == null || body.equals("") ) return this;
        Log.d(TAG, "setBody="+body); 
        mDataText = body; 
        mFlags.put(Flag.TEXT, true);
        if ( mDataText.contains("\n") )
            mFlags.put(Flag.DOUBLE_LINE, true);
        return this;
    }

    public NotificationUI setIcon(Icon icon) {
        if ( icon == null ) return this;
        Log.d(TAG, "setIcon="+icon); 
        mDataIcon = icon; 
        mFlags.put(Flag.ICON, true);
        return this;
    }

    public NotificationUI setRemoteViews(RemoteViews views) {
        if ( views == null ) return this;
        mDataRemoteViews = views.clone();
        Log.d(TAG, "setRemoteViews="+mDataRemoteViews);  
        mFlags.put(Flag.VIEWS, true); 
        return this;
    }

    public void updateBody(String body) {
        if ( body == null || mBody == null || mDataText == null ) return; 
        setBody(body); 
        if ( mFlags.get(Flag.TEXT) ) {
            mBody.setVisibility(View.VISIBLE); 
            mBody.setText(mDataText);
        } else {
            mBody.setVisibility(View.GONE);
        }
    }
}
