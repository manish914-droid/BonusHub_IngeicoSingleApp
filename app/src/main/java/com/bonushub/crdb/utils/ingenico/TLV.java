package com.bonushub.crdb.utils.ingenico;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * TLV数据元
 * @author chenwei
 *
 */
public class TLV {
	private byte[] data;
	private String tag;
	private int length = -1;
	private byte[] value;
	private TLV() {
		
	}
	
	/**
	 * 从原始数据中提取出有效的数值，并生成一个TLV对象
	 * @param tlData		含有TL的数据，如{0x9F, 0x01, 0x02}表示Tag:9F01, Len:02
	 * @param tlOffset		tl有效数据在tlData中的位置，如"tlData = {0x00, 0x00, 0x9F, 0x01, 0x02}, tl=2"
	 * @param vData			value数据，如{0x12, 0x34}
	 * @param vOffset		value的有效数据在vData中的位置，如"vData = {0x00, 0x00, 0x00, 0x12, 0x34}, vOffset=3"
	 * @return	TLV对象
	 */
	public static TLV fromRawData(byte[] tlData, int tlOffset, byte[] vData, int vOffset) {
		int tLen = getTLength(tlData, tlOffset);
		int lLen = getLLength(tlData, tlOffset+tLen);
		int vLen = calcValueLength(tlData, tlOffset+tLen, lLen);
		
		TLV d = new TLV();
		d.data = BytesUtil.merage(BytesUtil.subBytes(tlData, tlOffset, tLen+lLen), BytesUtil.subBytes(vData, vOffset, vLen));
		d.getTag();
		d.getLength();
		d.getBytesValue();
		
		return d;
	}
	
	/**
	 * 仅提供T和V来生成一个TLV对象
	 * @param tagName	TAG,如"9F01"
	 * @param value		完整的VALUE数据,如{0x12,0x34},自身包含长度
	 * @return	TLV对象
	 */
	public static TLV fromData(String tagName, byte[] value) {
		byte[] tag = BytesUtil.hexString2Bytes(tagName);
		TLV d = new TLV();
		d.data = BytesUtil.merage(tag, makeLengthData(value.length), value);
		d.tag = tagName;
		d.length = value.length;
		d.value = value;
		return d;
	}
	
	/**
	 * 从TLV缓冲区中生成一个TLV对象
	 * @param data		含TLV格式的数据
	 * @param offset	TAG所在位置的偏移量
	 * @return	TLV对象
	 */
	public static TLV fromRawData(byte[] data, int offset) {
		int len = getDataLength(data, offset);
		TLV d = new TLV();
		d.data = BytesUtil.subBytes(data, offset, len);
		d.getTag();
		d.getLength();
		d.getBytesValue();
		return d;
	}
	
	/**
	 * 获取TAG，如"9F01"
	 * @return	HEX字符串表示的TAG
	 */
	public String getTag() {
		if (tag != null) {
			return tag;
		}
		int tLen = getTLength(data, 0);
		return tag = BytesUtil.bytes2HexString(BytesUtil.subBytes(data, 0, tLen));
	}
	
	/**
	 * 获取Value长度，如Value={0x12,0x34}则返回2
	 * @return Value字节数据长度
	 */
	public int getLength() {
		if (length > -1) {
			return length;
		}
		int offset = getTLength(data, 0);
		int l = getLLength(data, offset);
		if (l == 1) {
			return data[offset] & 0xff;
		}

		int afterLen = 0;
		for (int i = 1; i < l; i++) {
			afterLen <<= 8;
			afterLen |= (data[offset + i]) & 0xff;
		}
		return length = afterLen;
	}
	
	/**
	 * 获取T和L的二进制数据长度, 如9F010201234则返回3(9F0102)
	 * @return T和L的数据长度
	 */
	public int getTLLength() {
		if (data == null) {
			return -1;
		}
		return data.length - getBytesValue().length;
	}
	
	/**
	 * 获取Value,BCD转换后的HEX字符串。例如Value={0x12,0x34}则返回"1234"
	 * @return HEX字符串所表示的Value
	 */
	public String getValue() {
		byte[] temp = getBytesValue();
		return BytesUtil.bytes2HexString(temp == null ? new byte[0] : temp);
	}
	
