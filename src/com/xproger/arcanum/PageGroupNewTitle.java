package com.xproger.arcanum;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.text.*;
import android.widget.*;

public class PageGroupNewTitle extends LinearLayout implements Common.Page {
	private ImageView picture;
	private EditText edit;
	private Button btn_done;	
	private String photo_path;
	    
    public PageGroupNewTitle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public void onFinishInflate() {
		picture = (ImageView)findViewById(R.id.picture);		
		picture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Main.main.showPopup(picture, R.menu.photo, true);
			}
		});
		
		edit = (EditText)findViewById(R.id.title);
		
		edit.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateDoneBtn();
			}			
		});			
    }

    public void clear() {
		picture.setImageResource(R.drawable.group_placeholder_blue);
		edit.setText("");		
		photo_path = null;    
		updateDoneBtn();
    }
        
	public void updateDoneBtn() {		
		String str = edit.getText().toString();
		str = str.trim();
		if (btn_done != null) {
			btn_done.setText(getResources().getString(R.string.btn_done));
			btn_done.setEnabled(!str.equals(""));
		}
	}
	
    public void setPicture(Bitmap bmp, String path) {
    	picture.setImageBitmap(bmp);
    	photo_path = path;
    }	
	
	@Override
	public void onMenuInit() {
		Main.showMenu(R.id.btn_done);
		btn_done = Main.main.titleBar.btn_done;
		Main.main.setActionTitle(getResources().getString(R.string.page_title_group), null);        
		updateDoneBtn();
	}

	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.btn_photo_take :
			case R.id.btn_photo_gallery :
				Main.main.chooseAvatar(id == R.id.btn_photo_take); 
				break;
			case R.id.btn_done :
				createChat(edit.getText().toString(), Main.main.pageGroupNew.users);
				break;
		}
	}
	
	private void createChat(String title, ArrayList<TL.Object> users) {
		final Chat chat = new Chat(0);
		chat.setTitle(title);
		for (int i = 0; i < users.size(); i++)
			chat.addUser(users.get(i).getInt("user_id"), false);
		/*
		try {
			if (photo_path != null)
				chat.photo_small = Common.loadBitmap(getContext(), photo_path, 160, 160, false, false, false);
		} catch (Exception e) {
			//
		}
		*/
				
		Chat.chats.put(0, chat);	// TODO: 0 - getRandomChatID() 	
		final Dialog d = Dialog.getDialog(-1, 0, true);
		Main.main.goDialog(d);
		Main.main.setTopDialog(d);
		Main.mtp.api(new TL.OnResultRPC() {			
			@Override
			public void onResultRPC(TL.Object result, Object param, boolean error) {
				if (error || result.id != 0xd07ae726) return;	// !messages.statedMessage
				TL.Object message = result.getObject("message");
				if (message == null) return;				 
				chat.id = message.getObject("to_id").getInt("chat_id");
				d.chat_id = chat.id;
				Chat.chats.remove(0);
				Chat.chats.put(chat.id, chat);
				if (photo_path != null)
					Main.main.uploadChatPhoto(chat.id, photo_path);
				photo_path = null;
				Main.main.updateDialog(d);		
			}
		}, null, "messages.createChat", TL.newVector(users.toArray()), title);
				
	}

}
