package com.xproger.arcanum;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.lang.reflect.Field;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Gallery;
import android.widget.Scroller;

public class MainPager extends ViewPager {
	
    public class SpeedScroller extends Scroller {
	    private int mDuration = 250;
	
	    public SpeedScroller(Context context) {
	        super(context);
	    }
	
	    public SpeedScroller(Context context, Interpolator interpolator) {
	        super(context, interpolator);
	    }
	
	    @Override
	    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
	        super.startScroll(startX, startY, dx, dy, mDuration);
	    }
	
	    @Override
	    public void startScroll(int startX, int startY, int dx, int dy) {
	        super.startScroll(startX, startY, dx, dy, mDuration);
	    }
	}	
	
	public Adapter.ChatPagerAdapter adapter;
	
    public MainPager(Context context, AttributeSet attrs) {
        super(context, attrs);
      //  setPageTransformer(true, new DepthPageTransformer());
    }
	
    @Override
    public void onFinishInflate() {  
        setAdapter(adapter = new Adapter.ChatPagerAdapter(getContext(), this, 5));
        
        try {
            Field mScroller;
            mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true); 
            SpeedScroller scroller = new SpeedScroller(getContext(), new DecelerateInterpolator());
            // scroller.setFixedDuration(5000);
            mScroller.set(this, scroller);
        } catch (Exception e) {}        
    }
/*
    @Override
    public boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
    	if (dx > 0 && getCurrentItem() <= adapter.getPagesCount() - 1)
    		return false;
    	return super.canScroll(v, checkV, dx, x, y);
    }
*/
    
    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
       if(v != this && ((v instanceof ViewPager) || (v instanceof Gallery) || (v instanceof ImageViewTouch) ))
          return true;       
       return super.canScroll(v, checkV, dx, x, y);
    }
    
	public void setPages(View ... views) {
		adapter.setPages(views);
	}
	
	public View getPage(int position) {
		return adapter.getPage(position);		
	}
}
