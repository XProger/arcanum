package com.xproger.arcanum;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class PageWallpaper extends RelativeLayout implements Common.Page, OnItemSelectedListener {
	private Adapter.WallpaperAdapter adapter;
	private GalleryView list;
	private ImageView preview;	
	private String customPath;
	private int wp_index, customColor;
	private View lay_custom;

	public PageWallpaper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    	
    @Override
    public void onFinishInflate() {    	
    	preview = (ImageView)findViewById(R.id.preview);
    	lay_custom = findViewById(R.id.lay_custom);
    	list = (GalleryView)findViewById(R.id.list);
    	list.setCallbackDuringFling(false);
    	list.setAdapter(adapter	= new Adapter.WallpaperAdapter());
    	list.setOnItemSelectedListener(this);
    	update();
    }    
    
    public static void getWallpapers() {
    	Main.mtp.api(new TL.OnResultRPC() {			
			@Override
			public void onResultRPC(TL.Object result, Object param, boolean error) {
				if (error) return;
				TL.Vector wp = (TL.Vector)result;
				Main.settings.set("wp", wp);				
				Main.settings.set("wp_index", wp.count > 0 ? 0 : -1);
				
				Main.main.mainView.post(new Runnable() {
					@Override
					public void run() {
						if (Main.main.pageWallpaper != null)
							Main.main.pageWallpaper.update();
						PageDialog.updateWallpaper();
					}
				});
			}
		}, null, "account.getWallPapers");    	
    }
    
    public void update() {
    	list.canUpdate = true;
    	adapter.update(Main.settings.getVector("wp"));
    	wp_index	= Main.settings.getInt("wp_index");
    	customPath	= Main.settings.getString("customPath");
    	customColor	= Main.settings.getInt("customColor");    
    	
    	if (list.getSelectedItemPosition() == (wp_index + 1))
    		updateItem();
    	else
    		list.setSelection(wp_index + 1);
    }
    
	@Override
	public void onMenuInit() {
		Main.main.setActionTitle(getResources().getString(R.string.page_title_wallpaper), null);
	}

	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.btn_cancel :
				Main.main.goBack();
				break;
			case R.id.btn_set :			
		    	Main.settings.set("wp_index", wp_index);
		    	Main.settings.set("customPath", customPath);
		    	Main.settings.set("customColor", customColor);    				
				PageDialog.updateWallpaper();
				Main.main.goBack();				
				break;
			case R.id.btn_choose_color :
				Common.logError("select color");
				customPath = "";
				// customColor = ...
				setCustomImage();
				break;
			case R.id.btn_choose_image :
				Common.MediaReq req = new Common.MediaReq() {
					@Override
					public void onMedia(final Uri media) {
						customPath = Common.getPathFromURI(getContext(), media);						
						setCustomImage();
						/*
				        new Thread( new Runnable() {
				        	@Override
				        	public void run() {
				        		
				        		// fileName
				        	}
				        } ).start();
				        */
					}
				};	  				
				Main.main.mediaPhoto(false, Main.DISPLAY_WIDTH, Main.DISPLAY_HEIGHT, req);
				break;
		}
	}

	public static String getScreenType(TL.Vector sizes) {
		if (sizes == null || sizes.count == 0)
			return null;
		String str[] = {"w", "y", "x", "m"};
		int i = Main.DISPLAY_WIDTH <= 800 ? 1 : 0;
		while (i < str.length) {
			if (Common.getPhotoSizeTL(sizes, str[i]) != null)
				return str[i];
			i++;
		}
		return sizes.getObject(sizes.count - 1).getString("type");
	}
	
	public static TL.Object getWallpaper(int wp_index) {
		if (wp_index == -1)
			return null;
		return Main.settings.getVector("wp").getObject(wp_index);
	}
	
	public static TL.Object getSize(int wp_index) {
		if (wp_index == -1)
			return null;
		TL.Vector sizes = getWallpaper(wp_index).getVector("sizes");
		return Common.getPhotoSizeTL(sizes, getScreenType(sizes));
	}
	
	public static String getPath(int wp_index) {		
		TL.Object size = getSize(wp_index);		
		if (size == null || size.id == 0xe17e23c)
			return null;	
		return Main.CACHE_PATH + FileQuery.getName(size.getObject("location"));	
	}
	
	public void setCustomImage() {
		Common.logError("customPath: " + customPath);
		preview.setTag(null);							
		if (customPath.equals("")) {
			preview.setImageDrawable(null);
			preview.setBackgroundColor(customColor);
			return;
		}
		Common.loadBitmapThread(Main.main, preview, customPath);   
	}
	
	public static void loadWallpaper(ImageView img, int wp_index) {
		Common.loadImageTL(img, getSize(wp_index), Main.DISPLAY_WIDTH, Main.DISPLAY_HEIGHT, true, false);
	}
	
	public void updateItem() {
		if (wp_index == -1) {
			lay_custom.setVisibility(View.VISIBLE);
			setCustomImage();			
		} else {
			lay_custom.setVisibility(View.GONE);
			loadWallpaper(preview, wp_index);
		}
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		wp_index = position - 1;
		updateItem();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {}	
    
}
