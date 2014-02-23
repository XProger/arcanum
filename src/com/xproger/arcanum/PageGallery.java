package com.xproger.arcanum;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

public class PageGallery extends GridView implements Common.Page {
    public Dialog dialog;	
    private Adapter.GalleryGridAdapter adapter;
	
	public PageGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {
    	adapter = new Adapter.GalleryGridAdapter(this);
    }    
    
    public void setDialog(Dialog dialog) {
    	this.dialog = dialog;
    	update();
    }
    
    public void update() {
    	adapter.update(dialog);
    }
    
	@Override
	public void onMenuInit() {
		Main.main.setActionTitle(getResources().getString(R.string.page_title_media), null);
	}

	@Override
	public void onMenuClick(View view, int id) {
		//
	}	
    
}
