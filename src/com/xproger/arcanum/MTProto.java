package com.xproger.arcanum;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;

import org.json.*;

import android.annotation.SuppressLint;
import android.support.v4.util.LongSparseArray;
import android.util.SparseArray;

@SuppressWarnings("unused")
public class MTProto {
	public static final int REUSE_MAIN		= 0;
	public static final int REUSE_UPLOAD	= 1;
	public static final int REUSE_DOWNLOAD	= 2;	
	
	public static ArrayList<MTProto> mtp = new ArrayList<MTProto>();
	public static ArrayList<TL.Object> dcStates = new ArrayList<TL.Object>();
	public static int dc_this, dc_date;
	static public int upd_pts, upd_date, upd_seq;

	public static class Message {
		TL.Object obj;
		Boolean encrypted, accept, accepted;
		TL.OnResultRPC result;
		Object param;
		
		Message(Boolean encrypted, Boolean accept, TL.Object obj, TL.OnResultRPC result, Object param) {
			this.obj		= obj;
			this.encrypted	= encrypted;
			this.accept		= accept;
			this.accepted	= false;
			this.result		= result;
			this.param		= param; 
		}
	}
	
	public static final String RSA_MODULUS	= "c150023e2f70db7985ded064759cfecf0af328e69a41daf4d6f01b538135a6f91f8f8b2a0ec9ba9720ce352efcf6c5680ffc424bd634864902de0b4bd6d49f4e580230e3ae97d95c8b19442b3c0a10d8f5633fecedd6926a7f6dab0ddb7d457f9ea81b8465fcd6fffeed114011df91c059caedaf97625f6c96ecc74725556934ef781d866b34f011fce4d835a090196e9a5f0e4449af7eb697ddb9076494ca5f81104a305b6dd27665722c46b60e5df680fb16b210607ef217652e60236c255f6a28315f4083a96791d7214bf64c1df4fd0db1944fb26a2a57031b32eee64ad15a8ba68885cde74a5bfc920f6abf59ba5c75506373e7130f9042da922179251f"; 
	public static final String RSA_EXPONENT	= "010001";
	public static final int	API_ID			= 1920;
	public static final String API_HASH		= "f1d4f2a03614193dcdd6a5e706d3ed3f";
	
	public MTProtoListener cb;
	public int reuseFlag;
	public TL.Object dcState;	
	private TransportTCP transport;
	private Common.AES aes = new Common.AES();
	public byte[] auth_key;
	public boolean bind, connected;
	private long auth_key_id, auth_key_aux_hash;
	private long server_salt = 0;
	private int seqno = 0, time_delta = 0, cur_msg_seq;
	private long cur_message_id;
	private long session = 13, last_message_id = 0, req_msg_id;
	private LongSparseArray<Message> TLMessage = new LongSparseArray<Message>();
	private ArrayList<Message> TLMessageQueue = new ArrayList<Message>();
	private ArrayList<Long> msg_ack = new ArrayList<Long>();
	private boolean bad_seq = false;
	private boolean waitResult = false;
	
// auth temporary
	private byte[] cl_nonce, sv_nonce, new_nonce;
	private long fp;
	private BigInteger g, g_a, dh_prime;
	
	public static void init() {
		mtp.clear();
		dcStates.clear();
	//	dcStates.add(TL.newObject("joim.dcState", 0, "95.142.192.65", 80, new byte[0], false, Common.random.nextLong(), 0L, 0));		
	//	dcStates.add(TL.newObject("joim.dcState", 1, "173.240.5.253", 443, new byte[0], false, Common.random.nextLong(), 0L, 0));		
		dcStates.add(TL.newObject("joim.dcState", 0, "173.240.5.1", 443, new byte[0], false, GEN_session_id(), 0L, 0));
		
		dc_date = dc_this = upd_date = upd_pts = upd_seq = 0;
	}
	
	public static void deinit() {
		for (int i = 0; i < mtp.size(); i++)
			mtp.get(i).transport.close();		
	}
		
	public static MTProto getActiveMTP(int dc_id, int reuseFlag) {
		for (int i = 0; i < mtp.size(); i++)
			if (mtp.get(i).dcState.getInt("id") == dc_id && mtp.get(i).reuseFlag == reuseFlag)
				return mtp.get(i);
		return null;
	}
	
	public static MTProto getConnection(int dc_id, MTProtoListener callback, int reuseFlag) {
		MTProto m = getActiveMTP(dc_id, reuseFlag);		
		if (m != null) {
			Common.logError("reuse");
			return m;	
		}
		m = new MTProto(dc_id, callback, reuseFlag);
		mtp.add(m);		
		return m;
	}
	
	public void setUpdate(int date, int pts, int seq) {
		if (date >= 0) upd_date = date;
		if (pts  >= 0) upd_pts  = pts;
		if (seq  >= 0) upd_seq  = seq;		
	}	
	
	public void updateDcStates() {		
		dcState.set("auth_key",		auth_key);
		dcState.set("bind",			bind);
		dcState.set("server_salt",	server_salt);
		if (reuseFlag == REUSE_MAIN) {
			dcState.set("session",		session);
			dcState.set("seqno",		seqno);
		}
		for (int i = 0; i < dcStates.size(); i++) {
			TL.Object state = dcStates.get(i);
			if (state.getInt("id") == dc_this) {
				state.set("auth_key",		auth_key);
				state.set("bind",			bind);				
				state.set("server_salt",	server_salt);				
				if (reuseFlag == REUSE_MAIN) {
					state.set("session",	session);
					state.set("seqno",		seqno);
				}
			}		
		}
	}
	
