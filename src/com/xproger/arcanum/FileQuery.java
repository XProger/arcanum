package com.xproger.arcanum;

import java.io.File;
import java.io.RandomAccessFile;

public class FileQuery implements MTProtoListener, TL.OnResultRPC {
	private long file_id;
	private long size, sentSize;
	private MTProto mtp;
	private FilePart[] parts;
	private RandomAccessFile file;
	private OnResultListener onResultListener;
	private byte[] buf;
//	private MessageDigest md5;
	private String fileName, name;
	private TL.Object location = null;
	private int partSize;
	private long startTime;
	private boolean canceled;
/*
	private int mtime = 0;
	private TL.Object type;
*/
	public static interface OnResultListener {
		public void onResult(TL.Object result);
		public void onProgress(float progress);
	}
	
	public static class FilePart {
		int id, size;
		long offset;
		boolean done, wait;
		
		public FilePart(int id, long offset, int size) {
			this.id = id;
			this.offset = offset;
			this.size = size;
			done = false;
			wait = false;
		}
	}
	
	public static String getName(TL.Object loc) {
		return loc.getInt("dc_id") + "_" + loc.getLong("volume_id") + "_" + loc.getInt("local_id");
	}
	
	public static String getName(int dc_id, long id) {
		return dc_id + "_" + id + ".mp4";
	}	
	
	public static String exists(TL.Object loc) {
		String name = getName(loc);
		
		File file = new File(Main.CACHE_PATH + name);
		if (file.exists()) 
			return file.getAbsolutePath();
		
		file = new File(Main.MEDIA_PATH + name + ".jpg");
		if (file.exists()) 
			return file.getAbsolutePath();

		return null;
	}

	
	public static String exists(int dc_id, long id) {
		String name = getName(dc_id, id);
		File file = new File(Main.CACHE_PATH + name);
		if (file.exists()) 
			return file.getAbsolutePath();
		
		file = new File(Main.MEDIA_PATH + name);
		if (file.exists()) 
			return file.getAbsolutePath();		
		
		return null;
	}
	
	public FileQuery(String fileName, OnResultListener onResultListener) {
		try {
			file = new RandomAccessFile(new File(fileName), "r");
			size = file.length();
		} catch (Exception e) {
			onResultListener.onResult(null);
			return;
		}
		sentSize = 0;
		this.fileName = fileName;
		name = Common.extractFileName(fileName).toLowerCase();
		
		if (name.endsWith(".3gp") || name.endsWith(".3gpp") ||
			name.endsWith(".avi") || name.endsWith(".ogm") ||
			name.endsWith(".jet") || name.endsWith(".mkv")) name += ".mp4";
				
		this.onResultListener = onResultListener;
		mtp = MTProto.getConnection(MTProto.dc_this, this, MTProto.REUSE_UPLOAD);
		if (mtp.connected && mtp.bind)	// reused MTProto
			onBind();		
	}
	
// download videos
	public FileQuery(int dc_id, long id, long access_hash, long size, OnResultListener onResultListener) throws Exception {
		fileName = exists(dc_id, id); 
		if (fileName != null) {			
			this.size = new File(fileName).length();
			if (this.size > 0) {
				onResultListener.onResult( TL.newObject("joim.file", fileName, this.size) );			
				return;
			}
		}
		
		name = getName(dc_id, id);
		fileName = Main.MEDIA_PATH + name;
		file = new RandomAccessFile(fileName, "rw");		
		location = TL.newObject("inputVideoFileLocation", id, access_hash);		
		this.size = size;
		partSize = 32 * 1024;
		this.onResultListener = onResultListener;
		mtp = MTProto.getConnection(dc_id, this, MTProto.REUSE_DOWNLOAD);
		if (mtp.connected && mtp.bind)	// reused MTProto
			onBind();
	}
	
// download images
	public FileQuery(TL.Object fileLocation, String destPath, OnResultListener onResultListener) throws Exception {
		fileName = exists(fileLocation); 
		if (fileName != null) {			
			size = new File(fileName).length();
			if (size > 0) {
				onResultListener.onResult( TL.newObject("joim.file", fileName, size) );			
				return;
			}
		}

		int		dc_id		= fileLocation.getInt("dc_id");		
		long	volume_id	= fileLocation.getLong("volume_id");
		int		local_id	= fileLocation.getInt("local_id");		
		
		name = getName(fileLocation);
		fileName = destPath == null ? Main.CACHE_PATH + name : destPath;
		file = new RandomAccessFile(fileName, "rw");		
		location = TL.newObject("inputFileLocation", volume_id, local_id, fileLocation.getLong("secret"));		
		size = 0;

		partSize = 32 * 1024;
		this.onResultListener = onResultListener;
		mtp = MTProto.getConnection(dc_id, this, MTProto.REUSE_DOWNLOAD);
		if (mtp.connected && mtp.bind)	// reused MTProto
			onBind();
	}
	
