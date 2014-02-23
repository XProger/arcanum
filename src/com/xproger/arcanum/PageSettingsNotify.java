package com.xproger.arcanum;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;

public class PageSettingsNotify extends ScrollView implements Common.Page {
	private TL.OnResultRPC onSettings;
	private View lay_users, lay_chats;	
	
    public PageSettingsNotify(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {
    	lay_users = findViewById(R.id.lay_users);
    	lay_chats = findViewById(R.id.lay_chats);
    	
    	OnClickListener onSoundClick = new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final TextView sound = (TextView)v.findViewById(R.id.text_sound); 
				Uri uri = (Uri)sound.getTag();
				if (uri == null)
					uri = Common.soundDef;
				Main.main.mediaRingtone(uri, new Common.MediaReq() {					
					@Override
					public void onMedia(Uri media) {
						if (media == null) return;	
						setSound(sound, media.toString());
						saveNotify((View)v.getParent());
					}
				});
			}
		};
    	
		lay_users.findViewById(R.id.btn_sound).setOnClickListener(onSoundClick);
		lay_chats.findViewById(R.id.btn_sound).setOnClickListener(onSoundClick);
		
		findViewById(R.id.btn_inapp_sound).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.settings.set("inapp_sound", ((CheckBox)view).isChecked());
			}
		});		
		
		findViewById(R.id.btn_inapp_vibrate).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.settings.set("inapp_vibrate", ((CheckBox)view).isChecked());
			}
		});
		
		findViewById(R.id.btn_inapp_prev).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.settings.set("inapp_preview", ((CheckBox)view).isChecked());
			}
		});
				
		findViewById(R.id.btn_reset).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				resetNotify();
			}
		});
		
		OnClickListener onAlertClick = new OnClickListener() {
			@Override
			public void onClick(View view) {
				View v = (View)view.getParent();
				setAlert(v, ((CheckBox)view).isChecked());
				saveNotify(v);
			}			
		};
		
		OnClickListener onPrevClick = new OnClickListener() {
			@Override
			public void onClick(View view) {
				saveNotify((View)view.getParent());
			}			
		};

		lay_users.findViewById(R.id.btn_alert).setOnClickListener(onAlertClick);
		lay_users.findViewById(R.id.btn_prev).setOnClickListener(onPrevClick);
		
		lay_chats.findViewById(R.id.btn_alert).setOnClickListener(onAlertClick);
		lay_chats.findViewById(R.id.btn_prev).setOnClickListener(onPrevClick);
		
    	onSettings = new TL.OnResultRPC() {
			@Override
			public void onResultRPC(final TL.Object result, Object param, boolean error) {
				if (error) return;
				final View v = ((View)param);
				post(new Runnable() {
					@Override
					public void run() {
						setAlert(v, result.getInt("mute_until") < Common.getUnixTime());
						((CheckBox)v.findViewById(R.id.btn_prev)).setChecked(result.getBool("show_previews"));
						setSound((TextView)v.findViewById(R.id.text_sound), result.getString("sound"));
					}
				});
			}
    	};    	
    }
    
    public void setAlert(View v, boolean checked) {
    	((CheckBox)v.findViewById(R.id.btn_alert)).setChecked(checked);
		v.findViewById(R.id.btn_prev).setEnabled(checked);
		v.findViewById(R.id.btn_sound).setEnabled(checked);    	
    }

    public void update() {
    	Main.mtp.api(onSettings, lay_users, "account.getNotifySettings", TL.newObject("inputNotifyUsers"));
    	Main.mtp.api(onSettings, lay_chats, "account.getNotifySettings", TL.newObject("inputNotifyChats"));
    	
		((CheckBox)findViewById(R.id.btn_inapp_sound)).setChecked(Main.settings.getBool("inapp_sound"));
		((CheckBox)findViewById(R.id.btn_inapp_vibrate)).setChecked(Main.settings.getBool("inapp_vibrate"));
		((CheckBox)findViewById(R.id.btn_inapp_prev)).setChecked(Main.settings.getBool("inapp_preview"));
    }
    
    public void saveNotify(View v) {
    	int mute = ((CheckBox)v.findViewById(R.id.btn_alert)).isChecked() ? 0 : (Common.getUnixTime() + Common.UNIX_YEAR);
    	boolean previews = ((CheckBox)v.findViewById(R.id.btn_prev)).isChecked();
    	Uri uri = (Uri)v.findViewById(R.id.text_sound).getTag();    	
    	String sound = uri != null ? uri.toString() : Common.soundDef.toString();
    	
    	TL.Object obj = TL.newObject(v == lay_users ? "inputNotifyUsers" : "inputNotifyChats");    	
    	TL.Object settings = TL.newObject("inputPeerNotifySettings", mute, sound, previews, TL.newObject("inputPeerNotifyEventsAll"));    			
    	Main.mtp.api(null, null, "account.updateNotifySettings", obj, settings);
    }
    
    public void resetNotify() {
        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());		
        Resources res = getResources();
        
    	adb.setTitle(res.getString(R.string.dialog_title_reset));		        
    	adb.setMessage(res.getString(R.string.dialog_reset));		        	
        	
        adb.setNegativeButton(res.getString(R.string.dialog_btn_cancel), null);
        adb.setPositiveButton(res.getString(R.string.dialog_btn_reset), new AlertDialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
		    	Main.mtp.api(new TL.OnResultRPC() {
					@Override
					public void onResultRPC(final TL.Object result, Object param, boolean error) {
						if (error) return;
						update();
					}}, null, "account.resetNotifySettings"); 
			}});		       
        adb.show();  
    }
    
    public void setSound(TextView sound, String path) {
    	Uri uri = null;
		if (path != null && !path.equals("") && !path.equals("default")) {
			uri = Uri.parse(path);
        	Ringtone ringtone = RingtoneManager.getRingtone(Main.main, uri);		
        	if (ringtone != null) {
        		path = ringtone.getTitle(Main.main);
	        	if (path.equals(Integer.toString(R.raw.sound_a)))
	        		path = null;
        	} else
        		path = null;
		} else
			path = null;	
		
		if (path == null) {
			path = getResources().getString(R.string.sound_def);
			uri = Common.soundDef;
		}
		
		sound.setText(path);
		sound.setTag(uri);
    }    
    
	@Override
	public void onMenuInit() {
		Main.main.setActionTitle(getResources().getString(R.string.settings_opt_notify), null);
	}

	@Override
	public void onMenuClick(View view, int id) {
		// TODO Auto-generated method stub
		
	}

	
}
		