	public void setAuthKey(byte[] value) {
		if (value == null || value.length == 0) {		
			auth_key = new byte[0];
			auth_key_aux_hash = auth_key_id = 0;
			return;
		}
		auth_key = value;
		dcState.set("auth_key",	auth_key);
		
		byte[] auth_hash = Common.getSHA1(auth_key);
		auth_key_aux_hash = Common.toLong(Common.ASUB(auth_hash, 0, 8)); 
		auth_key_id = Common.toLong(Common.ASUB(auth_hash, 12, 8));		
	}
	
	private void process(TL.Object obj) {
//		if (obj instanceof TL.Vector)
//			return;
		Method m;
		try {
			m = MTProto.class.getDeclaredMethod("TL_" + obj.type.replace(".", "_"), TL.Object.class);
			m.setAccessible(true);
		} catch (Exception e) {
			Common.logWarning("not handled: " + obj.type + " (" + obj.name + ")");
			return;
		}
		
		try {
			Common.logInfo("invoke: " +  obj.type + " (" + obj.name + ")");
			m.invoke(this, obj);
		} catch (Exception e) {
			Common.logError("process error");
			e.printStackTrace();
		}
	}
		
	
// MTProto
	public MTProto(int dc_id, MTProtoListener callback, int reuseFlag) {
		this.reuseFlag = reuseFlag;
		cb = callback;
		for (int i = 0; i < dcStates.size(); i++)
			if (dcStates.get(i).getInt("id") == dc_id) {
				dcState = dcStates.get(i);
				//Common.hexStringToByteArray("23D03699CE2AB29BA2273084D95DA126EC3A1D55BE4C317615CE66609D5562BDC0EFDE5AE1F9185001C35781F622B31DF5294685559340DE7D5CC8D7F6F86AE049107D8E498EB2AC3D6FA735DF90648EEC34A6B7BE3A5075A455F5696DB39280BF68C1637E1580E1EBA3F0C12EF2C03B8E9B5ECCFD3E4885BF636863388E3EC9E9EF60C722FF9B45CD93FA5E8D0D277B45A6A9370860582A159187F2F352D418D195D8E9310B5559E170F51CB2056F6CB6DB586E9349192A1B7EAA50887C115A14F996F5A855E90E47635A81EA3048615F4FD91347D73335E5503179857D0D29132483271B28E6591172C3D94686BD96E91FDB7AD9591A526218B3DDFC7A2A09")
				setAuthKey(dcState.getBytes("auth_key"));
				bind		= dcState.getBool("bind");
				seqno		= dcState.getInt("seqno");
				session		= dcState.getLong("session");
				server_salt	= dcState.getLong("server_salt");
				Common.logError("session: " + session + " seqno: " + seqno);
				
				connected	= false;
				Thread netThread = new Thread(transport = new TransportTCP(this, true));
				netThread.setPriority(Thread.MIN_PRIORITY);
				netThread.start();
				
				return;
			}
		Common.logError("dc not found: " + dc_id); 
	}

	public void onConnect() {
		connected = true;
		Common.logInfo("connected to dc[" + dcState.getInt("id") + "] " + dcState.getString("ip") + ":" + dcState.getInt("port"));
		if (auth_key == null || auth_key.length == 0) {
			auth();
		} else
			onAuthorized();
	}
	
	public void onAuthorized() {
		dcState.set("auth_key",		auth_key);
		dcState.set("server_salt",	server_salt);
		
		long upd_delta = (long)Common.getUnixTime() - (long)dc_date;		
		if (Main.mtp == this && upd_delta > 5 * 60)
			api_help_getConfig();
		
		if (!bind && this != Main.mtp) {
			final MTProto m = this;
			Common.logError("exportAuthorization");
			Main.mtp.api(new TL.OnResultRPC() {			
				@Override
				public void onResultRPC(TL.Object result, Object param, boolean error) {
					if (!error)	return;			
					if (result.id != 0xdf969c2d) return; // auth.ExportedAuthorization
					Common.logError("importAuthorization");
					m.api(null, null, "auth.importAuthorization", result.getInt("id"), result.getBytes("bytes"));
				}
			}, null, "auth.exportAuthorization", dcState.getInt("id"));		
			return;
		}

		if (bind)
			cb.onBind();			
		send_queue();
	}
	
	public void onDisconnect() {
		connected = false;		
		Common.logDebug("disconnected");
	}
	
