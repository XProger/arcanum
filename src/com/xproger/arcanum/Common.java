package com.xproger.arcanum;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneNumberUtils;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Common {
	public static final int COLOR_MASK1 = 0xff00ff; 
	public static final int COLOR_MASK2 = 0x00ff00;  
	
	public static final float BYTE_INV = 1.0f / 255.0f;
	public static final int UNIX_YEAR = 60 * 60 * 24 * 365;
	public static final String LOG_TAG = "arcanum";	
	public static final SecureRandom random = new SecureRandom();
	public static final Uri soundDef = Uri.parse("android.resource://com.xproger.arcanum/" + R.raw.sound_a);
	
	public static MTProto mtp;
	
	public static BigInteger rho(BigInteger n) {   
		ECM e = new ECM(n);
		e.factorize();
		return e.PD[0];
	}
	/*
	public static BigInteger rho(BigInteger n) {		
		final BigInteger ONE  = new BigInteger("1");		
		BigInteger x = n.add(BigInteger.TEN); // new BigInteger(n.bitLength(), random);
		BigInteger y = ONE;
		BigInteger z = new BigInteger("2");
		BigInteger divisor;
		BigInteger i = BigInteger.ZERO;   
		
		do {			
			if (i.equals(z)) {
				y = x;
				z = z.shiftLeft(1);
			}			
			x = x.multiply(x).add(ONE).mod(n);		
			i = i.add(ONE);	
			divisor = x.subtract(y).gcd(n);
		} while((divisor.equals(ONE)));
		return divisor;		
	}
	*/
/*	
	public static BigInteger rho(BigInteger N) {
		BigInteger divisor;
		BigInteger c  = new BigInteger(N.bitLength(), random);
		BigInteger x  = new BigInteger(N.bitLength(), random);
		BigInteger xx = x;

		// check divisibility by 2			
		if (N.mod(TWO).compareTo(ZERO) == 0) return TWO;

		do {
			x  =  x.multiply(x).mod(N).add(c).mod(N);
			xx = xx.multiply(xx).mod(N).add(c).mod(N);
			xx = xx.multiply(xx).mod(N).add(c).mod(N);
			divisor = x.subtract(xx).gcd(N);
		} while((divisor.compareTo(ONE)) == 0);

		return divisor;
	}		
*/
	public static class AES {
		byte[] key, iv; 
		
		public void prepare(byte[] cl_nonce, byte[] sv_nonce, byte[] new_nonce) {
			key	= new byte[32];
			iv	= new byte[32];
		// tmp_aes_key
			byte[] tmp_aes_key_1 = getSHA1(ASUM(new_nonce, sv_nonce));
			byte[] tmp_aes_key_2 = getSHA1(ASUM(sv_nonce, new_nonce));
			System.arraycopy(tmp_aes_key_1, 0, key, 0, 20);
			System.arraycopy(tmp_aes_key_2, 0, key, 20, 12);
		// tmp_aes_iv
			byte[] tmp = ASUM(new_nonce, new_nonce);
			byte[] tmp_aes_iv_1 = getSHA1(tmp); 
			System.arraycopy(tmp_aes_key_2, 12, iv, 0, 8);
			System.arraycopy(tmp_aes_iv_1, 0, iv, 8, 20);
			System.arraycopy(tmp, 0, iv, 28, 4);			
		}
		
		public void prepare(Boolean client, byte[] msg_key, byte[] auth_key) {
			int x = client ? 0 : 8;
			
			byte[] sha1_a = getSHA1(ASUM(msg_key, ASUB(auth_key, x, 32)));
			byte[] sha1_b = getSHA1(ASUM( ASUM( ASUB(auth_key, 32+x, 16), msg_key), ASUB(auth_key, 48+x, 16)));
			byte[] sha1_c = getSHA1(ASUM( ASUB(auth_key, 64+x, 32), msg_key));
			byte[] sha1_d = getSHA1(ASUM( msg_key, ASUB(auth_key, 96+x, 32)));
			key = ASUM( ASUM( ASUB(sha1_a, 0, 8), ASUB(sha1_b, 8, 12)), ASUB(sha1_c, 4, 12));
			iv  = ASUM( ASUM( ASUM( ASUB(sha1_a, 8, 12), ASUB(sha1_b, 0, 8)), ASUB(sha1_c, 16, 4)), ASUB(sha1_d, 0, 8));
		}
		
		public byte[] IGE(byte[] message, Boolean encrypt) {		
			try {
				Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
				cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
				
				final int blocksize = cipher.getBlockSize();
				
				byte[] xPrev = ASUB(iv, 0, blocksize);
				byte[] yPrev = ASUB(iv, blocksize, blocksize);
				if (encrypt) {
					byte[] tmp = xPrev;
					xPrev = yPrev;
					yPrev = tmp;			
				}
	
				byte[] decrypted = new byte[message.length];
	
				byte[] t, x = new byte[blocksize], p = new byte[blocksize], c = new byte[blocksize];
				int count = 0;
				int i = 0;
				final int len = message.length;
				while (i < len) {
					for (int j = 0; j < blocksize; j++) {
						x[j] = message[i++]; 
						p[j] = (byte)(x[j] ^ yPrev[j]);
					}
					cipher.doFinal(p, 0, blocksize, c, 0);					
					for (int j = 0; j < blocksize; j++)
						yPrev[j] = decrypted[count++] = (byte)(c[j] ^ xPrev[j]);
					
					t = xPrev;
					xPrev = x;
					x = t;
					
/*				// тормозной, но наглядный вариант 8)
					x = ASUB(message, i, blocksize);
					y = AXOR(cipher.doFinal(AXOR(x, yPrev)), xPrev);
					xPrev = x;
					yPrev = y;
					decrypted = ASUM(decrypted, y); 					
*/
				}
	
				return decrypted;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}		
	}
		
	public static interface Page {
		public void onMenuInit();
		public void onMenuClick(View view, int id);
	}
	
	public static class ByteBufferBackedInputStream extends InputStream {
		ByteBuffer buf;

		public ByteBufferBackedInputStream(ByteBuffer buf) {
			this.buf = buf;
		}

		public int read() throws IOException {
			if (!buf.hasRemaining()) {
				return -1;
			}
			return buf.get() & 0xFF;
		}

		public int read(byte[] bytes, int off, int len) throws IOException {
			if (!buf.hasRemaining()) {
				return -1;
			}
			len = Math.min(len, buf.remaining());
			buf.get(bytes, off, len);
			return len;
		}
	}
	
	public static interface MediaReq {
		public void onMedia(Uri media);
	}
	
	public static class MediaActivity extends ActionBarActivity {
		public SparseArray<MediaReq> mediaReq = new SparseArray<MediaReq>(); 	
		private Uri mCapturedImageURI;
		private boolean crop;
		private int outWidth, outHeight;
		
		public String getTempPath() {			
			String path;
			while (true) {
				int id = Math.abs(Common.random.nextInt());
				path = Main.MEDIA_PATH + id + ".jpg";
				File f = new File(path);
				if (!f.exists())
					break;
			}
			return path;
		}
		
		public void mediaPhoto(boolean take, boolean crop, MediaReq req) {
			mediaPhoto(take, crop ? 160 : 0, 160, req);
		}

		public void mediaPhoto(boolean take, int outWidth, int outHeight, MediaReq req) {
			crop = outWidth != 0 && outHeight != 0;
			this.outWidth = outWidth;
			this.outHeight = outHeight;
			Intent intent = new Intent(take ? MediaStore.ACTION_IMAGE_CAPTURE : Intent.ACTION_PICK);//, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);	
			
	//		ContentValues values = new ContentValues();  								
//			values.put(MediaStore.Images.Media.TITLE, "temp.jpg");		
			  //	  mCapturedImageURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

			if (take) {
				mCapturedImageURI = Uri.fromFile(new File(getTempPath()));
				intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
			} else {
				mCapturedImageURI = null;
				intent.setType("image/*");				
			}
				
			/*if (take) {				
								
				  
			} else {
				mCapturedImageURI = null;
				
			}*/
			/*
			if (crop) {
				intent.putExtra("crop", "true");
				intent.putExtra("outputX", 160);
				intent.putExtra("outputY", 160);
				intent.putExtra("aspectX", 1);
				intent.putExtra("aspectY", 1);
				intent.putExtra("scale", true);
				intent.putExtra("scaleUpIfNeeded", true);				
				intent.putExtra("noFaceDetection", false);				
			//intent.putExtra("return-data", true);
			} 
			*/
//			intent.putExtra("return-data", true);

			if (take || crop) {
				/*
				ContentValues values = new ContentValues();  								
				values.put(MediaStore.Images.Media.TITLE, "temp.jpg");					 
				mCapturedImageURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
				*/
			//	mCapturedImageURI = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "tmp_contact_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));
			//	intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mCapturedImageURI);	
				//mCapturedImageURI = null;
				//intent.setDataAndType(mCapturedImageURI, "image/*");
				//intent.putExtra(Intent.EXTRA_STREAM, mCapturedImageURI);
				//intent.putExtra("mimeType", "image/jpg");
			} else {
				mCapturedImageURI = null;
				intent.setType("image/*");
			}
			
			int key = getKey();
			mediaReq.put(key, req);

			try {
				startActivityForResult(intent, key);
			} catch (Exception e) {
				e.printStackTrace();
				mediaReq.remove(key);
			}
		}
		
		public int getKey() {
			int key = Common.random.nextInt() & 0xffff;
			while (key <= 0 || mediaReq.get(key) != null || key == 999)
				key = Common.random.nextInt() & 0xffff;			
			return key;
		}
		
		public void mediaVideo(boolean take, MediaReq req) {
			mCapturedImageURI = null;
			Intent intent = new Intent(take ? MediaStore.ACTION_VIDEO_CAPTURE : Intent.ACTION_PICK);	
			if (take == false)
				intent.setType("video/*");
			int key = getKey();
			mediaReq.put(key, req);
			startActivityForResult(intent, key); 
		}		
				
		public void mediaCrop(MediaReq req, Uri fileUri) {
			mCapturedImageURI = fileUri;
			crop = false;
			Intent intent = new Intent("com.android.camera.action.CROP");
			intent.setDataAndType(mCapturedImageURI, "image/*");
			intent.putExtra("crop", "true");
			intent.putExtra("outputX", outWidth);
			intent.putExtra("outputY", outHeight);
			intent.putExtra("aspectX", outWidth);
			intent.putExtra("aspectY", outHeight);
			intent.putExtra("scale", true);
			intent.putExtra("scaleUpIfNeeded", true);				
			intent.putExtra("noFaceDetection", false);	
			intent.putExtra("return-data", false);
			
			mCapturedImageURI = Uri.fromFile(new File(getTempPath()));
			intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

			int key = getKey();
			
			mediaReq.put(key, req);
			try {
				startActivityForResult(intent, key);
			} catch (Exception e) {
				e.printStackTrace();
				mediaReq.remove(key);
				req.onMedia(mCapturedImageURI); 
			}			
		}
		
		public void mediaRingtone(Uri sound, MediaReq req) {
			Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);	
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false); 
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true); 
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
			
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, soundDef);
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundDef.toString().equals(sound.toString()) ? Settings.System.DEFAULT_RINGTONE_URI : sound );
			
			int key = 999;
			mediaReq.put(key, req);
			try {
				startActivityForResult(intent, key);
			} catch (Exception e) {
				e.printStackTrace();
				mediaReq.remove(key);
			}	
		}		
		
		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent intent) { 
			super.onActivityResult(requestCode, resultCode, intent); 
			if (resultCode != RESULT_OK) return;
						
			MediaReq req = mediaReq.get(requestCode);
			if (req == null) return;
			mediaReq.remove(requestCode);
			
			if (requestCode == 999) {
				Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				if (uri != null) 
					req.onMedia(uri);												
				return;
			}
			
			mCapturedImageURI = mCapturedImageURI != null ? mCapturedImageURI : intent.getData();
			if (crop)
				mediaCrop(req, mCapturedImageURI);
			else {
				Common.logError("return media");
				Common.logError( Common.getPathFromURI(Main.main, mCapturedImageURI) );				
				req.onMedia(mCapturedImageURI);
			}
		}	
		  
	}

	
	public static void logError(String message) {
		Log.e(LOG_TAG, message);
	}
	
	public static void logInfo(String message) {
		Log.i(LOG_TAG, message);
	}
	
	public static void logWarning(String message) {
		Log.w(LOG_TAG, message);
	}
	
	public static void logDebug(String message) {
		Log.d(LOG_TAG, message);
	}
	
	public static int getRotation(Context ctx, String fileName) throws Exception {
		/*
		if (image.getScheme().equals("content")) {
			String[] projection = { Images.ImageColumns.ORIENTATION };
			Cursor c = ctx.getContentResolver().query(image, projection, null, null, null);
			if (c.moveToFirst())
				return c.getInt(0);		
		}
		*/
		//if (image.getScheme().equals("file")) {
		try {
			ExifInterface exif = new ExifInterface(fileName);//Common.getPathFromURI(ctx, image));
			switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
				case ExifInterface.ORIENTATION_ROTATE_270:
					return 270;
				case ExifInterface.ORIENTATION_ROTATE_180:
					return 180;
				case ExifInterface.ORIENTATION_ROTATE_90:
					return 90;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//}
		
		return 0;
	}	
	
	public static void loadBitmapThread(final Activity activity, ImageView imageView, final String fileName, final int width, final int height) {
		if (imageView == null)
			return;
		final WeakReference<ImageView> ref = new WeakReference<ImageView>(imageView);
		
		new Thread( new Runnable() {
			@Override
			public void run() {								
				try {					
					final Bitmap bitmap = loadBitmap(activity, fileName, width, height, true, true, false);
					activity.runOnUiThread(new Runnable() {
						public void run() {
							ImageView img = (ImageView)ref.get();
							if (img != null)
								img.setImageBitmap(bitmap);
						}
					});	
				} catch (Exception e) {}				
			}
		} ).start();
	}
	
	public static void loadBitmapThread(final Activity activity, ImageView imageView, final String fileName) {
		if (imageView == null)
			return;
		final WeakReference<ImageView> ref = new WeakReference<ImageView>(imageView);

		new Thread( new Runnable() {
			@Override
			public void run() {								
				try {									
					final Bitmap bitmap = BitmapFactory.decodeFile(fileName);
					activity.runOnUiThread(new Runnable() {
						public void run() {
							ImageView img = (ImageView)ref.get();							
							if (img != null)
								img.setImageBitmap(bitmap);							
						}
					});	
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
		} ).start();
	}	

	private static void applyRotate(int angle, Point size) {		
		if ((angle / 90) % 2 > 0) {
			int tmp = size.x;
			size.x = size.y;
			size.y = tmp;
		}
	}
	
	public static Bitmap loadBitmap(Context ctx, String fileName, int maxWidth, int maxHeight, boolean crop, boolean trueColor, boolean resave) throws Exception {
		int angle = getRotation(ctx, fileName);
		Point maxSize = new Point(maxWidth, maxHeight);
		applyRotate(angle, maxSize);
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;		
		options.inSampleSize = 1;
		if (!trueColor)
			options.inPreferredConfig = Bitmap.Config.RGB_565;

		BitmapFactory.decodeFile(fileName, options);	
				
		boolean changed = false;
		
		if (options.outWidth > maxSize.x || options.outHeight > maxSize.y) {
			final int widthRatio = Math.round((float)options.outWidth / (float)maxSize.x);
			final int heightRatio = Math.round((float)options.outHeight / (float)maxSize.y);
			options.inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
			changed = true;
		}
		options.inJustDecodeBounds = false;
		
		Common.logError("load: " + fileName + " " +options.outWidth + " " + options.outHeight);
		
		Bitmap photo = BitmapFactory.decodeFile(fileName, options);
				
		boolean flag = crop && (photo.getWidth() < maxSize.x || photo.getHeight() < maxSize.y);
		// resize
		if ((photo.getWidth() > maxSize.x || photo.getHeight() > maxSize.y) || flag) {		
			float scale = 1;
			/*
			if (flag) {
				scale = Math.max((float)maxSize.x / (float)photo.getWidth(), (float)maxSize.y / (float)photo.getHeight());
			} else*/ 				
				scale = Math.min((float)maxSize.x / (float)photo.getWidth(), (float)maxSize.y / (float)photo.getHeight());
			
			int w = Math.round((float)photo.getWidth() * scale);
			int h = Math.round((float)photo.getHeight() * scale);
			
			photo = Bitmap.createScaledBitmap(photo, w, h, true);
			
			if (crop) {
				w = photo.getWidth();
				h = photo.getHeight();
				scale = Math.min((float)w / (float)maxSize.x, (float)h / (float)maxSize.y);			
				maxSize.x *= scale;
				maxSize.y *= scale;
				Common.logError("to " + maxSize.x + " " + maxSize.y);
				
				int dx = (w - maxSize.x) / 2;
				int dy = (h - maxSize.y) / 2;			
				
				photo = Bitmap.createBitmap(photo, dx, dy, maxSize.x, maxSize.y, null, false);
			}
			
			changed = true;
		}	
		
		// rotate
		if (angle > 0) {
			Matrix matrix = null;
			matrix = new Matrix();
			matrix.preRotate(angle);
			Point rotSize = new Point(photo.getWidth(), photo.getHeight());
			applyRotate(angle, rotSize);
			photo = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);
			changed = true;
		}
		
		if (resave && changed) {
			logError("resave to jpeg");
			saveJPEG(fileName, photo, 87);
		}
		
		return photo;
	}	
	
	public static Bitmap imageScale(Bitmap photo, int maxWidth, int maxHeight) {
		 // resize
		Point maxSize = new Point(maxWidth, maxHeight);
		
		if (photo.getWidth() > maxSize.x || photo.getHeight() > maxSize.y) {		
			Point eSize = new Point(photo.getWidth(), photo.getHeight());
			
			float scale;			
			if (eSize.x > eSize.y)
				scale = (float)maxSize.x / (float)photo.getWidth();
			else
				scale = (float)maxSize.y / (float)photo.getHeight();
									
			int w = (int)((float)photo.getWidth() * scale);
			int h = (int)((float)photo.getHeight() * scale);
			
			photo = Bitmap.createScaledBitmap(photo, w, h, true);	
		}	

		return photo;
	}
	
	public static boolean downloadFile(String urlStr, String fileName) {
		try {
			URL url = new URL(urlStr);
			URLConnection connection = url.openConnection();
			connection.connect();			
			InputStream input = new BufferedInputStream(url.openStream());
			OutputStream output = new FileOutputStream(fileName);
			
			byte data[] = new byte[1024];
			int count;
			while ((count = input.read(data)) != -1)
				output.write(data, 0, count);			
			
			output.flush();
			output.close();
			input.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}		
	}
	
	public static Bitmap downloadBitmap(String urlStr) {
		try {
			return BitmapFactory.decodeStream((new URL(urlStr)).openConnection().getInputStream());
		} catch (Exception e) {
			return null;
		}
	}
	
	public static boolean saveJPEG(String fileName, Bitmap bmp, int quality) {
		File file = new File(fileName);
		if (file.exists())
			file.delete();		
		try {
			FileOutputStream out = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.JPEG, quality, out);
			out.flush();
			out.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean savePNG(String fileName, Bitmap bmp, int quality) {
		File file = new File(fileName);
		if (file.exists())
			file.delete();		
		try {
			FileOutputStream out = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.PNG, quality, out);
			out.flush();
			out.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static byte[] AXOR(byte[] a, byte[] b) {
		if (a.length != b.length) 
			System.out.println("ArrayXOR error");
		byte[] res = new byte[a.length];
		for (int i = 0; i < a.length; i++)
			res[i] = (byte)(a[i] ^ b[i]);
		return res;
	}
	
	public static byte[] ASUM(byte[] a, byte[] b) {
		byte[] res = new byte[a.length + b.length];
		System.arraycopy(a, 0, res, 0, a.length);
		System.arraycopy(b, 0, res, a.length, b.length);		
		return res;
	}
	
	public static byte[] ASUB(byte[] a, int offset, int length) {
		byte[] res = new byte[length];
		System.arraycopy(a, offset, res, 0, length);
		return res;
	}	
	
	public static byte[] HexToBytes(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
								 + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
		
	public static String BytesToHex(byte[] bytes) {
		final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};		
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for ( int j = 0; j < bytes.length; j++ ) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	public static String join(List<?> list, String separator) {
		String str = "";
		int count = list.size();
		for (int i = 0; i < count; i++)
			if (i < count - 1)
				str += list.get(i) + separator;
			else
				str += list.get(i);
		return str;
	}
	
	public static byte[] RSA(String m_hex, String e_hex, byte[] data) {
		try {
			BigInteger modulus = new BigInteger(m_hex, 16);
			BigInteger pubExp = new BigInteger(e_hex, 16);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");			
			RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(modulus, pubExp);			
			RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);
			Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, pubKey);
			return cipher.doFinal(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] getSHA1(byte[] data) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			sha1.reset();
			sha1.update(data);
			return sha1.digest();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] getMD5(byte[] data) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.reset();
			md5.update(data);
			return md5.digest();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static ByteBuffer gzipInflate(byte[] buf) {		
		try {
			GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(buf));			
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			byte[] data = new byte[1024];
			int count = is.read(data);
			while (count > 0) {
				out.write(data, 0, count);
				count = is.read(data);
			}
			
			is.close();
			ByteBuffer outBuf = ByteBuffer.wrap(out.toByteArray());
			outBuf.order(ByteOrder.LITTLE_ENDIAN);
		//	Common.logError("gzip: uncompress " + buf.length + " -> " + outBuf.remaining());
			
			return outBuf;			
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return null;
	}
	
	public static byte[] gzipDeflate(byte[] buf) {		
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();			
			GZIPOutputStream gos = new GZIPOutputStream(os);					
			gos.write(buf);
			gos.close();
			byte[] compressed = os.toByteArray();
			os.close();
		//	Common.logError("gzip: compress " + buf.length + " -> " + compressed.length);
			return compressed;
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return null;
	}	
	
	public static byte[] getBytes(ByteBuffer b, int count) {
		byte[] data = new byte[count];
		b.get(data);
		return data;
	}
	
	public static byte[] getStr(ByteBuffer b) {
		int len = (short)(b.get() & 0xff);
		if (len == 254)
			len = (b.get() & 0xff) | ((b.get() & 0xff) << 8) | ((b.get() & 0xff) << 16);	
		byte[] data = new byte[len];
		b.get(data);
		if (len > 253) len--;
   		while (++len % 4 > 0) b.get();
		return data;
	}
	
	public static void putStr(ByteBuffer b, byte[] data) {
		int len = data.length;
		if (len <= 253)
			b.put((byte)len);
		else
			b.putInt(len << 8 | 0xFE);
		b.put(data);
		if (len > 255) len--;
		while (++len % 4 > 0) b.put((byte)0);
	}	
	
	public static BigInteger getBigInt(ByteBuffer b) {
		return new BigInteger(1, getStr(b));
	}
	
	public static void putBigInt(ByteBuffer b, BigInteger value) {
		putStr(b, value.toByteArray());
	}	
	
	public static long toLong(byte[] data) {
		if (data.length != 8) {
			Common.logError("toLong invalid size");
			return 0;
		}
		ByteBuffer b = ByteBuffer.allocate(8);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.put(data);
		b.position(0);
		return b.getLong();		
	}
	
	public static byte[] toBytes(BigInteger value) {
		byte[] data = value.toByteArray();		
		for (int i = 0; i < data.length; i++)
			if (data[i] != 0) {
				if (i > 0) data = ASUB(data, i, data.length - i);
				break;
			}			
		if (data.length == 0) data = new byte[]{0};
		return data;		
	}
	
	public static void print_dump(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			if (i % 16 == 0)
				System.out.printf("\n %04X | ", i & 0xff);
			System.out.printf("%02X ", data[i] & 0xff);
		}
		System.out.printf("\n");
	}
	
	public static int getUnixTime() {
		return (int)(System.currentTimeMillis() / 1000L);
	}
	
	public static String formatPhone(String phone) {
	/* i18n... до лучших времён
		PhoneNumberUtil phoneUtil = PhoneNumberUtils.getInstance();
		PhoneNumber number = phoneUtil.parse(phone, Locale.getDefault().getCountry());
		phone = phoneUtil.format(number, PhoneNumberFormat.E164);
	*/		
		phone = phone.replaceAll("[^\\d]", "");
		switch (phone.length()) { // да, да, да...
			case 11 :
				if (phone.startsWith("7"))
					phone = phone.replaceFirst("(\\d{1})(\\d{3})(\\d{3})(\\d{2})(\\d{2})", "$1 ($2) $3-$4-$5");
				break;				
		}		
		return "+" + phone;
	}
	
	public static String phoneClear(String phone) {
		if (phone.startsWith("8"))	// Welcome to Russia
			phone = phone.replaceFirst("8", "7");
		return "+" + phone.replaceAll( "[^\\d]", "" );
	}
	
	public static String getPathFromURI(Context context, Uri contentUri) {		
		String path;
		Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null); 
		try {
			cursor.moveToFirst();
			int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
			path = cursor.getString(idx);
		} catch (Exception e) {
			path = contentUri.getPath();
		}
		if (cursor != null)
			cursor.close();
		return path;
		
		/*
		try {
			String[] proj = { MediaStore.Images.Media.DATA };
			CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
			Cursor cursor = loader.loadInBackground();
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} catch (Exception e) {
			return contentUri.getPath();
		}
		*/
	}
	
	public static String extractFileName(String path) {
		return path.substring(path.lastIndexOf('/') + 1, path.length());
	}
	
	public static String extractFileNameNoExt(String path) {
		String name = extractFileName(path);
		return name.substring(0, name.lastIndexOf('.'));		
	}
	
	public static void fileRename(String src, String dst) {
		Common.logError("file rename " + src + " to " + dst);
		File from = new File(src);
		if (from.exists()) {
			if (from.renameTo(new File(dst))) 				
				Common.logError("done");
			else
				Common.logError("error rename");
		}
	}
	
	public static void fileDelete(String fileName) {
		File file = new File(fileName);
		if (file.exists())
			file.delete();
	}	
	
	public static boolean fileExists(String fileName) {
		File file = new File(fileName);
		return file.exists(); 
	}	
	

	public static void keyboardShow() {
		InputMethodManager imm = (InputMethodManager)Main.main.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(Main.main.mainView, 0);		
	}	
	
	public static void keyboardHide() {
		InputMethodManager imm = (InputMethodManager)Main.main.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(Main.main.mainView.getApplicationWindowToken(), 0);		
	}
		
	public static View getListItem(ListView lv, int position) {		
		int index = position - lv.getFirstVisiblePosition() + lv.getHeaderViewsCount();
		if (index < 0 || index >= lv.getChildCount())
			return null;
		return lv.getChildAt(index);		
	}
	
	public static void setListViewHeightBasedOnChildren(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null)
			return;		
		int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.AT_MOST);
		int totalHeight = 0;
		View view = null;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			view = listAdapter.getView(i, view, listView);
			if (i == 0)
				view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, LayoutParams.WRAP_CONTENT));		   
			view.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
			totalHeight += view.getMeasuredHeight();
		}
		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}	
	
	public static void deleteFolder(File folder) {
		if (folder.isDirectory())
			for (File child : folder.listFiles())
				deleteFolder(child);
		folder.delete();
	}
	
	public static float dipToPixels(float dipValue) {
		DisplayMetrics metrics = Main.main.getResources().getDisplayMetrics();
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
	}
	
	public static void vibrate(int duration) {
		Vibrator mVibrate = (Vibrator)Main.main.getSystemService(Context.VIBRATOR_SERVICE);
		mVibrate.vibrate(duration);		
	}
	
	public static Uri getSoundURI(Context context, String snd) {
		try {
			Uri uri = Uri.parse(snd);
			Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
			return ringtone == null ? Common.soundDef : uri; 
		} catch (Exception e) {
			return Common.soundDef;
		}
	}
	
	private static MediaPlayer snd_msg;
	private static String snd_path = "";
	
	public static void playSound(Context context, Uri uri) {
		if (uri == null) return;
		
		if (snd_msg == null || !snd_path.equals(uri.toString())) {
			try {
				snd_msg = new MediaPlayer();
				snd_msg.setDataSource(context, uri);
				snd_msg.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
				snd_msg.prepare();
				snd_path = uri.toString();
			} catch (Exception e) {
				return;
			}
		}		
		snd_msg.start();
	}	
	
	public static void clipboardCopy(String text) {
		ClipboardManager cm = (ClipboardManager)Main.main.getSystemService(Main.main.CLIPBOARD_SERVICE);
		cm.setText(text);
		Toast.makeText(Main.main, Main.main.getResources().getString(R.string.msg_copy), Toast.LENGTH_SHORT).show();		
	}
	
	
	
	public static TL.Object getPhotoSizeTL(TL.Vector sizes, String type) {
		for (int i = 0; i < sizes.count; i++) {
			TL.Object item = sizes.getObject(i);
			Common.logError( String.format("%s %d %d", item.getString("type"), item.getInt("w"), item.getInt("h")) );			
			if (type.equals(item.getString("type")))
				return item;						
		}
		return null;
	}
	
	public static void loadImageTL(ImageView image, TL.Object size, final int width, final int height, final boolean crop, final boolean cache) {
		if (size == null || size.id == 0xe17e23c) {
			Common.logError("no size");
			return;
		}

		final TL.Object location = size.getObject("location");
		final WeakReference<ImageView> ref = new WeakReference<ImageView>(image);
		
		image.setTag(location);
		
		final String path = FileQuery.exists(location);  
				
		if (path != null) {
			Bitmap bmp = BitmapCache.get(path);
			if (bmp != null) {
				image.setImageBitmap(bmp);
				return;
			}
		}
							
		image.setImageResource(0);
						
		if (path != null) {			
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						final Bitmap bmp = cache ?  BitmapCache.loadBitmap(path, width, height, crop, true, false) : 
													Common.loadBitmap(Main.main, path, width, height, crop, true, false);
						final ImageView img = ref.get();
						if (img == null)
							return;
						
						img.post(new Runnable() {
							@Override
							public void run() {
								if (img.getTag() == location)
									img.setImageBitmap(bmp);									
							}
						});													
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
				}				
			}).start();			
			return;
		}
				

		try {
			new FileQuery(location, null, new FileQuery.OnResultListener() {			
				@Override
				public void onResult(TL.Object result) {
					if (result == null)
						return;
					
					try {
						String path = result.getString("fileName");

						final Bitmap bmp = cache ?  BitmapCache.loadBitmap(path, width, height, crop, true, true) : 
													Common.loadBitmap(Main.main, path, width, height, crop, true, true);
						
						final ImageView img = ref.get();
						if (img == null)
							return;
						
						
						img.post(new Runnable() {
							@Override
							public void run() {
								if (img.getTag() == location)
									img.setImageBitmap(bmp);									
							}
						});	

					} catch (Exception e) {
						Common.logError("error loading bitmap");
						e.printStackTrace();
					}										
				}

				@Override
				public void onProgress(float progress) {}
			});
		} catch (Exception e) {			
			Common.logError("error query file");
			e.printStackTrace();
		}				
	}
	
	public static int lerpColor(int a, int b, float t) {	
		int f2 = (int)(255 * t);
		int f1 = 255 - f2;
		return   ((((( a & COLOR_MASK1 ) * f1 ) + ( ( b & COLOR_MASK1 ) * f2 )) >> 8 ) & COLOR_MASK1 ) 
			   | ((((( a & COLOR_MASK2 ) * f1 ) + ( ( b & COLOR_MASK2 ) * f2 )) >> 8 ) & COLOR_MASK2 );		
	}
}
