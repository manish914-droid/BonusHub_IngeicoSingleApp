package com.bonushub.crdb.india.utils;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * 字节数组工具
 * @author chenwei
 *
 */
public class BytesUtil {
	private static final String EMPTY_STRING = "";
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	/** 字符集常量定义 */
	private static final String CHARSET_ISO8859_1 = "ISO-8859-1";
	private static final String CHARSET_GBK = "GBK";
	private static final String CHARSET_GB2312 = "GB2312";
	private static final String CHARSET_UTF8 = "UTF-8";
	
	private BytesUtil() {}

	/**
	 * asc 转  int
	 * @param sum 如0x33 转 3
	 * @return
	 */
	public static int ascToInt(byte sum) {
		return Integer.valueOf((char)sum + "");
	}

	/**
	 * 二进制数据转16进制表示的字符串
	 * @param data
	 * @return 转换结果
	 */
	public static String bytes2HexString(byte[] data) {
		if (isNullEmpty(data)) {
			return EMPTY_STRING;
		}
		StringBuilder buffer = new StringBuilder();
		for (byte b : data) {
			String hex = Integer.toHexString(b & 0xff);
			if (hex.length() == 1) {
				buffer.append('0');
			}
			buffer.append(hex);
		}
		return buffer.toString().toUpperCase();
	}

	/**
	 * 将ascii字节数组转成可显示的字符串
	 * @param asciiData
	 * @return 转换结果
	 */
	public static String toString(byte[] asciiData) {
		if (isNullEmpty(asciiData)) {
			return EMPTY_STRING;
		}
		try {
			int length = 0;
			for (int i = 0; i < asciiData.length; ++i) {
				if (asciiData[i] == 0) {
					length = i;
					break;
				}
			}
			return new String(asciiData, 0, length, CHARSET_GB2312);
		} catch (Exception e) {
			return EMPTY_STRING;
		}
	}

