package com.xproger.arcanum;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.google.android.gms.maps.model.LatLng;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.net.Uri;
import android.support.v4.util.LongSparseArray;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.widget.ImageView;

public class Dialog implements TL.OnResultRPC {
	public static final int MSG_HISTORY		= 1;
	public static final int MSG_INCOMING	= 2;
	
	public static ArrayList<Dialog> dialogs = new ArrayList<Dialog>();
	public static LongSparseArray<Message> waitMessage = new LongSparseArray<Message>();
	public static SparseArray<Message> messages = new SparseArray<Message>();

	public static SparseArray<Drawable> msgIcon = new SparseArray<Drawable>(); 	
	
	public static class Message {
		public static int max_id;
		
		public static class List extends ArrayList<Message> {
		    public int insertSorted(Message msg) {
		    	int i = size() - 1;
		    	while (i >= 0) {
		    		Message m = get(i);
		    		if (msg.id > m.id && m.id != 0) {
		    			add(i + 1, msg);		    			
		    			return i + 1;
		    		}		    			
		    		i--;
		    	}
		    	add(0, msg);
		    	return 0;
		    }		
		}	
		
		int id, from_id, date, fwd_from_id, fwd_date, to_id_user, to_id_chat;
		boolean unread, deleted = false, out = false;
		String message;
		TL.Object media;
		Object action;
		public String mediaPath = null, videoPath = null;
		Dialog dialog;

		public TL.Object object;
		
		public FileQuery query;
		public Bitmap thumb;
		public float progress;
		public TL.Object thumbInputFile;
		
		
		long random_id = 0;
		
		public Message(String dateText) {
			action = dateText; 
		}
		
		public Message(Dialog dialog, Bitmap thumb) {
			this.thumb = thumb;
			this.dialog = dialog;
			id   = 0;
			date = Common.getUnixTime();			
			from_id = User.self.id;
			out		= true;
			unread	= true;
			message = "loading...";
			action  = null;
			progress = 0;
		}
		
		public Message(TL.Object msg) {
			id		= msg.getInt("id");
			if (id > max_id)
				max_id = id;
			from_id	= msg.getInt("from_id"); 
			out		= msg.getBool("out");
			unread	= msg.getBool("unread");
			date	= msg.getInt("date");						
			
			TL.Object peer = msg.getObject("to_id");			
			to_id_user = peer.id == 0x9db1bc6d ? peer.getInt("user_id") : -1;
			to_id_chat = peer.id == 0xbad0e5bb ? peer.getInt("chat_id") : -1;
						
			if (msg.id == 0x5f46804) { // messageForwarded
				fwd_from_id	= msg.getInt("fwd_from_id");
				fwd_date	= msg.getInt("fwd_date");
			} else
				fwd_from_id = fwd_date = 0;
			
			if (msg.id != 0x9f8d60bb)	// messageService
				message = msg.getString("message");
			else
				action = msg.getObject("action");

			update(msg);			
		//	Common.logError("message: (" + id  + ") from: " + from_id + "  text: " + message);
		}
		
		public static TL.Object newObject(int id, int from_id, int chat_id, int user_id, int date, String message, TL.Object messageMedia) {
			return TL.newObject("message", id, from_id, 
					chat_id != -1 ? TL.newObject("peerChat", chat_id) : TL.newObject("peerUser", user_id), 
					from_id == User.self.id, true, date, message, messageMedia == null ? TL.newObject("messageMediaEmpty") : messageMedia);	
		}
		
		public void setUnread(boolean value) {
			unread = value;
			if (object != null) 
				object.set("unread", unread);
			Main.main.updateMessage(this);
		}

		public void setID(int value) {
			id = value;
			if (object != null)
				object.set("id", value);
			Main.main.updateMessage(this);
		}
		
