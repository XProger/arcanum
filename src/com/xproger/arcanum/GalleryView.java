package com.xproger.arcanum;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Gallery;

// Gallery в Android 2.3 при обновлении элементов во время анимации
// сбрасывает позицию и дёргается. Лучей поноса им...
public class GalleryView extends Gallery {
	public boolean canUpdate = true;
	
	public GalleryView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }        

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (canUpdate) {
			canUpdate = false;
			super.onLayout(changed, l, t, r, b);		
		}
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		Common.logError("aaa");
		if (w != oldw || h != oldh) {
			canUpdate = true;
			super.onSizeChanged(w, h, oldw, oldh);
		}
	}
}
