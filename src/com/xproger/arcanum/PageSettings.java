package com.xproger.arcanum;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.*;
import android.view.*;

public class PageSettings extends ScrollView implements Common.Page {
	private TextView name, status, phone, first_name, last_name;
	private View lay_name, lay_info;
	private ImageView picture;
	
    public PageSettings(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {    	
		picture = (ImageView)findViewById(R.id.picture);		
		name	= (TextView)findViewById(R.id.text_name);	
		status	= (TextView)findViewById(R.id.text_status);		
		phone	= (TextView)findViewById(R.id.text_phone);
		
		first_name	= (TextView)findViewById(R.id.text_first_name);
		last_name	= (TextView)findViewById(R.id.text_last_name);
		
		lay_name = findViewById(R.id.lay_name);
		lay_info = findViewById(R.id.lay_info);	
		
		findViewById(R.id.btn_opt_save_media).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.settings.set("save_media", ((CheckBox)view).isChecked());
			}
		});
				
		((Button)findViewById(R.id.btn_opt_notify)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.main.goSettingsNotify();
			}
		});
		
		((Button)findViewById(R.id.btn_opt_blocked)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.main.goSettingsBlocked();
			}
		});		

		((Button)findViewById(R.id.btn_logout)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.logout();
			}
		});
		
		Button btn_edit = (Button)findViewById(R.id.btn_edit);		
		btn_edit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				onEditClick();   
			}			
		});
		
		findViewById(R.id.btn_opt_question).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.main.goDialog(Dialog.getDialog(333000, -1, true));   
			}			
		});

    }
    
	public void onEditClick() {
    	lay_name.setVisibility(View.VISIBLE);
    	lay_info.setVisibility(View.GONE);
    	first_name.setText(User.self.first_name);
    	last_name.setText(User.self.last_name);
		Main.main.updateMenu();
	}
	
	public void onDone() {
    	lay_name.setVisibility(View.GONE);
    	lay_info.setVisibility(View.VISIBLE);
    	User.self.first_name = first_name.getText().toString();
    	User.self.last_name = last_name.getText().toString();
    	name.setText(User.self.getTitle());
    	Main.mtp.api_account_updateProfile(User.self.first_name, User.self.last_name);
    	Main.main.updateDialogItem(Dialog.getDialog(User.self.id, -1, false));    	    	
    	Main.main.updateMenu();
    	Common.keyboardHide();    	
	}    
    
    public void updateUser() {
		((TextView)findViewById(R.id.text_name)).setText(User.self.getTitle());
		User.self.getPhoto(picture);
		phone.setText(Common.formatPhone(User.self.phone));
		name.setText(User.self.getTitle());
    	status.setText(User.self.getStatus());
    	((CheckBox)findViewById(R.id.btn_opt_save_media)).setChecked(Main.settings.getBool("save_media"));
    }
    
    public void setPicture(Bitmap bmp) {
    	picture.setImageBitmap(bmp);
    	Main.main.updateUser(User.self);  	
    }

	@Override
	public void onMenuInit() {		
		if (lay_info.getVisibility() == View.GONE) 
			Main.showMenu(R.id.btn_done);
		Main.main.setActionTitle(getResources().getString(R.string.page_title_settings), null);
	}

	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.btn_done :
				onDone();
				break;		
			case R.id.btn_photo_take :
			case R.id.btn_photo_gallery :
				Main.main.chooseAvatar(id == R.id.btn_photo_take); 
				break;
			case R.id.btn_opt_wallpaper :
				Main.main.goWallpaper();
				break;
			case R.id.picture :
				Main.main.showPopup(picture, R.menu.photo, true);
				break;
		}
	}
}
		

