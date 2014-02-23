package com.xproger.arcanum;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ImageViewSquare extends ImageView {

    public ImageViewSquare(Context context) {
        super(context);
    }    	
	
    public ImageViewSquare(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    	

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
	
}
