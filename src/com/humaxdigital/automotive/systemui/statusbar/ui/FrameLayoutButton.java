package com.humaxdigital.automotive.systemui.statusbar.ui;

import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.humaxdigital.automotive.systemui.R; 

public class FrameLayoutButton extends FrameLayout {
    public FrameLayoutButton(Context context) {
        super(context);
        addButton(); 
    }

    public FrameLayoutButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        addButton();
    }

    public FrameLayoutButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        addButton();
    }

    public FrameLayoutButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        addButton();

    }

    private void addButton() {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_pressed}, this.getResources().getDrawable(R.drawable.co_btn_status_pre));
        states.addState(new int[] {android.R.attr.state_enabled}, this.getResources().getDrawable(R.drawable.co_btn_status_nor));
        this.setBackgroundDrawable(states);
    }
}
