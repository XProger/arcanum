package com.xproger.arcanum;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.*;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;


public class Main extends Common.MediaActivity implements MTProtoListener, OnPageChangeListener {	
	public static Main main;
	public static LayoutInflater inflater;
	
	public static String FILES_PATH = "";
	public static String CACHE_PATH = "";
	public static String MEDIA_PATH = "";
		
	public static int SIZE_THUMB_MEDIA, SIZE_THUMB_USER;
	public static int DISPLAY_WIDTH, DISPLAY_HEIGHT;
	
	
	public static final String EXTRA_USERID = "user_id";
	public static final String EXTRA_CHATID = "chat_id";
		
	public static MTProto mtp;	
	public static String contacts_phone_hash = "";
	
	public static TL.Object settings;
	private TL.Object config_dialogs;
	
	final ArrayList<TL.Object> contacts = new ArrayList<TL.Object>();
	 	
	private Updater updater;
	
	public View mainView;
	public MainPager pager;
	public String intentText;
	public Uri intentMedia;
	
	public TitleBar titleBar;

// TODO: заменить этот быдлокод нормальным менеджером страниц
	public PageIntro pageIntro;
	public PagePhoto pagePhoto;	
	public PageAuth pageAuth;	
	public PageDialog pageDialog;
	public PageDialogList pageDialogList;
	public PageLocation pageLocation;
	public PageForward pageForward;
	public PageContactList pageContactList;
	public PageSettings pageSettings;
	public PageSettingsNotify pageSettingsNotify;
	public PageSettingsBlocked pageSettingsBlocked;	
	public PageGroupNew pageGroupNew;
	public PageGroupNewTitle pageGroupNewTitle;
	public PageUserInfo pageUserInfo;
	public PageChatInfo pageChatInfo;
	public PageGallery pageGallery;
	public PageWallpaper pageWallpaper;
	

	public static String getResStr(int id) {
		return main.getResources().getString(id);
	}	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		long init_time = System.currentTimeMillis();
		main = this;		
		inflater = LayoutInflater.from(this);
		
		setRobotoFont();		
		
		initDirs();		
		
		BitmapCache.init();
		Emoji.init();
		MessageView.init();
		
