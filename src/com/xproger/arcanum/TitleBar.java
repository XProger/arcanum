package com.xproger.arcanum;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;

public class TitleBar extends LinearLayout {
	public TextView title, subTitle;
	public ImageView icon, btn_profile, notify_picture;	
	public Button btn_done;
	public View title_back, title_selection, title_notify;
	public View btn_done_select, btn_delete, btn_forward;
	public TextView text_selected, notify_title, notify_text;
	public boolean selectMode = true;
	public int notifyTime = 0;
	public Dialog notifyDialog;

    public TitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }        
    
    @Override
    public void onFinishInflate() { 
    	title_back	= findViewById(R.id.title_menu);
    	title_notify= findViewById(R.id.title_notify);
        icon		= (ImageView)title_back.findViewById(R.id.img_icon);
        title		= (TextView)title_back.findViewById(R.id.text_title);
        subTitle	= (TextView)title_back.findViewById(R.id.text_sub);        
        btn_profile	= (ImageView)findViewById(R.id.btn_profile);
        btn_done	= (Button)findViewById(R.id.btn_done);
        notify_picture	= (ImageView)findViewById(R.id.notify_picture);
        notify_title	= (TextView)findViewById(R.id.notify_title);
        notify_text		= (TextView)findViewById(R.id.notify_text);
        
        title_back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Main.main.goBack();				
			}
        });
        
    	title_selection = findViewById(R.id.title_selection);
    	    	
    	text_selected	= (TextView)title_selection.findViewById(R.id.text_selected);
    	btn_done_select = title_selection.findViewById(R.id.btn_select_done);
    	btn_forward		= title_selection.findViewById(R.id.btn_forward);
    	btn_delete		= title_selection.findViewById(R.id.btn_delete);
    	
    	OnClickListener onMenuClick = new OnClickListener() {
				@Override
				public void onClick(View view) {
					Main.main.onMenuClick(view);
				}
	        }; 
    	
    	btn_done_select.setOnClickListener(onMenuClick);
    	btn_delete.setOnClickListener(onMenuClick);
    	btn_forward.setOnClickListener(onMenuClick);
    	
		new Updater(1000, new Runnable() {
			@Override
			public void run() {
				if (notifyTime > 0) {
					notifyTime--;
					if (notifyTime == 0) 
						hideNotify();
				}
			}			
		}).startUpdates();
    }
    
	public void setTitle(CharSequence title, String subTitle) {		
		this.title.setText(title);
		if (subTitle != null) {
			this.subTitle.setText(Html.fromHtml(subTitle));
			this.subTitle.setVisibility(View.VISIBLE);
		} else
			this.subTitle.setVisibility(View.GONE);

		if (Main.main.pager == null || Main.main.pager.getCurrentItem() == 0) {
			icon.setImageResource(R.drawable.ic_ab_logo);
			title_back.setEnabled(false);
		} else {
			icon.setImageResource(R.drawable.ic_ab_back);
			title_back.setEnabled(true);			
		}		
	}
    
	public void setSelection(int count) {
		boolean visible = count > 0;
		if (selectMode != visible) {
			title_selection.setVisibility(visible ? View.VISIBLE : View.GONE);
			title_back.setVisibility(visible ? View.GONE : View.VISIBLE);
			selectMode = visible;
			Main.main.updateMenu();
		}
		if (visible)
			text_selected.setText(String.format(getResources().getString(R.string.header_selected), count));		
	}
	
	public void hideMenu() {
		for (int i = 0; i < getChildCount(); i++)
			getChildAt(i).setVisibility(View.GONE);
		
		if (notifyTime > 0) {
			title_notify.setVisibility(View.VISIBLE);
			return;
		}
		
		if (selectMode) {
			title_selection.setVisibility(View.VISIBLE);
			return;
		} else
			title_back.setVisibility(View.VISIBLE);		
	}
	
	public void showMenu(int id) {
		hideMenu();			
		if (id == 0)
			return;		
		View v = findViewById(id);
		if (v != null)
			v.setVisibility(View.VISIBLE);
	}
	
	public void showNotify(Dialog.Message msg) {
		notifyTime = 5;
		notifyDialog = msg.dialog;
		notify_title.setText(msg.getTitle());
		notify_text.setText(msg.getStatusText());
		notifyDialog.getPicture(notify_picture);
		Main.main.updateMenu();
	}
	
	public void hideNotify() {
		notifyTime = 0;
		Main.main.updateMenu();
	}
}
		

