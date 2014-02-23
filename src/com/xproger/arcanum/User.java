package com.xproger.arcanum;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.util.SparseArray;
import android.widget.ImageView;

public class User {
	
	public static SparseArray<User> users = new SparseArray<User>();
	public static User		self;
	public static String	contacts_hash;
	public static String	def_color[]		= { "#ee4928", "#41a903", "#e09602", "#0f94ed", "#8f3bf7", "#fc4380", "#00a1c4", "#eb7002" };
	public static int		def_picture[] = { 
		R.drawable.user_placeholder_blue,
		R.drawable.user_placeholder_cyan,
		R.drawable.user_placeholder_green,
		R.drawable.user_placeholder_orange,
		R.drawable.user_placeholder_pink,
		R.drawable.user_placeholder_purple,
		R.drawable.user_placeholder_red,
		R.drawable.user_placeholder_yellow
	};
	
	private static ArrayList<User> fullInfoQuery = new ArrayList<User>();
	
	public int id;
	public long access_hash = 0;
	public boolean contact = false;
	public String first_name = "", last_name = "", phone = "";
	public TL.Object photo = null;
	private TL.Object status;
	private TL.Object object;	// userFull
	private static Updater updater = null;
	
	public boolean typing = false, online = false, blocked = false;
	private int typing_time;
	private int typing_chat_id;
	
	public long client_id;
	
//	public Bitmap photo_small;	
	
	public static void init() {
		users.clear();
		fullInfoQuery.clear();
		
		self = addUser(TL.newObject("userEmpty", 0));
		contacts_hash = "";
				    
    // update typing
        updater = new Updater(3 * 1000, new Runnable() {
			@Override
			public void run() {
				int time = Common.getUnixTime();
				for (int i = 0; i < User.users.size(); i++) {
					User user = User.users.get(User.users.keyAt(i));
					if (user.typing && user.typing_time < time)		
						user.setTyping(user.typing_chat_id, false);			
				}
			}
		});
        updater.startUpdates();        
	}	
	
	public static void deinit() {
		updater.stopUpdates();
	}
	
	public static User addUser(TL.Object user) {
		if (user == null) return null;
				
		int id = user.name.equals("userFull") ? user.getObject("user").getInt("id") : user.getInt("id");
		User old = users.get(id);
		
		if (old != null) {
			old.update(user);
			getFullInfo();
			return old;
		} else {		
			User u = new User(user);		
			users.put(id, u);
			getFullInfo();
			return u;
		}		
	}
	
	public static void addUsers(TL.Vector users) {
		if (users == null) return;
		for (int i = 0; i < users.count; i++)
			User.addUser(users.getObject(i));		
	}
	
	public TL.Object getInputUser() {
		if (this == User.self)
			return TL.newObject("inputUserSelf");
		if (contact)
			return TL.newObject("inputUserContact", id);
		return TL.newObject("inputUserForeign", id, access_hash);			
	}
	
	public static void getFullInfo() {
		synchronized (fullInfoQuery) {
			if (fullInfoQuery.size() == 0)
				return;
			for (int i = 0; i < fullInfoQuery.size(); i++) {
				User user = fullInfoQuery.get(i);
				if (user.id == 0) continue; // inputUserEmpty
				TL.Object inputUser = user.getInputUser();
				Main.mtp.api(null, null, "users.getFullUser", inputUser);
			}		
			fullInfoQuery.clear();
		}
	}
	
	
	public static User getUser(int id) {
		return users.get(id);
	}
	