		JSONObject scheme = null;
		try {
			scheme = getJSON(R.raw.tl);			
			TL.init(scheme);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		
		try {
			scheme = getJSON(R.raw.tl8);			
			TL.dumpData(scheme);
		} catch (Exception e) {
			e.printStackTrace();
		}				
		
		
		MTProto.init();
		
		Dialog.init();
		User.init();
		Chat.init();
		config_load();
		
	// Init UI
		setContentView(R.layout.activity_main);
		
		mainView = findViewById(R.id.main_view);
				
		ActionBar ab = getSupportActionBar();
		ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
		
        titleBar = (TitleBar)inflater.inflate(R.layout.ab_title_bar, null, false);
        titleBar.setSelection(0);
        ab.setCustomView(titleBar, new ActionBar.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        
    	pager = (MainPager)findViewById(R.id.pager);		
		pager.setOnPageChangeListener(this);
		
		Common.logError("init_time: " + (System.currentTimeMillis() - init_time));
		
		// keep alive
		updater = new Updater(5 * 60 * 1000, new Runnable() {
			@Override
			public void run() {
				if (Main.mtp != null && Main.mtp.bind)
					Main.mtp.api_account_updateStatus(false);
			}
		});
		updater.startUpdates();		
		
		start();		
	}	
	
	@Override
	protected void onDestroy() {
		updater.stopUpdates();
		mtp.api_account_updateStatus(false);
		User.deinit();
		MTProto.deinit();
		config_save();
		//main = null;
		super.onDestroy();
	}		
	
	@Override
	public void onBackPressed() {
		if (pager.getCurrentItem() == 0) {
			finish();
			super.onBackPressed();			
		} else
			pager.setCurrentItem(pager.getCurrentItem() - 1, true);
	}	
	
	public void start() {
        updateMenu();		
		mtp = MTProto.getConnection(MTProto.dc_this, this, MTProto.REUSE_MAIN);	
		if (!mtp.bind)
			goIntro();
		else
			goDialogList();				
	}
	
    private void setRobotoFont() {
        try {
        	Typeface face;
        	Field field;        	
        // default = roboto-regular
        	face = Typeface.createFromAsset(getAssets(), "roboto_regular.ttf");        	
            field = Typeface.class.getDeclaredField("SANS_SERIF");
            field.setAccessible(true);
            field.set(null, face);
        // monospace = roboto-light
        	face = Typeface.createFromAsset(getAssets(), "roboto_light.ttf");        	
        	field = Typeface.class.getDeclaredField("MONOSPACE");
            field.setAccessible(true);
            field.set(null, face);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }	
    
	public void setActionTitle(CharSequence title, String subTitle) {		
		if (titleBar == null) return;
		titleBar.setTitle(title, subTitle);
	}    
	
	public void initDirs() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		SIZE_THUMB_MEDIA	= (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, metrics);
		SIZE_THUMB_USER		= (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96, metrics);
		
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point(display.getWidth(), display.getHeight());

		
		DISPLAY_WIDTH	= (int)Math.min(size.x, size.y);
		DISPLAY_HEIGHT	= (int)Math.max(size.x, size.y);
		
		float w = DISPLAY_WIDTH * 0.35f;		
		if (SIZE_THUMB_MEDIA > w)
			SIZE_THUMB_MEDIA = (int)w;
		
		
		FILES_PATH = getFilesDir().getAbsolutePath() + "/";
		CACHE_PATH = FILES_PATH + "img/";	
		MEDIA_PATH = Environment.getExternalStorageDirectory() + "/Pictures/Arcanum/";
		
		File cacheDir = new File(CACHE_PATH);
		cacheDir.mkdirs();		
		
		File mediaDir = new File(MEDIA_PATH);
		mediaDir.mkdirs();			
	}
		
	public void importContacts() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				contacts.clear();
				// get phones
					SparseArray<String> phones = new SparseArray<String>();
					try {
						Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, 
											new String[]{ Phone.RAW_CONTACT_ID, Phone.NUMBER }, 
											Data.MIMETYPE + " = ?",  new String[]{Phone.CONTENT_ITEM_TYPE}, null);
						while (cursor.moveToNext()) {
							try {
								int id = Integer.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(Phone.RAW_CONTACT_ID)));							
								String phone = Common.phoneClear(cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)));
								if (phone.equals("+333")) // Telegram
									continue;
								phones.put(id, phone);
							//	Common.logError("phone: " + id + " " + phone);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				//	Common.logError("phones: " + phones.size());
					
				// get names
					ArrayList<String> plist = new ArrayList<String>(); 
					try {
						Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, 
								new String[]{ StructuredName.RAW_CONTACT_ID, StructuredName.DISPLAY_NAME, StructuredName.GIVEN_NAME, StructuredName.FAMILY_NAME }, 
								Data.MIMETYPE + " = ?",  new String[]{StructuredName.CONTENT_ITEM_TYPE}, null);
						
						String first_name, last_name;
						while (cursor.moveToNext()) {
							try {
								int id = Integer.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(StructuredName.RAW_CONTACT_ID)));
								String phone = phones.get(id);
								if (phone == null)	// no phone
									continue;
								
								first_name = cursor.getString(cursor.getColumnIndexOrThrow(StructuredName.GIVEN_NAME));				
								if (first_name == null || first_name.equals("")) {
									first_name = cursor.getString(cursor.getColumnIndexOrThrow(StructuredName.DISPLAY_NAME));
									last_name = "";
								} else {
									last_name = cursor.getString(cursor.getColumnIndexOrThrow(StructuredName.FAMILY_NAME));
									if (last_name == null)
										last_name = "";
								}
								
								if (first_name == null)
									continue;				
								
							//	Common.logError("contact: " + first_name + " / " + last_name + " " + phone);				
								contacts.add(TL.newObject("inputPhoneContact", (long)id, phone, first_name, last_name));
								plist.add(phone);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					if (pageContactList != null) 
						pageContactList.updateContacts();					
												
				// contacts_phone_hash (for import)
					if (plist.size() > 0) {
						Collections.sort(plist);
						String hash_str = Common.join(plist, ",");
						final String phash = Common.BytesToHex(Common.getMD5(hash_str.getBytes()));
						
						mtp.api(new TL.OnResultRPC() {
							@Override
							public void onResultRPC(TL.Object result, Object param, boolean error) {					
								//if (phash.equals(contacts_phone_hash)) {
									mtp.api_contacts_importContacts(contacts, false);
									contacts_phone_hash = phash;
								//}
							}
						}, null, "contacts.getContacts", User.contacts_hash);
					}
			}			
		}).start();		
	}
	
	public JSONObject getJSON(int resId) throws Exception { 
		InputStream is = getResources().openRawResource(resId);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {		
			byte[] buf = new byte[4 * 1024];
	        int count = is.read(buf);
	        while (count != -1) {
	            os.write(buf, 0, count);
	            count = is.read(buf);
	        }
	        is.close();
        } catch (Exception e) {
            e.printStackTrace();
        	throw new Exception();
        }
		return new JSONObject(os.toString());
	}
	
	public PopupMenuAB showPopup(View anchor, int menuRes, boolean showIcons, int theme) {		
		Context ctx = theme == 0 ? this : new ContextThemeWrapper(this, theme);
		
		PopupMenuAB popup = new PopupMenuAB(ctx, anchor, showIcons);		
		popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {								
				Common.Page page = (Common.Page)getCurPage();				
				page.onMenuClick(null, item.getItemId());				
				return true;
			}});		
		popup.inflate(menuRes);
	    popup.show(); 
	    return popup;
	}
	
	public PopupMenuAB showPopup(View anchor, int menuRes, boolean showIcons) {	// alias
		return showPopup(anchor, menuRes, showIcons, 0);
	}
	
	public void setPage(final int index) {
		pager.post(new Runnable() {
			@Override
			public void run() {
				pager.setCurrentItem(index, true);
			}			
		});		
	}	
	
	public void setPages(View ... views) {
		pager.setPages(views);
	}
	
	public View getCurPage() {
		return pager != null ? pager.getPage(pager.getCurrentItem()) : null;
	}
		
	public void setTopDialog(Dialog dialog) {
		if (pageDialogList != null)
			pageDialogList.setTopDialog(dialog);		
	}
	
	public void goBack() {
		pager.setCurrentItem(pager.getCurrentItem() - 1, true);		
	}
	
	public void goIntro() {
		if (pageIntro == null)
			pageIntro = (PageIntro)inflater.inflate(R.layout.page_intro, null, false);
		getSupportActionBar().hide();
		setPages(pageIntro);
		setPage(0);		
	}		
	
	public void goAuth() {
		if (pageAuth == null)
			pageAuth = (PageAuth)inflater.inflate(R.layout.page_auth, null, false);
		getSupportActionBar().hide();
		if (pageIntro != null && getCurPage() == pageIntro) {
			setPages(pageIntro, pageAuth);
			setPage(1);			
		} else {
			setPages(pageAuth);
			setPage(0);
		}		
	}	
	
	private void goDialogListEx() {
		if (pageAuth != null && getCurPage() == pageAuth) {
			setPages(pageAuth, pageDialogList);
			setPage(1);
		} else {
			setPages(pageDialogList);
			setPage(0);
			updateMenu();
		}		
	}
	
	public void goDialogList() {
		if (pageDialogList == null) {
			Common.keyboardHide();
			pageDialogList = (PageDialogList)inflater.inflate(R.layout.page_dialog_list, null, false);
			getSupportActionBar().show();					
			registerGCM();
			SyncUtils.CreateSyncAccount(this, User.self.getTitle());
			
			if (config_dialogs != null && config_dialogs.getVector("dialogs").count > 0) {			
				Main.mtp.TL_messages_Dialogs(config_dialogs);
				config_dialogs = TL.newObject("messages.dialogs", TL.newVector(), TL.newVector(), TL.newVector(), TL.newVector());
			} else	
				mtp.api_messages_getDialogs(0, 0, 100);
			
			if (settings.getVector("wp").count == 0)
				PageWallpaper.getWallpapers();
			goDialogListEx();
			
			Intent intent = getIntent();
			if (intent != null) {
				String page;
			// from accounts & sync
				if ((page = intent.getStringExtra("page")) != null) {
					if (page.equals("settings"))	
						goSettings();
				}
			// from user push
				if ((page = intent.getStringExtra("user_id")) != null)
					goDialog(Dialog.getDialog(Integer.parseInt(page), -1, true));
			// from chat push
				if ((page = intent.getStringExtra("chat_id")) != null)
					goDialog(Dialog.getDialog(-1, Integer.parseInt(page), true));
			// text from other applications
				String type = intent.getType();
				if (type != null) {
					if (type.equals("text/plain")) {
						intentText = intent.getStringExtra(Intent.EXTRA_TEXT);
						if (intentText != null)
							Common.logError("intentText: " + intentText);
					} else					
						if (type.startsWith("image/")) { 
							intentMedia = intent.getParcelableExtra(Intent.EXTRA_STREAM);
							if (intentMedia != null)
								Common.logError("intentMedia: " + intentMedia.toString());
						}
				}
								
			// from contacts, etc.  
			    if (Intent.ACTION_SENDTO.equals(intent.getAction())) {
			        String phone = intent.getDataString();
			        phone = URLDecoder.decode(phone).replace("smsto:", "").replace("sms:", "");
			        phone = Common.phoneClear(phone);
			        Common.logError("intent phone: " + phone);
			        User user = User.getUser(phone);
			        if (user != null)
			        	goDialog(Dialog.getDialog(user.id, -1, true));			        
			    }
				
			}
			
		} else
			goDialogListEx();
	}
	
	public void goDialog(Dialog dialog) {
		if (pageDialog == null) {
			pageDialog = (PageDialog)inflater.inflate(R.layout.page_dialog, null, false);
			PageDialog.updateWallpaper();
		}
		pageDialog.setDialog(dialog);	
		
		if (getCurPage() == pageSettings) {
			setPages(pageDialogList, pageSettings, pageDialog);
			setPage(2);
			return;
		} 
		
		if (getCurPage() == pageContactList) {
			setPages(pageDialogList, null, pageContactList, pageDialog);
			setPage(3);
			return;
		} 
				
		setPages(pageDialogList, pageDialog);
		setPage(1);		
	}	
	
	public void goGallery(Dialog dialog) {
		if (pageGallery == null)
			pageGallery = (PageGallery)inflater.inflate(R.layout.page_gallery, null, false);				
		pageGallery.setDialog(dialog);
		if (pager.getCurrentItem() == 2) {
			setPages(pageDialogList, pageDialog, getCurPage(), pageGallery);
			setPage(3);		
		} else {
			setPages(pageDialogList, pageDialog, null, pagePhoto, pageGallery);
			setPage(4);					
		}			
	}
	
	public void goPhoto(Dialog.Message msg) {
		if (pagePhoto == null)
			pagePhoto = (PagePhoto)inflater.inflate(R.layout.page_photo, null, false);				
		pagePhoto.goPage(msg);		
		View page = getCurPage();
		if (pageDialog != null && page == pageDialog) {
			setPages(pageDialogList, pageDialog, pagePhoto);
			setPage(2);		
		}
		
		if (pageGallery != null && page == pageGallery) {
			setPages(pageDialogList, pageDialog, pageGallery, pagePhoto);
			setPage(3);		
		}		
	}
	
	public void goContactList() {
		if (pageContactList == null)
			pageContactList = (PageContactList)inflater.inflate(R.layout.page_contact_list, null, false);

		if (getCurPage() == pageChatInfo) {
			pageContactList.setChat(pageChatInfo.chat);
			pageContactList.updateContacts();
			setPages(pageDialogList, pageDialog, pageChatInfo, pageContactList);
			setPage(3);
		} else {
			pageContactList.setChat(null);
			pageContactList.updateContacts();
			setPages(pageDialogList, pageContactList);
			setPage(1);
		}
	}	
	
	public void goGroupNew() {
		if (pageGroupNew == null)
			pageGroupNew = (PageGroupNew)inflater.inflate(R.layout.page_group_new, null, false);
		pageGroupNew.clear();
		setPages(pageDialogList, pageGroupNew);
		setPage(1);
	}
	
	public void goGroupNewTitle() {
		if (pageGroupNewTitle == null)
			pageGroupNewTitle = (PageGroupNewTitle)inflater.inflate(R.layout.page_group_new_title, null, false);
		pageGroupNewTitle.clear();
		setPages(pageDialogList, pageGroupNew, pageGroupNewTitle);
		setPage(2);
	}

	public void goSettings() {
		if (pageSettings == null)
			pageSettings = (PageSettings)inflater.inflate(R.layout.page_settings, null, false);
		pageSettings.updateUser();
		setPages(pageDialogList, pageSettings);
		setPage(1);
	}
	
	public void goSettingsNotify() {
		if (pageSettingsNotify == null)
			pageSettingsNotify = (PageSettingsNotify)inflater.inflate(R.layout.page_settings_notify, null, false);
		pageSettingsNotify.update();
		setPages(pageDialogList, pageSettings, pageSettingsNotify);
		setPage(2);
	}
	
	public void goSettingsBlocked() {
		if (pageSettingsBlocked == null)
			pageSettingsBlocked = (PageSettingsBlocked)inflater.inflate(R.layout.page_settings_blocked, null, false);
		pageSettingsBlocked.update();
		setPages(pageDialogList, pageSettings, pageSettingsBlocked);
		setPage(2);
	}
	
	public void goUserInfo(User user) {
		if (user == User.self) {
			goSettings();
			return;
		}
		
		if (pageUserInfo == null)
			pageUserInfo = (PageUserInfo)inflater.inflate(R.layout.page_user_info, null, false);
		pageUserInfo.setUser(user);
		if (getCurPage() == pageChatInfo) {
			setPages(pageDialogList, pageDialog, pageChatInfo, pageUserInfo);
			setPage(3);			
		} else {
			setPages(pageDialogList, pageDialog, pageUserInfo);
			setPage(2);
		}
	}	

	public void goChatInfo(Chat chat) {
		if (pageChatInfo == null)
			pageChatInfo = (PageChatInfo)inflater.inflate(R.layout.page_chat_info, null, false);
		pageChatInfo.setChat(chat);
		setPages(pageDialogList, pageDialog, pageChatInfo);
		setPage(2);
	}		
	
	public void goLocation(LatLng point, User user) {
		if (pageLocation == null)			
			pageLocation = (PageLocation)inflater.inflate(R.layout.page_location, null, false);		
		
		if (point != null) {
			pageLocation.showMode(user);
			pageLocation.setUserLocation(point);
			pageLocation.setMarker(point);
			pageLocation.cameraGoLocation(point);
		} else {
			pageLocation.showMode(null);				
			pageLocation.waitMyLocation();			
		}
		
		setPages(pageDialogList, pageDialog, pageLocation);
		setPage(2);					
	}	
	
	public void goForward(ArrayList<Integer> msg_id) {
		if (pageForward == null)
			pageForward = (PageForward)inflater.inflate(R.layout.page_forward, null, false);
		pageForward.update(msg_id);
		View page = getCurPage();
		if (pageDialog != null && page == pageDialog) {
			setPages(pageDialogList, pageDialog, pageForward);
			setPage(2);							
		}
		
		if (pagePhoto != null && page == pagePhoto) {
			if (pager.getCurrentItem() == 2 && pageGallery != null) {
				setPages(pageDialogList, pageDialog, pagePhoto, pageForward);
				setPage(3);							
			} else {
				setPages(pageDialogList, pageDialog, pageGallery, pagePhoto, pageForward);
				setPage(4);							
			}
		}
	}
	
	public void goShare(User user) {
		// pageForward == pageShare (2 in 1)
		if (pageForward == null)
			pageForward = (PageForward)inflater.inflate(R.layout.page_forward, null, false);
		pageForward.update(user);
		setPages(pageDialogList, pageDialog, pageForward);	// TODO: 2 page ChatInfo/UserInfo
		setPage(2);							
	}
	
	public void onMenuClick(View view) {		
		if (view == null) {
			Common.logError("onMenuClick: null");
			return;
		}

		int id = view.getId();
		
		switch (id) {
			case R.id.notify_close : titleBar.hideNotify(); break;
			case R.id.title_notify : goDialog(titleBar.notifyDialog); titleBar.hideNotify(); break;
			default :
				Common.Page page = (Common.Page)getCurPage();
				page.onMenuClick(view, id);
		}
	}

	public void goWallpaper() {
		if (pageWallpaper == null)
			pageWallpaper = (PageWallpaper)inflater.inflate(R.layout.page_wallpaper, null, false);		
		pageWallpaper.update();
		setPages(pageDialogList, pageSettings, pageWallpaper);
		setPage(2);									
	}
	
	public void updateMenu() {
		titleBar.hideMenu();
		if (!titleBar.selectMode && titleBar.notifyTime == 0) {
		    Common.Page page = (Common.Page)getCurPage();
		    if (page != null)
		    	page.onMenuInit();
		}
	}
	
	public static void showMenu(int id) {	// alias		
		Main.main.titleBar.showMenu(id);		
	}

	
	public void updateMessage(final Dialog.Message msg) {
		if (pageDialog != null && pageDialog.dialog == msg.dialog) {
			mainView.post(new Runnable() {
				@Override
			    public void run() {				
					pageDialog.updateMessage(msg);
			    }		    
			});
		}
	}
	
	public void resetMessage(final Dialog.Message msg) {
		if (pageDialog != null && pageDialog.dialog == msg.dialog) {
			mainView.post(new Runnable() {
				@Override
			    public void run() {				
					pageDialog.resetMessage(msg);
			    }		    
			});
		}
	}
	
	public void updateDialog(Dialog dialog) {
		if (pageDialog != null && pageDialog.dialog == dialog) {		
			updateDialogItem(dialog);				
			mainView.post(new Runnable() {
				@Override
			    public void run() {				
					pageDialog.update();
			    }		    
			});
		}
		
		if (pagePhoto != null && pagePhoto.dialog == dialog)
			mainView.post(new Runnable() {
				@Override
			    public void run() {				
					pagePhoto.update();
			    }		    
			});
				
		
		if (pageGallery != null && pageGallery.dialog == dialog)
			mainView.post(new Runnable() {
				@Override
			    public void run() {				
					pageGallery.update();
			    }		    
			});
	}	
		
	public void updateDialogItem(final Dialog dialog) {
		if (dialog == null || pageDialogList == null) return;
		
		mainView.post(new Runnable() {
			@Override
		    public void run() {
				pageDialogList.update(dialog);				
				if (pageDialog != null && getCurPage() == pageDialog && pageDialog.dialog == dialog) {
					setActionTitle(dialog.getTitle(false), dialog.getStatus());
					pageDialog.updatePicture();					
				}
		    }
		});
	}	
	
	public void messageAdd(final Dialog.Message msg) {
		setTopDialog(msg.dialog);
		
		if (pageDialog != null && pageDialog.dialog == msg.dialog)
			pageDialog.post(new Runnable() {
				@Override
				public void run() {
					pageDialog.messageAdd(msg);
				}
			});
	}
	
	public void messageDelete(final Dialog.Message msg) {
		View curPage = getCurPage();
		
		if (pageDialog != null && pageDialog.dialog == msg.dialog)
			pageDialog.post(new Runnable() {
				@Override
				public void run() {
					pageDialog.messageDelete(msg);
				}
			});
		
		if (pagePhoto != null && curPage == pagePhoto && pagePhoto.dialog == msg.dialog)
			mainView.post(new Runnable() {
				@Override
			    public void run() {				
					pagePhoto.update();
			    }		    
			});
				
		
		if (pageGallery != null && curPage == pageGallery && pageGallery.dialog == msg.dialog)
			mainView.post(new Runnable() {
				@Override
			    public void run() {				
					pageGallery.update();
			    }		    
			});		
	}
	
	public void resetDialogs() {
		mainView.post(new Runnable() {
			@Override
		    public void run() {
				Common.logError("update dialogs " + Dialog.dialogs.size());
				
				ArrayList<Dialog> list = new ArrayList<Dialog>();
				for (int i = 0; i < Dialog.dialogs.size(); i++) 
					list.add(Dialog.dialogs.get(i));
				
		    	Collections.sort(list, new Comparator<Dialog>() {
					@Override
					public int compare(Dialog d1, Dialog d2) {
						return Integer.valueOf(d2.getUnixTime()).compareTo(d1.getUnixTime());
					}
		    	});

				if (pageDialogList != null) {
					pageDialogList.adapter.clear();
					for (int i = 0; i < list.size(); i++)
						pageDialogList.adapter.add(list.get(i));
				}
				
		    }
		});
	}	
	
	public void readHistory(Dialog dialog) {
		if (dialog == null || pageDialog == null) return;
		if (getCurPage() == pageDialog && pageDialog.dialog == dialog) 
			dialog.readHistory();		
	}
	
	public void updateUser(final User user) {
		if (mainView == null) return;
		mainView.post(new Runnable() {
			@Override
		    public void run() {
				if (user == User.self && pageSettings != null) 
					pageSettings.updateUser();
				if (pageDialog != null && pageDialog.dialog != null && pageDialog.dialog.userIncluded(user)) {
					updateDialogItem(pageDialog.dialog);
					pageDialog.updateTitle();
				}
				updateDialogItem(Dialog.getDialog(user.id, -1, false));
			}
		});	
	}
	
	public void updateChat(final Chat chat) {
		if (mainView == null) return;
		mainView.post(new Runnable() {
			@Override
		    public void run() {
				updateDialogItem(Dialog.getDialog(-1, chat.id, false));
				if (pageChatInfo != null && getCurPage() == pageChatInfo)
					pageChatInfo.setChat(pageChatInfo.chat);
			}
		});
	}		
	
	public void removeDialog(Dialog dialog) {
		if (pageDialog != null && pageDialog.dialog == dialog) {
			pageDialog.setDialog(null);
			goDialogList();
		}			
		 
		if (Dialog.dialogs.contains(dialog)) {
			Dialog.dialogs.remove(dialog);
			resetDialogs();		
		}
	}
	
	
	public void showNotify(final Dialog.Message msg) {
		titleBar.post(new Runnable() {
			@Override
			public void run() {
				View page = getCurPage();
				if (page == pageDialogList)
					return;
				if (pageDialog != null && page == pageDialog && pageDialog.dialog == msg.dialog)
					return;
				titleBar.showNotify(msg);	
			}
		});
	}
	
	/*
	public void msgUpdate(boolean flag) {		
	// костыль :(
		if (flag) {
			if (msgList.getLastVisiblePosition() == msgAdapter.getCount() - 1) {
				msgIdx = -1;
				return;
			}
			msgIdx = msgList.getFirstVisiblePosition();
			msgTop = msgList.getChildCount() == 0 ? 0 : msgList.getChildAt(0).getTop();			
		} else {
			if (msgIdx > -1)
				msgList.setSelectionFromTop(msgIdx, msgTop);

			msgList.postDelayed(new Runnable() {
				@Override
				public void run() {					
					if (msgIdx > -1)
						msgList.setSelectionFromTop(msgIdx, msgTop);
					else
						scrollDialogDown();
				}
			}, 500);
			
		}
	}
*/		
	
	public void uploadFile(String fileName, final FileQuery.OnResultListener onResultListener) {
		try {
			new FileQuery(fileName, new FileQuery.OnResultListener() {
				@Override
				public void onResult(TL.Object result) {
					if (result == null) {
						Common.logError("error while uploading file");
						// TODO : show error alert
						return;
					}
					onResultListener.onResult(result);					
				}

				@Override
				public void onProgress(float progress) {
					onResultListener.onProgress(progress);
				}
			});
		} catch (Exception e) {
			Common.logError("invalid file");
			onResultListener.onResult(null);
		}
	}	
	
	public void uploadProfilePhoto(final String fileName) {
		uploadFile(fileName, new FileQuery.OnResultListener() {
			@Override
			public void onResult(TL.Object result) {
				Main.mtp.api(new TL.OnResultRPC() {
					@Override
					public void onResultRPC(TL.Object result, Object param, boolean error) {
						if (error) {};
						Common.fileDelete(fileName);
					}					
				}, null, "photos.uploadProfilePhoto", result, "", TL.newObject("inputGeoPointEmpty"), TL.newObject("inputPhotoCropAuto"));
			}

			@Override
			public void onProgress(float progress) {}
		});
	}
	
	public void uploadChatPhoto(final int chat_id, final String fileName) {
		uploadFile(fileName, new FileQuery.OnResultListener() {
			@Override
			public void onResult(TL.Object result) {
				if (result == null) {
					Common.logError("uploadFile return null");
					return;
				}
				Main.mtp.api(new TL.OnResultRPC() {
					@Override
					public void onResultRPC(TL.Object result, Object param, boolean error) {
						if (error) {};
						Common.fileDelete(fileName);
					}					
				}, null, "messages.editChatPhoto", chat_id, TL.newObject("inputChatUploadedPhoto", result, TL.newObject("inputPhotoCropAuto")) );
			}

			@Override
			public void onProgress(float progress) {}
		});
	}	
	public static Bitmap saveProfileImage(Context ctx, String source, String dest) throws Exception {
		final Bitmap bitmap = Common.loadBitmap(ctx, source, 160, 160, true, true, false);
		Common.savePNG(dest, bitmap, 90);
		return bitmap;
	}
	
	public void chooseAvatar(boolean take) {
		final Activity act = this;
		Common.MediaReq req = new Common.MediaReq() {
			@Override
			public void onMedia(final Uri media) {
				if (media == null)
					return;					
		        new Thread( new Runnable() {
		        	@Override
		        	public void run() {        		        		
		        		try {        			
		        			final String pngPath = Main.CACHE_PATH + Common.random.nextLong() + ".png";
		        			final Bitmap bmp = saveProfileImage(act, Common.getPathFromURI(act, media), pngPath);		        			
		        			View page = getCurPage();
		        			
		        			if (page == pageAuth)
		        				pageAuth.setPicture(pngPath, bmp);		        			
		        			
		        			if (page == pageSettings) {		        				
			        			uploadProfilePhoto(pngPath);			        			
			        			page.post(new Runnable() {
								    public void run() {
								    	pageSettings.setPicture(bmp);
								    }
								});
		        			}		        			
		        			
		        			if (page == pageGroupNewTitle) {
			        			page.post(new Runnable() {
								    public void run() {
								    	pageGroupNewTitle.setPicture(bmp, pngPath);
								    }
								});	
		        			}
		        			
		        			if (page == pageChatInfo) {
		        				uploadChatPhoto(pageChatInfo.chat.id, pngPath);	
			        			page.post(new Runnable() {
								    public void run() {
								    	pageChatInfo.setPicture(bmp);
								    }
								});	
		        			}		        			
		        			
		        		} catch (Exception e) {}        		
		        	}
		        } ).start();
			}
		};
		mediaPhoto(take, true, req);     
	}	
	
	public static void logout() {
		User.deinit();
		MTProto.deinit();		
		Common.fileDelete(FILES_PATH + "config");
		Common.fileDelete(FILES_PATH + "dialogs");
		Common.fileDelete(FILES_PATH + "settings");
		Common.deleteFolder(new File(CACHE_PATH));
		File file = new File(CACHE_PATH);
		file.mkdirs();		
		MTProto.init();
		Dialog.init();
				
		// TODO: hide menu
		
		User.init();	
		Chat.init();
		main.start();
	}
	
	public void config_load() {
		settings = TL.load(FILES_PATH + "settings");
		if (settings != null && !settings.name.equals("arcanum.settings"))
			settings = null;
		
		if (settings == null) {
			Common.logError("new settings");
			settings = TL.newObject("arcanum.settings", true, true, true, true, TL.newVector(), -1, "", 0xffd6e4ef);			
		}
		
		try {				
			TL.Object cfg = TL.load(FILES_PATH + "config");
			if (cfg == null)
				return;
			
			config_dialogs = TL.load(FILES_PATH + "dialogs");
			
			
			MTProto.dc_this = cfg.getInt("dc_this");			
			MTProto.dc_date = cfg.getInt("dc_date");
			
			TL.Vector dc_states = cfg.getVector("dc_states");
			MTProto.dcStates.clear();
			for (int i = 0; i < dc_states.count; i++)
				MTProto.dcStates.add(dc_states.getObject(i));
						
			User.contacts_hash	= cfg.getString("contacts_hash");			// remove
			contacts_phone_hash	= cfg.getString("contacts_phone_hash");
			MTProto.upd_date	= cfg.getInt("upd_date");
			MTProto.upd_pts		= cfg.getInt("upd_pts");
			MTProto.upd_seq		= cfg.getInt("upd_seq");		
			
			//User.addUsers(cfg.getVector("users"));
			User.calcHash();
			Common.logError("config load: ok");
		} catch (Exception e) {
			Common.logError("error: config load");
			e.printStackTrace();
		}
	}
	
	
	private void writeFileLocation(DataOutputStream out, TL.Object loc) {		
		int		dc_id		= loc == null ? 0 : loc.getInt("dc_id");		
		long	volume_id	= loc == null ? 0 : loc.getLong("volume_id");
		int		local_id	= loc == null ? 0 : loc.getInt("local_id");
		try {
			out.writeInt(dc_id);
			out.writeLong(volume_id);
			out.writeInt(local_id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void config_save() {		
		TL.save(FILES_PATH + "settings", settings);		

		mtp.updateDcStates();
		User.calcHash();
		TL.Vector dc_info = TL.newVector(MTProto.dcStates.toArray());
		TL.Object cfg = TL.newObject("joim.config", MTProto.dc_this, MTProto.dc_date, dc_info, 
													User.contacts_hash, contacts_phone_hash, 
													MTProto.upd_date, MTProto.upd_pts, MTProto.upd_seq,
													TL.newVector());
		TL.save(FILES_PATH + "config", cfg);		

		TL.save(FILES_PATH + "dialogs", 
				TL.newObject("messages.dialogs", 
						Dialog.getVector(), 
						Dialog.Message.getVector(), 
						Chat.getVector(), 
						User.getVector()));	
		
		try {
			FileOutputStream file; 
			DataOutputStream ds;			

		// user icons
			file = new FileOutputStream(FILES_PATH + "icon_user"); 
			ds = new DataOutputStream(file);
			ds.writeInt(User.users.size());			
			for (int i = 0; i < User.users.size(); i++) {
				User user = User.users.get(User.users.keyAt(i));
				ds.writeInt(user.id);
				writeFileLocation(ds, user.getPhotoLocation());
			}
			ds.close();
			file.close();
			
		// chat icons
			file = new FileOutputStream(FILES_PATH + "icon_chat"); 
			ds = new DataOutputStream(file);
			ds.writeInt(Chat.chats.size());			
			for (int i = 0; i < Chat.chats.size(); i++) {
				Chat chat = Chat.chats.get(Chat.chats.keyAt(i));
				ds.writeInt(chat.id);
				writeFileLocation(ds, chat.getPhotoLocation());
			}
			ds.close();
			file.close();			
			
			
			Common.logError("config save: ok");
		} catch (Exception e) {
			Common.logError("error: config save");
			e.printStackTrace();
		}
	}			

	public void registerGCM() {
		try {
			GCMRegistrar.checkDevice(this);
			GCMRegistrar.checkManifest(this);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		String token = GCMRegistrar.getRegistrationId(this);
		if (token != null && token != "")
			registerDevice(token);
		else
			GCMRegistrar.register(this, GCMIntentService.GCM_SENDER_ID);	
	}
	
	public void registerDevice(String token) {
		String device_model = Build.MANUFACTURER;
		device_model = device_model != null && device_model.length() > 0 ? device_model + " " + Build.MODEL : Build.MODEL; 
		String system_version = Build.VERSION.RELEASE;
		String app_version = "";
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(this.getPackageName(), 0); 
			app_version = packageInfo.versionName;
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		Common.logError(String.format("reg device: %s %s %s %s",  token, device_model, system_version, app_version));
		mtp.api(null, null, "account.registerDevice", 2, token, device_model, system_version, app_version, false);		
	}
	
	@Override
	public void onRedirect(MTProto mtp) {
		Main.mtp = mtp;
	}		
	
	@Override
	public void onConnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(String msg) {
		// TODO Auto-generated method stub		
	}
	
	@Override 
	public void onReady() {	
	//	config_save();
	}
	
	@Override
	public void onAuth(TL.Object user) {
		User.self = User.addUser(user);
		Common.logInfo("user auth:");
		Common.logInfo(" " + User.self.id);
		Common.logInfo(" " + User.self.first_name);
		Common.logInfo(" " + User.self.last_name);
		Common.logInfo(" " + User.self.phone);
		onReady();
	}

	@Override
	public void onBind() {
		importContacts();				
		config_save();
		mtp.checkUpdates();		
	}
	

		
	@Override
	public void onMessage(TL.Object message, int msgFlag) {	
		Dialog.newMessage(message, msgFlag);
	}
	
	@Override
	public void onUserName(int user_id, String first_name, String last_name) {
		User user = User.getUser(user_id);	
		if (user == null) return;
		user.setName(first_name, last_name);
		updateUser(user);
	}

	@Override
	public void onPageSelected(int position) {		
		View page = pager.getPage(position);
		if (page == pageAuth || page == pagePhoto)
			getSupportActionBar().hide();
		else
			getSupportActionBar().show();	
		titleBar.setSelection(0);
		titleBar.notifyTime = 0;
		
		if (pageDialog != null && page == pageDialog) {
			readHistory(pageDialog.dialog);			
			pageDialog.scrollDown();
		}
		updateMenu();
	}
		
	@Override
	public void onPageScrollStateChanged(int state) {
		if (state != 0) return;
		int position = pager.getCurrentItem();		
		View v = pager.getPage(position);

		if (position > 0 && pageAuth != null && v == pageAuth) {
			pageIntro = null;
			setPages(pageAuth);
			pager.setCurrentItem(0, false);			
		}
		
		if (position == 0) {
			if (pageDialog != null && pageDialog.dialog != null)
				setPages(pageDialogList, pageDialog);
			else
				setPages(v);			
		}
		
		if (position == 2 && (v == pageUserInfo || v == pageChatInfo)) 
			setPages(pageDialogList, pageDialog, v);

		if (position == 1 && v == pageGroupNew)
			setPages(pageDialogList, pageGroupNew);		
		
		if (position == 1 && v == pageDialog)
			setPages(pageDialogList, pageDialog);	
		
		if (position > 1 && v == pageDialog) {
			setPages(pageDialogList, pageDialog);
			pager.setCurrentItem(1, false);
		}
		
		if (position > 0 && v == pageDialogList) {
			if (pageAuth != null) {
				pageAuth.stopUpdater();
				pageAuth = null;
			}
			setPages(pageDialogList);
			pager.setCurrentItem(0, false);
		}		
		
		if (position > 1 && v == pageGallery) {
			setPages(pageDialogList, pageDialog, pageGallery);
			pager.setCurrentItem(2, false);
		}		
					
		if (pageDialog != null)
			if (v != pageDialog)
				Common.keyboardHide();
			else
				pageDialog.focus();
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}	
	
}
