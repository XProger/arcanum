package com.xproger.arcanum;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

public class PageUserInfo extends ScrollView implements Common.Page {
	public User user;
	private ImageView picture;
	private TextView name, status, phone, sound, media, first_name, last_name;
	private Uri soundUri = Common.soundDef;
	private View lay_name, lay_info, lay_phone;
	private Button btn_share;
	private CheckBox btn_notify;
	
    public PageUserInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {
		picture	= (ImageView)findViewById(R.id.picture);
		name	= (TextView)findViewById(R.id.text_name);	
		status	= (TextView)findViewById(R.id.text_status);		
		phone	= (TextView)findViewById(R.id.text_phone);
		sound	= (TextView)findViewById(R.id.text_sound);
		media	= (TextView)findViewById(R.id.text_media);		
		
		first_name	= (TextView)findViewById(R.id.text_first_name);
		last_name	= (TextView)findViewById(R.id.text_last_name);
		
		lay_name = findViewById(R.id.lay_name);
		lay_info = findViewById(R.id.lay_info);
		lay_phone = findViewById(R.id.lay_phone);
		
		findViewById(R.id.spinner).setVisibility(View.GONE);
		
		findViewById(R.id.btn_opt_sound).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Main.main.mediaRingtone(soundUri, new Common.MediaReq() {					
					@Override
					public void onMedia(Uri media) {
						if (media == null) return;	
						setSoundUri(media);
						saveNotify();
					}
				});
			}
		});
		
		findViewById(R.id.btn_edit).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				onEditClick();       
			}			
		});
		
		findViewById(R.id.btn_msg).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.main.goDialog(Dialog.getDialog(user.id, -1, true));       
			}			
		});		
		
		btn_share = (Button)findViewById(R.id.btn_share);
		btn_notify = (CheckBox)findViewById(R.id.btn_opt_notify);
		btn_notify.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				saveNotify();
			}			
		});		
    }
    
    public void setUser(User user) {
    	this.user = user;
    	user.getPhoto(picture);
    	name.setText(user.getTitle());
    	status.setText(user.getStatus());
    	phone.setText(Common.formatPhone(user.phone));
    	media.setText("91");    	
    	lay_name.setVisibility(View.GONE);
    	lay_info.setVisibility(View.VISIBLE);

    	boolean foreign = user.phone == null || user.phone.equals("");    	
    	lay_phone.setVisibility(foreign ? View.GONE : View.VISIBLE);
    	btn_share.setVisibility(foreign ? View.VISIBLE : View.GONE);
    	
    	Dialog dialog = Dialog.getDialog(user.id, -1, true);
    	String str = String.valueOf(dialog.getMediaList().size());
    	if (!dialog.noHistory)
    		str += "+";
    	media.setText(str);
    	getNotify();
    }    
    
	@Override
	public void onMenuInit() {
		
		if (lay_info.getVisibility() == View.GONE) {
			Main.showMenu(R.id.btn_done);
		} else
			Main.showMenu(R.id.user_info);
		
		
		Main.main.setActionTitle(getResources().getString(R.string.page_title_user_info), null);
	}
	
	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.btn_done :
				onDone();
				break;
			case R.id.user_info :
				PopupMenuAB popup = Main.main.showPopup(view, R.menu.contact, false);
				if (user.client_id == 0)
					popup.getMenu().getItem(0).setVisible(false);
				break;
			case R.id.btn_view :
				if (user.client_id == 0)
					return;
				Common.logError("view contact: " + user.client_id);
				try {
					Intent intent = new Intent(Intent.ACTION_VIEW);	
					Uri uri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, user.client_id);
//					Uri uri = Uri.withAppendedPath(ContactsContract.RawContacts.CONTENT_URI, String.valueOf(user.client_id));
					Common.logError(uri.toString());
					intent.setData(uri);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getContext().startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case R.id.btn_share :
				Main.main.goShare(user);
				break;
			case R.id.btn_block :
				Main.mtp.api(null, null, "contacts.block", user.getInputUser());
				break;			
			case R.id.btn_opt_media :
				Main.main.goGallery(Dialog.getDialog(user.id, -1, true));
				break;
		}
	}	
	
	public void onEditClick() {
    	lay_name.setVisibility(View.VISIBLE);
    	lay_info.setVisibility(View.GONE);
    	first_name.setText(user.first_name);
    	last_name.setText(user.last_name);
		Main.main.updateMenu();
	}
	
	public void onDone() {
    	lay_name.setVisibility(View.GONE);
    	lay_info.setVisibility(View.VISIBLE);
    	user.first_name = first_name.getText().toString();
    	user.last_name = last_name.getText().toString();
    	name.setText(user.getTitle());    	
    	Main.main.updateMenu();    	
    	Common.keyboardHide();
		
    	//ArrayList<TL.Object> contact = new ArrayList<TL.Object>();
    	//contact.add(TL.newObject("inputPhoneContact", 1L, user.phone, user.first_name, user.last_name));
    	//Main.mtp.api_contacts_importContacts(contact, false);
    	SyncUtils.updateContact(Main.main, user);
	}

    public void saveNotify() {
    	TL.Object notifyPeer = TL.newObject("inputNotifyPeer", user.getInputPeer());
    	int mute_until = btn_notify.isChecked() ? 0 : (Common.getUnixTime() + Common.UNIX_YEAR);
    	TL.Object notifySettings = TL.newObject("inputPeerNotifySettings", mute_until, soundUri.toString(), true, TL.newObject("peerNotifyEventsAll"));
		Main.mtp.api(null, null, "account.updateNotifySettings", notifyPeer, notifySettings);    	
		getNotify();
    }
    
    public void setSoundUri(Uri uri) {    	
		String snd = uri.toString();
		if (!snd.equals("") && !snd.equals("default")) {
			soundUri = uri;
        	Ringtone ringtone = RingtoneManager.getRingtone(Main.main, soundUri);		
        	if (ringtone != null) {
	        	snd = ringtone.getTitle(Main.main);
	        	if (snd.equals(Integer.toString(R.raw.sound_a)))
	        		snd = "";
        	} else
        		snd = "";
		} else
			snd = "";							
		if (snd.equals("")) {
			snd = getResources().getString(R.string.sound_def);
			soundUri = Common.soundDef;
		}
		sound.setText(snd);
    }
    
    public void getNotify() {
    	Main.mtp.api(new TL.OnResultRPC() {
			@Override
			public void onResultRPC(final TL.Object result, Object param, boolean error) {
				if (error) return; 
				status.post(new Runnable() {
					@Override
					public void run() {
						if (result.id == 0x70a68512) {
							sound.setText(getResources().getString(R.string.sound_def));
							btn_notify.setChecked(true);
							return;
						}
						setSoundUri(Uri.parse(result.getString("sound")));
						btn_notify.setChecked(result.getInt("mute_until") < Common.getUnixTime());
					}
				});

			}    		
    	}, null, "account.getNotifySettings", TL.newObject("inputNotifyPeer", user.getInputPeer()));    	
    }	
}