	/**
	 * 16进制表示的字符串转二进制数据
	 * @param data
	 * @return 转换结果
	 */
	public static byte[] hexString2Bytes(String data) {
		if (isNullEmpty(data)) {
			return EMPTY_BYTE_ARRAY;
		}

		byte[] result = new byte[(data.length() + 1) / 2];
		if ((data.length() & 1) == 1) {
			data += "0";
		}
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) (hex2byte(data.charAt(i * 2 + 1)) | (hex2byte(data.charAt(i * 2)) << 4));
		}
		return result;
    }
	
	/**
	 * 16进制字符转二进制
	 * @param hex
	 * @return 转换结果
	 */
	public static byte hex2byte(char hex){
		if(hex <= 'f' && hex >= 'a') {
			return (byte) (hex - 'a' + 10);
		}
		
		if(hex <= 'F' && hex >= 'A') {
			return (byte) (hex - 'A' + 10);
		}
		
		if(hex <= '9' && hex >= '0') {
			return (byte) (hex - '0');
		}
		
		return 0;
	}

	/**
	 * 获得子数组
	 * @param data		数据
	 * @param offset	偏移位置。0~data.length
	 * @param len		长度。为负数表示正常范围的最大长度
	 * @return	子数组
	 */
	public static byte[] subBytes(byte[] data, int offset, int len){
		if (isNullEmpty(data)) {
			return null;
		}

		if(offset < 0 || data.length <= offset) {
			return null;
		}
		
		if(len < 0 || data.length < offset + len) {
			len = data.length - offset;
		}
		
		byte[] ret = new byte[len];
		
		System.arraycopy(data, offset, ret, 0, len);
		return ret;
	}
	
	/**
	 * 将多个数据并起来
	 * @param data	数据数组，可传任意个
	 * @return		合并的数据
	 */
	public static byte[] merage(byte[]... data) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			for (byte[] d : data) {
				if (d == null) {
					throw new IllegalArgumentException("");
				}
				buffer.write(d);
			}
			return buffer.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				buffer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	/**
	 * 将字节数组按大端模式转为整数
	 * @param bytes	
	 * @return	转换结果
	 */
	public static int bytesToInt(byte[] bytes) {
		if (isNullEmpty(bytes)) {
			return 0;
		}

		if(bytes.length > 4) {
			return -1;
		}
		
		int lastIndex = bytes.length - 1;
		int result = 0;
		for(int i=0; i<bytes.length; i++) {
			result |= (bytes[i] & 0xFF) << ((lastIndex-i)<<3);
		}
		
		return result;
	}
	
	/**
	 * 将字节数组按小端模式转为整数
	 * @param bytes	
	 * @return	转换结果
	 */
	public static int littleEndianBytesToInt(byte[] bytes) {
		if (isNullEmpty(bytes)) {
			return 0;
		}

		if(bytes.length > 4) {
			return -1;
		}
		
		int result = 0;
		for(int i=0; i<bytes.length; i++) {
			result |= (bytes[i] & 0xFF) << (i<<3);
		}
		
		return result;
	}
	
	/**
	 * 将整数按小端模式转为4字节数组
	 * @param intValue
	 * @return	转换结果
	 */
	public static byte[] intToBytesByLow(int intValue) {
		byte[] bytes = new byte[4];
		for(int i=0; i<bytes.length; i++) {
			bytes[i] = (byte) ((intValue >> ((3-i)<<3)) & 0xFF);
		}
		return bytes;
	}

	/**
	 * 将整数转换为 按大端排序的4字节数组
	 * 大端(big endian):低地址存放高有效字节
	 * 如：0x12345678 则对应的byte[]
	 *        低地址位     高低址位
	 * 大端： 12  34        56   78
	 * 小端： 78  56        34   12
	 */
	public static byte[] intToBytesByHigh(int value) {
		byte[] src = new byte[4];
		src[3] =  (byte) ((value>>24) & 0xFF);
		src[2] =  (byte) ((value>>16) & 0xFF);
		src[1] =  (byte) ((value>>8) & 0xFF);
		src[0] =  (byte) (value & 0xFF);
		return src;
	}

	/**
	 * BCD转换成ASCII
	 * @param bcd BCD字节数组
	 * @return ASCII字符串
	 */
	public static String bcd2Ascii(final byte[] bcd) {
		if (isNullEmpty(bcd)) {
			return EMPTY_STRING;
		}

		StringBuilder sb = new StringBuilder(bcd.length << 1);
		for (byte ch : bcd) {
			byte half = (byte) (ch >> 4);
			sb.append((char) (half + ((half > 9) ? ('A' - 10) : '0')));
			half = (byte) (ch & 0x0f);
			sb.append((char) (half + ((half > 9) ? ('A' - 10) : '0')));
		}
		return sb.toString();
	}

	/**
	 * ASCII字符串压缩成BCD格式
	 * @param ascii ASCII字符串
	 * @return 压缩后的BCD字节数组
	 */
	public static byte[] ascii2Bcd(String ascii) {
		if (isNullEmpty(ascii)) {
			return EMPTY_BYTE_ARRAY;
		}

		if ((ascii.length() & 0x01) == 1) {
			ascii = "0" + ascii;
		}
		byte[] asc = ascii.getBytes();
		byte[] bcd = new byte[ascii.length() >> 1];
		for (int i = 0; i < bcd.length; i++) {
			bcd[i] = (byte) (hex2byte((char) asc[2 * i]) << 4 | hex2byte((char) asc[2 * i + 1]));
		}
		return bcd;
	}
	
	/**
	 * 字符串转成字节数组
	 * @param data  字符串
	 * @param charsetName 字符集名称
	 * @return 字节数组
	 */
	public static byte[] toBytes(String data, String charsetName) {
		if (isNullEmpty(data)) {
			return EMPTY_BYTE_ARRAY;
		}

		try {
			return data.getBytes(charsetName);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * 字符串转换成ISO8859-1字节表示
	 * @param data 字符串
	 * @return 字节数组
	 */
	public static byte[] toBytes(String data) {
		return toBytes(data, CHARSET_ISO8859_1);
	}

	/**
	 * 字符串转换成GBK字节表示
	 * @param data 字符串
	 * @return 字节数组
	 */
	public static byte[] toGBK(String data) {
		return toBytes(data, CHARSET_GBK);
	}

	/**
	 * 字符串转换成GB2312字节表示
	 * @param data 字符串
	 * @return 字节数组
	 */
	public static byte[] toGB2312(String data) {
		return toBytes(data, CHARSET_GB2312);
	}

	/**
	 * 字符串转换成UTF-8字节表示
	 * @param data 字符串
	 * @return 字节数组
	 */
	public static byte[] toUtf8(String data) {
		return toBytes(data, CHARSET_UTF8);
	}

	/**
	 * 字节数组转换成字符串
	 * @param data  字节数组
	 * @param charsetName 字符集名称
	 * @return 字符串
	 */
	public static String fromBytes(byte[] data, String charsetName) {
		try {
			return new String(data, charsetName);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * ISO8859-1字节数组转换成字符串
	 * @param data 字节数组
	 * @return 字符串
	 */
	public static String fromBytes(byte[] data) {
		return fromBytes(data, CHARSET_ISO8859_1);
	}

	/**
	 * GBK字节数组转换成字符串
	 * @param data 字节数组
	 * @return 字符串
	 */
	public static String fromGBK(byte[] data) {
		return fromBytes(data, CHARSET_GBK);
	}

	/**
	 * GB2312字节数组转换成字符串
	 * @param data 字节数组
	 * @return 字符串
	 */
	public static String fromGB2312(byte[] data) {
		return fromBytes(data, CHARSET_GB2312);
	}

	/**
	 * UTF-8字节数组转换成字符串
	 * @param data 字节数组
	 * @return 字符串
	 */
	public static String fromUtf8(byte[] data) {
		return fromBytes(data, CHARSET_UTF8);
	}

	public static boolean isAllNullEmpty(String... strings) {
		for (String str : strings) {
			if (!isNullEmpty(str)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isAllNullEmpty(byte[]... arrays) {
		for (byte[] array : arrays) {
			if (!isNullEmpty(array)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isNullEmpty(String str) {
		return TextUtils.isEmpty(str);
	}

	public static boolean isNullEmpty(byte[] array) {
		return (array == null) || (array.length == 0);
	}

	public static boolean checkPartEquals(byte[] data1, byte[] data2) {
		if (data1 == null || data2 == null) {
			return false;
		}

		if (data2.length == data1.length) {
			return Arrays.equals(data2, data1);
		} else {
			int cmpLen = Math.min(data1.length, data2.length);
			if (data2.length < data1.length) {
				return Arrays.equals(data2, Arrays.copyOfRange(data1, 0, cmpLen));
			} else {
				return Arrays.equals(data1, Arrays.copyOfRange(data2, 0, cmpLen));
			}
		}
	}
}
