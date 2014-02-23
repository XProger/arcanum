package com.xproger.arcanum;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.SupportMapFragment;

public class PageMapFragment extends SupportMapFragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		setMapTransparent((ViewGroup) view);
		return view;
	};

	private void setMapTransparent(ViewGroup group) {
		int childCount = group.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View child = group.getChildAt(i);
			if (child instanceof ViewGroup) {
				setMapTransparent((ViewGroup) child);
			} else 
				if (child instanceof SurfaceView) {
					child.setBackgroundColor(0x00000000);
				}
		}
	}
}
