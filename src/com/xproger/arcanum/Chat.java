package com.xproger.arcanum;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.util.SparseArray;
import android.widget.ImageView;

public class Chat {
	public static SparseArray<Chat> chats = new SparseArray<Chat>();
	public static int def_picture[] = { 
			R.drawable.group_placeholder_blue,
			R.drawable.group_placeholder_cyan,
			R.drawable.group_placeholder_green,
			R.drawable.group_placeholder_orange,
			R.drawable.group_placeholder_pink,
			R.drawable.group_placeholder_purple,
			R.drawable.group_placeholder_red,
			R.drawable.group_placeholder_yellow
		};
		
	public static Chat getChat(int id) {
		Chat chat = chats.get(id);
		if (chat == null) {
			chat = new Chat(id);
			chats.put(id, chat);
		}
		return chat;
	}
	
	public static void addChats(TL.Vector chats) {
		if (chats == null) return;
		for (int i = 0; i < chats.count; i++)
			Chat.addChat(chats.getObject(i));		
	}

	public static Chat addChat(TL.Object chat) {
		Chat c = getChat(chat.getInt("id"));
		c.update(chat);		
		return c;
	}
	
	public static TL.Vector getVector() {
		ArrayList<TL.Object> res = new ArrayList<TL.Object>();
		for (int i = 0; i < Chat.chats.size(); i++)
			res.add(Chat.chats.get(Chat.chats.keyAt(i)).getObject());
		return TL.newVector(res.toArray());
	}	
	
	public static void init() {
		chats.clear();
	}
	
	public int id;
	private String title;
	public TL.Object objectFull, objectChat, photo;
//	public Bitmap photo_small;
	public ArrayList<User> users = new ArrayList<User>();
	public boolean forbidden = false, left = false;
	public int admin_id, version;

	public void initObj() {
		objectChat = TL.newObject("chatEmpty", id);
		objectFull = TL.newObject("chatFull", id, 
							TL.newObject("chatParticipantsForbidden", id), 
							TL.newObject("photoEmpty", 0L),
							TL.newObject("peerNotifySettingsEmpty"));
	}
	
	public Chat(int id) {
		this.id = id;
		initObj();
	}
		
	public Chat(TL.Object chat) {
		id = chat.getInt("id");
		initObj();		
		update(chat);
	}
	
	public void update(TL.Object chat) {
		if (chat.id == 0x630e61be)
			processFull(objectFull = chat);
		else
			if (chat.id == 666666001) {	// arcanum.chat
				processChat(objectChat = chat.getObject("objectChat"));
				processFull(objectFull = chat.getObject("objectFull"));
			} else 				
				processChat(objectChat = chat);
	}
	
	private void processChat(TL.Object chat) {
		if (chat.id != 0x9ba2d800) {	// !chatEmpty				
			setTitle(chat.getString("title"));
			// date ...	
			if (chat.id != 0xfb0ccc41) {	// !chatForbidden
				setPhoto(chat.getObject("photo"));		
				left = chat.getBool("left");
				int oldVersion = version;
				version = chat.getInt("version");					
				if (oldVersion != version)
					Main.mtp.api_messages_getFullChat(id);	// TODO: ???
			} else
				forbidden = true;
		}
		if (Main.main != null)
			Main.main.updateChat(this);						
	}
	
	private void processFull(TL.Object chat) {
		setParticipants(chat.getObject("participants"));
	}
	
	public TL.Object getObject() {
		return TL.newObject("arcanum.chat", id, objectChat, objectFull);		
	}
	
	public void setParticipants(TL.Object obj) {		
		if (obj == null) return;
		objectFull.set("participants", obj);
		if (obj.id == 0xfd2bb8a) return; // chatParticipantsForbidden
		
		admin_id = obj.getInt("admin_id");
		version = obj.getInt("version");
		TL.Vector p =  obj.getVector("participants");
		for (int i = 0; i < p.count; i++)
			addUser(p.getObject(i).getInt("user_id"), false);		
		Main.main.updateChat(this);	
	}
	
	public void setUsers(TL.Vector users) {
		this.users.clear();
		for (int i = 0; i < users.count; i++) {
			int id = users.getInt(i);
			addUser(id, false);
		}
		Main.main.updateChat(this);
	}
		
	public void addUser(int id, boolean updateUI) {
		User user = User.getUser(id);
		if (users.contains(user)) return;
		users.add(user);
		if (updateUI)
			Main.main.updateChat(this);		
	}
	
	public void delUser(int id) {
		User user = User.getUser(id);
		if (!users.contains(user)) return;
		users.remove(user);		
		Main.main.updateChat(this);		
	}
	
	public void setTitle(String title) {
		this.title = title;		
		Main.main.updateChat(this);		
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getStatusHtml() {
		String typingStr = null;
		int typingCount = 0, onlineCount = 0;
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			if (user != null) {
				if (user.typing) {
					if (typingStr == null)					
						typingStr = user.first_name;
					else
						typingStr += ", " + user.first_name;
					typingCount++;
				}
				if (user.online)
					onlineCount++;
			}
		}

		switch (typingCount) {
			case 0 : 
				return String.format(Main.getResStr(R.string.status_online_chat), users.size(), onlineCount);
			case 1 :
				return String.format(Main.getResStr(R.string.status_typing_chat1), typingStr);
			case 2 :
				return String.format(Main.getResStr(R.string.status_typing_chat2), typingStr);
		}			
		return String.format(Main.getResStr(R.string.status_typing_chatN), typingCount);			
	}
	
	public CharSequence getStatus() {
		return Html.fromHtml(getStatusHtml());
	}
			
	public void setPhoto(TL.Object photo) {				
		boolean upd_photo = this.photo == null || !Arrays.equals(this.photo.serialize(), photo.serialize());
					
//		if (object != null && object.id == 0x630e61be)
//			object.getObject("user").set("photo", photo);		
			
		this.photo = photo;
		if (upd_photo)
			Main.main.updateChat(this);
	}	
	
	public int getDefPhoto() {		
		return def_picture[id % def_picture.length];
	}
	
	public TL.Object getPhotoLocation() {
		if (photo == null || photo.id == 0x37c1011c) // chatPhotoEmpty
			return null;
		
		if (photo.id == 0x6153276a)
			return photo.getObject("photo_small");
		
		if (photo.id == 0x22b56751) {
			TL.Vector sizes = photo.getVector("sizes");
			for (int i = 0; i < sizes.count; i++) {
				TL.Object photoSize = sizes.getObject(i);
				if (photoSize.getString("type").equals("a")) 
					return photoSize.getObject("location");
			}
		}
		
		return null;		
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

	
	public TL.Object getInputPeer() {
		return TL.newObject("inputPeerChat", id);		
	}
	
	public void apiUserDel(User user) {
		Main.mtp.api(null, null, "messages.deleteChatUser", id, user.getInputUser());
	}

	public void apiUserAdd(User user) {		
		Main.mtp.api(null, null, "messages.addChatUser", id, user.getInputUser(), 20);
	}	
	
}
