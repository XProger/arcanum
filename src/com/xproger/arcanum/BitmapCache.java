package com.xproger.arcanum;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.util.LruCache;

public class BitmapCache {	
	private static LruCache<String, Bitmap> cache;
	
	public static class RetainFragment extends Fragment {
	    private static final String TAG = "RetainFragment";
	    public LruCache<String, Bitmap> mRetainedCache;

	    public RetainFragment() {}

	    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
	        RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
	        if (fragment == null) {
	            fragment = new RetainFragment();
	        }
	        return fragment;
	    }

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setRetainInstance(true);
	    }
	}	

	public static void init() {
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
	    final int cacheSize = maxMemory / 2;
	    
	    RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(Main.main.getSupportFragmentManager());
		if ((cache = retainFragment.mRetainedCache) == null) {
			cache = new LruCache<String, Bitmap>(cacheSize) {
			    @Override
			    protected int sizeOf(String key, Bitmap bitmap) {
			        return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
			    }
			};
			retainFragment.mRetainedCache = cache;
		};
	}
	
	public static synchronized void set(String fileName, Bitmap bitmap) {
		if (get(fileName) == null)
			cache.put(fileName, bitmap);
	}
	
	public static synchronized Bitmap get(String fileName) {
		return cache.get(fileName);
	}

	public static Bitmap loadBitmap(String fileName, int maxWidth, int maxHeight, boolean crop, boolean trueColor, boolean resave) {
		Bitmap bmp = get(fileName);
		if (bmp == null) {
			try {
				bmp = Common.loadBitmap(Main.main, fileName, maxWidth, maxHeight, crop, trueColor, resave);
				set(fileName, bmp);
				return bmp;
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
		return bmp;
	}

}
