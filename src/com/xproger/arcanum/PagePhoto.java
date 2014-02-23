package com.xproger.arcanum;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PagePhoto extends RelativeLayout implements Common.Page, OnPageChangeListener {
	private Adapter.PhotoPagerAdapter adapter;
	public ProgressBar progress;
	public Dialog dialog;
	private String mediaPath, videoPath;	
	private ViewPager pager;
	private TextView title, date, menu, btn_view;
	private View btn_load;
    
	public PagePhoto(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {
    	menu		= (TextView)findViewById(R.id.title_menu);    
    	title		= (TextView)findViewById(R.id.text_title);
    	date		= (TextView)findViewById(R.id.text_date);
    	btn_view	= (TextView)findViewById(R.id.btn_view);
    	btn_load	= findViewById(R.id.btn_load);
    	progress	= (ProgressBar)btn_load.findViewById(R.id.progress);
    	pager		= (ViewPager)findViewById(R.id.pager);  
    	
    	adapter = new Adapter.PhotoPagerAdapter(pager, this);
    	pager.setOnPageChangeListener(this);      	
    }
    
    private TL.Object getSizeItem(String type, TL.Vector photoSizes, boolean exists) {
		for (int j = 0; j < photoSizes.count; j++) {
			TL.Object item = photoSizes.getObject(j);					
			if (item.id != 0xe17e23c && type.equals(item.getString("type")) && ( !exists || FileQuery.exists(item.getObject("location")) != null ) )
				return item;
		}
		return null;
    }
    
	private TL.Object getExistsPhoto(TL.Vector photoSizes, boolean thumb, boolean exists) {
		String sizes[] = {"s", "a", "m", "b", "x", "c", "y", "d"};
	// FIX! говнокод
		TL.Object item = null;
		if (thumb) {
			for (int i = 0; i < sizes.length; i++)
				if ((item = getSizeItem(sizes[i], photoSizes, exists)) != null)
						break;			
		} else {
			for (int i = sizes.length - 1; i >= 4; i--)
				if ((item = getSizeItem(sizes[i], photoSizes, exists)) != null)
					break;				
		}
		return item;
	}
	
	public void goPage(Dialog.Message msg) {
		dialog = msg.dialog;
		update();		
		pager.setCurrentItem(adapter.getPosition(msg));
	}
    
	public void update() {
		adapter.update(dialog);
		updateHeader();
	}
	
    public void getMedia(final Dialog.Message msg, ImageView img) { 
    	videoPath = null;
    	mediaPath = null;    	
    	if (!msg.hasMedia()) return;
    	
    	if (msg.media.id != 0xc8c45a2a && msg.media.id != 0xa2d24290) return; // not Photo/Video media 
    	
    	TL.Object bigSize = null, thumbSize = null;
    	
    	if (msg.media.id == 0xc8c45a2a) {	// messageMediaPhoto
    		TL.Object inputPhoto = msg.media.getObject("photo");
        	if (inputPhoto == null) return;
        	TL.Vector photoSizes = inputPhoto.getVector("sizes");
        	        	
        	bigSize = getExistsPhoto(photoSizes, false, true);
    		if (bigSize != null && (mediaPath = FileQuery.exists(bigSize.getObject("location"))) != null) {
        		Common.loadBitmapThread(Main.main, img, mediaPath);
        		return;
    		}           	
        	
        	thumbSize = getExistsPhoto(photoSizes, true, true);        	
    		if (thumbSize != null && (mediaPath = FileQuery.exists(thumbSize.getObject("location"))) != null)
    			Common.loadBitmapThread(Main.main, img, mediaPath);
        	
        	bigSize = getExistsPhoto(photoSizes, false, false);    	
    	} else {	// messageMediaVideo
    		TL.Object video = msg.media.getObject("video");
    		
    		videoPath = null; // getVideoPath or null
    		    		
    		bigSize = video.getObject("thumb");
    		if (bigSize.id == 0xe17e23c)
    			bigSize = null;
    		
        	if (bigSize != null) {
        		mediaPath = FileQuery.exists(bigSize.getObject("location"));
        		if (mediaPath != null) {
        			Common.loadBitmapThread(Main.main, img, mediaPath);
        			return;
        		}        		        		
           	} else {
           		img.setImageResource(R.drawable.video_placeholder);
    			return;
           	}
    	}
    	
    	if (bigSize == thumbSize)
    		return;

    	final TL.Object location = bigSize.getObject("location");    	
    	final WeakReference<ImageView> ref = new WeakReference<ImageView>(img);
    	
		try {
	    	final int size = bigSize.getInt("size");
			String path = (videoPath != null ? Main.CACHE_PATH : Main.MEDIA_PATH) + FileQuery.getName(location) + ".jpg";
			
			msg.progress = 0;
			msg.query = new FileQuery(location, path, new FileQuery.OnResultListener() {			
				@Override
				public void onResult(TL.Object result) {
					msg.query = null;
					if (result == null)
						return;
				// save to gallery
					mediaPath = result.getString("fileName");
					Common.loadBitmapThread(Main.main, (ImageView)ref.get(), mediaPath);
				}

				@Override
				public void onProgress(final float value) {
					msg.progress = (float)value / (float)size;
					updateProgress(msg);					
				}
			});
		} catch (Exception e) {			
			Common.logError("error query file");
			e.printStackTrace();
		}
    	
    }

    private void updateHeader() {
    	int position = pager.getCurrentItem();
    	
		if (position > adapter.getCount() - 5)
			dialog.queryHistory(20);		
		Dialog.Message msg = adapter.getMessage();
		if (msg == null) {
			Main.main.goBack();
			return;
		}
		
		User user = User.getUser(msg.from_id);
		title.setText(user.getTitle());
		date.setText(new SimpleDateFormat("dd.MM.yyyy").format(msg.date * 1000L));
		
		String str = String.format(Main.getResStr(R.string.photo_title), position + 1, adapter.getCount());
		if (!dialog.noHistory)
			str += "+";		
		menu.setText(str);
		
		updateProgress(adapter.getMessage());
    }
    
    public void updateProgress(final Dialog.Message msg) {    	
    	if (msg != adapter.getMessage() || msg == null)
    		return;    	
    	
		progress.post(new Runnable() {
			@Override
			public void run() {
		    	if (msg.query != null && msg.progress < 0.9999f) {
		    		btn_view.setVisibility(View.GONE);
		    		btn_load.setVisibility(View.VISIBLE);
		    		progress.setProgress((int)(msg.progress * 100f));
		    	} else {
		    		btn_load.setVisibility(View.GONE);
		    		if (msg.media.id == 0xa2d24290) {	// messageMediaVideo
		    			btn_view.setVisibility(View.VISIBLE);
		    			btn_view.setText(msg.getMediaViewText());
		    		} else
		    			btn_view.setVisibility(View.GONE);
		    	}
			}
		});
		
    	    	
    }
    
	@Override
	public void onMenuInit() {
	}

	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.btn_share :
				if (mediaPath == null)
					return;
				try {
					Intent intent = new Intent(Intent.ACTION_SEND);		
					intent.setType(videoPath != null ? "video/*" : "image/jpeg");
					intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + (videoPath != null ? videoPath : mediaPath) ));	
					Main.main.startActivity(Intent.createChooser(intent, Main.getResStr(R.string.choose_share_media)));
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case R.id.btn_delete :
				dialog.deleteMessages(adapter.getMessage());
				break;
			case R.id.btn_other :
				Main.main.showPopup(view, R.menu.media, true, R.style.Theme_Arcanum_Black);
				break;
			case R.id.btn_media_gallery :
				Main.main.goGallery(dialog);
				break;
			case R.id.btn_media_forward :
				Dialog.Message m = adapter.getMessage();
				if (m == null)
					return;
				ArrayList<Integer> msgs = new ArrayList<Integer>();
				msgs.add(m.id);
				Main.main.goForward(msgs);
				break;				
			case R.id.title_menu :
				Main.main.goBack();
				break;
			case R.id.btn_view :
				Dialog.Message vmsg = adapter.getMessage();
				if (vmsg != null && vmsg.query == null)
					PageDialog.mediaPlay(vmsg);
				break;
			case R.id.btn_load :
				Dialog.Message msg = adapter.getMessage();
				if (msg != null && msg.query != null)
					msg.query.cancel();
				break;
		}
	}

	@Override
	public void onPageSelected(int position) {
		updateHeader();
	}
	
	@Override
	public void onPageScrollStateChanged(int arg0) {}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {}	    
}
