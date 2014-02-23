package com.xproger.arcanum;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class PageIntro extends LinearLayout implements Common.Page, OnPageChangeListener {
	private int bgColor[]		= { 0xf2f2f2, 0xf45c2d, 0xf79317, 0xf8c900, 0x5ac221, 0x2c92e6 };
	private int scrollColor[]	= { 0xb2b2b2, 0xf85d29, 0xfd9415, 0xfbcb00, 0x5bc620, 0x2c94eb };
	private int resIcon[]		= { R.drawable.icon0, R.drawable.icon1, R.drawable.icon2, R.drawable.icon3, R.drawable.icon4, R.drawable.icon5 };
	
	private ViewPager pager;
	private View scroll;
	private ImageView icon;
	
    public PageIntro(Context context, AttributeSet attrs) {
        super(context, attrs);        
    }    
    
    @Override
    public void onFinishInflate() {
    	scroll = findViewById(R.id.scroll);
    	icon = (ImageView)findViewById(R.id.icon);
    	icon.setTag(0);
    	pager = (ViewPager)findViewById(R.id.pager);
    	pager.setOnPageChangeListener(this);
    	pager.setAdapter(new Adapter.IntroPager(getContext()));
    	updateScroll(0, 0.0f);
    }
    
	@Override
	public void onMenuInit() {
	}

	@Override
	public void onMenuClick(View view, int id) {
		if (id == R.id.btn_start)
			Main.main.goAuth();
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {					
		updateScroll(position, positionOffset);
	}

	@Override
	public void onPageScrollStateChanged(int state) {}
	
	@Override
	public void onPageSelected(int position) {
		//icon.setImageResource(resIcon[position]);		
	}
	
	private void updateScroll(int position, float positionOffset) {
	// update image
		int c = bgColor[position];
		if (positionOffset > 0.0f)
			c = Common.lerpColor(c, bgColor[position + 1], positionOffset);
		c |= 0xff << 24;
		icon.setBackgroundColor(c);
		
		int index = position + (positionOffset > 0.5f ? 1 : 0);	
		
		if ((Integer)icon.getTag() != index) {
			icon.setTag(index);
			Common.logError("image");
			icon.setImageResource(resIcon[index]);			
		}
		float alpha = Math.abs((positionOffset - 0.5f) * 2.0f);
		icon.getDrawable().setAlpha((int)(alpha * 255));
		
	// update scroll
		float w = ((View)scroll.getParent()).getWidth() / 6;			
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int)w, LayoutParams.MATCH_PARENT);
		params.gravity = Gravity.LEFT | Gravity.TOP;
		params.setMargins((int)((position + positionOffset) * w), 0, 0, 0);
		
		c = scrollColor[position];
		if (positionOffset > 0.0f)
			c = Common.lerpColor(c, scrollColor[position + 1], positionOffset);
		c |= 0xff << 24;
		scroll.setBackgroundColor(c);		
		scroll.setLayoutParams(params);		
		scroll.invalidate();
	}	
	
	 @Override
	 public void onWindowFocusChanged(boolean hasFocus) {
		 super.onWindowFocusChanged(hasFocus);
		 updateScroll(pager.getCurrentItem(), 0);
	 }
}
		