		public String getText() {
			User user = User.getUser(from_id);
			if (user == null)
				return "[empty]";
			
			if (media != null && action == null) {
				if (media.id == 0x5e7d2f39) {
					User u = User.getUser(media.getInt("user_id"));
					return String.format("<font color=\"%s\">%s</font><br/><small>%s</small>", u.getColor(), u.getTitle(), Common.formatPhone(u.phone));
				}
				return "";
			}
			
			if (action != null) {
				if (action instanceof TL.Object) {
					String name = user == User.self ? Main.getResStr(R.string.info_you) : user.getTitle();					
					String msg = "";
					TL.Object act = (TL.Object)action;
					switch (act.id) {
						case 0xa6638b9a : msg = String.format(Main.getResStr(R.string.group_info_create), name); break; 
						case 0xb5a1ce5a : msg = String.format(Main.getResStr(R.string.group_info_title), name, act.getString("title")); break;
						case 0x7fcb13a8 : msg = String.format(Main.getResStr(R.string.group_info_photo_edit), name); break;
						case 0x95e3fbef : msg = String.format(Main.getResStr(R.string.group_info_photo_delete), name); break;
						case 0x5e3cfc4b : 
						case 0xb2ae9b0c :
							User u = User.getUser(act.getInt("user_id"));
							String n = u == null ? "[user]" : u == User.self ? Main.getResStr(R.string.info_you) : u.getTitle();						
							if (u == user)
								msg = String.format(Main.getResStr(R.string.group_info_suicide), name);	// suicide 
							else
								msg = String.format(Main.getResStr(act.id == 0x5e3cfc4b ? R.string.group_info_add : R.string.group_info_del), name, n); 
							break;
					}				
					return msg;
				} else
					return "<big>" + (String)action + "</big>";
			}
			
			String msgStr = message;
			
			if (!out && dialog != null && dialog.chat_id != -1)
				msgStr = String.format("<font color=\"%s\">%s</font><br/>%s", user.getColor(), user.getTitle(), msgStr);
						
			if (fwd_from_id != 0) {
				String fromStr = String.format(Main.main.getResources().getString(R.string.msg_forwarded), User.getUser(fwd_from_id).getTitle());
				msgStr = fromStr + msgStr; 
			}
			
			return msgStr;
		}
		
		public CharSequence getTextSpanned() {
			SpannableStringBuilder sb = new SpannableStringBuilder(Html.fromHtml(getText()));
			Emoji.replace(sb);
			return sb;
		}
		
		public boolean hasMedia() {
			return thumb != null || (media != null && !media.name.equals("messageMediaEmpty"));
		}
		
		public LatLng getGeoPoint() {
			if (media == null || media.id != 0x56e0d474)
				return null;
			TL.Object geo = media.getObject("geo");
			double g_long = geo.getDouble("long");
			double g_lat = geo.getDouble("lat");
			return new LatLng(g_lat, g_long);
		}
				
		public boolean update(TL.Object msg) {
			object = msg;
			if (media != null)
				return false;
			
			mediaPath = null;			
			if (msg.id == 0x9f8d60bb || msg.id == 0x83e5de54) { // messageService || messageEmpty				
				if (action != null && action instanceof TL.Object && ((TL.Object)action).id == 0x7fcb13a8)
					media = (TL.Object)action;
				else
					media = null;
				return false;
			} else
				message = msg.getString("message").replaceAll("\n", "<br>");
			
			media = msg.getObject("media");
			
			if (media.id == 0x3ded6320) // messageMediaEmpty
				media = null;
			else
				if (media.id == 0xa2d24290)	{// messageMediaVideo
					TL.Object video = media.getObject("video");
					if (video.id != 0xc10658a8)
						videoPath = FileQuery.exists(video.getInt("dc_id"), video.getLong("id"));				
				}
			
			return media != null;
		}
		
		private TL.Object getThumbPhoto(TL.Vector photoSizes) {
			String sizes[] = {"m", "b", "x", "c", "s", "a", "y", "d", "w"};
			for (int i = 0; i < sizes.length; i++)
				for (int j = 0; j < photoSizes.count; j++) {
					TL.Object item = photoSizes.getObject(j);					
					if (sizes[i].equals(item.getString("type")))
						return item;					
				}
			return null;
		}
		
		public void getUserPhoto(ImageView img) {
			User.getUser(from_id).getPhoto(img);
		}
		
		public ImageView getImage(WeakReference<ImageView> ref) {			
			ImageView img = (ImageView)ref.get();
			return (img != null && img.getTag() == this) ? img : null; 
		}
		
		public void resizeImage(ImageView image, int width, int height) {
		/*
			LayoutParams lp = image.getLayoutParams(); 
			lp.width	= width;
			lp.height	= height;
			image.setLayoutParams(lp);
		*/
		}
		
