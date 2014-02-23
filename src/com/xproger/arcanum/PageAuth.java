package com.xproger.arcanum;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

public class PageAuth extends LinearLayout implements Common.Page, OnItemSelectedListener {
	public static final String FILTER = "com.xproger.arcanum.SMS";
	public static final String EXTRA_CODE = "code";	
	
	public Animation anim_from_right, anim_to_left, anim_from_left, anim_to_right;
	public ViewFlipper flipper;
	public Spinner countrySpinner;
	public ImageView picture;
	public Button btnNext;
	public String tmp_photo = null;
	private BroadcastReceiver receiver;
	private EditText ed_phone_code, ed_phone, ed_code;
	private boolean phone_registered; 
	private String phone_number, phone_code, phone_code_hash = null;	
	private Updater updater = null;
	private TextView hint_code, hint_call;
	private int ticker;
	
	public PageAuth(Context context, AttributeSet attrs) {
		super(context, attrs);		
		anim_from_right	= AnimationUtils.loadAnimation(context, R.anim.from_right);
		anim_to_left	= AnimationUtils.loadAnimation(context, R.anim.to_left);
		anim_from_left	= AnimationUtils.loadAnimation(context, R.anim.from_left);
		anim_to_right	= AnimationUtils.loadAnimation(context, R.anim.to_right);		
	}	
	
