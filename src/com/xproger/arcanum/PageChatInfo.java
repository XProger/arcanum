package com.xproger.arcanum;

import android.app.AlertDialog;
import android.content.*;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.*;
import android.view.View.*;
import android.widget.*;

public class PageChatInfo extends ScrollView implements Common.Page, OnClickListener, OnLongClickListener {
	public Chat chat;
	private ImageView picture;
	private TextView name, status, sound, media, members;
	private EditText title;
	private Uri soundUri = Common.soundDef;
	private View lay_info;
	private Button btn_done;
	private CheckBox btn_notify;
	private LinearLayout list;
	
    public PageChatInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {
		picture	= (ImageView)findViewById(R.id.picture);
		name	= (TextView)findViewById(R.id.text_name);	
		status	= (TextView)findViewById(R.id.text_status);		
		sound	= (TextView)findViewById(R.id.text_sound);
		media	= (TextView)findViewById(R.id.text_media);
		members	= (TextView)findViewById(R.id.text_members);
		list	= (LinearLayout)findViewById(R.id.list_users);	
		
		findViewById(R.id.btn_add).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addMember();
			}
		});
				
		lay_info	= findViewById(R.id.lay_info);		
		title		= (EditText)findViewById(R.id.text_title);
		
		picture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Main.main.showPopup(picture, R.menu.photo, true);
			}
		});		
		
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
		
		findViewById(R.id.btn_delete).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
		        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());		
		        Resources res = getResources();
		        
		        final Dialog d = Dialog.getDialog(-1, chat.id, true);
	        	adb.setTitle(res.getString(R.string.dialog_title_group));		        
	        	adb.setMessage(res.getString(R.string.dialog_delete_group));		        			        	
		        adb.setNegativeButton(res.getString(R.string.dialog_btn_cancel), null);
		        adb.setPositiveButton(res.getString(R.string.dialog_btn_delete), new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						d.leave();
					}});		       
		        adb.show(); 				
			}			
		});		
		
		btn_notify = (CheckBox)findViewById(R.id.btn_opt_notify);
		btn_notify.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				saveNotify();
			}			
		});			
    }

    public void setChat(Chat chat) {
    	this.chat = chat;
    	getNotify();
    	chat.getPhoto(picture);
    	name.setText(chat.getTitle());
    	status.setText(chat.getStatus());
    	
    	updateList();
		scrollTo(0, 0);				
		
    	title.setVisibility(View.GONE);
    	lay_info.setVisibility(View.VISIBLE);
    	
    	Dialog dialog = Dialog.getDialog(-1, chat.id, true);
    	String str = String.valueOf(dialog.getMediaList().size());
    	if (!dialog.noHistory)
    		str += "+";
    	media.setText(str);    	
    }    
    
    public void updateList() {
    	list.removeAllViews();
		for (int i = 0; i < chat.users.size(); i++) {
			User user = chat.users.get(i);
			if (user == null) continue;
	    	DialogItem item = (DialogItem)Main.inflater.inflate(R.layout.item_dialog, list, false);
	    	item.setContact(new Adapter.Contact(user));
	    	item.setClickable(true);
	    	item.setBackgroundResource(R.drawable.btn_option);
	    	item.setOnClickListener(this);	    	
	    	item.setOnLongClickListener(this);
	    	list.addView(item);
	    	
	    	View view = new View(getContext());
	    	view.setBackgroundResource(R.drawable.divider);
	    	list.addView(view);
		}				
		int count = chat.users.size();
		members.setText(String.format(getResources().getString(count > 1 ? R.string.info_header_members : R.string.info_header_member), count));
    }
    
    public void setPicture(Bitmap bmp) {
    	picture.setImageBitmap(bmp);
    	Main.main.updateChat(chat);
    }	    
    
	public void addMember() {
		Main.main.goContactList();
	}
	
	@Override
	public void onMenuInit() {
		Main.showMenu(lay_info.getVisibility() == View.GONE ? R.id.btn_done : R.id.btn_add_member);
		Main.main.setActionTitle(getResources().getString(R.string.page_title_chat_info), null);
	}

	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.btn_done :
				onDone();
				break;			
			case R.id.btn_add_member :
				addMember();
				break;
			case R.id.btn_photo_take :
			case R.id.btn_photo_gallery :
				Main.main.chooseAvatar(id == R.id.btn_photo_take); 
				break;
			case R.id.btn_opt_media :
				Main.main.goGallery(Dialog.getDialog(-1, chat.id, true));
				break;				
		}		
	}	
	
	@Override
	public void onClick(View v) {
		Main.main.goUserInfo(((DialogItem)v).contact.user);
	}
	
	@Override
	public boolean onLongClick(final View v) {
		if (chat.admin_id != User.self.id) {
			Toast.makeText(getContext(), getResources().getString(R.string.hint_not_group_admin), Toast.LENGTH_SHORT).show();
			return true;
		}
		
		if (((DialogItem)v).contact.user == User.self) 
			return false;
		
        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());		
        Resources res = getResources();
        adb.setTitle(res.getString(R.string.dialog_title_kick));		        
        adb.setMessage(res.getString(R.string.dialog_kick));
        adb.setNegativeButton(res.getString(R.string.dialog_btn_cancel), null);
        adb.setPositiveButton(res.getString(R.string.dialog_btn_kick), new AlertDialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				chat.apiUserDel(((DialogItem)v).contact.user);
				int i = list.indexOfChild(v);
				list.removeViewAt(i);
				list.removeViewAt(i); // divider
			}});	
        
        AlertDialog alert = adb.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show(); 		
		return true;
	}
	
	public void leaveChat() {
        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());		
        Resources res = getResources();
        adb.setTitle(res.getString(R.string.dialog_title_kick));		        
        adb.setMessage(res.getString(R.string.dialog_kick));
        adb.setNegativeButton(res.getString(R.string.dialog_btn_cancel), null);
        adb.setPositiveButton(res.getString(R.string.dialog_btn_kick), new AlertDialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Dialog.getDialog(-1, chat.id, true).leave();
			}});	
        
        AlertDialog alert = adb.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
	}
	
	public void onEditClick() {
    	title.setText(chat.getTitle());
    	title.setVisibility(View.VISIBLE);
    	lay_info.setVisibility(View.GONE);
		Main.main.updateMenu();
	}
	
	public void onDone() {
    	title.setVisibility(View.GONE);
    	lay_info.setVisibility(View.VISIBLE);
    	chat.setTitle(title.getText().toString());    	
    	name.setText(chat.getTitle());
    	Main.main.updateMenu();    	
    	Common.keyboardHide();		
    	Main.mtp.api(null, null, "messages.editChatTitle", chat.id, chat.getTitle());
	}

    public void setSoundUri(Uri uri) {	
    	Common.logError("notify sound: " + uri.toString());
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
	
    public void saveNotify() {    	
    	TL.Object notifyPeer = TL.newObject("inputNotifyPeer", chat.getInputPeer());
    	int mute_until = btn_notify.isChecked() ? 0 : (Common.getUnixTime() + Common.UNIX_YEAR);
    	TL.Object notifySettings = TL.newObject("inputPeerNotifySettings", mute_until, soundUri.toString(), true, TL.newObject("peerNotifyEventsAll"));
		Main.mtp.api(null, null, "account.updateNotifySettings", notifyPeer, notifySettings);    	
		getNotify();
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
    	}, null, "account.getNotifySettings", TL.newObject("inputNotifyPeer", chat.getInputPeer()));    	
    }	
}

