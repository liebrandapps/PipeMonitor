package eu.liebrand.pipemonitor;

import java.io.IOException;
import java.io.OutputStream;

public class StreamWriter {

	public static void write(OutputStream stream, DataItem item) throws IOException {
        stream.write(item.getType());
        switch (item.getType()) {
                case DataItem.TYPE_STRING:
                        writeRawString(stream, item.getKey());
                        writeRawString(stream, item.getString());
                        break;
                case DataItem.TYPE_BINARY:
                    writeRawString(stream, item.getKey());
                    writeRawBinary(stream, item.getBinary());
                    break;
                case DataItem.TYPE_NUMBER:
                    writeRawString(stream, item.getKey());
                    writeRawLong(stream, item.getLong());
                    break;
			case DataItem.TYPE_LONGDIRECT:
				writeRawLong(stream, item.getLong());
				break;
                    
        }

	}	

	public static void writeBinaryDirect(OutputStream stream, byte [] data) throws IOException {
        stream.write(data);
	}

	private static void writeRawString(OutputStream stream, String strg) throws IOException {
		int ln=strg.length();
		byte hiByte=(byte)(ln / 256);
		byte loByte=(byte)(ln % 256);
		stream.write((char)hiByte);
		stream.write((char)loByte);
		stream.write(strg.getBytes());
	}

	private static void writeRawBinary(OutputStream stream, byte [] binary) throws IOException {
		int ln=binary.length;
		int hiInt=(int)(ln / 65536);
		int loInt=(int)(ln % 65536);
		byte hiByte=(byte)(hiInt / 256);
		byte loByte=(byte)(hiInt % 256);
		stream.write((char)hiByte);
		stream.write((char)loByte);
		hiByte=(byte)(loInt / 256);
		loByte=(byte)(loInt % 256);
		stream.write((char)hiByte);
		stream.write((char)loByte);
		stream.write(binary);
	}

	private static void writeRawLong(OutputStream stream, long lng) throws IOException {
		int hiInt=(int)(lng / 65536);
		int loInt=(int)(lng % 65536);
		byte hiByte=(byte)(hiInt / 256);
		byte loByte=(byte)(hiInt % 256);
		stream.write((char)hiByte);
		stream.write((char)loByte);
		hiByte=(byte)(loInt / 256);
		loByte=(byte)(loInt % 256);
		stream.write((char)hiByte);
		stream.write((char)loByte);
	}
	

}
