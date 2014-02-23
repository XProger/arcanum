package com.xproger.arcanum;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();		
		if (bundle == null) 
			return;

		try {		
			Object[] pdus = (Object[])bundle.get("pdus");
			SmsMessage[] msgs = new SmsMessage[pdus.length];				
			for (int i = 0; i < msgs.length; i++) {
				msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
				String body = msgs[i].getMessageBody();
				String[] code = body.split("arcanum://");
				if (code.length != 2)
					continue;
				try {
					Intent smsIntent = new Intent(PageAuth.FILTER);
					smsIntent.putExtra(PageAuth.EXTRA_CODE, code[code.length - 1]);				
					context.sendBroadcast(smsIntent);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

}