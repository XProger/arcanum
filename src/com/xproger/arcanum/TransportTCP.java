package com.xproger.arcanum;

import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

public class TransportTCP extends Transport {
	private Socket socket;
	public Boolean lite;
	
	public TransportTCP(MTProto mtp, Boolean lite) {
		super(mtp);
		this.lite = lite;
	}

	public void close() {
		try {
			if (socket != null)
				socket.close();
		} catch (Exception e) {}
		super.close();
	}	
	
	@Override
	protected Boolean connect() {
		try {
			socket = new Socket(mtp.dcState.getString("ip"), mtp.dcState.getInt("port"));		
			
			socket.setReceiveBufferSize(1024 * 1024);
			socket.setSendBufferSize(1024 * 1024);
			
			if (lite)
				socket.getOutputStream().write(0xef);
			mtp.onConnect();
			return true;
		} catch (Exception e) {
			try { Thread.sleep(3000); } catch (Exception ex) {}
			return false;
		}
	}
	
	public void disconnect() {
		try {
			socket.close();
		} catch (Exception e) {};
	}
			
	@Override
	protected void recvLoop() throws Exception {
		int readed, len, count = 0; 		
		byte[] data, buf = new byte[1024 * 64];
		ByteBuffer b = ByteBuffer.wrap(buf);
		b.order(ByteOrder.LITTLE_ENDIAN);
				
		InputStream in = socket.getInputStream();
		while ( (readed = in.read( buf, count, buf.length - count )) > 0 ) {
			count += readed;
			while (true) {
				b.position(0);
				if (lite) {
					len = b.get();
					if (len == 0x7f) {
						b.position(b.position() - 1);					
						len = (b.getInt() & 0xFFFFFF00) >> 8;
					}
					len *= 4;
					
					if (count >= b.position() + len) {
						data = new byte[len];
						b.get(data);
					} else
						break;
				} else
					if (count > 12 && count >= (len = b.getInt())) {					
						CRC32 crc32 = new CRC32();
						crc32.update(buf, 0, len - 4);
						
						b.position(len - 4);
						if (b.getInt() != (int)crc32.getValue()) {
							Common.logError("invalid packet CRC32");
							disconnect();
						}

						data = new byte[len - 12];
						System.arraycopy(buf, 8, data, 0, len - 12);					
					} else
						break;					
				count -= b.position();
				System.arraycopy(buf, b.position(), buf, 0, count);
				
				ByteBuffer packet = ByteBuffer.wrap(data);
				packet.order(ByteOrder.LITTLE_ENDIAN);
				Common.logDebug(String.format("recv packet: %d bytes", packet.remaining()));
				mtp.onReceive(packet);				
			}
			/*
			b.position(0);
			if (lite) {
				len = b.get();
				if (len == 0x7f) {
					b.position(b.position() - 1);					
					len = (b.getInt() & 0xFFFFFF00) >> 8;
				}
				len *= 4;
				
				if (pos >= b.position() + len) {
					data = new byte[len];
					System.arraycopy(buf, b.position(), data, 0, len);
					System.arraycopy(buf, b.position() + len, buf, 0, pos -= b.position() + len);
				} else
					continue;				
			} else
				if (pos > 12 && pos >= (len = b.getInt())) {					
					CRC32 crc32 = new CRC32();
					crc32.update(buf, 0, len - 4);
					
					b.position(len - 4);
					if (b.getInt() != (int)crc32.getValue()) {
						Common.logError("invalid packet CRC32");
						break;
					}
	
					data = new byte[len - 12];
					System.arraycopy(buf, 8, data, 0, len - 12);
					System.arraycopy(buf, len, buf, 0, pos -= len);					
				} else
					continue;

			
			ByteBuffer packet = ByteBuffer.wrap(data);
			packet.order(ByteOrder.LITTLE_ENDIAN);
			Common.logDebug(String.format("recv packet: %d bytes", packet.remaining()));
			mtp.onReceive(packet);
			*/
		}
		close();
	}

	public void send(byte[] data) throws Exception {
		byte[] packet_data = new byte[data.length + 4 + 4 + 4];			
		ByteBuffer b = ByteBuffer.wrap(packet_data);
		b.order(ByteOrder.LITTLE_ENDIAN);
		
		if (lite) {
			int len = data.length / 4;
			if (len < 127)
				b.put((byte)len);
			else
				b.putInt(len << 8 | 0x7f);		
			b.put(data);
		} else {		
			b.putInt(packet_data.length);
			b.putInt(msg_num);
			b.put(data);
			CRC32 crc32 = new CRC32();
			crc32.update(packet_data, 0, packet_data.length - 4);
			b.putInt((int)crc32.getValue());
		}
		
		Common.logDebug(String.format("send packet: %d bytes", data.length));
		//print_dump(data);
		synchronized (socket) {
			socket.getOutputStream().write(packet_data, 0, b.position());
			socket.getOutputStream().flush();
		}
		msg_num++;
	//	Thread.sleep(1000);
	}
}