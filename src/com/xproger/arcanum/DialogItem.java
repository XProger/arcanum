package com.xproger.arcanum;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DialogItem extends RelativeLayout {
	public ImageView picture;
	public TextView info, time, title, badge;
	public Adapter.Contact contact = null;
	private Dialog dialog = null;
	private CheckBox check;
	
    public DialogItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
	
    @Override
    public void onFinishInflate() {
    	picture	= (ImageView)findViewById(R.id.picture);
    	info	= (TextView)findViewById(R.id.info);
    	time	= (TextView)findViewById(R.id.time);
    	title	= (TextView)findViewById(R.id.title);
    	badge	= (TextView)findViewById(R.id.badge);
    	check	= (CheckBox)findViewById(R.id.check);
    }
    
	public void setContact(Adapter.Contact contact) {
		this.dialog = null;
		this.contact = contact;
		if (badge != null) badge.setVisibility(View.GONE);
		update();
	}

	public void setDialog(Dialog dialog) {
		this.dialog = dialog;
		this.contact = null;
		update();
	}
	
	public void setChecked(boolean checked) {
		if (check != null)
			check.setChecked(checked);
	}

	public void update() {
		if (contact != null) {
			if (contact.user != null) {
				User user = contact.user;
				user.getPhoto(picture);
				info.setText( user.blocked ? Common.formatPhone(user.phone) : Html.fromHtml(user.getStatusHtml()));
				title.setText(user.getTitleColored());
			}
			
			if (contact.dialog != null) {
				Dialog dialog = contact.dialog;
				dialog.getPicture(picture);
				info.setText("");
				title.setText(dialog.getTitle(true));
			}
			
			if (time != null) time.setText("");			
			setChecked(contact.checked);
		};
		
		if (dialog != null) {
			dialog.getPicture(picture);
			info.setText(dialog.getInfo());
			if (time != null)
				time.setText(dialog.getTime());
			title.setText(dialog.getTitle(true));
			if (dialog.unread_count > 0) {
				badge.setVisibility(View.VISIBLE);			
				badge.setText(dialog.getUnreadCount());
			} else
				badge.setVisibility(View.GONE);
		}
	}

}
