package com.xproger.arcanum;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.*;
import java.util.*;
import org.json.*;

import android.os.Environment;
import android.util.SparseArray;

public class TL {
	private static final int boolFalse	= 0xbc799737;
	private static final int boolTrue	= 0x997275b5;
	
	private static final int VAR_BOOL	= 0;
	private static final int VAR_INT	= 1;
	private static final int VAR_LONG	= 2;
	private static final int VAR_DOUBLE	= 3;
	private static final int VAR_STRING	= 4;
	private static final int VAR_BYTES	= 5;
	private static final int VAR_INT128	= 6;
	private static final int VAR_INT256	= 7;
	private static final int VAR_VECTOR	= 8;
	private static final int VAR_OBJECT	= 9;
	private static final int VAR_NULL	= 10;
	
	private static final int	VAR_ID[]	= {VAR_BOOL, VAR_INT, VAR_LONG, VAR_DOUBLE, VAR_STRING, VAR_BYTES, VAR_INT128, VAR_INT256, VAR_VECTOR, VAR_OBJECT, VAR_NULL};	
	private static final String	VAR_NAME[]	= {"Bool", "int", "long", "double", "string", "bytes", "int128", "int256", "Vector", "Object", "null"};
	private static ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);	

	public static SparseArray<Object> ctor_id;
	public static Map<String, Object> ctor_name;	
	
	public static interface OnResultRPC {
		public void onResultRPC(TL.Object result, java.lang.Object param, boolean error);
	}

	public static class Object {
		public static final boolean verbose = false;
		private static String nbsp = "";
		
		public int id, sub_id;
		public String name, type;
		public java.lang.Object value;		
		public Object params[];
		
		Object(int id, String name) {
			this.id		= id;
			this.name	= name;
			this.value  = null;
		}
		
		Object(ByteBuffer b, int naked_id) throws Exception {	
			id = naked_id == -1 ? b.getInt() : naked_id;
			
			if (id == 0x1cb5c415)	// Vector<t>
				return;
			
			Object tpl = ctor_id.get(id);
			if (tpl == null) {
				Common.logError("deserialization error for " + id);
				throw new Exception();
			}						
			name = tpl.name;
			type = tpl.type;
			params = new Object[tpl.params.length];
			
			if (verbose) {
				Common.logDebug(nbsp + type + ": " + name + " (" + id + ")");
				nbsp += " ";
			}
			
			for (int i = 0; i < tpl.params.length; i++) {
				params[i] = new Object(tpl.params[i].id, tpl.params[i].name);
				params[i].value = readVar(b, tpl.params[i].id, tpl.params[i].sub_id, false);
				if (verbose) {
					if (tpl.params[i].id == VAR_OBJECT)
						Common.logDebug(nbsp + params[i].name + " = {");
					if (tpl.params[i].id == VAR_OBJECT)
						Common.logDebug(nbsp + "}");
					else
						Common.logDebug(nbsp + params[i].name + " = " + getParamStr(params[i].value) );
				}
			}
			
			if (verbose) {
				nbsp = nbsp.substring(1);
			}
		}
		
		public static String getParamStr(java.lang.Object obj) {
			switch (getVarID(obj)) {
				case VAR_BOOL	: 
				case VAR_INT	: 
				case VAR_LONG	: 
				case VAR_DOUBLE	: 					
				case VAR_STRING	: return obj.toString();
				case VAR_BYTES	: 
				case VAR_INT128	: 
				case VAR_INT256	: return "[bytes]";
				/*
					byte[] data = (byte[])obj;
					String str = "";					
					for (int i = 0; i < data.length; i++) 
						str += String.format("%02X", data[i] & 0xff);
					return str;
				*/
				case VAR_VECTOR	: return "[vector]"; 
				case VAR_OBJECT	: return "[object]";
			}
			return "[null]";
		}
		
		public static Object init(String ctor, java.lang.Object[] params) {
			Object tpl = ctor_name.get(ctor);
			if (tpl == null) {
				Common.logError("constructor not found " + ctor);
				return null;
			}
			
			if (tpl.params.length != params.length) {
				Common.logError("not equal params count for " + tpl.name + " (" + params.length + " need " + tpl.params.length + ")");
				return null;			
			}
			
			Object obj = new Object(tpl.id, tpl.name);
			obj.type = tpl.type;
			obj.params = new Object[tpl.params.length];
			for (int i = 0; i < obj.params.length; i++) {
				obj.params[i] = new Object(tpl.params[i].id, tpl.params[i].name);			
				if (params[i] == null) {
					Common.logError("init param with [null] value: " + tpl.params[i].name);
					continue;
				}				
				obj.params[i].value = getEqualType(params[i], obj.params[i].id);
				if (obj.params[i].value == null)
					Common.logError("invalid type cast " + obj.name + "[" + obj.params[i].name + "]:" + VAR_NAME[obj.params[i].id] + " set to " + VAR_NAME[getVarID(params[i])] );
			}	
					
			return obj;
		}
		
		protected static int getVarID(java.lang.Object obj) {
			if (obj == null) return VAR_NULL;
			if (obj instanceof Boolean)		return VAR_BOOL;
			if (obj instanceof Integer)		return VAR_INT;
			if (obj instanceof Long)		return VAR_LONG;
			if (obj instanceof Double)		return VAR_DOUBLE;
			if (obj instanceof String)		return VAR_STRING;
			if (obj instanceof BigInteger)	return VAR_BYTES;
			if (obj instanceof byte[]) {
				byte[] d = (byte[])obj;
				if (d.length == 16) return VAR_INT128;
				if (d.length == 32) return VAR_INT256;			
				return VAR_BYTES;
			}
			if (obj instanceof Vector)	return VAR_VECTOR;
			return VAR_OBJECT;
		}
		
		private static java.lang.Object getEqualType(java.lang.Object obj, int vtype) {
			int id = getVarID(obj); 
			if (id == vtype) {
				if (vtype == VAR_BYTES && obj instanceof BigInteger)
					return Common.toBytes((BigInteger)obj);
				return obj;
			}			
			return null;
		}		
		
		protected static java.lang.Object readVar(ByteBuffer b, int id, int sub_id, Boolean naked) throws Exception {
			if (b.remaining() == 0) return null;
			java.lang.Object value = null;
			switch (id) {
				case VAR_BOOL	: value = (Boolean)(b.getInt() == boolTrue); break;
				case VAR_INT	: value = (Integer)b.getInt(); break;
				case VAR_LONG	: value = (Long)b.getLong(); break;
				case VAR_DOUBLE	: value = (Double)b.getDouble(); break;
				case VAR_STRING	: value = new String(Common.getStr(b)); break;
				case VAR_BYTES	: value = Common.getStr(b); break;
				case VAR_INT128	: value = Common.getBytes(b, 16); break;
				case VAR_INT256	: value = Common.getBytes(b, 32); break;			
				case VAR_VECTOR	: value = new Vector(b, sub_id, false); break;
				default			: value = naked ? new Object(b, id) : deserialize(b); 
			}
			return value;	
		}
		
		protected void writeVar(ByteBuffer b, java.lang.Object value) throws Exception {
			switch (getVarID(value)) {
				case VAR_BOOL	: b.putInt((Boolean)value ? boolTrue : boolFalse ); break;
				case VAR_INT	: b.putInt((Integer)value); break;
				case VAR_LONG	: b.putLong((Long)value); break;
				case VAR_DOUBLE	: b.putDouble((Double)value); break;
				case VAR_STRING	: Common.putStr(b, ((String)value).getBytes()); break;
				case VAR_BYTES	: Common.putStr(b, (byte[])value); break;
				case VAR_INT128	: 
				case VAR_INT256	: b.put((byte[])value); break;					
				case VAR_VECTOR	:  
				case VAR_OBJECT	: ((Object)value).serialize(b); break;
			}			
		}
		
		protected static int getVarByName(String name) {
			for (int i = 0; i < VAR_NAME.length; i++)
				if (name.equals(VAR_NAME[i]))
					return VAR_ID[i];
			if (name.startsWith(VAR_NAME[VAR_VECTOR]))
				return VAR_VECTOR;			
			return VAR_OBJECT;
		}
				
		protected Object newParam(String name, String type) {
			Object obj = new Object(getVarByName(type), name);
			if (obj.id == VAR_VECTOR) 
				obj.sub_id = getVarByName(type.substring(7, type.length() - 1));			
			return obj;
		}
		
		protected void serialize(ByteBuffer b) throws Exception {
			b.putInt(id);
			for (int i = 0; i < params.length; i++) {
				java.lang.Object value = params[i].value;
				if (value == null) {
					Common.logError("serialize null param: " + params[i].name);
					throw new Exception();
				}
				writeVar(b, params[i].value);
			}
		}
		
		public byte[] serialize() {
			try {
				synchronized (buf) {
					buf.position(0);
					serialize(buf);
					return Common.ASUB(buf.array(), 0, buf.position());
				}
			} catch (Exception e) {
				Common.logError("BaseObject.serialize error");
				e.printStackTrace();
				return null;
			}
		}
		
		public java.lang.Object getValue(String name, int vtypes, boolean check) {
			for (int i = 0; i < params.length; i++)
				if (vtypes == params[i].id && params[i].name.equals(name))
					return params[i].value;
			if (check)
				Common.logError("getParam not found for " + name);
			return null;
		}	
		
		public boolean getBool(String name) {
			return (Boolean)getValue(name, VAR_BOOL, true);
		}
		
		public int getInt(String name) {
			return (Integer)getValue(name, VAR_INT, true);
		}
		
		public long getLong(String name) {
			return (Long)getValue(name, VAR_LONG, true);
		}
		
		public double getDouble(String name) {
			return (Double)getValue(name, VAR_DOUBLE, true);
		}
		
		public String getString(String name) {
			return (String)getValue(name, VAR_STRING, true);
		}

		public byte[] getBytes(String name) {
			byte[] data;
			if ((data = (byte[])getValue(name, VAR_INT128, false)) != null || 
				(data = (byte[])getValue(name, VAR_INT256, false)) != null ||
				(data = (byte[])getValue(name, VAR_BYTES, true)) != null)
				return data;
			return null;
		}
		
		public Vector getVector(String name) {
			return (Vector)getValue(name, VAR_VECTOR, true);
		}
		
		public Object getObject(String name) {
			return (Object)getValue(name, VAR_OBJECT, true);
		}
		
		public void set(String name, java.lang.Object value) {
			for (int i = 0; i < params.length; i++)
				if (params[i].name.equals(name) ) {
					params[i].value = value;
					return;
				}
			Common.logError("set " + this.name + "[" + name + "] not found");
		}		
	}
	
	public static class Vector extends Object {
		public int count;
		
		Vector() {
			super(0x1cb5c415, "vector");
			type = "Vector";
		}
		
		Vector(ByteBuffer b, int sub_id, Boolean naked) throws Exception {
			super(b, -1);
			if (sub_id == -1) {
				Common.logError("vector of vector");
				return;
			}
			
			this.sub_id = (sub_id < 0 || sub_id > 9) ? VAR_OBJECT : sub_id;			
			count = b.getInt();
			params = new Object[1 + count];
			params[0] = new Object(VAR_INT, null);
			params[0].value = count;
			for (int i = 1; i < params.length; i++) {
				params[i] = new Object(sub_id, null);
				params[i].value = readVar(b, sub_id, -1, naked);
				if (params[i].value == null)
					throw new Exception();
			}
		}
		
		public static Vector init(java.lang.Object[] params) {
			Vector v = new Vector();
			v.count = params.length;
			v.params = new Object[v.count + 1];
			v.params[0] = new Object(VAR_INT, null);
			v.params[0].value = v.count;
			v.sub_id = v.count > 0 ? getVarID(params[0]) : VAR_NULL;
		
			for (int i = 1; i < v.params.length; i++) {
				v.params[i] = new Object(v.sub_id, null);
				v.params[i].value = params[i - 1];
			}
			return v;
		}
		
		public java.lang.Object getValue(int index, int vtype) {
			if (vtype != sub_id) {
				Common.logError("invalid vector item id " + vtype + " need " + sub_id);
				return null;
			}
			if (index < 0 || index >= count) {
				Common.logError("out of bounds " + index);
				return null;
			}
			return params[index + 1].value;
		}	
		
		public boolean getBool(int index) {
			return (Boolean)getValue(index, VAR_BOOL);
		}

		public int getInt(int index) {
			return (Integer)getValue(index, VAR_INT);
		}

		public long getLong(int index) {
			return (Long)getValue(index, VAR_LONG);
		}

		public double getDouble(int index) {
			return (Double)getValue(index, VAR_DOUBLE);
		}

		public String getString(int index) {
			return (String)getValue(index, VAR_STRING);
		}

		public byte[] getBytes(int index) {
			byte[] data;
			if ((data = (byte[])getValue(index, VAR_INT128)) != null || 
				(data = (byte[])getValue(index, VAR_INT256)) != null ||
				(data = (byte[])getValue(index, VAR_BYTES)) != null)
				return data;
			return null;
		}

		public Vector getVector(int index) {
			return (Vector)getValue(index, VAR_VECTOR);
		}

		public Object getObject(int index) {
			return (Object)getValue(index, VAR_OBJECT);
		}
	}
	
	public static void init(JSONObject scheme) throws Exception {
		buf.order(ByteOrder.LITTLE_ENDIAN);		

		ctor_id		= new SparseArray<Object>();
		ctor_name	= new HashMap<String, Object>();	

		JSONArray ctor = scheme.getJSONArray("constructors");		
		for (int i = 0; i < ctor.length(); i++) {
			JSONObject jobj = ctor.getJSONObject(i);			
			Object bobj = new Object(jobj.getInt("id"), jobj.getString("predicate"));
			JSONArray pArr = jobj.getJSONArray("params");
			bobj.type = jobj.getString("type");
			bobj.params = new Object[pArr.length()];
			for (int j = 0; j < pArr.length(); j++) {
				jobj = pArr.getJSONObject(j);				
				bobj.params[j] = bobj.newParam(jobj.getString("name"), jobj.getString("type"));
			}			
			ctor_id.put(bobj.id, bobj);
			ctor_name.put(bobj.name, bobj);
		}			
	}
	
	public static void dumpData(JSONObject scheme) throws Exception {
		int BUF_SIZE = 1024 * 1024;
		
		ByteBuffer ctorBuf = ByteBuffer.allocate(BUF_SIZE);
		ByteBuffer infoBuf = ByteBuffer.allocate(BUF_SIZE);
		ctorBuf.order(ByteOrder.LITTLE_ENDIAN);
		infoBuf.order(ByteOrder.LITTLE_ENDIAN);
		
		JSONArray ctor = scheme.getJSONArray("constructors");
		
		ctorBuf.putInt(ctor.length());		
		for (int i = 0; i < ctor.length(); i++) {
			JSONObject jobj = ctor.getJSONObject(i);		
			ctorBuf.putInt(jobj.getInt("id"));
			ctorBuf.putShort((short)infoBuf.position());
			JSONArray pArr = jobj.getJSONArray("params");
			infoBuf.put((byte)pArr.length());
			
			for (int j = 0; j < pArr.length(); j++) {
				jobj = pArr.getJSONObject(j);	
				String typeStr = jobj.getString("type");
				int type = Object.getVarByName(typeStr);
				infoBuf.put((byte)type);
				if (type == VAR_VECTOR) {
					type = Object.getVarByName(typeStr.substring(7, typeStr.length() - 1));
					infoBuf.put((byte)type);
				}
			}			
		}
		
		String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";		
		FileOutputStream file = new FileOutputStream(path + "ctor.dat"); 
		DataOutputStream ds = new DataOutputStream(file);	
		ds.write(ctorBuf.array(), 0, ctorBuf.position());
		ds.close();
		file.close();			
		
		file = new FileOutputStream(path + "info.dat"); 
		ds = new DataOutputStream(file);	
		ds.write(infoBuf.array(), 0, infoBuf.position());
		ds.close();
		file.close();			

	
        BufferedWriter out = new BufferedWriter(new FileWriter(path + "scheme.inc"));
        
		int p = (ctorBuf.position() + 3) / 4;
		ctorBuf.putInt(0);
		ctorBuf.position(0);					
		out.write("int DATA_CTOR[] = {\n\t");		
        for (int i = 0; i < p; i++) {
        	if (i > 0 && i % 8 == 0)
        		out.write("\n\t");
        	out.write( String.format("0x%08x,", ctorBuf.getInt()) );
        }        
        out.write("\n};\n\n");
        
		p = (infoBuf.position() + 3) / 4;
		infoBuf.putInt(0);
		infoBuf.position(0);					
		out.write("int DATA_TYPE[] = {\n\t");		
        for (int i = 0; i < p; i++) {
        	if (i > 0 && i % 8 == 0)
        		out.write("\n\t");
        	out.write( String.format("0x%08x,", infoBuf.getInt()) );
        }
        out.write("\n};\n\n");
        
        
		BigInteger modulus = new BigInteger(MTProto.RSA_MODULUS, 16);		
		//BigInteger pubExp = new BigInteger(MTProto.RSA_EXPONENT, 16);
		
		infoBuf.clear();
		infoBuf.position(0);
		infoBuf.put(Common.toBytes(modulus));
		
		p = (infoBuf.position() + 3) / 4;
		infoBuf.putInt(0);
		infoBuf.position(0);					
		out.write("int RSA_KEY[] = {\n\t");		
        for (int i = 0; i < p; i++) {
        	if (i > 0 && i % 8 == 0)
        		out.write("\n\t");
        	out.write( String.format("0x%08x,", infoBuf.getInt()) );
        }
        out.write("\n};");    
        
        out.close();		

        
        
	}
	
	
	public static Object newObject(String ctor, java.lang.Object ... params) {
		return Object.init(ctor, params);
	}

	public static Vector newVector(java.lang.Object ... params) {
	//	if (params.length == 1 && params[0] instanceof java.lang.Object[])
	//		return Vector.init( (java.lang.Object[])params[0] );	
		return Vector.init(params);
	}
	
	public static synchronized Object deserialize(ByteBuffer data) {
		int id = data.getInt();
		data.position(data.position() - 4); // return pos	
		
		if (id == 0x3072cfa1) {	// gzip_packed
			try {
				TL.Object gzip = new Object(data, -1);			
				return deserialize(Common.gzipInflate( gzip.getBytes("packed_data") ));
			} catch (Exception e) {
				return null;
			}
		}		
		
		if (id == 0x1cb5c415) {
			
			int pos = data.position();
			try {
				return  new Vector(data, VAR_OBJECT, false);				
			} catch (Exception e) {
				try {
					data.position(pos);
					return new Vector(data, VAR_INT, false);
				} catch (Exception ex) {
					ex.printStackTrace();
					return null;
				}				
			}
		}
		
		try {
			if (id == 0x73f1f8dc)	// msg_container
				return new Vector(data, 0x5bb8E511, true);
			else
				return new Object(data, -1);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static Object load(String filePath) {
		try {
			FileInputStream file = new FileInputStream(filePath); 			
			DataInputStream ds = new DataInputStream(file);
						
			ByteBuffer data = ByteBuffer.allocate(ds.available());
			data.order(ByteOrder.LITTLE_ENDIAN);
			ds.read(data.array());
			data.position(0);
			
			ds.close();
			file.close();
			
			return deserialize(data);
		} catch (Exception e) {
			return null;
		}
	}
		
	public static boolean save(String filePath, Object obj) {
		try {
			FileOutputStream file = new FileOutputStream(filePath); 
			DataOutputStream ds = new DataOutputStream(file);
			ds.write(obj.serialize());			
			ds.close();
			file.close();				
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
