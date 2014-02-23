package com.xproger.arcanum;

public class Transport implements Runnable {
	protected MTProto mtp;
	protected int msg_num = 0;
	protected boolean closed = false; 
	
	public Transport(MTProto mtp) {
		this.mtp = mtp;
	}
	
	public void close() {
		closed = true;
	}
	
	protected Boolean connect() {
		return false;
	}	
	
	protected void recvLoop() throws Exception {}
	
	public void send(byte[] data) throws Exception {}

	@Override
	public void run () {
		while (!closed) {
			try {
				if (connect()) recvLoop();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(1000);					
			} catch (Exception se) {
				Common.logError("No Time To Sleep!");
				break;
			}
		}	
		mtp.onDisconnect();
	}		

}