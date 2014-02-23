package com.xproger.arcanum;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class PageContactList extends LinearLayout implements Common.Page {
	public static String inviteText = null; 
	public Adapter.ContactAdapter adapter;
	public ListView list;
	private Chat chat;
	
    public PageContactList(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (inviteText == null) {
        	Main.mtp.api(new TL.OnResultRPC() {				
				@Override
				public void onResultRPC(TL.Object result, Object param, boolean error) {
					if (error) return;
					inviteText = result.getString("message");
					Common.logError("inviteText: " + inviteText);					
				}
			}, null, "help.getInviteText", "en");        	
        }
    }    
    
    @Override
    public void onFinishInflate() {    	
    	list = (ListView)findViewById(R.id.list_contacts);
		list.setAdapter(adapter = new Adapter.ContactAdapter(getContext(), R.layout.item_dialog));	
		adapter.inviteItem = true;
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position, long id) {
				Adapter.Contact c = adapter.getItem(position);
				if (c.user != null)
					if (chat != null) {
						chat.apiUserAdd(c.user);
						Main.main.goChatInfo(chat);
					} else 
						Main.main.goDialog(Dialog.getDialog(c.user.id, -1, true));
				
				if (c.contact != null) {
					try {
						Intent smsIntent = new Intent(Intent.ACTION_VIEW);         
						smsIntent.setData(Uri.parse("sms:" + c.contact.getString("phone")));
						smsIntent.putExtra("sms_body", inviteText); 
						getContext().startActivity(smsIntent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}	
			}
		});
    }

    public void setChat(Chat chat) {
    	this.chat = chat;
    }
    
	public void updateContacts() {
		post(new Runnable() {
			@Override
		    public void run() {
		    	ArrayList<User> users = User.getUsers(true);				
		    	adapter.clear();
				for (int i = 0; i < users.size(); i++) {
					User user = users.get(i);
					if (user.id == 0 || (chat != null && chat.users.contains(user)) || user.blocked)
						continue;
					adapter.add(new Adapter.Contact(users.get(i)));
				}
				
				String cat = "";
				
				ArrayList<TL.Object> contacts = Main.main.contacts;
				for (int i = 0; i < contacts.size(); i++) {
					TL.Object c = contacts.get(i);					
					String n = c.getString("first_name") + " " + c.getString("last_name");
					String nc = n.substring(0, 1);
					if (!cat.equals(nc)) {
						cat = nc;
						adapter.add(new Adapter.Contact(cat));
					}					
					adapter.add(new Adapter.Contact(c));					
				}
								
				adapter.notifyDataSetChanged();
		    }
		});
	}    
    
	@Override
	public void onMenuInit() {
	//	Main.showMenu(R.id.search);
		Main.main.setActionTitle(getResources().getString(R.string.page_title_contacts), null);
	}

	@Override
	public void onMenuClick(View view, int id) {
		if (id == R.id.invite_contact && inviteText != null) {
			try {
				Intent intent = new Intent(Intent.ACTION_SEND);		
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, inviteText);	
				Main.main.startActivity(Intent.createChooser(intent, Main.getResStr(R.string.invite_text)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
					
	}
}
		