		public void getMedia(ImageView image, String type) {
			if (image == null || !hasMedia()) return;
			
			image.setTag(this);
						
			if (thumb != null) {
				image.setImageBitmap(thumb);
				return;
			}

			if (mediaPath != null) {
				Bitmap bmp = BitmapCache.get(mediaPath);
				if (bmp != null) {
					BitmapDrawable bd = (BitmapDrawable)image.getDrawable();
					if (bd != null) {
						if (bd.getBitmap() != bmp) {
							try {
								resizeImage(image, bmp.getWidth(), bmp.getHeight());
								image.setImageBitmap(bmp);
							} catch (Exception e) {}
						}
						return;
					}
				}
			}
			
			if (media.id == 0x5e7d2f39) {				
				User.getUser(media.getInt("user_id")).getPhoto(image);
				return;
			}
			
			final WeakReference<ImageView> ref = new WeakReference<ImageView>(image);
			
			if (media.id == 0x56e0d474) {	// messageMediaGeo	
				TL.Object geo = media.getObject("geo");
				double g_long = geo.getDouble("long");
				double g_lat = geo.getDouble("lat");
				
				resizeImage(image, Main.SIZE_THUMB_MEDIA, Main.SIZE_THUMB_MEDIA);
				image.setImageResource(0);				
				
				PageDialog.mapCapture(new Common.MediaReq() {
					@Override
					public void onMedia(Uri media) {
						if (media == null) return;
						try {
							if (getImage(ref) == null)
								return;
							
							mediaPath = Common.getPathFromURI(Main.main, media);

							final Bitmap bmp = BitmapCache.loadBitmap(mediaPath, Main.SIZE_THUMB_MEDIA, Main.SIZE_THUMB_MEDIA, false, false, true);
							Main.main.mainView.post(new Runnable() {
								@Override
							    public void run() {
									try {
										ImageView img = getImage(ref); 
										if (img != null) 
											img.setImageBitmap(bmp);										
									} catch (Exception e) {}
							    }
							});	
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, g_lat, g_long);
			}

			 
					
			if (media.id == 0xc8c45a2a || media.id == 0xa2d24290 || media.id == 0x7fcb13a8) {	// messageMediaPhoto, messageMediaVideo, messageActionChatEditPhoto				
				TL.Object photoSize = null;
				final int size = action != null ? Main.SIZE_THUMB_USER : Main.SIZE_THUMB_MEDIA;
				
				if (media.id == 0xc8c45a2a || media.id == 0x7fcb13a8) {	// messageMediaPhoto || messageActionChatEditPhoto
					TL.Object photo = media.getObject("photo");				
					if (!photo.name.equals("photo")) return;				
					photoSize = getThumbPhoto(photo.getVector("sizes"));
				} else {	// messageMediaVideo
					TL.Object video = media.getObject("video");
					if (video.id == 0x5a04a49f) { // !videoEmpty
						photoSize = video.getObject("thumb");
						if (photoSize.id == 0xe17e23c)
							photoSize = null;
					}
					if (photoSize == null) {						
						resizeImage(image, size, size);
						Common.logError("video placeholder");
						image.setImageResource(R.drawable.video_placeholder);						
					}					
				}
				
				if (photoSize == null || photoSize.id == 0xe17e23c) return;
				
				TL.Object location = photoSize.getObject("location");
				if (location == null) return;
				/*
				int w = photoSize.getInt("w");
				int h = photoSize.getInt("h");
				
				if (w > size || h > size) {
					float s = (float)size / (float)Math.max(w, h);
					w = (int)(s * (float)w);
					h = (int)(s * (float)h);
				}
				
				resizeImage(image, w, h);
				*/
				image.setImageResource(0);
					
				try {
					new FileQuery(location, null, new FileQuery.OnResultListener() {			
						@Override
						public void onResult(TL.Object result) {
							if (result == null)
								return;
							
							try {
								if (Main.main != null) {									
									mediaPath = result.getString("fileName");
									
									if (getImage(ref) == null)
										return;							
									
									final Bitmap bmp = BitmapCache.loadBitmap(mediaPath, size, size, false, false, true);									
									Main.main.mainView.post(new Runnable() {
										@Override
									    public void run() {
											try {
												ImageView img = getImage(ref);
												if (img != null) 
													img.setImageBitmap(bmp);												
											} catch (Exception e) {}
									    }
									});									
								}
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
		}
		
		public CharSequence getStatusTime() {
			if (out) {
				if (thumb != null)
					return getTime();				
				
				int resId = (media != null && (media.id == 0xc8c45a2a || media.id == 0xa2d24290)) ?  
						(id == 0 ? (timeOut() ? R.drawable.msg_warning : R.drawable.msg_clock_photo) : (unread ? R.drawable.msg_check_photo : R.drawable.msg_halfcheck_photo)) :
						(id == 0 ? (timeOut() ? R.drawable.msg_warning : R.drawable.msg_clock) : (unread ? R.drawable.msg_check : R.drawable.msg_halfcheck));
				Drawable d = msgIcon.get(resId);
				if (d == null) {
					Common.logError("null");
					return getTime();
				}

				SpannableStringBuilder builder = new SpannableStringBuilder();
				builder.append(getTime() + " ");
				int len = builder.length();	
				ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
				builder.append(" ");
				builder.setSpan(icon, len, len + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				return builder;
			} else				
				return getTime();			
		}
		
		public String getTime() {
			String format = DateFormat.is24HourFormat(Main.main) ? "HH:mm" : "h:mm a";
			return new SimpleDateFormat(format).format(date * 1000L);			
		}
		
		public int getStatusColorText() {
			if (thumb != null)
				return 0;
			if (media == null || media.id == 0x5e7d2f39)
				return out ? 0xff70b15c : 0xffa1aab3;	// text message
			switch (media.id) {
				case 0x56e0d474 : return 0xff70b15c;	// messageMediaGeo
				case 0xc8c45a2a : 						// messageMediaPhoto
				case 0xa2d24290 : return 0xffffffff;	// messageMediaVideo
				default : return 0xffa1aab3;			// messageMediaUnsupported
			}
 		}
		
		public int getStatusColorBack() {
			if (thumb != null)
				return 0;			
			if (media == null || media.id == 0x5e7d2f39)
				return 0;	// text message
			switch (media.id) {
				case 0x56e0d474 : return 0xa0ffffff;	// messageMediaGeo
				case 0xc8c45a2a : 						// messageMediaPhoto
				case 0xa2d24290 : return 0x80000000;	// messageMediaVideo
				default : return 0;						// messageMediaUnsupported
			}
		}		
		
		public boolean hasVideoMedia() {
			return media != null && media.id == 0xa2d24290;
		}
		
		public String getVideoText() {
			if (!hasVideoMedia())
				return "";
			TL.Object video = media.getObject("video");
			if (video.id == 0xc10658a8)
				return "";
			int sec = video.getInt("duration");
			return String.format("%02d:%02d", (int)(sec / 60), sec % 60);
		}
		
		public String getMediaViewText() {
    		int resId = R.string.media_unsupported;
    		if (media != null)
	    		switch (media.id) {
	    			case 0xc8c45a2a : resId = R.string.media_photo; break;
	    			case 0xa2d24290 : resId = videoPath != null ? R.string.media_video : R.string.media_download; break;
	    			case 0x56e0d474 : resId = R.string.media_location; break;    		
	    		}    		
    		String str = Main.getResStr(resId);
    		if (resId == R.string.media_download) {
    			int size = 0;
    			TL.Object video = media.getObject("video"); 
    			if (video.id != 0xc10658a8)
    				size = video.getInt("size");    			
    			return String.format(str, (float)size / 1024.0f / 1024.0f);
    		}
    		return str;
		}
		
		public CharSequence getTitle() {
			User user = User.getUser(from_id);
			return Html.fromHtml(String.format(Main.getResStr(R.string.notify_format), user.getTitle()));
		}

		private String getInfoStr(int id) {
			return String.format(Main.getResStr(R.string.info_format_media), Main.getResStr(id));
		}
			
		public String getInfoMedia() {
			switch (media.id) {
				case 0xc8c45a2a : return getInfoStr(R.string.info_photo); 
				case 0xa2d24290 : return getInfoStr(R.string.info_video);
				case 0x56e0d474 : return getInfoStr(R.string.info_location);
				case 0x5e7d2f39 : return getInfoStr(R.string.info_contact);
				case 0x29632a36 : return getInfoStr(R.string.info_document);
			}	
			return "";
		}
		
		public CharSequence getStatusText() {
			if (fwd_from_id != 0)
				return Html.fromHtml(getInfoStr(R.string.info_forwarded));				
			if (action != null) {
				return Html.fromHtml(getText());		
			} else
				if (media != null) {
					return Html.fromHtml(getInfoMedia());
				} else {
					SpannableStringBuilder sb = new SpannableStringBuilder(Html.fromHtml(message));
					Emoji.replace(sb);
					return sb;
				}
		}
		
		public static TL.Vector getVector() {
			ArrayList<TL.Object> res = new ArrayList<TL.Object>();
			for (int i = 0; i < messages.size(); i++)
				res.add(messages.get(messages.keyAt(i)).object);
			return TL.newVector(res.toArray());
		}		
		
		public boolean timeOut() {
			return (Common.getUnixTime() - date) > 10000;	// TODO: server_time
		}
	}
		
	public int user_id, chat_id, date = 0;
	public int min_id, unread_count;
	public boolean waitHistory = false, noHistory = false, updating = false;
		
	public Message.List msgList = new Message.List();
	public CharSequence msgText = "";
	
	
	public static void init() {
		Message.max_id = 0;
		dialogs.clear();
		waitMessage.clear();
		messages.clear();
				
		int iconId[] = {
			R.drawable.dialogs_check,
			R.drawable.dialogs_halfcheck,
			R.drawable.dialogs_warning,
			R.drawable.msg_check,
			R.drawable.msg_halfcheck,
			R.drawable.msg_check_photo,
			R.drawable.msg_halfcheck_photo,
			R.drawable.msg_clock,
			R.drawable.msg_warning
		};
		
		Resources r = Main.main.getResources();
		for (int i = 0; i < iconId.length; i++) {
			Drawable d = r.getDrawable(iconId[i]);		
			d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			msgIcon.put(iconId[i], d);
		}
	}
	
	public static void newMessage(TL.Object message, int msgFlag) {
		if (message.id == 0x83e5de54) return; // messageEmpty
 		
		if (getMessage(message.getInt("id")) != null) return;

		int old_max_id = Message.max_id;
		
		Message msg = new Message(message);
		
		Dialog dialog = null;
		if (msg.to_id_chat == -1)
			dialog = getDialog(msg.out ? msg.to_id_user : msg.from_id, -1, true);
		else
			dialog = getDialog(-1, msg.to_id_chat, true);
				
		if (dialog == null) return;		
		
		dialog.addMessage(msg);		
		
		if ((msgFlag & MSG_INCOMING) > 0) {
			if (message.id == 0x9f8d60bb)	// messageService
				dialog.serviceMessage(message);
			
		//	if (blink && Main.main != null)
		//		Main.main.setTopDialog(dialog);
			
			if (old_max_id < Message.max_id)
				Main.mtp.api(null,  null, "messages.receivedMessages", Message.max_id);	
			
			if (msg.from_id != User.self.id) {
				vibrate();
				sound();
				
				Main.main.showNotify(msg);
			}				
		}
		
		if (((msgFlag & MSG_INCOMING) > 0) && msg.from_id != User.self.id) {
			vibrate();
			sound();
		}		
	}
	
	public static void vibrate() {
		if (Main.settings.getBool("inapp_vibrate"))
			Common.vibrate(200);		
	}
	
	public static void sound() {
		if (Main.settings.getBool("inapp_sound"))
			Common.playSound(Main.main, Common.soundDef);		
	}
	
	public static Dialog getDialog(int user_id, int chat_id, boolean create) {
		Dialog d;
		for (int i = 0; i < dialogs.size(); i++) {
			d = dialogs.get(i);
			if (d.user_id == user_id && d.chat_id == chat_id)
				return d;
		}		
		if (create) {
			d = new Dialog(user_id, chat_id);
			dialogs.add(d);
			if (Main.main != null)
				Main.main.resetDialogs();
		} else
			d = null;
		return d;	
	}	
	
	public static Dialog getDialog(int from_id, TL.Object peer, boolean create) {
		int user_id = -1, chat_id = -1;
		
		if (peer.name.equals("peerUser"))
			user_id = from_id == -1 ? peer.getInt("user_id") : from_id;
		else
			chat_id = peer.getInt("chat_id");
		
		return getDialog(user_id, chat_id, create);
	}
	
	public Dialog(int user_id, int chat_id) {
		min_id = Integer.MAX_VALUE;
		unread_count = 0;
		
		if (user_id != -1 && User.getUser(user_id) == null)
			User.addUser(TL.newObject("userEmpty", user_id));

		this.user_id = user_id;
		this.chat_id = chat_id;
	}
	
	public void getPicture(ImageView img) {
		if (user_id != -1)
			User.getUser(user_id).getPhoto(img);
		else
			if (chat_id != -1)
				Chat.getChat(chat_id).getPhoto(img);
	}
	
	public CharSequence getTitle(boolean color) {
		if (user_id != -1) {
			User user;
			return (user = User.getUser(user_id)) != null ? (color ? user.getTitleColored() : user.getTitle()) : "null user";
		}	
		
		if (chat_id != -1) {
			Chat chat;
			return (chat = Chat.getChat(chat_id)) != null ? chat.getTitle() : "null chat";
		}
		
		return "null";
	}
	
	
	public String getTyping() {
		return "";
	}
	
	private String getInfoStr(int id) {
		return String.format(Main.getResStr(R.string.info_format_media), Main.getResStr(id));
	}
	
	public CharSequence getInfo() {
		String info = getTyping();
		
		if (info.equals("") && msgList.size() > 0) {		
			Message msg = msgList.get(msgList.size() - 1);
			if (msg.action != null) {
				info = msg.getText();
			} else {
				if (msg.fwd_from_id != 0)
					info = getInfoStr(R.string.info_forwarded);				
				if (msg.media != null)
					info = msg.getInfoMedia();				
				
				if (info.equals(""))
					info = msgList.get(msgList.size() - 1).message;	// TODO: sending info				
					
				if (chat_id != -1) {
					if (!msg.out) {
						User user = User.getUser(msg.from_id);
						if (user != null)
							info = String.format("<font color=\"#006fc8\"><b>%s</b></font><br/>%s", user.getTitle(), info);
					} else
						info = Main.getResStr(R.string.info_you) + "<br/>" + info;
				}				
			}
			
		}
		
		SpannableStringBuilder sb = new SpannableStringBuilder(Html.fromHtml(info));
		Emoji.replace(sb);
		return sb;		
	}
	
	public String getStatus() {
		if (user_id != -1)
			return User.getUser(user_id).getStatusHtml();		
		if (chat_id != -1)
			return Chat.getChat(chat_id).getStatusHtml();		
		return "";
	}
	
	public CharSequence getTime() {
		if (msgList.size() == 0)
			return "";
		Message msg = msgList.get(msgList.size() - 1);

		String text = "";
		
		Calendar c = Calendar.getInstance();		
		c.setTime(new Date(msg.date * 1000L));
		int day_msg = c.get(Calendar.DAY_OF_YEAR);
		c.setTime(new Date(Common.getUnixTime() * 1000L));
		int day_cur = c.get(Calendar.DAY_OF_YEAR);
		
		if (day_msg == day_cur)
			text = msg.getTime();
		else
			if (day_msg == day_cur - 1)
				text = Main.getResStr(R.string.info_yesterday);
			else
				text = new SimpleDateFormat("dd MMM").format(msg.date * 1000L);
			
		if (msg.out) {
			Drawable d = msgIcon.get(
				msg.id == 0 ? 
					(msg.timeOut() ? R.drawable.dialogs_warning : R.drawable.msg_clock) :
					(msg.unread ? R.drawable.dialogs_check : R.drawable.dialogs_halfcheck));  
			SpannableStringBuilder builder = new SpannableStringBuilder();
			builder.append("  " + text);
			ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
			builder.setSpan(icon, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			return builder;
		} else
			return text;
	}
	
	public String getUnreadCount() {
		return String.valueOf(unread_count);
	}
	
	public int getUnixTime() {
		if (msgList.size() == 0)
			return date;
		return msgList.get(msgList.size() - 1).date;
	}
	
	public void queryHistory(int count) {
		if (waitHistory || noHistory || user_id == 0 || chat_id == 0) return;
		
		waitHistory = true;
		
		final Dialog dialog = this;		
		Main.mtp.api_messages_getHistory(getInputPeer(), 0, min_id, count, new TL.OnResultRPC() {			
			@Override
			public void onResultRPC(TL.Object result, Object param, boolean error) { // param == getHistory.limit value
				if (error) {
					waitHistory = false;
					return;					
				}

				User.addUsers(result.getVector("users"));
				Chat.addChats(result.getVector("chats"));
				
				Common.logError("history for " + user_id + "/" + chat_id);
				
				final TL.Vector messages = result.getVector("messages");				
				
				dialog.updating = true;
				for (int i = 0; i < messages.count; i++)
					dialog.addMessage(new Message(messages.getObject(i)));
				dialog.updating = false;
				
				Common.logError("messages.count : " + messages.count);
				int leftCount = (Integer)param - messages.count;
				
				if (result.id == 0x8c718e87 || leftCount <= 0 || messages.count == 0) {	// messages.messages					
					if (leftCount > 0)
						noHistory = true;
				
					waitHistory = false;
					Main.main.updateDialog(dialog);					
				} else 
					Main.mtp.api_messages_getHistory(getInputPeer(), 0, min_id, leftCount, this);
			}
		});
	}
	
	public TL.Object getInputPeer() {
		if (chat_id != -1)
			return Chat.getChat(chat_id).getInputPeer();		
		if (user_id != -1)
			return User.getUser(user_id).getInputPeer();
		return TL.newObject("inputPeerEmpty");
	}
	
	public static Message getMessage(int id) {
		return messages.get(id);
	}
	
	public int getMessageIndex(Message msg) {
		return msgList.indexOf(msg);
	}
	
	public void addMessage(Message message) {
		message.dialog = this;
		/*
		if (message.out)
			Common.logError("message -> " + user_id + "/" + chat_id + " : " + message.message);
		else
			Common.logError("message <- " + user_id + "/" + chat_id + " : " + message.message);
		*/
		int msg_index = msgList.size();
		if (message.id != 0) {
			if (getMessage(message.id) != null)
				return;
			msg_index = msgList.insertSorted(message);
						
			if (!message.out && message.unread)
				unread_count++;
			
			if (message.id != 0 && message.id < min_id)
				min_id = message.id;
			
			messages.put(message.id, message);
		} else
			msgList.add(message);		
		
		if (!updating) {
			// TODO : Main.main.addMessage(index)
			Main.main.messageAdd(message);
			//Main.main.updateDialog(this);
			//Main.main.setTopDialog(this);
		}
	}
	
	public void serviceMessage(TL.Object msg) {
		Common.logError("service action");
		TL.Object action = msg.getObject("action");
		
		Chat chat = Chat.getChat(chat_id);
		
		int id = msg.getInt("id");
		if (id < min_id)
			min_id = id;
		
		switch (action.id) {
			case 0xa6638b9a :	// messageActionChatCreate	
				chat.setUsers(action.getVector("users"));
				break;
			case 0xb5a1ce5a :	// messageActionChatEditTitle
				chat.setTitle(action.getString("title"));
				break;
			case 0x7fcb13a8 :	// messageActionChatEditPhoto
				chat.setPhoto(action.getObject("photo"));
				break;
			case 0x95e3fbef :	// messageActionChatDeletePhoto
				//chat.setDefPhoto();
				break;
			case 0x5e3cfc4b :	// messageActionChatAddUser
				chat.addUser(action.getInt("user_id"), true);
				break;
			case 0xb2ae9b0c :	// messageActionChatDeleteUser
				int user_id = action.getInt("user_id");
				if (user_id == User.self.id)
					deleteHistory(); // suicide
				else
					chat.delUser(action.getInt("user_id"));
				break;
		}
	}
	
/*	
	private Message addMessage(TL.Object msg) {		
		int id = msg.getInt("id");
		Message message = id == 0 ? null : getMessage(id); 
		if (message == null) {
			message = new Message(msg);
			addMessage(message);
			if (message.id != 0)
				messages.put(message.id, message);
		} else
			Common.logError("message exists");
		
		return message;
	}
*/	
	public void deleteMessages(Object ... msgObj) {
		ArrayList<Integer> msg_id = new ArrayList<Integer>();
		for (int i = 0; i < msgObj.length; i++) {
			Message msg = (Message)msgObj[i];
			msg.deleted = true;
			msgList.remove(msg);
			Main.main.messageDelete(msg);
			if (msg.id != 0)
				msg_id.add(msg.id);
		}
//		Main.main.updateDialog(this);		
		TL.Vector id = TL.newVector(msg_id.toArray());
		Main.mtp.api_messages_deleteMessages(id);
	}
	
	public Message sendMessage(String text, TL.Object inputMedia) {
		Message message = new Message(Message.newObject(0, User.self.id, chat_id, user_id, Common.getUnixTime(), text, null));
		addMessage(message);
		long random_id = Common.random.nextLong();
		waitMessage.put(random_id, message);
		if (inputMedia != null)
			Main.mtp.api_messages_sendMedia(getInputPeer(), inputMedia, random_id, this);
		else
			Main.mtp.api_messages_sendMessage(getInputPeer(), text, random_id, this);		
		//Main.main.updateDialog(this);		
		//Main.main.setTopDialog(this);
		return message;
	}
	
	public static void messageUpdateID(long random_id, int id, TL.Object msg) {
		Message message = waitMessage.get(random_id);
		if (message != null) {
			message.setID(id);
			messages.put(message.id, message);
			waitMessage.remove(random_id);
			if (msg != null)
				message.update(msg);
			
		//	Main.main.updateDialog(message.dialog);
			Main.main.resetMessage(message);

			Common.logError("set message id " + random_id + " -> " + id);
		} else
			Common.logError("message update id " + random_id + " not found");			
	}
	
	public static void messageRead(int id) {
		Message message = messages.get(id);
		if (message != null) {
			message.setUnread(false);			
		} else
			Common.logError("messageRead error " + id);
	}
	
	public boolean userIncluded(User user) {
		if (user == null) return false;
		if (user_id != -1)
			return user_id == user.id;
		if (chat_id != -1) {
			Chat chat = Chat.getChat(chat_id);
			if (chat != null)
				return chat.users.indexOf(user) > -1;
		}
		return false;
	}
	
	public void readHistory() {
		if (unread_count == 0) return;

		final Message msg = msgList.get(msgList.size() - 1);
				
		unread_count = 0;
		Main.main.updateDialogItem(this);
			
		Main.mtp.api(new TL.OnResultRPC() {
			@Override
			public void onResultRPC(TL.Object result, Object param, boolean error) {
				if (error) return;
				int offset = result.getInt("offset");
				if (offset > 0)
					Main.mtp.api(this, null, "messages.readHistory", getInputPeer(), msg.id, offset); 					
			}
		}, null, "messages.readHistory", getInputPeer(), msg.id, 0); 
	}
	
	public static void messageDelete(int id) {
		Message message = messages.get(id);
		if (message == null)
			Common.logError("messageDelete error " + id);
		else {
			message.deleted = true;
			message.dialog.msgList.remove(message);
			Main.main.messageDelete(message);
//			Main.main.updateDialog(message.dialog);
		}
	}
	
	public static void messageRestore(int id) {
		Message message = messages.get(id);
		if (message == null)
			Common.logError("messageRestore error " + id);
		else {
			message.deleted = false;
			message.dialog.msgList.insertSorted(message);
			//Main.chat.addMessage(message, message.dialog.msgList.indexOf(message));
			Main.main.updateDialog(message.dialog);
		}
	}
	
	@Override
	public void onResultRPC(TL.Object result, Object param, boolean error) {
		if (result.id == 0xd1f4d35c)	// messages.sentMessage
			messageUpdateID((Long)param, result.getInt("id"), null);
		if (result.id == 0xd07ae726) {	// messages.statedMessage
			TL.Object message = result.getObject("message");
			messageUpdateID((Long)param, message.getInt("id"), message);
		}		
	}
	
	public ArrayList<Dialog.Message> getMediaList() {
		ArrayList<Dialog.Message> list = new ArrayList<Dialog.Message>();
		for (int i = 0; i < msgList.size(); i++) {
			Dialog.Message msg = msgList.get(i);
			if (msg.media != null && (msg.media.id == 0xc8c45a2a || msg.media.id == 0xa2d24290) && msg.action == null)
				list.add(msg);			
		}
		return list;
	}
	
	public void deleteHistory() {
		final Dialog d = this; 
		Main.mtp.api(new TL.OnResultRPC() {
			@Override
			public void onResultRPC(TL.Object result, Object param, boolean error) {
				if (error) return;
				int offset = result.getInt("offset");
				if (offset > 0) 
					Main.mtp.api(this, null, "messages.deleteHistory", getInputPeer(), offset);
				else {
					Main.main.removeDialog(d);
				}
			}			
		}, null, "messages.deleteHistory", getInputPeer(), 0);
		
	}
	
	public static TL.Vector getVector() {
		ArrayList<TL.Object> res = new ArrayList<TL.Object>();
		for (int i = 0; i < dialogs.size(); i++)
			res.add(dialogs.get(i).getObject());
		return TL.newVector(res.toArray());
	}
	
	public TL.Object getObject() {
		TL.Object peer = null;
		if (user_id != -1) peer = TL.newObject("peerUser", user_id);
		if (chat_id != -1) peer = TL.newObject("peerChat", chat_id);
		return TL.newObject("dialog", peer, noHistory ? -7 : 0, unread_count);
	}
	
	public void leave() {
		Main.main.removeDialog(this);		
		if (chat_id != -1)
			Chat.getChat(chat_id).apiUserDel(User.self);
		if (user_id != -1)
			deleteHistory();
	}
}
