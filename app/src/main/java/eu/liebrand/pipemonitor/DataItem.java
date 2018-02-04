package eu.liebrand.pipemonitor;
/**
 * 
 * @author mark
 * $Revision: 1.2 $
 * $Source: /share/Public/cvs/MarksHomeAutomationClient/src/eu/liebrand/mhac/util/DataItem.java,v $
 */
public class DataItem {

	public static final byte TYPE_STRING=1;
	public static final byte TYPE_NUMBER=2;
	public static final byte TYPE_COMMAND=3;
	public static final byte TYPE_BINARY=4;
	public static final byte TYPE_LONGDIRECT=64;
	
	private int length;
	private byte type;
	private String key;
	private String valueString;
	private long valueLong;
	private byte [] valueBinary;
	
	public DataItem(String key, String valueString) {
		type=TYPE_STRING;
		this.key=key;
		this.valueString=valueString;
	}
	
	public DataItem(String key, long valueLong) {
		type=TYPE_NUMBER;
		this.key=key;
		this.valueLong=valueLong;
	}
	
	public DataItem(String key, byte [] binary) {
		type=TYPE_BINARY;
		this.key=key;
		this.valueBinary=binary;
	}

	public DataItem(long valueLong) {
		type=TYPE_LONGDIRECT;
		this.valueLong=valueLong;
	}
	
	public byte getType() {
		return(type);
	}
	
	public String getKey() {
		return(key);
	}

	public String getString() {
		return(valueString);
	}
	
	public byte [] getBinary() {
		return(valueBinary);
	}
	
	public long getLong() {
		return(valueLong);
	}
	
	public void setLenInStream(int len) {
		length=len;
	}
	
	public int getLenInStream() {
		return(length);
	}
	
	public byte [] getHashBytes() {
		switch(type) {
		case TYPE_STRING:
		case TYPE_COMMAND:
			return valueString.getBytes();
		case TYPE_BINARY:
			return valueBinary;
		case TYPE_NUMBER:
			return String.valueOf(valueLong).getBytes();
		default:
			return new byte[0];
			
		}
	}

}
