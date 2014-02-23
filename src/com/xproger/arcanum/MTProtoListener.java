package com.xproger.arcanum;

public interface MTProtoListener {
	public void onRedirect(MTProto mtp);
	public void onConnected();
	public void onDisconnected();
	public void onError(String msg);
	public void onReady();
	public void onAuth(TL.Object user);
	public void onBind();
	public void onMessage(TL.Object message, int msgFlag);
	public void onUserName(int user_id, String first_name, String last_name);
}