	/**
	 * 获取Value的首字节数据
	 * @return Value的首字节数据
	 */
	public byte getByteValue() {
		return getBytesValue()[0];
	}
	
	/**
	 * 获取Value,GBK编码的字符串
	 * @return 转换后的Value
	 */
	public String getGBKValue() {
		try {
			return new String(getBytesValue(), "GBK");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 获取Value,并将Value转为数字字符串,例如Value={0x31,0x32}则返回"12"。如果Value包含非数字字符或者小数，则抛出{@link NumberFormatException}
	 * @return 转换后的Value
	 */
	public String getNumberValue() {
		String num = getValue();
		if(num == null) {
			return null;
		}
		return String.valueOf(Long.parseLong(num));
	}
	
	/**
	 * 获取Value，并将Value转为ASC码
	 * @return 转换后的Value
	 */
	public byte[] getNumberASCValue() {
		try {
			String result = getNumberValue();
			if(result == null) {
				return null;
			}
			return result.getBytes("GBK");
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	/**
	 * 获取BCD压缩后的Value，例如Value={0x31,0x32}，则返回0x12
	 * @return 转换后的Value
	 */
	public byte[] getBCDValue() {
		String result = getGBKValue();
		if(result == null) {
			return null;
		}
		return BytesUtil.hexString2Bytes(result);
	}
	
	/**
	 * 获取原始的TLV数据,如{0x9F,0x01,0x02,0x12,0x34}
	 * @return	原始的TLV格式数据
	 */
	public byte[] getRawData() {
		return data;
	}
	
	/**
	 * 获取原始的Value字节数组
	 * @return	原始Value
	 */
	public byte[] getBytesValue() {
		if(value != null) {
			return value;
		}
		int l = getLength();
		return value = BytesUtil.subBytes(data, data.length - l, l);
	}
	
	/**
	 * 数据是否合法
	 * @return	数据不合法则返回false，否则返回true
	 */
	public boolean isValid() {
		return data != null;
	}
	
	private static int getTLength(byte[] data, int offset) {
		if ((data[offset] & 0x1F) == 0x1F) {
			return parseTLength(data, ++offset, 2);
		}
		return 1;
	}

	private static int parseTLength(byte[] data, int nextOffset, int clen) {
		if ((data[nextOffset] & 0x80) == 0x80) {
			clen = parseTLength(data, ++nextOffset, ++clen);
		}
		return clen;
	}

	private static int getLLength(byte[] data, int offset) {
		if ((data[offset] & 0x80) == 0) {
			return 1;
		}
		return (data[offset] & 0x7F) + 1;
	}
	
	private static int getDataLength(byte[] data, int offset) {
		int tLen = getTLength(data, offset);
		int lLen = getLLength(data, offset+tLen);
		int vLen = calcValueLength(data, offset+tLen, lLen);
		return tLen + lLen + vLen;
	}
	
	private static int calcValueLength(byte[] l, int offset, int lLen) {
		if (lLen == 1) {
			return l[offset] & 0xff;
		}
		
		int vLen = 0;
		for(int i=1; i<lLen; i++) {
			vLen <<= 8;
			vLen |= (l[offset+i])&0xff;
		}
		return vLen;
	}

	private static byte[] makeLengthData(int len) {
		if (len > 127) {
			byte[] tempLen = BytesUtil.intToBytesByLow(len);
			int start = 0;
			for (int i = 0; i < tempLen.length; i++) {
				if (tempLen[i] != 0x00) {
					start = i;
					break;
				}
			}

			byte[] lenData = BytesUtil.subBytes(tempLen, start, -1);
			lenData = BytesUtil.merage(new byte[] {(byte) (0x80 | lenData.length)}, lenData);

			Log.e("test", "lenData: " + BytesUtil.bytes2HexString(lenData));
			return lenData;
		} else {
			return new byte[] { (byte) len };
		}
	}
	
	/**
	 * 比较TLV的差异
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		
		if(!(obj instanceof TLV)) {
			return false;
		}
		
		if(data == null || ((TLV)obj).data == null) {
			return false;
		}
		
		return Arrays.equals(data, ((TLV)obj).data);
	}
	
	/**
	 * 以HEX字符串方式显示TLV
	 */
	@Override
	public String toString() {
		if(data == null) {
			return super.toString();
		}
		return BytesUtil.bytes2HexString(data);
	}
}