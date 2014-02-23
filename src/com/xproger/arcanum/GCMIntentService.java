package com.xproger.arcanum;

import java.io.FileInputStream;
import java.nio.ByteBuffer;

import org.json.JSONObject;

import com.google.android.gcm.GCMBaseIntentService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

public class GCMIntentService extends GCMBaseIntentService {
	
	public static final String GCM_SENDER_ID = "970116570455";
	
	private static final String EXTRA_MESSAGE = "message";
	private static final String EXTRA_CUSTOM = "custom";
	private static final String EXTRA_BADGE = "badge";
	private static final String EXTRA_SOUND = "sound";

	
	private static long lastTime;
	
	public GCMIntentService() {
		super(GCM_SENDER_ID);
		lastTime = 0;
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
				
		String sound = ""; 
		int badge = 0;
		try {
			badge = Integer.valueOf(extras.getString(EXTRA_BADGE));
			sound = String.valueOf(extras.getString(EXTRA_SOUND));			 
		} catch (Exception e) {};
		
		JSONObject custom = null;
		try {
			custom = new JSONObject(extras.getString(EXTRA_CUSTOM));
		} catch (Exception ex) {}
				
		String icon = null;		
		String icon_cfg = null; 
		
		int user_id = -1, chat_id = -1, id = 0;
		
		try {
			if (custom != null)
				if (custom.has("chat_id")) {
					chat_id = custom.getInt("chat_id");
					icon_cfg = "icon_chat";			
				} else
					if (custom.has("from_id")) {		
						user_id = custom.getInt("from_id"); 					
						icon_cfg = "icon_user";			
					}
		} catch (Exception e) {
			icon_cfg = null;
			e.printStackTrace();
		}
		
		if (user_id != -1) id = user_id;
		if (chat_id != -1) id = chat_id;		
		
		String FILES_PATH = getFilesDir().getAbsolutePath() + "/";  
		
		if (icon_cfg != null) { 
			icon_cfg = FILES_PATH + icon_cfg;
			try {
				FileInputStream is = new FileInputStream(icon_cfg);
				byte[] data = new byte[is.available()];
				is.read(data);
				is.close();
				
				ByteBuffer buf = ByteBuffer.wrap(data);
				int count = buf.getInt();
				for (int i = 0; i < count; i++) {					
					if (id == buf.getInt()) {
						icon = FILES_PATH + ("img/" + (buf.getInt() + "_") + (buf.getLong() + "_") + buf.getInt());
						break;
					} else
						buf.position(buf.position() + 4 + 8 + 4); 
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String message = extras.getString(EXTRA_MESSAGE);
		Common.logError(extras.toString());
		if (message != null)
			notify(context, message, badge, Common.getSoundURI(context, sound), icon, user_id, chat_id);		
	}
	
	public static void clear(Context context) {
		NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();		
	}	
	
	@Override
	protected void onError(Context context, String registrationId) {
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Main.main.registerDevice(registrationId);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
	}
	
	private static void notify(Context context, String message, int badge, Uri soundUri, String icon, int user_id, int chat_id) {
		Bitmap bmp;
		try {
			bmp = icon == null ? null : BitmapFactory.decodeFile(icon);
		} catch (Exception e) {
			bmp = null;
		}
		
		//if (bmp == null)
			//bmp = BitmapFactory.decodeResource(context.getRe, R.drawable.ic_launcher);
		
		int defaults = 0;
		if (System.currentTimeMillis() - lastTime > 5000) {
			//if (SettingsUtil.isNotificationsSoundEnabled(VKApplication.getInstance()))
				defaults |= Notification.DEFAULT_SOUND;
			//if (SettingsUtil.isNotificationsVibrateEnabled(VKApplication.getInstance()))
				defaults |= Notification.DEFAULT_VIBRATE;
			lastTime = System.currentTimeMillis();
		}

		Intent intent = new Intent(context, Main.class);
		if (user_id != -1) intent.putExtra(Main.EXTRA_USERID, String.valueOf(user_id));
		if (chat_id != -1) intent.putExtra(Main.EXTRA_CHATID, String.valueOf(chat_id));
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);			
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);		
		
		NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context)
			.setAutoCancel(true)	
			.setSmallIcon(R.drawable.ic_notify)			
			.setLargeIcon(bmp)
			.setContentTitle(context.getText(R.string.app_name))			
			.setContentText(message)		
			.setContentIntent(contentIntent)
			.setSound(soundUri)
			.setTicker(message)
			.setNumber(badge)
			.setDefaults(defaults);
				
		Notification notify = nBuilder.build(); 
//		notify.largeIcon = bmp;
//		notify.icon = R.drawable.ic_notify;
		
		NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(777, notify);
	}
}
