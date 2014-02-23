package com.xproger.arcanum;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class Adapter {

	public static class DialogListAdapter extends ArrayAdapter<Dialog> {
		
	    public DialogListAdapter(Context context) {	        
	    	super(context, R.layout.item_dialog);
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {	    
	    	DialogItem item = (DialogItem)(convertView == null ? Main.inflater.inflate(R.layout.item_dialog, parent, false) : convertView);
	    	item.setDialog(getItem(position));
	        return item;
	    }
	}
	
	public static class Contact {
		public User user = null;
		public Dialog dialog = null;
		public TL.Object contact = null;
		public String category = null;
		public boolean checked;
		
		public Contact(Object obj) {
			if (obj instanceof User)		user = (User)obj;
			if (obj instanceof TL.Object)	contact = (TL.Object)obj;
			if (obj instanceof String)		category = (String)obj;
			if (obj instanceof Dialog)		dialog = (Dialog)obj;
		}
	}
	
	public static class ContactAdapter extends ArrayAdapter<Contact> {
		private int resource;
		private boolean updating = false;
		public boolean inviteItem = false;
		
	    public ContactAdapter(Context context, int resource) {	        
	    	super(context, resource);
	    	this.resource = resource;
	    }
	    
	    @Override
	    public Contact getItem(int position) {
	    	if (inviteItem)
	    		return position == 0 ? null : super.getItem(position - 1);	    	
	    	return super.getItem(position);
	    }
	    
	    @Override
	    public int getItemViewType(int position) {
	    	if (position == 0 && inviteItem)
	    		return 3;
	    	
	    	Contact c = getItem(position);
	    	if (c.user != null || c.dialog != null)
	    		return 0;
	    	if (c.category != null)
	    		return 1;
	    	return 2;
	    }	    
	    
	    @Override
	    public int getCount() {
	        return super.getCount() + (inviteItem ? 1 : 0);
	    }	    
	    
	    @Override
	    public int getViewTypeCount() {
	        return 4;
	    }

	    @Override
	    public boolean areAllItemsEnabled() {  
	        return false;  
	    }	    
	    	    
	    @Override
	    public boolean isEnabled(int position) {
			return getItemViewType(position) != 1;    	
	    }

	    public void updateBegin() {
	    	updating = true;
	    }

	    public void updateEnd() {
	    	updating = false;
	    	notifyDataSetChanged();
	    }
	    
		@Override
		public void notifyDataSetChanged() {			
			if (!updating)
				super.notifyDataSetChanged();			
		}	    
	    
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {	    	    		    		    
	    	final Contact c = getItem(position);

	    	TextView v;
	    	View view = null;
	    	switch (getItemViewType(position)) {
		    	case 0 :
			    	DialogItem item = (DialogItem)(convertView == null ? Main.inflater.inflate(resource, parent, false) : convertView);
			    	item.setContact(c);
			    	view = item;
			    	break;
		    	case 1 :
	    			v = (TextView)(convertView == null ? Main.inflater.inflate(R.layout.item_category, parent, false) : convertView);
	    			v.setText(c.category);
	    			view = v;
		    		break;
		    	case 2 : 
	    			v = (TextView)(convertView == null ? Main.inflater.inflate(R.layout.item_contact, parent, false) : convertView);
	    			String str = c.contact.getString("first_name") + " " + c.contact.getString("last_name");
	    			str = str.trim();
	    			int idx = str.lastIndexOf(" ");
	    			if (idx > 0) 
	    				str = str.substring(0, idx) + "<b>" + str.substring(idx, str.length()) + "</b>";
	    			v.setText(Html.fromHtml(str));
	    			view = v;		    		
		    		break;
		    	case 3 : 
		    		view = Main.inflater.inflate(R.layout.item_invite, parent, false);		    		
		    		break;
	    	}
	        return view;
	    }
	}
	

/*
	public static class MessagesAdapter extends CursorAdapter {
		private LayoutInflater inflater;

		public MessagesAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Dialog.Message msg = (Dialog.Message)getItem( cursor.getPosition() ); 
			((MessageView)view).setMessage(msg);			
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
    		int resId = MessageView.getRes(getItemViewType( cursor.getPosition() ));
			return (MessageView)inflater.inflate(resId, parent, false);
		}
		
	    @Override
	    public int getViewTypeCount() {
	        return MessageView.getTypeCount();
	    }		

	    @Override
	    public int getItemViewType(int position) {
	    	return MessageView.getType((Dialog.Message)getItem(position));
	    }	    
	    
	}
*/
/*
	public static class MessagesAdapter extends BaseAdapter {				
		private Dialog dialog;
		private LayoutInflater inflater;
		private int lastCount;
		
		public MessagesAdapter(Context context) {
			super();
	    	inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public void setDialog(Dialog dialog) {
			this.dialog = dialog;
			lastCount = -1;
		}
		
		@Override
		public int getCount() {
			Common.logError("getCount");

			int count = dialog == null ? 0 : dialog.msgList.size();
			if (lastCount != count)
				notifyDataSetChanged();
			
			lastCount = count;
			return count;
		}

		@Override
		public void notifyDataSetChanged() {
			Common.logError("notifyDataSetChanged");
			super.notifyDataSetChanged();
		}
		
	
		@Override
		public Dialog.Message getItem(int position) {
			return dialog.msgList.get(position);
		}
	
		@Override
		public long getItemId(int position) {
			return getItem(position).id;
		}
		
	    @Override
	    public int getItemViewType(int position) {
	    	return MessageView.getType(getItem(position));
	    }	    
	    
	    @Override
	    public int getViewTypeCount() {
	        return MessageView.getTypeCount();
	    }		
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	    	Dialog.Message msg = getItem(position);   	
	    	if (position == 0)
	    		dialog.queryHistory(20);
	    	
	    	MessageView view = (MessageView)convertView;	    	
	    	if (view == null)
		    	view = (MessageView)inflater.inflate( MessageView.getRes(getItemViewType(position)) , parent, false);	    	
	    	
	    	view.setMessage(msg);
	        return view;
		}
		
	}
*/

	public static class MessagesAdapter extends ArrayAdapter<Dialog.Message> {
		private Dialog dialog;				
		private boolean updating = false;
		private Calendar calendar = Calendar.getInstance();
		private int day;		
		public SparseArray<Dialog.Message> selected = new SparseArray<Dialog.Message>();		
		
	    public MessagesAdapter(Context context) {	        
	    	super(context, R.layout.msg_in);	    	
	    }

		public void setDialog(Dialog dialog) {
			this.dialog = dialog;
			update();
		}
		
		public boolean select(Dialog.Message msg) {
			if (selected.get(msg.id) == null) {
				selected.put(msg.id, msg);						
				return true;
			}
			selected.remove(msg.id);
			
			return false;
		}
		
		public void update() {
			calendar.setTime(new Date(Common.getUnixTime() * 1000L));			
			day = calendar.get(Calendar.DAY_OF_YEAR);

			updating = true;
			clear();			
			for (int i = 0; i < dialog.msgList.size(); i++)
				add(dialog.msgList.get(i));
			
		// update selected (if deleted)
			int i = 0;
			while (i < selected.size()) {
				Dialog.Message sel = selected.get(selected.keyAt(i));
				Dialog.Message msg = Dialog.messages.get(sel.id);
				if (msg == null || msg.deleted)
					selected.remove(sel.id);
				else
					i++;
			}
			updating = false;
			notifyDataSetChanged();
		}
		
		@Override
		public void add(Dialog.Message msg) {
			calendar.setTime(new Date(msg.date * 1000L));					
			int msg_day = calendar.get(Calendar.DAY_OF_YEAR);
						
			if (day != msg_day) {				
				day = msg_day;
				// TODO: msg_year
				super.add(new Dialog.Message(new SimpleDateFormat("MMMMM d").format(msg.date * 1000L)));
			}
			
			super.add(msg);
		}
		
		@Override
		public void notifyDataSetChanged() {			
			if (!updating)
				super.notifyDataSetChanged();			
		}		
		
	    @Override
	    public int getItemViewType(int position) {
	    	return MessageView.getType(getItem(position));
	    }	    
	    
	    @Override
	    public int getViewTypeCount() {
	        return MessageView.getTypeCount();
	    }
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	Main.main.readHistory(dialog);
	    	
	    	Dialog.Message msg = getItem(position);   	
	    	int resId = 0;
	    	
	    	MessageView view = null;
	    	if (convertView == null) {
	    		resId = MessageView.getRes(getItemViewType(position));
		    	view = (MessageView)Main.inflater.inflate(resId, parent, false);
	    	} else 
	    		view = (MessageView)convertView;	 
	    	
	    	view.setMessage(msg);
	    	view.setSelectedMode(selected.get(msg.id) != null);
	        return view;
	    }
	}
	
	public static class ChatPagerAdapter extends PagerAdapter {
	    private FrameLayout[] pages;
	    public ViewPager pager;
	    public int count = 0;
	    
	    public ChatPagerAdapter(Context context, ViewPager pager, int maxCount) {	    
    		pages = new FrameLayout[maxCount];    		
	    	this.pager = pager;
	    	for (int i = 0; i < pages.length; i++)
	    		pages[i] = new FrameLayout(context);	    
	    }
	    
	    public View getPage(int position) {
	    	return pages[position].getChildAt(0);
	    }
	    
	    @Override
	    public Object instantiateItem(View collection, int position) {
	    	((ViewPager) collection).addView(pages[position], 0);
	        return pages[position];
	    }
	    
	    @Override
	    public void destroyItem(View collection, int position, Object view) {
	        ((ViewPager)collection).removeView((View)view);
	    }
	    
	    @Override
	    public int getCount(){
	        return count;
	    }
	    
		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
		
		public void setPages(View ... views) {		
			count = views.length;			
			for (int i = 0; i < pages.length; i++)
				if (i >= views.length || views[i] != pages[i].getChildAt(0))
					pages[i].removeAllViews();
				else
					views[i] = null;
			
			for (int i = 0; i < views.length; i++)
				if (views[i] != null)
					pages[i].addView(views[i]);
			notifyDataSetChanged();
		}
	}	
	
	public static class EmojiGridAdapter extends BaseAdapter {
		private int page;
		private OnClickListener listener;
		
		EmojiGridAdapter(int page, OnClickListener listener) {
			this.page = page;
			this.listener = listener;
		}
		
		@Override
		public int getCount() {			
			return Emoji.data[page].length;
		}

		@Override
		public Object getItem(int position) {
			return Emoji.data[page][position];
		}

		@Override
		public long getItemId(int position) {
			return (Long)getItem(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView view = (ImageView)convertView;
			if (view == null) {				
				view = new ImageView(parent.getContext());
				view.setScaleType(ScaleType.CENTER);
				view.setBackgroundResource(R.drawable.btn_ab);
				int s = (int)Common.dipToPixels(4);
				view.setPadding(s, s, s, s);
				view.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				view.setOnClickListener(listener);		
			}			
			long code = (Long)getItem(position);
			int index = Emoji.getIndex(code);
			view.setImageDrawable( new Emoji.Smile(index, 1.5f) );
//			view.setImageResource(R.drawable.ic_msg_panel_smiles);
			view.setTag(code);
			return view;
		}
	}
	
	public static class EmojiPagerAdapter extends PagerAdapter {
		private OnClickListener listener;
		
		EmojiPagerAdapter(OnClickListener listener) {
			this.listener = listener;
		}
		
		@Override
		public int getCount() {			
			return Emoji.data.length;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
		
	    @Override
	    public Object instantiateItem(View collection, int position) {
	    	GridView grid = new GridView(collection.getContext());
	    	grid.setNumColumns(GridView.AUTO_FIT);
	    	grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
	    	grid.setColumnWidth(Emoji.Smile.SMILE_SIZE * 2);
	    	
	    	grid.setAdapter(new EmojiGridAdapter(position, listener));
	    	grid.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	    	((ViewPager)collection).addView(grid);
	        return grid;
	    }
	    
	    @Override
	    public void destroyItem(View collection, int position, Object view) {
	        ((ViewPager)collection).removeView((View)view);
	    }		
	}
	
	
	public static class PhotoPagerAdapter extends PagerAdapter {
	    private ViewPager pager;
	    private PagePhoto pagePhoto;
	    private ArrayList<Dialog.Message> msgList = new ArrayList<Dialog.Message>(); 
	    
	    public PhotoPagerAdapter(ViewPager pager, PagePhoto pagePhoto) {
	    	this.pager = pager;
	    	this.pagePhoto = pagePhoto;
	    	pager.setAdapter(this);
	    }
	    
	    public void update(Dialog dialog) {	 
	    	int index = pager.getCurrentItem();
	    	int size = msgList.size();
	    	Dialog.Message msg = null, msg_next = null;	    	
	    	if (size > 0) {
	    		msg = msgList.get(index);
	    		if (size > 1)
	    			msg_next = msgList.get(index + (index + 1 < size ? 1 : -1));
	    	}
	    	
	    	pager.setAdapter(null);
	    	msgList = dialog.getMediaList();
	    	Collections.reverse(msgList);
	    	pager.setAdapter(this);
	    	
	    	if (msgList.size() > 0 && msg != null) {
	    		index = msgList.indexOf(msg);
	    		if (index == -1)
	    			index = msgList.indexOf(msg_next);
	    		if (index != -1)
	    			pager.setCurrentItem(index);
	    	}	    	
	    }
	    
	    public int getPosition(Dialog.Message msg) {
	    	return msgList.indexOf(msg);
	    }
	    
	    public Dialog.Message getMessage() {
	    	int index = pager.getCurrentItem();
	    	return index == -1 || msgList.size() == 0 ? null : msgList.get(pager.getCurrentItem());	    	
	    }
	    
	    @Override
	    public Object instantiateItem(View collection, int position) {
	    	if (msgList.size() == 0)
	    		return null;	    	
	    	ImageViewTouch view = new ImageViewTouch(collection.getContext());	 
	    	view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	    	view.setDisplayType( DisplayType.FIT_TO_SCREEN );	   
	    	pagePhoto.getMedia(msgList.get(position), view);	    	
	    	((ViewPager) collection).addView(view, 0);	    	
	        return view;
	    }
	    
	    @Override
	    public void destroyItem(View collection, int position, Object view) {
	        ((ViewPager)collection).removeView((View)view);
	    }
	    
	    @Override
	    public int getCount() {
	        return msgList.size();
	    }
	    
		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
	}		
 
	public static class GalleryGridAdapter extends BaseAdapter implements OnClickListener {
	    private ArrayList<Dialog.Message> msgList = new ArrayList<Dialog.Message>(); 		    
		private GridView grid;
				
		GalleryGridAdapter(GridView grid) {
			this.grid = grid;
		}
		
		public void update(Dialog dialog) {
			if (dialog == null)
				return;
	    	grid.setAdapter(null);
	    	msgList = dialog.getMediaList();
	    	Collections.reverse(msgList);
	    	grid.setAdapter(this);			
		}
		
		@Override
		public int getCount() {			
			return msgList.size();
		}

		@Override
		public Dialog.Message getItem(int position) {
			return msgList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).id;
		}	

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {				
				view = Main.inflater.inflate(R.layout.item_photo, parent, false);
				view.setOnClickListener(this);
				ImageView img = (ImageView)view.findViewById(R.id.picture);
				img.setScaleType(ScaleType.CENTER_CROP);
				view.setTag(R.id.picture, img);
				view.setTag(R.id.msg_video, view.findViewById(R.id.msg_video));
			}
			Dialog.Message msg = getItem(position);
			msg.getMedia((ImageView)view.getTag(R.id.picture), "m");
			TextView vtext = (TextView)view.getTag(R.id.msg_video);
			
			if (msg.hasVideoMedia()) {
				vtext.setText(msg.getVideoText());
				vtext.setVisibility(View.VISIBLE);
			} else
				vtext.setVisibility(View.GONE);
			
			view.setTag(R.id.msg_media, msg);			
			
			return view;
		}

		@Override
		public void onClick(View view) {
			Main.main.goPhoto((Dialog.Message)(view.getTag(R.id.msg_media)));			
		}
	}
		
	public static class WallpaperAdapter extends BaseAdapter {
		private TL.Vector wallpaper = null; 

		@Override
		public int getCount() {
			return wallpaper == null ? 0 : wallpaper.count + 1;
		}

		@Override
		public TL.Object getItem(int position) {
			return position == 0 ? null : wallpaper.getObject(position - 1);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		public void update(TL.Vector wp) {
			wallpaper = wp;
	    	notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {			
			ImageView view = (ImageView)convertView;			
			if (view == null) {
				view = new ImageView(parent.getContext());
				int s = parent.getHeight();
				view.setLayoutParams(new Gallery.LayoutParams(s, s));
			}	
				
			TL.Object obj = getItem(position);
			
			if (obj == null) {
				view.setBackgroundColor(0x40062040);
				view.setScaleType(ImageView.ScaleType.CENTER);
				view.setImageResource(R.drawable.ic_gallery_background);			
			} else {				
				view.setScaleType(ImageView.ScaleType.CENTER_CROP);				
				TL.Object size = Common.getPhotoSizeTL(obj.getVector("sizes"), "m");
				int s = parent.getHeight();
				Common.loadImageTL(view, size, s, s, true, true);
			}

			return view;
		}
	}	
	
	
	public static class IntroPager extends PagerAdapter {
		private int resTitle[] = {
			R.string.intro_title0,
			R.string.intro_title1,
			R.string.intro_title2,
			R.string.intro_title3,
			R.string.intro_title4,
			R.string.intro_title5
		};		
		
		private int resText[] = {
			R.string.intro_text0,
			R.string.intro_text1,
			R.string.intro_text2,
			R.string.intro_text3,
			R.string.intro_text4,
			R.string.intro_text5
		};		
		
	    public IntroPager(Context context) {
	    	//
	    }
	    
	    @Override
	    public Object instantiateItem(View collection, int position) {
	    	View view = Main.inflater.inflate(R.layout.item_intro, null, false);
	    	((TextView)view.findViewById(R.id.title)).setText(Html.fromHtml(Main.getResStr(resTitle[position])));
	    	((TextView)view.findViewById(R.id.text)).setText(Html.fromHtml(Main.getResStr(resText[position])));	 
	    	((ViewPager) collection).addView(view, 0);
	        return view;
	    }
	    
	    @Override
	    public void destroyItem(View collection, int position, Object view) {
	        ((ViewPager)collection).removeView((View)view);
	    }
	    
	    @Override
	    public int getCount() {
	        return resText.length;
	    }
	    
		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
	}			
	
}
