package com.xproger.arcanum;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class PageLocation extends FrameLayout implements Common.Page, OnClickListener {
	private GoogleMap map;
	private Updater mapUpdater;
	public Marker mapMarker = null;	
	public int mode;
	private View lay_info;
	private TextView text_name, text_dist;
	private User user;
	private Location userLocation = null;
	private ImageView picture;
	
	public PageLocation(Context context, AttributeSet attrs) {
		super(context, attrs);
	}	
	
	@Override
	public void onFinishInflate() {		
		((Button)findViewById(R.id.btn_send_location)).setOnClickListener(this);
		lay_info = findViewById(R.id.lay_info);
		text_name = (TextView)lay_info.findViewById(R.id.text_name);
		text_dist = (TextView)lay_info.findViewById(R.id.text_dist);
		picture	  = (ImageView)lay_info.findViewById(R.id.picture);
		
		SupportMapFragment mapFragment = ((SupportMapFragment)Main.main.getSupportFragmentManager().findFragmentById(R.id.map));
		
		map = mapFragment.getMap();
		map.getUiSettings().setMyLocationButtonEnabled(false);
		map.setIndoorEnabled(true);
		
		map.setMyLocationEnabled(true);
		
		final LocationManager locManager = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);		
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {

			@Override
			public void onLocationChanged(Location loc) {
					locManager.removeUpdates(this);
			}

			@Override
			public void onProviderDisabled(String arg0) {
				//
			}

			@Override
			public void onProviderEnabled(String arg0) {}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}			
		});		

		map.setOnMapClickListener(new OnMapClickListener() {
			@Override
			public void onMapClick(LatLng point) {
				setMarker(point);
			}
		});	
	}
	
	private void updateDist() {
		Location loc = map.getMyLocation();
		if (user == null || loc == null || userLocation == null)
			return;		
		int dist = (int)loc.distanceTo(userLocation);
		text_dist.setText(String.format(Main.getResStr(R.string.location_dist), dist));
	}
	
	public void waitMyLocation() {
		mapUpdater = new Updater(500, new Runnable() {
			@Override
			public void run() {
				if (Main.main.getCurPage() != Main.main.pageLocation)
					return;
				Location loc = map.getMyLocation();
				if (loc != null) {
					if (mapMarker == null) {
						LatLng point = new LatLng(loc.getLatitude(), loc.getLongitude());  
						setMarker(point);
						cameraGoLocation(point);
					}
					//mapUpdater.stopUpdates();
					updateDist();
				}
			}
		});
		mapUpdater.startUpdates();		
	}
   
	public void setMarker(LatLng point) {
		if (mapMarker == null) {
			mapMarker = map.addMarker(new MarkerOptions()
				.position(point)
				.draggable(true)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin)));
		} else
			mapMarker.setPosition(point);		
	}
	
	public void cameraGoLocation(LatLng point) {
		if (point == null)		
			return;
				
	//	CameraPosition position = map.getCameraPosition();
		CameraPosition.Builder builder = new CameraPosition.Builder();
		builder.zoom(15);
		builder.target(point);
		map.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()));				
	}
	
	public void setUserLocation(LatLng point) {
		userLocation = new Location("");
		userLocation.setLatitude(point.latitude);
		userLocation.setLongitude(point.longitude);		
	}
	
	public void goMyLocation() {
		Location location;
		try {
			location = map.getMyLocation();
		} catch (Exception e) {
			location = null;
		}

		if (location != null) {
			LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
			cameraGoLocation(point);
		}
	}		
	
	public void setMapType(int type) {
		map.setMapType(type);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_send_location) {
			if (mapMarker == null || mapMarker.getPosition() == null) return;			
			Main.main.goDialog(Main.main.pageDialog.dialog);
			Main.main.pageDialog.uploadMediaGeo(mapMarker.getPosition());
		}
	}	
	
	public void showMode(User user) {
		this.user = user;
		Button btn =  (Button)findViewById(R.id.btn_send_location);		
		btn.setVisibility(user == null ? Button.VISIBLE : Button.GONE);
		lay_info.setVisibility(user == null ? Button.GONE : Button.VISIBLE);
		if (user != null) {
			user.getPhoto(picture);
			text_name.setText(user.getTitle());
			text_dist.setText("");
			userLocation = null;
		}
	}

	@Override
	public void onMenuInit() {
		Main.showMenu(R.id.location);
		Main.main.setActionTitle(getResources().getString(user == null ? R.string.page_title_share_location : R.string.page_title_location), null);
	}	
	
	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.btn_map_type :
				Main.main.showPopup(view, R.menu.map_type, false);
				break;		
			case R.id.btn_loc_home :
				goMyLocation();
				break;
			case R.id.btn_loc_normal :
				setMapType(GoogleMap.MAP_TYPE_NORMAL);
				break;
			case R.id.btn_loc_satellite :
				setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				break;
			case R.id.btn_loc_hybrid :
				setMapType(GoogleMap.MAP_TYPE_HYBRID);
				break;		
		}		
	}
	
}
		

