package com.xproger.arcanum;

import java.io.File;
import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AbsListView.OnScrollListener;

public class PageDialog extends FrameLayout implements Common.Page {
	public Dialog dialog;
	public ImageView wpImage;
	private EditText edit;
	private ListView list;
	private Adapter.MessagesAdapter adapter;	
	public static String wallpaper = null;	
	
	public int typing_time;
	
    public PageDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {   
    	wpImage = (ImageView)findViewById(R.id.wallpaper);
    	MessageView.pageDialog = this;
		edit = (EditText)findViewById(R.id.edit_msg);
		final Button btn_send = (Button)findViewById(R.id.btn_send);

		edit.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String text = edit.getText().toString();
				if ((text = text.trim()).length() > 0) {
					int time = Common.getUnixTime(); 
					if (time > typing_time) {					
						typing_time = time + 6;
						if (dialog != null)
							Main.mtp.api_messages_setTyping(dialog.getInputPeer(), true);
					}
					btn_send.setEnabled(true);
				} else
					btn_send.setEnabled(false);		
				
				Emoji.replace(s);
				if (dialog != null)
					dialog.msgText = s;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}			
		});

		edit.setText("");		
		new Emoji.Keyboard(Main.main, Main.main.mainView, edit, (CheckBox)findViewById(R.id.btn_smile));		

		list = (ListView)findViewById(R.id.dialog_msgs);		
		list.setAdapter(adapter = new Adapter.MessagesAdapter(getContext()));		

		list.setEmptyView(findViewById(R.id.empty));		
		
		btn_send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				sendMessage(edit.getText().toString());
				dialog.msgText = "";
				edit.setText(dialog.msgText);				
				typing_time = 0;
			//	MainActivity.mtp.api_messages_setTyping(dialog.getInputPeer(), false);
			}});	
		
		
		list.setOnScrollListener(new OnScrollListener() {
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					if (firstVisibleItem < 20 && dialog != null)		
				    	dialog.queryHistory(20);				
				/*
					int loadedItems = firstVisibleItem + visibleItemCount;				
					if((loadedItems == totalItemCount) && !isloading){
						if(task != null && (task.getStatus() == AsyncTask.Status.FINISHED)){
							task = new MyTask();
							task.execute();
						}
				*/
					}
	
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
				}
			});		
    }
    
    public static void updateWallpaper() {    	
    	int wp_index = Main.settings.getInt("wp_index");
    	if (wp_index == -1) {    		
    		wallpaper = Main.settings.getString("customPath");
    		if (wallpaper.equals("") || !Common.fileExists(wallpaper))
    			wallpaper = null;
    	} else
    		wallpaper = PageWallpaper.getPath(wp_index);    		
    	
    	if (Main.main.pageDialog != null) {
    		if (wallpaper == null) {
    			Main.main.pageDialog.wpImage.setImageDrawable(null);
    			Main.main.pageDialog.wpImage.setBackgroundColor(Main.settings.getInt("customColor"));
    		} else {
    			if (Common.fileExists(wallpaper))
    				Common.loadBitmapThread(Main.main, Main.main.pageDialog.wpImage, wallpaper);
    			else
    				PageWallpaper.loadWallpaper(Main.main.pageDialog.wpImage, wp_index);    			
    		}
    	}    	
    }
    
    public void setDialog(Dialog dialog) {
    	this.dialog = dialog;
    	if (dialog == null)
    		return;
    	
    	clearSelection();
    	
		adapter.setDialog(dialog);		
		scrollDown();		
	// restore draft text
		CharSequence str = Main.main.intentText == null ? dialog.msgText : Main.main.intentText;
		edit.setText(str);
		Main.main.intentText = null;
		edit.setSelection(str.length());
		((TextView)list.getEmptyView()).setText(Html.fromHtml("<i>" + (dialog.user_id == 333000 ? getResources().getString(R.string.empty_question) : getResources().getString(R.string.empty_message)) + "</i>") );
		
		if (Main.main.intentMedia != null) {
	        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());		
	        
        	adb.setTitle(Main.getResStr(R.string.dialog_title_media));		        
        	adb.setMessage(Main.getResStr(R.string.dialog_send_media));
	        	
	        adb.setNegativeButton(Main.getResStr(R.string.dialog_btn_cancel), null);
        	adb.setNeutralButton(Main.getResStr(R.string.dialog_btn_discard),new AlertDialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Main.main.intentMedia = null;
				}});	
	        adb.setPositiveButton(Main.getResStr(R.string.dialog_btn_send), new AlertDialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendImage( Common.getPathFromURI(Main.main, Main.main.intentMedia) );
					Main.main.intentMedia = null;
				}});	
	        AlertDialog alert = adb.create();
	        alert.setCanceledOnTouchOutside(true);
	        alert.show();		
		}
    }
    
	public void scrollDown() {
		list.postDelayed(new Runnable() {
			@Override
			public void run() {
				list.setSelection(adapter.getCount() - 1);
			}			
		}, 500);
				
	}    
    
	public void update() {
		if (list.getChildCount() > 0) {
			int offset = list.getChildAt(0).getTop();		
			Dialog.Message msg = adapter.getItem(list.getFirstVisiblePosition());
			//boolean noscroll = adapter.getItem(adapter.getCount() - 1) == msg;			
			adapter.update();		
			//if (!noscroll)
			list.setSelectionFromTop(dialog.getMessageIndex(msg), offset);	
		} else
			adapter.update();
		
		updateSelectHeader();
	}	
	
	public void messageAdd(Dialog.Message msg) {
		adapter.add(msg);
	}
	
	public void messageDelete(Dialog.Message msg) {
		adapter.remove(msg);
	}
	
	public void resetMessage(Dialog.Message msg) {
		int pos = adapter.getPosition(msg);
		if (pos != -1) {
			adapter.remove(msg);
			adapter.insert(msg, pos);
			updateMessage(msg);
		}		
	}
	
	
	public void updateMessage(Dialog.Message msg) {		
		int pos = adapter.getPosition(msg);
		MessageView mv = (MessageView)Common.getListItem(list, pos);
		if (mv != null) 
			mv.update();
	}
	
    public void focus() {
		edit.requestFocus();    	
    }

	public void sendMessage(String text) {
		if (text == null || (text = text.trim()).length() == 0) 
			return;		
		dialog.sendMessage(text, null);	
		//scrollDown();
		//Main.main.updateDialog(dialog);
	}
	
	public void updatePicture() {
		dialog.getPicture(Main.main.titleBar.btn_profile);
	}
	
	@Override
	public void onMenuInit() {
		if (updateSelectHeader())
			return;
		Main.showMenu(R.id.dialog);   
    	updateTitle();    	    	
	}	
	
	public void updateTitle() {
		updatePicture();
    	Main.main.setActionTitle(dialog.getTitle(false), dialog.getStatus());		
	}
	
	@Override
	public void onMenuClick(final View view, final int id) {
		switch (id) {
	    	case R.id.btn_attach :
	    		// clearSelection(); ???
	    		Main.main.showPopup(view, R.menu.attach, true);
	    		break;
	    	case R.id.btn_attach_photo   :
	    	case R.id.btn_attach_gallery :	    		
			case R.id.btn_attach_video_record :
			case R.id.btn_attach_video_choose :
				Common.MediaReq req = new Common.MediaReq() {
					@Override
					public void onMedia(Uri media) {
						final String fileName = Common.getPathFromURI(getContext(), media);					
				        new Thread( new Runnable() {
				        	@Override
				        	public void run() {        		        		
				        		try {
				        			switch (id) {
					        			case R.id.btn_attach_photo :
					        			case R.id.btn_attach_gallery :
					        				sendImage(fileName);
					        				break;
					        			case R.id.btn_attach_video_record :
					        			case R.id.btn_attach_video_choose :
					        				uploadMediaVideo(fileName, dialog);
					        				break;
				        			}
				        		} catch (Exception e) {}        		
				        	}
				        } ).start();
					}
				};	  
				
				switch (id) {
					case R.id.btn_attach_photo :
					case R.id.btn_attach_gallery :
						Main.main.mediaPhoto(id == R.id.btn_attach_photo, false, req);
						break;
					case R.id.btn_attach_video_record :
					case R.id.btn_attach_video_choose :
						Main.main.mediaVideo(id == R.id.btn_attach_video_record, req);
						break;
				}
	    		break;
			case R.id.btn_attach_location :	
				Main.main.goLocation(null, null);
				break;
			case R.id.btn_select_done :
				clearSelection();
				break;
			case R.id.btn_forward :
				ArrayList<Integer> msg_id = new ArrayList<Integer>();
				for (int i = 0; i < adapter.selected.size(); i++)
					msg_id.add(adapter.selected.get(adapter.selected.keyAt(i)).id);				
				Main.main.goForward(msg_id);
				break;
			case R.id.btn_delete :
				ArrayList<Dialog.Message> msgs = new ArrayList<Dialog.Message>();
				for (int i = 0; i < adapter.selected.size(); i++)
					msgs.add(adapter.selected.get(adapter.selected.keyAt(i)));				
				dialog.deleteMessages(msgs.toArray());
				clearSelection();
				break;
			case R.id.btn_profile :
				if (dialog.user_id != -1)
					Main.main.goUserInfo(User.getUser(dialog.user_id));
				else
					Main.main.goChatInfo(Chat.getChat(dialog.chat_id));
				break;
			case R.id.btn_contact_add :
				User user = ((User)view.getTag());				
				TL.Vector contacts = TL.newVector(TL.newObject("inputPhoneContact", 0L, user.phone, user.first_name, user.last_name));
				Main.mtp.api(new TL.OnResultRPC() {					
					@Override
					public void onResultRPC(TL.Object result, Object param, boolean error) {
						if (error) return;			
						view.post(new Runnable() {
							@Override
							public void run() {
								view.setVisibility(View.GONE);								
							}
						});
					}
				}, null, "contacts.importContacts", contacts, false);				
				break;
		}
	}
	
	public void sendImage(String fileName) {
		try {
			final Bitmap bitmap = Common.loadBitmap(Main.main, fileName, 800, 800, false, true, false);						        			
			String jpegPath = Main.CACHE_PATH + Common.random.nextLong() +  ".jpg";
			Common.saveJPEG(jpegPath, bitmap, 87);
			uploadMediaPhoto(jpegPath, dialog);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void uploadMediaPhoto(final String fileName, final Dialog dialog) {
		Bitmap thumb = null;
		try {
			thumb = Common.loadBitmap(getContext(), fileName, Main.SIZE_THUMB_MEDIA, Main.SIZE_THUMB_MEDIA, false, false, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		final Dialog.Message msg = new Dialog.Message(dialog, thumb);		
		
		msg.query = new FileQuery(fileName, new FileQuery.OnResultListener() {
			@Override
			public void onResult(TL.Object result) {
				msg.query = null;
				Common.fileDelete(fileName);
				//dialog.msgList.remove(msg);
				dialog.deleteMessages(msg);
				if (result == null) {
					//Main.main.updateDialog(dialog);
					return;
				}
				TL.Object inputMedia = TL.newObject("inputMediaUploadedPhoto", result);
				dialog.sendMessage("", inputMedia);				
			}

			@Override
			public void onProgress(float progress) {
				msg.progress = progress;
				msg.date = Common.getUnixTime();
				MessageView v = (MessageView)Common.getListItem(list, adapter.getPosition(msg));
				if (v != null)
					v.updateProgress();
			}
		});
		
		dialog.addMessage(msg);
	}
	
	public void uploadMediaVideo(final String fileName, final Dialog dialog) {	
		int d = 0, w = 0, h = 0;
		try {
			MediaPlayer mp = MediaPlayer.create(getContext(), Uri.parse(fileName));
			mp.getVideoHeight();
			d = mp.getDuration() / 1000;			
			w = mp.getVideoWidth();	
			h = mp.getVideoHeight();
			
			mp.release();
		} catch (Exception e) {
			e.printStackTrace();
		} 		
		final int duration = d, width = w, height = h;
		Common.logError("duration : " + duration);
		Common.logError("width    : " + width);
		Common.logError("height   : " + height);

		
		Bitmap thumb = ThumbnailUtils.createVideoThumbnail(fileName, MediaStore.Video.Thumbnails.MINI_KIND);
		if (thumb == null)
			thumb = ThumbnailUtils.createVideoThumbnail(fileName, MediaStore.Video.Thumbnails.MICRO_KIND);
		
		Bitmap preview = thumb == null ? null : (thumb.getWidth() > thumb.getHeight() ? Common.imageScale(thumb, 480, 320) : Common.imageScale(thumb, 320, 480));				
		final String jpegPath = preview == null ? null : (Main.CACHE_PATH + Common.random.nextLong() +  ".jpg");			
		if (jpegPath != null)
			Common.saveJPEG(jpegPath, preview, 87);
		if (thumb != null)
			thumb = Common.imageScale(thumb, Main.SIZE_THUMB_MEDIA, Main.SIZE_THUMB_MEDIA);
		if (preview == null) Common.logError("no preview");		
		
		
		final Dialog.Message msg = new Dialog.Message(dialog, thumb);

	// preview upload
		if (preview != null) {
			new FileQuery(jpegPath, new FileQuery.OnResultListener() {
				@Override
				public void onResult(TL.Object result) {
					Common.fileDelete(jpegPath);
					msg.thumbInputFile = result;
					
					// video upload
					msg.query = new FileQuery(fileName, new FileQuery.OnResultListener() {	// TODO: video encoding while uploading
						@Override
						public void onResult(TL.Object result) {	
							msg.query = null;
							msg.thumb = null;
							dialog.deleteMessages(msg);
							//dialog.msgList.remove(msg);				
							if (result == null) {
							//	Main.main.updateDialog(dialog);
								msg.thumbInputFile = null;
								return;
							}
											
							TL.Object inputMedia = msg.thumbInputFile == null ? 
														TL.newObject("inputMediaUploadedVideo", result, duration, width, height) :
														TL.newObject("inputMediaUploadedThumbVideo", result, msg.thumbInputFile, duration, width, height);
							msg.thumbInputFile = null;
							dialog.sendMessage("", inputMedia);
						}

						@Override
						public void onProgress(float progress) {
							msg.progress = progress;
							msg.date = Common.getUnixTime();
							MessageView v = (MessageView)Common.getListItem(list, adapter.getPosition(msg));
							if (v != null)
								v.updateProgress();
						}
					});		
					
					dialog.addMessage(msg);
				}
	
				@Override
				public void onProgress(float progress) {}
			});		
		}
		
	}	
	
	public void uploadMediaGeo(LatLng point) {		
		TL.Object inputMedia = TL.newObject("inputMediaGeoPoint", TL.newObject("inputGeoPoint", point.latitude, point.longitude));
		if (dialog != null)
			dialog.sendMessage("", inputMedia);	
	}	
	
	public static void mapCapture(final Common.MediaReq req, final Double g_lat, final Double g_long) {
		final int i_lat = (int)(g_lat * 100000);
		final int i_long = (int)(g_long * 100000);
		
		final String fileName = Main.CACHE_PATH + i_lat + "_" + i_long;

		File file = new File(fileName);
		if (file.exists()) {
			req.onMedia( Uri.fromFile(file) );
			return;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				int size = Main.SIZE_THUMB_MEDIA;
				String url = "http://maps.google.com/maps/api/staticmap?center=" + g_lat.toString() + "," + g_long.toString() + "&zoom=14&size=" + size + "x" + size + "&sensor=false&maptype=roadmap";
				
				Bitmap bmp = Common.downloadBitmap(url);
				if (bmp == null) {
					req.onMedia(null);
					return;
				}
			
				Bitmap pin = BitmapFactory.decodeResource(Main.main.getResources(), R.drawable.map_pin);
				bmp = bmp.copy(Bitmap.Config.RGB_565, true);
				Canvas canvas = new Canvas(bmp);
				Matrix m = new Matrix();
				
				float s = 64.0f / pin.getHeight();				
				float pw = s * pin.getWidth();
				float ph = s * pin.getHeight();
				
				m.preTranslate( (bmp.getWidth() - pw) / 2, bmp.getHeight() / 2 - ph);
				m.preScale(s, s);				
				canvas.drawBitmap(pin, m, null);
				
				if (Common.savePNG(fileName, bmp, 90)) 
					req.onMedia(Uri.fromFile(new File(fileName)));
				else
					req.onMedia(null);
			}}).start();
	}
	
	public boolean updateSelectHeader() {
		int count = adapter.selected.size();
		Main.main.titleBar.setSelection(count);
		return count > 0;
	}
	
	public void clearSelection() {
		if (adapter.selected.size() > 0) {			
    		adapter.selected.clear();
    		adapter.notifyDataSetChanged();
    		updateSelectHeader();
    	}		
	}
	
	public void select(Dialog.Message msg, View v) {		
		((MessageView)v).setSelectedMode(adapter.select(msg));		
		updateSelectHeader();
	}	
	
	public void msgClick(final Dialog.Message msg, View v) {
		if (msg.id == 0) return;
		
		if (adapter.selected.size() > 0) {
			select(msg, v);
			return;
		}
		
		AlertDialog.Builder adb = new AlertDialog.Builder(Main.main);
		Resources res = this.getResources();
		adb.setTitle(res.getString(R.string.msg_opt_title));

		adb.setItems(msg.hasMedia() ? R.array.msg_opt_media : R.array.msg_opt_text, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInt, int item) {
				if (item == 0) {	// forward msg
					ArrayList<Integer> msg_id = new ArrayList<Integer>();
					msg_id.add(msg.id);
					Main.main.goForward(msg_id);
				}
				
            	if (item == 1) {
            		if (msg.hasMedia()) { // 1 == delete
            			dialog.deleteMessages(msg);
            		} else {	// 1 == copy (fot text messages)
            			Common.clipboardCopy(msg.message);
            		}
            	}
            	
            	if (item == 2)
            		dialog.deleteMessages(msg);
            }
        });
        
		AlertDialog alert = adb.create();
		alert.setCanceledOnTouchOutside(true); 
        alert.show();
	}
	
	public void msgLongClick(Dialog.Message msg, View v) {
		if (msg.id == 0) return;		
		select(msg, v);
	}
	
	public void msgView(Dialog.Message msg) {
		if (msg == null || !msg.hasMedia())
			return;

		if (msg.media.id == 0x56e0d474)		// messageMediaGeo
			Main.main.goLocation(msg.getGeoPoint(), User.getUser(msg.from_id));
		
		if (msg.media.id == 0xc8c45a2a)		// messageMediaPhoto
			Main.main.goPhoto(msg);
		
		if (msg.media.id == 0xa2d24290)
			mediaPlay(msg);
	}
	
	public static void mediaDownload(final Dialog.Message msg) {
		if (!msg.hasVideoMedia()) 
			return;
		TL.Object video = msg.media.getObject("video");
		
		try {
			msg.progress = 0;
			msg.query = new FileQuery(video.getInt("dc_id"), video.getLong("id"), video.getLong("access_hash"), (long)video.getInt("size"), new FileQuery.OnResultListener() {
				@Override
				public void onResult(TL.Object result) {
					msg.query = null;
					if (result != null) {
						msg.videoPath = result.getString("fileName");
						Common.logError("result");
					}
					Common.logError("msg videoPath: " + msg.videoPath);
					Main.main.resetMessage(msg);			
				}
		
				@Override
				public void onProgress(float progress) {
					msg.progress = progress;
					if (Main.main.pageDialog != null && Main.main.getCurPage() == Main.main.pageDialog) {
						MessageView v = (MessageView)Common.getListItem(Main.main.pageDialog.list,  Main.main.pageDialog.adapter.getPosition(msg));
						if (v != null)
							v.updateProgress();
					}
					
					if (Main.main.pagePhoto != null && Main.main.getCurPage() == Main.main.pagePhoto)
						Main.main.pagePhoto.updateProgress(msg);											
				}
			});	
			Main.main.resetMessage(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	public static void mediaPlay(Dialog.Message msg) {
		if (msg.videoPath != null) {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW);		
				intent.setDataAndType(Uri.parse(msg.videoPath), "video/*");
				Main.main.startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
			mediaDownload(msg);		
	}
}
		

