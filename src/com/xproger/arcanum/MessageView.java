package com.xproger.arcanum;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View;
import android.view.View.*;

public class MessageView extends RelativeLayout implements OnClickListener, OnLongClickListener {
	private static int msgRes[] = {
		R.layout.msg_out_media,
		R.layout.msg_in_media,
		R.layout.msg_out,	
		R.layout.msg_in,
		R.layout.msg_group_in_media,
		R.layout.msg_group_in,
		R.layout.msg_info,
		R.layout.msg_info_photo,		
		R.layout.msg_out_load,
		R.layout.msg_out_contact,
		R.layout.msg_in_contact,
		R.layout.msg_group_in_contact,
		R.layout.msg_in_load,
		R.layout.msg_group_in_load
	};
	
	private static CharSequence time_span[] = new CharSequence[2];	
	public static PageDialog pageDialog;	
	private boolean selected = false;
	
	public static void init() {		
		SpannableStringBuilder sb = new SpannableStringBuilder();
		sb.append(" abcd");
		sb.setSpan(new ForegroundColorSpan(0), 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		time_span[0] = sb;
		
		sb = new SpannableStringBuilder();
		sb.append(" abcdef");
		sb.setSpan(new ForegroundColorSpan(0), 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		time_span[1] = sb;
	}	
	
	public TextView	msg_text, msg_status, msg_video, btn_view;
	public ImageView msg_media, msg_user, btn_contact_add;
	public ProgressBar progress;
	public View lay_load;
	public Dialog.Message msg;
	
	public static int getTypeCount() {
		return msgRes.length;
	}
	
	public static int getType(Dialog.Message msg) {
		if (msg.query != null) {
			return msg.out ? 8 : (msg.dialog.chat_id == -1 ? 12 : 13);
		}
		
		if (msg.action != null)
			return msg.media != null ? 7 : 6; // messageActionChatEditPhoto : others

		if (msg.dialog.chat_id == -1) {
			if (msg.hasMedia()) {
	    		if (msg.thumb == null && msg.media.id == 0x5e7d2f39)
	    			return msg.out ? 9 : 10;
	    		return msg.out ? 0 : 1;
			}
    		return msg.out ? 2 : 3;
		}; 
		
    	if (msg.hasMedia()) {
    		if (msg.media.id == 0x5e7d2f39)
    			return msg.out ? 9 : 11;
    		return msg.out ? 0 : 4;
    	}
		return msg.out ? 2 : 5;	    	
	}
	
	public static int getRes(int type) {
		return msgRes[type];
	}

	public MessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {
    	msg_text	= (TextView)findViewById(R.id.msg_text);
    	msg_status	= (TextView)findViewById(R.id.msg_status);
    	msg_media	= (ImageView)findViewById(R.id.msg_media);
    	msg_video	= (TextView)findViewById(R.id.msg_video);
    	msg_user	= (ImageView)findViewById(R.id.msg_user);
    	btn_view	= (TextView)findViewById(R.id.btn_view);
    	
    	btn_contact_add = (ImageView)findViewById(R.id.btn_contact_add);
    	
    	lay_load = findViewById(R.id.lay_load);
    	if (lay_load != null) {
    		progress = (ProgressBar)lay_load.findViewById(R.id.progress);
    		lay_load.setOnClickListener(this);
    	}
    	
    	if (btn_view != null)
    		btn_view.setOnClickListener(this);
    }
    	
    public void setMessage(Dialog.Message msg) {
	    this.msg = msg;  
	    update();
    }
    
    public void update() {
    	if (msg == null) return;
    	
    	if (msg_media != null)
    		msg.getMedia(msg_media, "m");    	
    	
   		if (msg_text != null) {   			
   			CharSequence text = msg.getTextSpanned();
   			if (msg.action == null && msg.media == null) 
   				text = TextUtils.concat(text, msg.out ? time_span[1] : time_span[0]);
   			msg_text.setText(text);
   		}   			
   			
    	if (msg_status != null) {
    		msg_status.setText(msg.getStatusTime());
    		msg_status.setBackgroundColor(msg.getStatusColorBack());
    		msg_status.setTextColor(msg.getStatusColorText());
    	}

    	if (msg_user != null) {
    		msg.getUserPhoto(msg_user);
    		msg_user.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {    				
    				Main.main.goUserInfo(User.getUser(msg.from_id));
    			}		
    		});      		
    	}
    	
    	if (btn_view != null && msg.media != null) 
    		btn_view.setText(msg.getMediaViewText());

    	if (msg.action == null) {
	    	setOnClickListener(this);
	    	setOnLongClickListener(this);
    	} else {
	    	setOnClickListener(null);
	    	setOnLongClickListener(null);    		
    	}
    	
    	if (msg.media != null && msg.media.id == 0x5e7d2f39) {
    		final User user = User.getUser(msg.media.getInt("user_id"));
    		if (btn_contact_add != null) {
    			btn_contact_add.setTag(user);
    			btn_contact_add.setVisibility(user.contact ? View.GONE : View.VISIBLE);
    		}
    		
    		msg_media.setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {    				
    				Main.main.goUserInfo(user);
    			}		
    		});    		
    	}
    	
    	if (msg_video != null) 
    		if (msg.hasVideoMedia()) {
    			msg_video.setVisibility(View.VISIBLE);
    			msg_video.setText(msg.getVideoText());
    		} else
    			msg_video.setVisibility(View.GONE);
    	
    	updateProgress();    	
    }

	@Override
	public void onClick(View v) {
		if (v == lay_load) {
			if (msg.query != null)		
				msg.query.cancel();
			return;
		}
		
		if (v == btn_view) {
			pageDialog.msgView(msg);
			return;
		}
		
		pageDialog.msgClick(msg, v);		
	}
    
	@Override
	public boolean onLongClick(View v) {		
		pageDialog.msgLongClick(msg, v);
		return true;
	}
	
	public void setSelectedMode(boolean value) {
		selected = value;
		super.setSelected(value);
	}
	
	@Override
	public void setSelected(boolean value) {
		if (!selected)
			super.setSelected(value);
	//	setBackgroundResource(value ? R.color.color_sel_trans : R.drawable.msg_selector);
	}
	
	public void updateProgress() {		
		if (progress != null) 
			progress.post(new Runnable() {
				@Override
				public void run() {
					progress.setProgress((int)(msg.progress * 100));
				}
			});
	}
}