	public void cancel() {
		try {
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (location != null)
			Common.fileDelete(fileName);		
		
		canceled = true;
		onResultListener.onResult(null);
	}
	
	public int getNextPart(int id) {
		boolean wait = false;
		for (int i = id; i < parts.length; i++)
			if (parts[i].wait)
				wait = true;		
			else
				if (!parts[i].done)
					return i;
		
		for (int i = 0; i < id; i++)
			if (parts[i].wait)
				wait = true;		
			else
				if (!parts[i].done)
					return i;
		
		return wait ? -2 : -1;
	}
	
	public void sendFilePart(int id) {
		if (canceled)
			return;
		Common.logInfo("file: send part " + id + " / " + parts.length + " / " + parts[0].size + " / " + size);
		if (id > -1) {	
			try {
				parts[id].wait = true;
				file.seek(parts[id].offset);
				file.read(buf, 0, parts[id].size);			
				byte[] data = parts[id].size == buf.length ? buf : Common.ASUB(buf, 0, parts[id].size);
				mtp.api(this, id, "upload.saveFilePart", file_id, id, data);
				//if (!parts[id].done)
				//    md5.update(data, 0, data.length);
				sentSize += parts[id].size;
				onResultListener.onProgress((float)((double)sentSize / (double)size));
			} catch (Exception e) {
				Common.logError("file: error reading");
				onResultListener.onResult(null);
				e.printStackTrace();				
			}
		} else {// all parts is done
			if (id == -2) // waiting other "threads"
				return;
			Common.logError("file send time: " + ((System.currentTimeMillis() - startTime) / 1000L));			
			onResultListener.onResult( TL.newObject("inputFile", file_id, parts.length, name, ""/*Common.BytesToHex(md5.digest())*/ ));
			try { file.close(); } catch (Exception e) {};
		}
	}
	
	public void recvFilePart() {
		Common.logInfo("file: recv part");
		mtp.api(this, null, "upload.getFile", location, (int)sentSize, partSize);			
	}	
	
	@Override
	public void onResultRPC(TL.Object result, Object param, boolean error) {
		if (canceled)
			return;
		
		if (location == null) {
			int id = (Integer)param;
			if (error || !result.name.equals("boolTrue")) {
				Common.logError("file: error sending file part " + id);
				onResultListener.onResult(null);
				return;
			}		
			parts[id].done = true;
			parts[id].wait = false;
			sendFilePart(getNextPart(id));
			return;
		}
			
		byte[] data = result.getBytes("bytes");		
		try {			
			//file.seek(size);
			sentSize += data.length;
			if (size != 0)
				onResultListener.onProgress((float)((double)sentSize / (double)size));
			else
				onResultListener.onProgress(sentSize);
			file.write(data);					
		} catch (Exception e) {
			Common.logError("file: can''t write");
			e.printStackTrace();
		}
		/*
		if (mtime == 0) {
			mtime	= result.getInt("mtime");
			type	= result.getObject("type");
		}
		*/
		if (data.length < partSize) {
			try { file.close(); } catch (Exception e) {};	
			onResultListener.onResult( TL.newObject("joim.file", fileName, size) );
		} else		
			recvFilePart();
	}

	@Override
	public void onRedirect(MTProto mtp) {
		this.mtp = mtp;
	}	
	
	@Override
	public void onConnected() {}

	@Override
	public void onDisconnected() {}

	@Override
	public void onError(String msg) {}

	@Override
	public void onReady() {}

	@Override
	public void onAuth(TL.Object user) {}

	@Override
	public void onBind() {
		if (location != null) {
			partSize = 32 * 1024;
			Common.logError("<- file \"" + name + "\"");
			recvFilePart();
			return;
		}
		
		startTime = System.currentTimeMillis();
		
		partSize = 128 * 1024; 
		while (size / partSize > 1000)
			partSize *= 2;

		if (partSize >= 512 * 1024) {
			Common.logError("file: too large: " + size + " / " + partSize);
			onResultListener.onResult(null);
			return;
		}

		try {
//			md5 = MessageDigest.getInstance("MD5");
//		    md5.reset();
		} catch (Exception e) {
			Common.logError("file: impossible md5 error");
			onResultListener.onResult(null);
			return;
		}

		buf = new byte[partSize];
		file_id = Common.random.nextLong();

	// prepare parts
		long offset = 0;
		parts = new FilePart[(int)( (size + partSize - 1) / partSize )];
		for (int i = 0; i < parts.length; i++) {
			parts[i] = new FilePart(i, offset, (int)Math.min((long)partSize, size - offset));
			offset += parts[i].size;
		}
		Common.logError("-> file \"" + name + "\" " + size + " / " + partSize + " = " + parts.length);
		
	// start "threads"
		for (int i = 0; i < 4; i++)
			sendFilePart(getNextPart(0));
	}

	@Override
	public void onMessage(TL.Object message, int msgFlag) {}

	@Override
	public void onUserName(int user_id, String first_name, String last_name) {}
}
