package com.xproger.arcanum;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;

public class PageSettingsBlocked extends ListView implements Common.Page {
	public Adapter.ContactAdapter adapter;	

    public PageSettingsBlocked(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {    	
		setAdapter(adapter = new Adapter.ContactAdapter(getContext(), R.layout.item_dialog));		
		setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position, long id) {
				final Adapter.Contact c = adapter.getItem(position);
				
		        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());		
		        Resources res = getResources();
	        	adb.setTitle(res.getString(R.string.dialog_title_unblock));		        
	        	adb.setMessage(res.getString(R.string.dialog_unblock));
	        	
		        adb.setNegativeButton(res.getString(R.string.dialog_btn_cancel), null);
		        adb.setPositiveButton(res.getString(R.string.dialog_btn_unblock), new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						c.user.blocked = false;						
						adapter.remove(c);
						Main.mtp.api(null, null, "contacts.unblock", c.user.getInputUser());
					}});	
		        
		        AlertDialog alert = adb.create();
		        alert.setCanceledOnTouchOutside(true);
		        alert.show(); 		
			}
		});

    }
    
    public void update() {
    	adapter.clear();
    	Main.mtp.api(new TL.OnResultRPC() {			
			@Override
			public void onResultRPC(TL.Object result, Object param, boolean error) {
				if (error) return;
				final TL.Vector users = result.getVector("users");
				User.addUsers(users);

				post(new Runnable() {
					@Override
					public void run() {
						for (int i = 0; i < users.count; i++) {
							TL.Object c = users.getObject(i);
							User user = User.getUser(c.getInt("id"));
							user.blocked = true;
							adapter.add(new Adapter.Contact(user));					
						}
					}					
				});

			/*
				contacts.blockedSlice#900802a1 count:int blocked:Vector<ContactBlocked> users:Vector<User> = contacts.Blocked;
				---functions---
			*/	
			}
		}, null, "contacts.getBlocked", 0, 100);
    }

	@Override
	public void onMenuInit() {
		Main.main.setActionTitle(getResources().getString(R.string.settings_opt_blocked), null);
	}

	@Override
	public void onMenuClick(View view, int id) {
		// TODO Auto-generated method stub		
	}
	
}
		

