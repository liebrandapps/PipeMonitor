package eu.liebrand.pipemonitor;

import java.io.IOException;
import java.io.InputStream;

public class StreamReader {
		
	public static DataItem readItem(InputStream inputStream) throws IOException {
		String key="";
		DataItem item=null;
		byte [] type=new byte[1];
		inputStream.read(type);
		if(type[0]!=DataItem.TYPE_LONGDIRECT) {
			key=readRawString(inputStream);
		}
		switch (type[0]) {
			case DataItem.TYPE_STRING:
				String value=readRawString(inputStream);
				item=new DataItem(key, value);
				item.setLenInStream(5+key.length()+value.length());
				break;
			case DataItem.TYPE_LONGDIRECT:
				long lngValue=readLongDirect(inputStream);
				item=new DataItem("", lngValue);
				item.setLenInStream(5);
				break;
			case DataItem.TYPE_BINARY:
				byte [] binValue=readBinary(inputStream);
				item= new DataItem(key, binValue);
				item.setLenInStream(5+key.length()+binValue.length);				
				break;
			case DataItem.TYPE_NUMBER:
				lngValue=readLongDirect(inputStream);
				item=new DataItem(key, lngValue);
				item.setLenInStream(5+key.length()+4);
				break;
		}
		return item;
	}

	private static String readRawString(InputStream inputStream) throws IOException {
		byte [] lenBytes = new byte[2];
		inputStream.read(lenBytes);
		int idx = 0;
		int len = unsignedByteToInt(lenBytes[idx++]) << 8;
		len += unsignedByteToInt(lenBytes[idx]) << 0;
		byte [] dta=new byte [len];
		inputStream.read(dta);
		return(new String(dta));
	}

	private static byte [] readBinary(InputStream inputStream) throws IOException {
		int len=(int)readLongDirect(inputStream);
		byte [] dta=new byte[len];
		inputStream.read(dta);
		return(dta);
	}

	private static long readLongDirect(InputStream inputStream) throws IOException {
		byte [] lngBytes =new byte[4];
		inputStream.read(lngBytes);
		int idx = 0;
		long lng = unsignedByteToInt(lngBytes[idx++]) << 24;
		lng += unsignedByteToInt(lngBytes[idx++]) << 16;
		lng += unsignedByteToInt(lngBytes[idx++]) << 8;
		lng += unsignedByteToInt(lngBytes[idx]) << 0;
	
		return(lng);
	}
	
	private static int unsignedByteToInt(byte b) {
		return (int) b & 0xFF;
	}
}
