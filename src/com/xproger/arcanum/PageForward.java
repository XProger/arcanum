package com.xproger.arcanum;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class PageForward extends ListView implements Common.Page {
	private Adapter.ContactAdapter adapter;
	private ArrayList<Object> list = new ArrayList<Object>();
	private ArrayList<Integer> msg_id;
	private boolean shareContact;
	private User user;
	private OnItemClickListener clickForward, clickShare;

    public PageForward(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {    	
		setAdapter(adapter = new Adapter.ContactAdapter(getContext(), R.layout.item_dialog));		
		adapter.inviteItem = false;
		clickForward = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position, long id) {								
				final Adapter.Contact c = adapter.getItem(position);
				Dialog dialog = c.dialog != null ? c.dialog : Dialog.getDialog(c.user.id, -1, true);				
				Main.mtp.api(null, null, "messages.forwardMessages", dialog.getInputPeer(), TL.newVector(msg_id.toArray()));
				Main.main.goBack();
			}
		};

		clickShare = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position, long id) {								
				final Adapter.Contact c = adapter.getItem(position);
				Dialog dialog = c.dialog != null ? c.dialog : Dialog.getDialog(c.user.id, -1, true);
				dialog.sendMessage("", TL.newObject("inputMediaContact", user.phone, user.first_name, user.last_name));				
				Main.main.goBack();
			}
		};

    }
    
    private void updateList() {
    	list.clear();
    // add dialogs
    	int count = 0;
    	for (int i = 0; i < Dialog.dialogs.size(); i++)
    		list.add(Dialog.dialogs.get(i));
    	count = list.size();
    // add users
    	for (int i = 0; i < User.users.size(); i++) {
    		User user = User.users.get(User.users.keyAt(i));
    		if (user.contact && !user.blocked && user != this.user) {
    			boolean flag = true;
    			for (int j = 0; j < count; j++) {
    				Dialog dialog = (Dialog)list.get(i);
    				if (dialog.userIncluded(user)) {
    					flag = false;
    					break;
    				}
    			}    			
    			if (flag) list.add(user);
    		}
    	}
    	
    	adapter.clear();
    	for (int i = 0; i < list.size(); i++)
    		adapter.add(new Adapter.Contact(list.get(i))); 
    }
    
    public void update(ArrayList<Integer> msg_id) {
    	shareContact = false;
    	user = null;
    	this.msg_id = msg_id;
    	setOnItemClickListener(clickForward);
    	updateList();
    }
    
    public void update(User user) {
    	shareContact = true;
    	this.user = user;
    	setOnItemClickListener(clickShare);
    	updateList();
    }
    
	@Override
	public void onMenuInit() {
		Main.main.setActionTitle(getResources().getString(shareContact ? R.string.page_title_share : R.string.page_title_forward), null);
	}

	@Override
	public void onMenuClick(View view, int id) {
	}
	
}
		

