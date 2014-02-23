package com.xproger.arcanum;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class PageDialogList extends LinearLayout implements Common.Page {
	public ListView list;
	public Adapter.DialogListAdapter adapter;
	
    public PageDialogList(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    @Override
    public void onFinishInflate() {    	
    	list = (ListView)findViewById(R.id.list_dialogs);
		list.setAdapter(adapter = new Adapter.DialogListAdapter(getContext()));	
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position, long id) {
				Main.main.goDialog(adapter.getItem(position));
			}
		});
		
		list.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
		        AlertDialog.Builder adb = new AlertDialog.Builder(getContext());		
		        Resources res = getResources();
		        
		        final Dialog d = adapter.getItem(position); 
		        if ( d.chat_id == -1 ) {		        	
		        	adb.setTitle(res.getString(R.string.dialog_title_dialog));		        
		        	adb.setMessage(res.getString(R.string.dialog_delete_dialog));
		        } else {
		        	adb.setTitle(res.getString(R.string.dialog_title_group));		        
		        	adb.setMessage(res.getString(R.string.dialog_delete_group));		        	
		        }
		        	
		        adb.setNegativeButton(res.getString(R.string.dialog_btn_cancel), null);
		        adb.setPositiveButton(res.getString(R.string.dialog_btn_delete), new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						d.leave();
					}});	
		        AlertDialog alert = adb.create();
		        alert.setCanceledOnTouchOutside(true);
		        alert.show();
				return true;
			}
		});
    }

	@Override
	public void onMenuInit() {
		Main.showMenu(R.id.dialog_list);		
    	Main.main.setActionTitle(getResources().getString(R.string.page_title_dialogs), null);
	}    
    
	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.btn_compose :
				Main.main.showPopup(view, R.menu.compose, false);
				break;
			case R.id.btn_other :
				Main.main.showPopup(view, R.menu.other, false);
				break;	    		
			case R.id.btn_compose_contact:
				Main.main.goContactList();
			    break;
			case R.id.btn_compose_group:
				Main.main.goGroupNew();
				break;	    		    		
			case R.id.btn_settings :
				Main.main.goSettings();
				break;
		}		
	}
	
	public void setTopDialog(final Dialog dialog) {
		post(new Runnable() {
			@Override
		    public void run() {		
				adapter.remove(dialog);
				adapter.insert(dialog, 0);
		    }
		});		
	}
	
	public void update(Dialog dialog) {
		int pos = adapter.getPosition(dialog);
		if (pos == -1)
			return;				
		DialogItem item = (DialogItem)Common.getListItem(list, pos);
		if (item != null)
			item.update();
	}
	
}
		

