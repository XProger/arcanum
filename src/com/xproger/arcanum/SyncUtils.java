package com.xproger.arcanum;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

public class SyncUtils {
    private static final long SYNC_FREQUENCY		= 60 * 60; 
    private static final String CONTENT_AUTHORITY	= "com.android.contacts";
    private static final String CONTENT_TYPE		= "com.xproger.arcanum.account";
    private static final String MIME_TYPE			= "vnd.android.cursor.item/vnd.arcanum.profile";

    public static void CreateSyncAccount(Context context, String name) {
//        boolean setupComplete = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE, false);        
//      PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREF_SETUP_COMPLETE, true).commit();
        
        Account account = new Account(name, CONTENT_TYPE);
        AccountManager accountManager = AccountManager.get(context);
        if (accountManager.addAccountExplicitly(account, null, null)) {       
            Bundle params = new Bundle();
            params.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
            params.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, false);
            params.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
            
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY, params, SYNC_FREQUENCY);
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            ContentResolver.requestSync(account, CONTENT_AUTHORITY, params);
        }
    }
    
    public static void updateContacts(Context context) {
    	ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    	for (int i = 0; i < User.users.size(); i++) {
    		User user = User.users.get(User.users.keyAt(i));
    		if (user.client_id != 0) {
    			Common.logError("update contact: " + user.getTitle());
    			
    			ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI) 
    			     .withValue(RawContacts.ACCOUNT_TYPE, CONTENT_TYPE) 
    			     .withValue(RawContacts.ACCOUNT_NAME, String.valueOf(user.id)) 
    			     .build()); 
    			
    			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI) 
    			     .withValueBackReference(Data.RAW_CONTACT_ID, 0) 
    			     .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE) 
    			     .withValue(StructuredName.GIVEN_NAME, user.first_name)
    			     .withValue(StructuredName.FAMILY_NAME, user.last_name)
    			     .build());
    			
    			
    			break;
    			/* 
    			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI) 
    			     .withValueBackReference(Data.RAW_CONTACT_ID, 0) 
    			     .withValue(Data.DATA1, user.phone)
    			     .withValue(Data.DATA2, "hello")
    			     .build()); 
    			*/
    			/*
    			Uri rawContactUri = context.getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, new ContentValues());
    			
    			long rawContactId =  ContentUris.parseId(rawContactUri);
                
    			ContentValues values = new ContentValues();
    			values.put(Data.RAW_CONTACT_ID, rawContactId);
    			values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
    			values.put(StructuredName.DISPLAY_NAME, "Robert Smith");    			                
    			context.getContentResolver().insert(Data.CONTENT_URI, values);
    			
    			
    			
    			 ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
    			          .withValue(Data.RAW_CONTACT_ID, 0)
    			          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
    			          .withValue(Phone.NUMBER, "1-800-GOOG-411")
    			          .withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
    			          .withValue(Phone.LABEL, "free directory assistance")
    			          .build());
    			 getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
    			 
    			
    			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)    					
    				//ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
    		        //.withSelection(ContactsContract.Data.CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?", new String[]{String.valueOf(user.client_id), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
    		        
    				.withValue(Data.MIMETYPE, MIME_TYPE)
    				.withValue(Data.DATA1, user.phone)
    		        //.withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, null)
    				//.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, null)    					
    				.build());
    			
    			*/
    			
    		} else {
    			// add contact
    		}
    	}
    	
    	try {
    	//	context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    
    public static void updateContact(final Context context, final User user) {
    	if (user.client_id == 0)
    		return;
    	
    	new Thread(new Runnable() {
			@Override
			public void run() {
		    	try {    	
			    	ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
					ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
						.withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=?" + " AND " + ContactsContract.Data.MIMETYPE + "=?", new String[]{String.valueOf(user.client_id), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
						.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, user.first_name)    					
						.withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, user.last_name)
						.build());		
					
		    		context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
		    	} catch (Exception e) {
		    		e.printStackTrace();
		    	}
			}    		
    	});

    }
}
