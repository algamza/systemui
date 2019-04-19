package com.humaxdigital.automotive.statusbar.notificationui;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import com.humaxdigital.automotive.statusbar.R;

public class NotificationUI extends LinearLayout {
    private Context mContext;
    private TextView mTitle;
    private TextView mBody;
    private ImageView mIcon;
    private ImageView mLine;

    public NotificationUI(Context context) {
        super(context);
        mContext = context;
        inflate();
    }

    private void inflate() {
        if ( mContext == null ) return;
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.notification, this, true);
        mTitle = (TextView)this.findViewById(R.id.title);
        mBody = (TextView)this.findViewById(R.id.body);
        mIcon = (ImageView)this.findViewById(R.id.icon);
        mLine = (ImageView)this.findViewById(R.id.line);
    }

    public NotificationUI setTitle(String title) {
        if ( mTitle == null ) return this;
        if ( title.equals("") ) {
            mTitle.setVisibility(View.GONE);
            mLine.setVisibility(View.GONE);
        } else {
            mTitle.setVisibility(View.VISIBLE); 
            mLine.setVisibility(View.VISIBLE); 
            mTitle.setText(title);
        }
        return this;
    }

    public NotificationUI setBody(String body) {
        if ( mBody == null ) return this;
        if ( body.equals("") ) {
            mBody.setVisibility(View.GONE);
        } else {
            mBody.setVisibility(View.VISIBLE); 
            mBody.setText(body);
        }
        return this;
    }

    public NotificationUI setIcon(Icon icon) {
        if ( mIcon == null ) return this; 
        if ( icon == null ) {
            mIcon.setVisibility(View.GONE); 
        }
        else {
            mIcon.setVisibility(View.VISIBLE); 
            mIcon.setImageIcon(icon);
        }
        return this;
    }
}