	@Override
	public void onFinishInflate() {		
		// view flipper
		flipper = (ViewFlipper)findViewById(R.id.flipper);		

	// country Spinner
		countrySpinner = (Spinner)findViewById(R.id.country_spinner);
		countrySpinner.setOnItemSelectedListener(this);		
		
		ed_phone_code	= (EditText)findViewById(R.id.edit_phone_code);
		ed_phone		= (EditText)findViewById(R.id.edit_phone);
		ed_code			= (EditText)findViewById(R.id.edit_code);
		
		hint_code		= (TextView)findViewById(R.id.auth_hint_code);
		hint_call		= (TextView)findViewById(R.id.auth_hint_call);
		
	// profile picture ImageView		
		picture = (ImageView)findViewById(R.id.picture);
		
		findViewById(R.id.lay_name).setVisibility(View.VISIBLE);
		findViewById(R.id.lay_info).setVisibility(View.GONE);
		
		countrySpinner.setSelection(173);
		try {
			String str = ((TelephonyManager)Main.main.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
			if (str != null && !str.equals("")) {										
				str = Common.phoneClear(str).substring(1);				
				String[] reg = getResources().getStringArray(R.array.country_code);
				for (int i = reg.length - 1; i >= 0; i--)
					if (str.startsWith(reg[i])) {
						str = str.substring(reg[i].length());
						countrySpinner.setSelection(i);
						break;
					}				
			}
			
			ed_phone.setText(str);
		} catch (Exception e) {			
			e.printStackTrace();
		}		
		
		ed_phone.requestFocus();
		ed_code.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable arg0) {
				checkCode();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				//
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				//
			}});
		
		//inputField.addTextChangedListener(new PhoneNumberFormattingTextWatcher());		
		
	// next button
		btnNext = (Button)findViewById(R.id.btn_next);
		
		receiver = new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {				
				if (flipper.getDisplayedChild() == 1) {
					try {				
						String code = intent.getStringExtra(EXTRA_CODE);
						ed_code.setText(code);
						onNext();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};

		start();
	}
	
	public void start() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(FILTER);		
		Main.main.registerReceiver(receiver, filter);						
	}
	
	public void stop() {
		Main.main.unregisterReceiver(receiver);		
	}
	
	@Override
	public void onMenuInit() {
	}

	@Override
	public void onMenuClick(View view, int id) {
		switch (id) {
			case R.id.picture :
				Main.main.showPopup(picture, R.menu.photo, true);
				break;
			case R.id.btn_photo_take :
			case R.id.btn_photo_gallery :
				Main.main.chooseAvatar(id == R.id.btn_photo_take); 
				break;
			case R.id.btn_next :
				onNext();
				break;
			case R.id.btn_prev :
				break;
		}
	}
	
	public void onNext() {
		switch (flipper.getDisplayedChild()) {
		
			case 0 :				
				String code = ed_phone_code.getText().toString().substring(1);
				String phone = ed_phone.getText().toString();

				phone_number = code + phone;				
				Main.mtp.api(new TL.OnResultRPC() {					
					@Override
					public void onResultRPC(TL.Object result, Object param, boolean error) {
						if (error) {
							try {
								if (result.getString("error_message").indexOf("_MIGRATE_") > -1) 
									return;		
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							Common.logError("error");
							stopUpdater();
							Main.main.mainView.post(new Runnable() {
								@Override
								public void run() {	
									if (flipper.getDisplayedChild() == 1)
										goBack();
								}								
							});														
							return;
						}
						phone_registered	= result.getBool("phone_registered"); 
						phone_code_hash		= result.getString("phone_code_hash");						
					}
				}, null, "auth.sendCode", phone_number, 1, MTProto.API_ID, MTProto.API_HASH);												
				
				stopUpdater();
				
				ticker = 60;
				updater = new Updater(1000, new Runnable() {
					@Override
					public void run() {
						if (ticker <= 0) return;
						ticker--;										
						hint_call.setText(String.format(Main.getResStr(R.string.auth_hint_call), String.valueOf(ticker)));						
						if (ticker == 0) {			
							hint_call.setText("");													
							Main.mtp.api(null, null, "auth.sendCall", phone_number, phone_code_hash);
							stopUpdater();
						}
					}});
				updater.startUpdates();

				String str = String.format(Main.getResStr(R.string.auth_hint_code), "<b>+" + phone_number + "</b>");
				hint_code.setText(Html.fromHtml(str));				
				goNextAnim();
				break;
				
			case 1 :				
				if (phone_code_hash == null)	// ещё не пришёл auth.sentCode
					return;

				phone_code = ed_code.getText().toString();
				
				if (phone_registered) {	
					checkCode();
					return;
				}				
				
				stopUpdater();
				stop();
				
				goNextAnim();					
				break;				
			case 2 :
				String first_name = ((EditText)findViewById(R.id.text_first_name)).getText().toString();
				String last_name  = ((EditText)findViewById(R.id.text_last_name)).getText().toString();
			
				btnNext.setVisibility(View.GONE);
				Main.mtp.api(new TL.OnResultRPC() {					
					@Override
					public void onResultRPC(TL.Object result, Object param, boolean error) {
						btnNext.setVisibility(View.VISIBLE);
						if (error) {
							Common.logError("error");
							return;
						}

						if (tmp_photo != null) 			
							Main.main.uploadProfilePhoto(tmp_photo);				

						Main.main.mainView.post(new Runnable() {
							@Override
							public void run() {						
								Main.main.goDialogList();
							}								
						});
					}
				}, null, "auth.signUp", phone_number, phone_code_hash, phone_code, first_name, last_name);
		}		
	}
	
	public void checkCode() {
		phone_code = ed_code.getText().toString();
		if (phone_code == null || phone_code.equals(""))
			return;
	//	btnNext.setVisibility(View.GONE);
		Main.mtp.api(new TL.OnResultRPC() {					
			@Override
			public void onResultRPC(TL.Object result, Object param, boolean error) {
	//			btnNext.setVisibility(View.VISIBLE);				
				if (error) {
					Common.logError("error");
					return;
				}
				
				
				stopUpdater();
				stop();
				
				Main.main.mainView.post(new Runnable() {
					@Override
					public void run() {						
						Main.main.goDialogList();
					}								
				});
		
			}
		}, null, "auth.signIn", phone_number, phone_code_hash, phone_code);
	}
	
	public void setPicture(String path, Bitmap bmp) {
		picture.setImageBitmap(bmp);
		tmp_photo = path;
	}
	
	public void stopUpdater() {
		if (updater != null) {
			updater.stopUpdates();
			updater = null;
		}
		ticker = 0;
	}
	
	
	public void goNextAnim() {
		flipper.setInAnimation(anim_from_right);
		flipper.setOutAnimation(anim_to_left);		
		flipper.showNext();
	}
	

	public void goBack() {
		if (flipper.getDisplayedChild() == 1) 
			stopUpdater();
		flipper.setInAnimation(anim_from_left);
		flipper.setOutAnimation(anim_to_right);		
		flipper.showPrevious();
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
		String[] str = getResources().getStringArray(R.array.country_code);
		EditText editPhoneCode = (EditText)findViewById(R.id.edit_phone_code);
		editPhoneCode.setText("+" + str[position]);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parentView) {}
	
}
		

