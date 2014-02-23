package com.xproger.arcanum;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

public class MsgTextView extends TextView {
    
    public MsgTextView(Context context, AttributeSet attrs) {
        super(context, attrs);	    
    }

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	    Layout layout = getLayout();
	    if (layout != null) {
	        int width = (int)Math.ceil(getMaxLineWidth(layout)) + getCompoundPaddingLeft() + getCompoundPaddingRight();
	        int height = getMeasuredHeight();            
	        setMeasuredDimension(width, height);
	    }
	}

	private float getMaxLineWidth(Layout layout) {
	    float max_width = 0.0f;
	    int lines = layout.getLineCount();
	    for (int i = 0; i < lines; i++) {
	        if (layout.getLineWidth(i) > max_width) {
	            max_width = layout.getLineWidth(i);
	        }
	    }
	    return max_width;
	}	
}	
