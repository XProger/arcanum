package com.xproger.arcanum;


import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class SyncAuth extends Service {
	
	private static class Authenticator extends AbstractAccountAuthenticator {
		public static final String ACCOUNT_NAME = "com.xproger.arcanum.account.name";
	    public static final String ACCOUNT_TYPE = "com.xproger.arcanum.account.type";
	    public static final String AUTH_TOKEN = "kAuthToken";
	    public static final String AUTH_TOKEN_LABEL = "AuthTokenLabel";
		
	    // Simple constructor
	    public Authenticator(Context context) {
	        super(context);
	    }

	    private Bundle createResultBundle() {
	        Bundle result = new Bundle();
	        result.putString(AccountManager.KEY_ACCOUNT_NAME, ACCOUNT_NAME);
	        result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
	        result.putString(AccountManager.KEY_AUTHTOKEN, AUTH_TOKEN);
	        return result;
	    }    
	    
	    @Override
	    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
	            String authTokenType, String[] requiredFeatures, Bundle options)
	            throws NetworkErrorException {
	        return createResultBundle();
	    }

	    @Override
	    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
	        return createResultBundle();
	    }

	    @Override
	    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
	            String authTokenType, Bundle options) throws NetworkErrorException {
	        return createResultBundle();
	    }

	    @Override
	    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
	            Bundle options) throws NetworkErrorException {

	        Bundle result = new Bundle();
	        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
	        return result;
	    }

	    @Override
	    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
	            String authTokenType, Bundle options) throws NetworkErrorException {
	        return createResultBundle();
	    }

	    @Override
	    public String getAuthTokenLabel(String authTokenType) {
	        return AUTH_TOKEN_LABEL;
	    }

	    @Override
	    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
	            String[] features) throws NetworkErrorException {

	        Bundle result = new Bundle();
	        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
	        return result;
	    }

	}	
	
	
	private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        Common.logError("SyncAuth created");
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Common.logError("SyncAuth destroyed");        
    }

    @Override
    public IBinder onBind(Intent intent) {
    	return mAuthenticator.getIBinder();
    }
}