	public void onReceive(ByteBuffer b) {
		if (b.remaining() == 4) {
			Common.logError("invalid packet:");
			byte[] data = new byte[b.remaining()];
			b.get(data);
			Common.print_dump(data);
		//	retry( TLMessage.keyAt(  TLMessage.size() - 1) );
			return;
		}
		
		try {
			if (b.getLong() == 0) {	// auth_key_id
				b.getLong();		// message_id
				b.getInt();			// message length
				process( TL.deserialize(b) );				
			} else {
				byte[] msg_key = new byte[16];
				b.get(msg_key);
				byte[] data = new byte[b.remaining()];
				b.get(data);
							
				synchronized (aes) {
					aes.prepare(false, msg_key, auth_key);
					data = aes.IGE(data, false);
				}
				
				ByteBuffer btmp = ByteBuffer.wrap(data);
				btmp.order(ByteOrder.LITTLE_ENDIAN);
				server_salt = btmp.getLong();
				btmp.getLong();						// session_id
				cur_message_id = btmp.getLong();	// message_id
				cur_msg_seq = btmp.getInt();		// seq_no
				//if (cur_msg_seq > seqno)
				//	seqno = cur_msg_seq + cur_msg_seq % 2;
				int bsize = btmp.getInt();
				if (bsize < 0 || bsize > 1024 * 1024) {
					Common.print_dump(btmp.array());
					Common.logError(new String(btmp.array()));					
					Common.logError("FFFUUUUUUUUUUU!!!");
				}
				
				b = BufferAlloc(bsize);
				btmp.get(b.array());
				b.getInt();
				ack_message(cur_msg_seq, cur_message_id);
				b.position(0);			

				process( TL.deserialize(b) );				
				send_accept();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ByteBuffer BufferAlloc(int size) {
		ByteBuffer b = ByteBuffer.allocate(size);
		b.order(ByteOrder.LITTLE_ENDIAN);
		return b;
	}

	public synchronized boolean sendMessage(Message msg) {		
		if (msg == null)
			return false;

		if (!connected || (msg.encrypted && (auth_key == null || auth_key.length == 0) ) || bad_seq) {
			Common.logError("add to queue " + connected + " " + msg.encrypted);
			TLMessageQueue.add(msg);
			return false;
		}
		ByteBuffer b = null; 
				
		Common.logInfo("message send: " + msg.obj.name + ":" + msg.obj.type);
		byte[] msg_data = msg.obj.serialize();
		
		/*
		if (msg_data.length > 255) {
			Common.logError("compress...");
			byte[] data = Common.gzipDeflate(msg_data); // TODO: compress message data
			if (data.length < msg_data.length && data.length > 32)
				msg_data = TL.newObject("gzip_packed", data).serialize();
		}
		*/
		
		if (msg.encrypted) {
			long message_id = GEN_message_id();
			TLMessage.put(message_id, msg);
			
			int size_real = 32 + msg_data.length;
			int size_padding = (size_real + 15) / 16 * 16;

			ByteBuffer p = BufferAlloc(size_padding);
			p.putLong(server_salt);
			p.putLong(session);
			p.putLong(message_id);				
			
			if (msg.accept) seqno++;
			p.putInt(seqno);
			if (msg.accept) seqno++;
			
			p.putInt(msg_data.length);
			p.put(msg_data);			
			byte[] data = p.array();
			GEN_random_bytes(data, p.position(), data.length);
			
		// encrypt data
			byte[] msg_key = Common.ASUB(Common.getSHA1(Common.ASUB(data, 0, size_real)), 4, 16);
			
			synchronized (aes) {
				aes.prepare(true, msg_key, auth_key);
				data = aes.IGE(data, true);
			}
			
		// write encrypted packet
			b = BufferAlloc(24 + data.length);
			b.putLong(auth_key_id);
			b.put(msg_key);
			b.put(data);
		} else {
			b = BufferAlloc(20 + msg_data.length); 				
			b.putLong(0);
			b.putLong(GEN_message_id());
			b.putInt(msg_data.length);
			b.put(msg_data);
		}
		
		try {
			synchronized (transport) {
				transport.send(b.array());
				return true;
			}
		} catch (Exception e) {
			return false;
		}		
	}
	
	public void send(TL.Object obj, Boolean encrypted, Boolean accept, TL.OnResultRPC result, Object param) {		
		Message msg = new Message(encrypted, accept, obj, result, param);
		sendMessage(msg);
	}

	public void send(TL.Object obj, Boolean encrypted, Boolean accept) {
		send(obj, encrypted, accept, null, null);
	}
	
	
	public void retry(long message_id) {
		Common.logError("retry");
		Message msg = TLMessage.get(message_id);
		if (msg == null)
			return;
		TLMessage.delete(message_id);
		sendMessage(msg);		
	}
	
	public void send_queue() {
		for (int i = 0; i < TLMessageQueue.size(); i++)
			sendMessage(TLMessageQueue.get(i));		
		TLMessageQueue.clear();		
	}
	
	public void checkUpdates() {
		if (upd_pts == 0)
			api_updates_getState();
		else
			api_updates_getDifference();
	}	
	
	public void auth() {
		Common.logError("auth");
		TL.Object req_pq = TL.newObject("req_pq", cl_nonce = GEN_random_bytes(16)); 
		send(req_pq, false, false);
	}

// TL constructors
	public void ack_message(int seq_no, long message_id) {
		if (seq_no % 2 == 0)
			return;
		msg_ack.add(message_id);
	}
	
	@SuppressLint("SimpleDateFormat")
	private String getMessageTime(long message_id) {
		return new SimpleDateFormat("HH:mm:ss").format(new Date( (message_id >> 32) * 1000L));		
	}
	
	private long GEN_message_id() {
		long time = System.currentTimeMillis() + time_delta * 1000;
		long id = (time / 1000L << 32) + (time % 1000) / 4 * 4;
		if (last_message_id >= id) id = last_message_id + 4;
		last_message_id = id;
		return id;
	}
	
	private static long GEN_session_id() {
		long s = 0;
		while ((s = Common.random.nextLong()) == 0);
		return s;
	}
	
	private byte[] GEN_random_bytes(int length) {
		byte[] data = new byte[length];
		Common.random.nextBytes(data);
		return data;
	}
	
	private void GEN_random_bytes(byte[] data, int start, int end) {
		for (int i = start; i < end; i++)
			data[i] = (byte)(Common.random.nextInt() & 0xFF);
	}
/*	
	public byte[] GEN_nonce_hash(int id) {
		byte[] nonce_data = Common.ASUM(Common.ASUM(new_nonce, new byte[]{(byte) id}), auth_key_aux_hash);
		return Common.ASUB(Common.getSHA1(nonce_data), 4, 16);
	}	
*/
	private void send_accept() {
		if (msg_ack.size() == 0)
			return;
	/*	
		Common.logInfo("send accept:");		
		for (int i = 0; i < msg_ack.size(); i++)
			Common.logInfo(" - " + getMessageTime(msg_ack.get(i))  + " " + msg_ack.get(i));		
	*/
		TL.Object msgs_ack = TL.newObject("msgs_ack", TL.newVector(msg_ack.toArray()));
		
		send(msgs_ack, true, false );
		msg_ack.clear();
	}	
	
	private void send_ping() {
		send( TL.newObject("ping", Common.random.nextLong()), true, false);
	}	
	
	private void send_client_DH_inner_data(long retry_id) {
		byte[] b_data = new byte[256];		
		Common.random.nextBytes(b_data);		
		
		BigInteger b = new BigInteger(1, b_data);
		BigInteger g_b = g.modPow(b, dh_prime);
		Common.logError("g_b length: " + g_b.toByteArray().length + " -> " + Common.toBytes(g_b).length);
		
		BigInteger akey = g_a.modPow(b, dh_prime);
		Common.logError("auth_key: " + akey.toString());
		setAuthKey(Common.toBytes(akey));
		
	// gen data (client_DH_inner_data)
		TL.Object data_obj = TL.newObject("client_DH_inner_data", cl_nonce, sv_nonce, retry_id, g_b);
		byte[] data = data_obj.serialize();		
		byte[] hash = Common.getSHA1(data);
		
		byte[] data_with_hash = new byte[(hash.length + data.length + 15) / 16 * 16];
		System.arraycopy(hash, 0, data_with_hash, 0, hash.length);
		System.arraycopy(data, 0, data_with_hash, hash.length, data.length);
		
	// send set_client_DH_params
		TL.Object req_obj = TL.newObject( "set_client_DH_params", cl_nonce, sv_nonce, aes.IGE(data_with_hash, true) );
		send(req_obj, false, false);			
	}

	private void TL_ResPQ(TL.Object obj) {
		sv_nonce = obj.getBytes("server_nonce");
		BigInteger pq = new BigInteger(1, obj.getBytes("pq"));
		TL.Vector v_fp = obj.getVector("server_public_key_fingerprints");
		fp = v_fp.getLong(0);
		Common.logError("pq: " + pq.toString());

	// prime factorization for pq
		BigInteger q = Common.rho(pq);
		BigInteger p = pq.divide(q);
		if (p.compareTo(q) > 0) {
			BigInteger t = p;
			p = q;
			q = t;
		}
		SecureRandom rnd = new SecureRandom();
		new_nonce = new byte[32]; 
		rnd.nextBytes(new_nonce);
		
	// generate encrypted_data
		TL.Object data_obj = TL.newObject( "p_q_inner_data", pq, p, q, cl_nonce, sv_nonce, new_nonce );
		
		byte[] data = data_obj.serialize();
		byte[] hash = Common.getSHA1(data);

		byte[] data_with_hash = new byte[255];
		System.arraycopy(hash, 0, data_with_hash, 0, hash.length);
		System.arraycopy(data, 0, data_with_hash, hash.length, data.length);
		GEN_random_bytes(data_with_hash, data.length + hash.length, 255);
		
		byte[] encrypted_data = Common.RSA(RSA_MODULUS, RSA_EXPONENT, data_with_hash);		
				
	// req_DH_params		
		TL.Object req_obj = TL.newObject( "req_DH_params", cl_nonce, sv_nonce, p, q, fp, encrypted_data );
		send(req_obj, false, false);
	}
	
	private void TL_Server_DH_Params(TL.Object obj) {
		if (obj.name.equals("server_DH_params_ok")) {
			aes.prepare(cl_nonce, sv_nonce, new_nonce);		
			byte[] answer = aes.IGE(obj.getBytes("encrypted_answer"), false);		
	
			ByteBuffer btmp = ByteBuffer.wrap(answer);
			btmp.order(ByteOrder.LITTLE_ENDIAN);
			btmp.position(20);
			process( TL.deserialize(btmp) );
		}
		
		if (obj.name.equals("server_DH_params_fail"))
			transport.disconnect();
	}
	
	private void TL_Server_DH_inner_data(TL.Object obj) {
		g			= BigInteger.valueOf(obj.getInt("g"));
		dh_prime	= new BigInteger(1, obj.getBytes("dh_prime"));
		g_a			= new BigInteger(1, obj.getBytes("g_a"));
		time_delta	= (int)((long)obj.getInt("server_time") - (long)System.currentTimeMillis() / 1000L);
		last_message_id = 0;
		send_client_DH_inner_data(0);
	}
	
	private void TL_Set_client_DH_params_answer(TL.Object obj) {
		if (obj.name.equals("dh_gen_ok")) {			
		// check auth key	
			/*
			if (!Arrays.equals( obj.getBytes("new_nonce_hash1"), GEN_nonce_hash(1))) {
				Common.logError("auth_key check failed");
				setAuthKey(null);
				auth();
			//	transport.disconnect();
				return;
			}
			*/
			
		// done MTProto authorization
			server_salt = Common.toLong( Common.AXOR(Common.ASUB(new_nonce, 0, 8), Common.ASUB(sv_nonce, 0, 8)) );
			
		// clean temporary data
			cl_nonce = sv_nonce = new_nonce = null;
			g = g_a = dh_prime = null;
			fp = 0;
			onAuthorized();
		}
		
		if (obj.name.equals("dh_gen_retry"))
			send_client_DH_inner_data(auth_key_aux_hash);

		if (obj.name.equals("dh_gen_fail"))
			transport.disconnect();	
	}
	
	private void TL_MessageContainer(TL.Object obj) {
		TL.Vector v = (TL.Vector)obj;
		
		int count = v.count;
		Common.logDebug("msg_count: " + count);
		for (int i = 0; i < v.count; i++) {
			TL.Object message = v.getObject(i); 			
			ack_message(message.getInt("seqno"), message.getLong("msg_id"));
			process(message.getObject("body"));
		}		
	}
	
	private void TL_NewSession(TL.Object obj) {
		server_salt = obj.getLong("server_salt");
		bad_seq = false;
		send_queue();
	}
	
	private void TL_MsgsAck(TL.Object obj) {
		TL.Vector msg_ids = obj.getVector("msg_ids");
		for (int i = 0; i < msg_ids.count; i++) {
			Message msg = TLMessage.get(msg_ids.getLong(i));
			if (msg != null)
				msg.accepted = true;
			//TLMessage.delete(msg_ids.getLong(i));
		}
			
	}		

	private void TL_RpcResult(TL.Object obj) {
		req_msg_id = obj.getLong("req_msg_id");		
		TL.Object result = obj.getObject("result");
		Message msg = TLMessage.get(req_msg_id);
		if (msg != null && msg.result != null)
			msg.result.onResultRPC(result, msg.param, result.name != null && result.name.equals("rpc_error"));		
		process(result);
		TLMessage.delete(req_msg_id);
	}

	private void TL_RpcError(TL.Object obj) {
		int code = obj.getInt("error_code");
		String msg = obj.getString("error_message");		
		
		Common.logError(String.format("rpc_error: %s (%d)\n", msg, code));
		
		Message req_msg = TLMessage.get(req_msg_id);
		if (req_msg == null)
			return;
		Common.logError("message object: " + req_msg.obj.name + ":" + req_msg.obj.type);
				
		int idx = msg.indexOf("_MIGRATE_"); 
		if (idx > 0) {
			String type = msg.substring(0, idx);			
			
			String num = msg.substring(idx + 9);
			if ( (idx = num.indexOf(":")) > 0) 
				num = num.substring(0, idx);				
			int dc_id = Integer.parseInt(num);
			Common.logError("redirect to dc: " + dc_id);
			
			MTProto m = MTProto.getConnection(dc_id, cb, reuseFlag); 
			cb.onRedirect(m);
			if (type.equals("PHONE") || type.equals("NETWORK") || type.equals("USER"))
				dc_this = dc_id;
			
			m.sendMessage( req_msg );
		}
	}
	
	private void TL_BadMsgNotification(TL.Object obj) {
		int error_code = obj.getInt("error_code");
			
		Message msg = TLMessage.get( obj.getLong("bad_msg_id") );
		Common.logError("bad_msg: " + error_code + " " + msg.obj.name + ":" + msg.obj.type);
		
		if (error_code == 16 || error_code == 17) {
			time_delta = (int)((cur_message_id >> 32) - Common.getUnixTime());
			last_message_id = 0;
			
		}
		
		if (error_code == 32 || error_code == 33) {
			Common.logError("cur seq: " + cur_msg_seq);
			Common.logError("old seq: " + seqno);
			if (!bad_seq) {
				session = GEN_session_id();
				seqno = 0;
				send_ping();
				bad_seq = true;	
			}
			
			//seqno = cur_msg_seq + (cur_msg_seq % 2) + 100;
		//	session = GEN_session_id();
		//	seqno = 0;
		}
		
		if (obj.id == 0xedab447b) { // bad_server_salt
			server_salt = obj.getLong("new_server_salt");
			dcState.set("server_salt", server_salt);
		}
		
		retry(obj.getLong("bad_msg_id"));
	}
	
	private void TL_Config(TL.Object obj) {
		dc_date = obj.getInt("date");
		MTProto.dc_this = obj.getInt("this_dc");
		dcState.set("id", dc_this);		
		updateDcStates();
				
		TL.Vector dc_options = obj.getVector("dc_options");		
		
		ArrayList<TL.Object> new_dcStates =  new ArrayList<TL.Object>();

		for (int i = 0; i < dc_options.count; i++) {
			TL.Object dcObj = dc_options.getObject(i);
			int id = dcObj.getInt("id");

			TL.Object state = TL.newObject("joim.dcState", id, dcObj.getString("ip_address"), dcObj.getInt("port"), new byte[0], false,  GEN_session_id(), 0L, 0);			
			
			for (int j = 0; j < MTProto.dcStates.size(); j++) {
				TL.Object item = MTProto.dcStates.get(j);
				if (id == item.getInt("id") && state.getString("ip").equals(item.getString("ip"))) {
					state.set("auth_key",		item.getBytes("auth_key"));
					state.set("bind",			item.getBool("bind"));
					state.set("session",		item.getLong("session"));
					state.set("server_salt",	item.getLong("server_salt"));
					state.set("seqno",			item.getInt("seqno"));
					if (state.getInt("port") == item.getInt("port"))
						dcState = state;
					break;
				}
			}

			new_dcStates.add(state);
		}
		
		dcStates = new_dcStates;		
		send_queue();
		cb.onReady();		
	}
	/*
	private void TL_Packed(TL.Object obj) {
		if (obj.name.equals("gzip_packed")) {
			Common.logError("get data");
			byte[] data = obj.getBytes("packed_data");
			Common.logError("uncompress");
			data = Common.gzipInflate( data );
			Common.logError("uncompress done");
			ByteBuffer buf = ByteBuffer.wrap( data );
			buf.order(ByteOrder.LITTLE_ENDIAN);
			Common.logError("parse");
			process( TL.deserialize(buf) );
			Common.logError("parse done");
		}
	}
	*/
	private void TL_auth_SentCode(TL.Object obj) {
		//
	}
	
	private void TL_auth_Authorization(TL.Object obj) {
	//	int expires = obj.getInt("expires");
		bind = true;
		dcState.set("bind", bind);
		cb.onAuth(obj.getObject("user"));
		cb.onBind();
	}
		
	private void TL_Pong(TL.Object obj) {		
		if (obj.name.equals("ping"))			
			send( TL.newObject("pong", 0L, obj.getLong("ping_id") ), true, false );
		
		if (obj.name.equals("pong")) {
			/*
			if (!ready) {
				send_queue();
				ready = true;
			}
			*/
		}
			
	}	

	private void TL_Bool(TL.Object update) {
		// OnResultRPC only
	}	

	private void TL_contacts_ImportedContacts(TL.Object obj) {
		User.addUsers(obj.getVector("users"));
		TL.Vector imported = obj.getVector("imported");		
		for (int i = 0; i < imported.count; i++) {
			TL.Object c = imported.getObject(i);
			User.getUser(c.getInt("user_id")).client_id = c.getLong("client_id"); 
		}
		SyncUtils.updateContacts(Main.main);
	}
	
	private void TL_contacts_Contacts(TL.Object obj) {		
		if (obj.name.equals("contacts.contacts"))
			User.addUsers(obj.getVector("users"));
	}
	
	public void invoke(String name, TL.Object obj) {
		Method m;
		try {
			m = MTProto.class.getDeclaredMethod("TL_" + name.replace(".", "_"), TL.Object.class);
			m.setAccessible(true);
		} catch (Exception e) {
			Common.logWarning("not handled: Main::" + name);
			return;
		}
		
		try {
			Common.logInfo("invoke: Main::" +  name);
			m.invoke(this, obj);
		} catch (Exception e) {
			Common.logError("invoke error in Main");
			e.printStackTrace();
		}	
	}	

	public void onUpdate(TL.Object update, TL.Vector users, TL.Vector chats) {
		User.addUsers(users);
		Chat.addChats(chats);
		invoke(update.name, update);
	}
	
	private void TL_updates_Difference(TL.Object obj) {
		if (obj.name.equals("updates.differenceEmpty"))
			setUpdate(obj.getInt("date"), -1, obj.getInt("seq"));
		
		if (obj.name.equals("updates.difference") || obj.name.equals("updates.differenceSlice")) {
			
			TL.Vector other_updates = obj.getVector("other_updates");
			for (int i = 0; i < other_updates.count; i++)
				onUpdate(other_updates.getObject(i), obj.getVector("users"), obj.getVector("chats"));
			
			TL.Vector new_messages = obj.getVector("new_messages");
			for (int i = 0; i < new_messages.count; i++)
				Dialog.newMessage(new_messages.getObject(i), Dialog.MSG_HISTORY);
			
//			if (new_messages.count > 0)
//				Dialog.vibrate();
						
			if (obj.name.equals("updates.differenceSlice")) {
				TL_updates_State(obj.getObject("intermediate_state"));
				this.api_updates_getDifference();
			} else 
				TL_updates_State(obj.getObject("state"));
		}
			
	}
	
	private void TL_Updates(TL.Object obj) {
		if (obj.name.equals("updatesTooLong"))
			api_updates_getDifference();
	
		boolean chat = obj.name.equals("updateShortChatMessage");
		if (obj.name.equals("updateShortMessage") || chat) {
			cb.onMessage(Dialog.Message.newObject(
						obj.getInt("id"),
						obj.getInt("from_id"), 
						chat ? obj.getInt("chat_id") : -1, 
						obj.getInt("from_id"), 
						obj.getInt("date"), 
						obj.getString("message"), null), Dialog.MSG_INCOMING | Dialog.MSG_HISTORY);
			setUpdate(obj.getInt("date"), obj.getInt("pts"), obj.getInt("seq"));  
		}
		
		if (obj.name.equals("updateShort")) {
			onUpdate(obj.getObject("update"), null, null);
			setUpdate(obj.getInt("date"), -1, -1);
		}
		
		if (obj.name.equals("updatesCombined") || obj.name.equals("updates")) {
			TL.Vector updates = obj.getVector("updates");
			for (int i = 0; i < updates.count; i++)
				onUpdate(updates.getObject(i), obj.getVector("users"), obj.getVector("chats"));
			setUpdate(obj.getInt("date"), -1, obj.getInt("seq"));
		}
	}
	
	private void TL_updates_State(TL.Object obj) {
		setUpdate(obj.getInt("date"), obj.getInt("pts"), obj.getInt("seq"));
		// unread_count:int
	}
	
	private void TL_updateNewMessage(TL.Object update) {
		cb.onMessage(update.getObject("message"), Dialog.MSG_INCOMING | Dialog.MSG_HISTORY);
		setUpdate(-1, update.getInt("pts"), -1);
	}
	
	private void TL_updateMessageID(TL.Object update) {
		Dialog.messageUpdateID(update.getLong("random_id"), update.getInt("id"), null);
	}
	
	private void TL_updateReadMessages(TL.Object update) {
		setUpdate(-1, update.getInt("pts"), -1);
		TL.Vector messages = update.getVector("messages");
		for (int i = 0; i < messages.count; i++)
			Dialog.messageRead(messages.getInt(i));
	}
	
	private void TL_updateDeleteMessages(TL.Object update) {
		setUpdate(-1, update.getInt("pts"), -1);
		TL.Vector messages = update.getVector("messages");
		for (int i = 0; i < messages.count; i++)
			Dialog.messageDelete(messages.getInt(i));
	}
	
	private void TL_updateRestoreMessages(TL.Object update) {
		setUpdate(-1, update.getInt("pts"), -1);
		TL.Vector messages = update.getVector("messages");
		for (int i = 0; i < messages.count; i++)
			Dialog.messageRestore(messages.getInt(i));
	}
	
	private void userTyping(int chat_id, int user_id) {
		User user = User.getUser(user_id);
		if (user != null)
			user.setTyping(chat_id, true);
	}
	
	private void TL_updateUserTyping(TL.Object update) {		
		userTyping(-1, update.getInt("user_id"));
	}
	
	private void TL_updateChatUserTyping(TL.Object update) {
		userTyping(update.getInt("chat_id"), update.getInt("user_id"));
	}
	
	private void TL_updateChatParticipants(TL.Object update) {
		TL.Object p = update.getObject("participants");
		Chat chat = Chat.getChat(p.getInt("chat_id"));
		if (chat != null)
			chat.setParticipants(p);
	}
	
	private void TL_updateUserStatus(TL.Object update) {
		User user = User.getUser(update.getInt("user_id"));
		if (user != null)
			user.setStatus(update.getObject("status"));
	}
	
	private void TL_updateUserName(TL.Object update) {
		cb.onUserName(update.getInt("user_id"), update.getString("first_name"), update.getString("last_name"));
	}
	
	private void TL_updateUserPhoto(TL.Object update) {
		User user = User.getUser(update.getInt("user_id"));
		if (user != null)
			user.setPhoto(update.getObject("photo"));
	}
	
	private void TL_updateContactRegistered(TL.Object update) {
	//	cb.onUserRegister(update.getInt("user_id"), update.getInt("date"));
	}

	private void TL_updateContactLink(TL.Object update) {
	//	#51a48a9a user_id:int my_link:contacts.MyLink foreign_link:contacts.ForeignLink = Update;
	}
	
	private void TL_updateActivation(TL.Object update) {
	//	cb.onUserActivation(update.getInt("user_id"));
	}
	
	private void TL_User(TL.Object user) {
		User.addUser(user);
	}
	
	private void TL_updateNewAuthorization(TL.Object update) {
	//	cb.onNewAuthorization(update.getInt("date"), update.getString("device"), update.getString("location"));
	}
	
	private void TL_messages_StatedMessage(TL.Object obj) {		
		setUpdate(-1, obj.getInt("pts"), obj.getInt("seq"));

		User.addUsers(obj.getVector("users"));
		TL.Object message = obj.getObject("message");
		Dialog d = Dialog.getDialog(message.getInt("from_id"), message.getObject("to_id"), true);		
		Dialog.newMessage(message, Dialog.MSG_INCOMING);
	}
	
	private void TL_messages_StatedMessages(TL.Object obj) {		
		setUpdate(-1, obj.getInt("pts"), obj.getInt("seq"));

		User.addUsers(obj.getVector("users"));
		TL.Vector messages = obj.getVector("messages");
		for (int i = 0; i < messages.count; i++) {
			TL.Object message = messages.getObject(i);		
			Dialog d = Dialog.getDialog(message.getInt("from_id"), message.getObject("to_id"), true);		
			Dialog.newMessage(message, Dialog.MSG_INCOMING);
		}
	}	
	
	private void TL_messages_Messages(TL.Object obj) {
		// OnRpcResult only
	}
	
	public void TL_messages_Dialogs(TL.Object obj) {				
		User.addUsers(obj.getVector("users"));
		Chat.addChats(obj.getVector("chats"));
		
	// dialogs
		TL.Vector dialogs = obj.getVector("dialogs");				
		for (int i = 0; i < dialogs.count; i++) { 
			TL.Object dobj = dialogs.getObject(i);
			Dialog d = Dialog.getDialog(-1, dobj.getObject("peer"), true);
			d.updating = true;
		}
		
	// messages
		TL.Vector messages = obj.getVector("messages");
		for (int i = 0; i < messages.count; i++)
			cb.onMessage(messages.getObject(i), Dialog.MSG_HISTORY);
		
		for (int i = 0; i < dialogs.count; i++) {
			TL.Object dobj = dialogs.getObject(i);
			Dialog d = Dialog.getDialog(-1, dobj.getObject("peer"), true);	
			if (dobj.getInt("top_message") == -7)	// config_dialogs magic
				d.noHistory = true;
			d.unread_count = dobj.getInt("unread_count");
			d.updating = false;
			Main.main.updateDialog(d);
		}
		
		Common.logError("dialogs: " + Dialog.dialogs.size());

	// slice
		if (obj.name.equals("messages.dialogsSlice") && dialogs.count > 0) {
			int limit = obj.getInt("count") - Dialog.dialogs.size();
			if (limit > 0)
				api_messages_getDialogs(Dialog.dialogs.size(), 0, limit);
		}
		
		if (Main.main != null)
			Main.main.resetDialogs();
	}
	
	private void TL_messages_dialogsSlice(TL.Object obj) {
		//
	}
	
	private void TL_messages_SentMessage(TL.Object obj) {
	//	Dialog.messageSent(obj.getInt("id"), obj.getInt("date"));
		setUpdate(obj.getInt("date"), obj.getInt("pts"), obj.getInt("seq"));
	}
	
	private void TL_messages_AffectedHistory(TL.Object obj) {
		setUpdate(-1, obj.getInt("pts"), obj.getInt("seq"));
	}
	
	private void TL_photos_Photo(TL.Object obj) {
		User.addUsers(obj.getVector("users"));
	}	
	
	private void TL_UserFull(TL.Object userFull) {
		User.addUser(userFull);
	}
	
	private void TL_messages_ChatFull(TL.Object obj) {
		User.addUsers(obj.getVector("users"));		
		TL.Object full_chat = obj.getObject("full_chat");
		Chat chat = Chat.getChat(full_chat.getInt("id"));
		if (chat != null)
			chat.update(full_chat);		
	}
	
// API	
	public void api(TL.OnResultRPC result, Object param, String method, Object ... params) {		
		send( TL.newObject(method, params), true, true, result, param);
	}
	
	public void api_help_getConfig() {
		api(null, null, "help.getConfig");
	}
	
	public void api_auth_sendCode(String phone_number) {
		api(null, null, "auth.sendCode", phone_number, 1, API_ID, API_HASH);
	}
	
	public void api_auth_signIn(String phone_number, String phone_code_hash, String phone_code) {
		api(null, null, "auth.signIn", phone_number, phone_code_hash, phone_code);
	}
	
	public void api_auth_signUp(String phone_number, String phone_code_hash, String phone_code, String first_name, String last_name) {
		api(null, null, "auth.signUp", phone_number, phone_code_hash, phone_code, first_name, last_name);
	}
	

	public void api_contacts_importContacts(ArrayList<TL.Object> contacts, boolean replace) {
		api(null, null, "contacts.importContacts", TL.newVector(contacts.toArray()), replace);
	}

	public void api_updates_getDifference() {
		if (upd_pts > 0)
			api(null, null, "updates.getDifference", upd_pts, upd_date);
	}

	public void api_updates_getState() {
		api(null, null, "updates.getState");
	}

	public void api_messages_getDialogs(int offset, int max_id, int limit) {
		Common.logError("get dialogs: " + offset + " " + max_id + " " + limit);
		api(null, null, "messages.getDialogs", offset, max_id, limit);
	}
	
	public void api_messages_sendMessage(TL.Object peer, String message, long random_id, TL.OnResultRPC result) {
		api(result, random_id, "messages.sendMessage", peer, message, random_id);
	}
	
	public void api_messages_sendMedia(TL.Object peer, TL.Object inputMedia, long random_id, TL.OnResultRPC result) {
		api(result, random_id, "messages.sendMedia", peer, inputMedia, random_id);
	}	
	
	public void api_messages_getHistory(TL.Object peer, int offset, int max_id, int limit, TL.OnResultRPC result) {
		Common.logError("get history " + offset + " " + max_id + " " + limit);
		api(result, limit, "messages.getHistory", peer, offset, max_id, limit);
	}
	
	public void api_account_updateProfile(String first_name, String last_name) {
		api(null, null, "account.updateProfile", first_name, last_name);
	}
	
	public void api_account_updateStatus(boolean offline) {
		api(null, null, "account.updateStatus", offline);
	}
	
	public void api_messages_setTyping(TL.Object peer, boolean typing) {
		api(null, null, "messages.setTyping", peer, typing);
	}
	
	public void api_messages_deleteMessages(TL.Vector id) {
		api(null, null, "messages.deleteMessages", id);
	}
	
	public void api_messages_getFullChat(int chat_id) {
		api(null, null, "messages.getFullChat", chat_id);
	}	
}
