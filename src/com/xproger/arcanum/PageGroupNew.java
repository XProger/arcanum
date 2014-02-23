package com.xproger.arcanum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class PageGroupNew extends LinearLayout  implements Common.Page, OnItemClickListener {
	private ListView list;
	private EditText edit;
	private Button btn_done;
	private Adapter.ContactAdapter adapter;
	private BubbleSpan bubble = null;
	public ArrayList<TL.Object> users = new ArrayList<TL.Object>();
	
	public class BubbleSpan extends ImageSpan {
		public String text;
		public int id;
		
		public BubbleSpan(Drawable d, int verticalAlignment, int id, String text) {
			super(d, Integer.toString(id), verticalAlignment);
			this.text = text;
			this.id = id;
		}		
	}
	
    public BitmapDrawable getBubble(String text, boolean down) {
   	// ах ты ж грёбаный зелёный ёжик! :(((
    	TextView v = new TextView(getContext());
		v.setText(text);
		v.setTextSize(14);
		v.setTextColor(down ? 0xffffffff : 0xff035ea8);		
		v.setBackgroundResource(down ? R.drawable.compose_bubble_down : R.drawable.compose_bubble_up);
		
		int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		v.measure(spec, spec);
		v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
		Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth() + 8, v.getMeasuredHeight() + 4, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);		
		c.translate(-v.getScrollX() + 4, -v.getScrollY() + 2);	
		v.draw(c);
		BitmapDrawable d = new BitmapDrawable(getContext().getResources(), b);
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		return d; 
    }
    
    public void addBubble(final String text, int id) {
    	SpannableStringBuilder sb = new SpannableStringBuilder(Integer.toString(id));
        int start = 0;
        int end = sb.length();        
        sb.setSpan(new BubbleSpan(getBubble(text, false), BubbleSpan.ALIGN_BOTTOM, id, text), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        Editable e = edit.getEditableText();
        e.append(sb);	
    }
    
    public void updateBubbles(boolean SpanToCheck) {    	
    	Editable s = edit.getEditableText();    	
    	BubbleSpan[] image_spans = s.getSpans(0, s.length(), BubbleSpan.class);
    	
		if (SpanToCheck) {
			for (BubbleSpan span : image_spans) {
	            Adapter.Contact contact = adapter.getItem(Integer.parseInt(span.getSource()));
	            if (!contact.checked) {            	
	            	int start = s.getSpanStart(span);
	            	int end = s.getSpanEnd(span);
	            	s.removeSpan(span);
	            	s.delete(start, end);
	            }
			}
		} else
			for (int i = 0; i < adapter.getCount(); i++) {
				Adapter.Contact contact = adapter.getItem(i);
				if (contact.user != null && contact.checked) {
					for (BubbleSpan span : image_spans)
						if (Integer.parseInt(span.getSource()) == i) {
							contact = null;
							break;
						}				
					
					if (contact != null) {
						contact.checked = false;
						DialogItem item = (DialogItem)Common.getListItem(list, i);
						if (item != null)
							item.setChecked(false);
					}
					
				}
			}
		
		updateDoneBtn();
    }
    
    public void updateDoneBtn() {
    	if (btn_done == null) return;
    	
    	users.clear();    	
		int count = 0;
		for (int i = 0; i < adapter.getCount(); i++) {
			Adapter.Contact c = adapter.getItem(i);
			if (c.user != null && c.checked) {
				users.add(c.user.getInputUser());
				count++;
			}
		}
		btn_done.setEnabled(count > 0);
		String str = Main.getResStr(R.string.btn_done);
		if (count > 0)
			str += " (" + count + ")";
		btn_done.setText(Html.fromHtml(str));
    }
    
    public PageGroupNew(Context context, AttributeSet attrs) {
        super(context, attrs);
    }    
    
    public void clear() {
    	updateUsers();
    }
    
    @Override
    public void onFinishInflate() {
    	list = (ListView)findViewById(R.id.list);    	
    	list.setAdapter(adapter = new Adapter.ContactAdapter(getContext(), R.layout.item_user));
    	
    	list.setOnItemClickListener(this);
    	edit = (EditText)findViewById(R.id.edit);
    	//edit.setMovementMethod(LinkMovementMethod.getInstance());    	
    	//edit.setHighlightColor(Color.BLUE);    	    
    	
    	edit.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				updateBubbles(false);				
			}			
		});
    	
    	edit.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				int action = event.getAction();

				if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL)
					return false;				
				
				int x = (int) event.getX();
	            int y = (int) event.getY();

	            x -= edit.getTotalPaddingLeft();
	            y -= edit.getTotalPaddingTop();

	            x += edit.getScrollX();
	            y += edit.getScrollY();

	            Layout layout = edit.getLayout();
	            int line = layout.getLineForVertical(y);
	            int off = layout.getOffsetForHorizontal(line, x);

	            edit.setSelection(off);

	            int id = -1;
	            BubbleSpan[] link = edit.getText().getSpans(off, off, BubbleSpan.class);
	            Editable s = edit.getEditableText(); 
	            
	            if (link.length == 1) {
	            	BubbleSpan span = link[0];	            		           
	            	int start = s.getSpanStart(span);
	            	int end = s.getSpanEnd(span);
	            	
	            	if (action == MotionEvent.ACTION_DOWN) {
	            		String text = span.text;
	            		id = span.id;	            		
	            		s.removeSpan(span);
	            		s.setSpan(bubble = new BubbleSpan(getBubble(text, true), BubbleSpan.ALIGN_BOTTOM, id, text), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            		return true;
	            	}
	            	
	            	if (action == MotionEvent.ACTION_UP && bubble == span) {	// release bubble
            			s.removeSpan(span);
            			s.delete(start, end);
            			updateBubbles(false);
            			bubble = null;
            			return true;
	           		}
	            }
	            
	            if ( (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && bubble != null) { 	// tap and move, set bubble up
	            	String text = bubble.text;
            		id = bubble.id;	     
	            	int start = s.getSpanStart(bubble);
	            	int end = s.getSpanEnd(bubble);
            		s.removeSpan(bubble);
            		s.setSpan(new BubbleSpan(getBubble(text, false), BubbleSpan.ALIGN_BOTTOM, id, text), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					bubble = null;
            		return true;
	            }	            
	            
	            return false;
			}
    	});
    }
    
    public void updateUsers() {   
    	users.clear();
    	edit.setText("");
    	ArrayList<User> users = User.getUsers(true);
    	Collections.sort(users, new Comparator<User>() {
			@Override
			public int compare(User user1, User user2) {
				return user1.getTitle().compareTo(user2.getTitle());
			}
    	});

    	adapter.clear();
		String cat = "";
		for (int i = 0; i < users.size(); i++) {
			User user = users.get(i);
			Common.logError("contact user: " + user.id + " " + user.getTitle());
			String nc = user.getTitle().substring(0, 1);
			if (!cat.equals(nc)) {
				cat = nc;
				adapter.add(new Adapter.Contact(cat));
			}					
			adapter.add(new Adapter.Contact(user));
		}		
		updateDoneBtn();
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Adapter.Contact contact = adapter.getItem(position);		
		contact.checked = !contact.checked;
		((DialogItem)view).setChecked(contact.checked);
		if (contact.checked) {
			addBubble(contact.user.getTitle(), position);
			updateDoneBtn();
		} else
			updateBubbles(true);
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
		if (id == R.id.btn_done)
			Main.main.goGroupNewTitle();
	}

}