	public static User getUser(String phone) {
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(users.keyAt(i));
			if (user.phone != null && user.phone.equals(phone))
				return user;
		}
		return null;
	}	
	
	public static ArrayList<User> getUsers(boolean contact) {
		ArrayList<User> res = new ArrayList<User>();
		for (int i = 0; i < User.users.size(); i++) {
			User user = User.users.get(User.users.keyAt(i)); 
			if (user.id != 0 && (!contact || user.contact))
				res.add(user);
		}
		return res;
	}
	
	public static TL.Vector getVector() {
		ArrayList<TL.Object> res = new ArrayList<TL.Object>();
		for (int i = 0; i < User.users.size(); i++)
			res.add(User.users.get(User.users.keyAt(i)).object);
		return TL.newVector(res.toArray());
	}
	
	public static String calcHash() {	
		ArrayList<Integer> ulist = new ArrayList<Integer>();
		for (int i = 0; i < User.users.size(); i++) {
			User user = User.users.get(User.users.keyAt(i));
			if (user.contact)
				ulist.add(user.id);
		}
		
		if (ulist.size() > 0) {
			Collections.sort(ulist);	
			String hash_str = Common.join(ulist, ",");
			contacts_hash = Common.BytesToHex(Common.getMD5(hash_str.getBytes()));
		} else
			contacts_hash = "";

		Common.logDebug("contacts_hash: " + contacts_hash);			
		return contacts_hash;		
	}
	
	private User(TL.Object user) {
		photo = null;
//		photo_small = null;		
		if (!user.name.equals("userFull")) {			
			user = TL.newObject("userFull", user, 
									TL.newObject("contacts.link", TL.newObject("contacts.myLinkEmpty"), TL.newObject("contacts.foreignLinkUnknown"), user),
									TL.newObject("photoEmpty", 0L),
									TL.newObject("peerNotifySettingsEmpty"),
									false, "", "");
			update(user);
			synchronized (fullInfoQuery) {
				fullInfoQuery.add(this);
			}
		} else
			update(user);
	}
	
	private void update(TL.Object someUser) {
		if (someUser.id == 0x771095da) {	// userFull
			object = someUser;
			blocked = object.getBool("blocked");
		} else
			object.set("user", someUser);
			
		TL.Object user = object.getObject("user");
		
		id = user.getInt("id");		
		
		if (!user.name.equals("userEmpty")) {
			first_name = user.getString("first_name");
			last_name = user.getString("last_name");			
			
			if (!user.name.equals("userDeleted")) { 				
				setPhoto(user.getObject("photo"));		
				setStatus(user.getObject("status"));

				if (user.name.equals("userSelf"))
					User.self = this;
				else
					access_hash = user.getLong("access_hash");			
						
				if (!user.name.equals("userForeign")) {
					if (user.name.equals("userContact"))
						contact = true;
					
					phone = user.getString("phone");
				}
			}
		}		
	}
		
	public TL.Object getObject() {			
		return object;
	}

	public String getTitle() {
		if (first_name.equals("") && last_name.equals(""))
			return "[anonymous]";
		if (first_name.equals(""))
			return last_name;
		if (last_name.equals(""))
			return first_name;
		return first_name + " " + last_name;
	}
	
	public CharSequence getTitleColored() {
		String title = getTitle();
		if (object != null) {
			TL.Object u = object.getObject("user");
			if (u != null) {
				String color = "010101";	// userContact
				switch (u.id) {
					case 0x22e8ceb0 :	// userRequest
					case 0x5214c89d	:	// userForeign
						color = "006fc8";
						break;
					case 0xb29ad7cc :	// userDeleted
						color = "666666";
						break;
				}
				return Html.fromHtml("<font color=\"#" + color + "\">" + title + "</font>");
			}
		}
		return title;
	}
	
	public String getStatus() {
		if (status != null) {
			if (status.id == 0xedb93949) {	// userStatusOnline
				if (User.self == this)
					return Main.getResStr(R.string.status_online_user);
				return typing ? Main.getResStr(R.string.status_typing_user) : Main.getResStr(R.string.status_online_user);
			}
			
			if (status.id == 0x8c703f) {	// userStatusOffline
				if (User.self == this)
					return Main.getResStr(R.string.status_offline_user);
				int date = status.getInt("was_online");
				int delta = Common.getUnixTime() - date;
				if (delta < 5 * 60)	// < 5 min 
					return Main.getResStr(R.string.status_offline1);
				if (delta < 60 * 60)	// < 1 hour
					return String.format(Main.getResStr(R.string.status_offline2), delta / 60);
				if (delta < 2 * 60 * 60)	// < 2 hours
					return Main.getResStr(R.string.status_offline3);
				if (delta < 24 * 60 * 60)	// < 24 hours
					return String.format(Main.getResStr(R.string.status_offline4), delta / 60 / 60);
				return new SimpleDateFormat("dd MMM").format(date * 1000L);
			}
		}
		return "null";
	}
	
	public String getStatusHtml() {
		return String.format(Main.getResStr(status.id == 0xedb93949 ? R.string.info_format_online : R.string.info_format_offline), getStatus()); 
	}
	
	public void setTyping(int chat_id, boolean isTyping) {
		Common.logError(getTitle() + " is typing " + isTyping);
		if (typing != isTyping) {			
			typing = isTyping;
			if (typing) {
				typing_time = Common.getUnixTime() + 6;
				typing_chat_id = chat_id;
			};
			Dialog dialog = chat_id != -1 ? Dialog.getDialog(-1, chat_id, true) : Dialog.getDialog(id, -1, true);			
			Main.main.updateDialogItem(dialog);
			Main.main.updateUser(this);
		}		
	}

	public void setStatus(TL.Object userStatus) {			
		boolean update = this.status == null || !Arrays.equals(status.serialize(), userStatus.serialize());
		if (update)
			try {
				status = userStatus;
				online = status.id == 0xedb93949;				
				object.getObject("user").set("status", status);				
				Main.main.updateUser(this);
			} catch (Exception e) {
				e.printStackTrace();
			}		
	}
	
	public void setName(String first_name, String last_name) {
		if (User.self == this) {
			this.first_name = first_name;
			this.last_name = last_name;
		}
		object.set("real_first_name", first_name);
		object.set("real_last_name", last_name);		
	}
	
	public void getPhoto(ImageView img) {		
		final TL.Object location = getPhotoLocation();
		img.setTag(location);
		
		if (location == null) {
			img.setImageResource(getDefPhoto());
			return;
		}
			
		String path = FileQuery.exists(location);
		if (path != null) {
			Bitmap bmp = BitmapCache.get(path);
			if (bmp != null) {
				img.setImageBitmap(bmp);
				return;
			}
		}

		final WeakReference<ImageView> ref = new WeakReference<ImageView>(img);
		
		try {
			new FileQuery(location, null, new FileQuery.OnResultListener() {			
				@Override
				public void onResult(TL.Object result) {
					ImageView img = ref.get();
					if (img == null || img.getTag() != location)
						return;
					
					final Bitmap bmp = result != null ? BitmapCache.loadBitmap(result.getString("fileName"), Main.SIZE_THUMB_USER, Main.SIZE_THUMB_USER, true, false, true) :
														BitmapFactory.decodeResource(Main.main.getResources(), getDefPhoto());
					
					try {		
						img.post(new Runnable() {
							@Override
							public void run() {
								ImageView img = ref.get();
								if (img == null || img.getTag() != location)
									return;
								img.setImageBitmap(bmp);
							}							
						});
					} catch (Exception e) {
						Common.logError("error loading bitmap");
						e.printStackTrace();
					}
				}

				@Override
				public void onProgress(float progress) {}
			});
		} catch (Exception e) {			
			Common.logError("error query file");
			e.printStackTrace();
		}
	}
	
	public void setPhoto(TL.Object userPhoto) {				
		boolean update = photo == null || !Arrays.equals(photo.serialize(), userPhoto.serialize());
		if (update)
			try {
				photo = userPhoto;
				object.getObject("user").set("photo", photo);
				Main.main.updateUser(this);			
			} catch (Exception e) {
				e.printStackTrace();
			}		
	}
			
	public int getDefPhoto() {
		return id != -1 ? def_picture[id % def_picture.length] : def_picture[0];
	}
		
	public String getColor() {
		return def_color[id % 8];
	}
	
	public TL.Object getPhotoLocation() {
		if (photo == null || photo.name.equals("userProfilePhotoEmpty"))		
			return null;
		try {
			return photo.getObject("photo_small");			
		} catch (Exception e) {
			return null;
		}
	}

	public TL.Object getInputPeer() {
		if (this == User.self)
			return TL.newObject("inputPeerSelf");
		if (!contact)
			return TL.newObject("inputPeerForeign", id, access_hash);
		return TL.newObject("inputPeerContact", id);
	}		
}
