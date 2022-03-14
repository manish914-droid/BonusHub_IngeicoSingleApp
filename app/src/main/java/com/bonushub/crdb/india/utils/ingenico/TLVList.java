package com.bonushub.crdb.india.utils.ingenico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TLV列表，TLV对象的数据集
 * @author chenwei
 *
 */
public class TLVList {
	private List<TLV> data = new ArrayList<TLV>();
	
	/**
	 * 从TLV二进制数据中生成一个TLVList对象，该对象中可包含0~n个TLV对象
	 * @param data	TLV数据，如{0x9F,0x01,0x02,0x12,0x34,0x80,0x01,0x56}
	 * @return	TLVList对象
	 */
	public static TLVList fromBinary(byte[] data) {
		TLVList l = new TLVList();
		int offset = 0;
		while(offset < data.length) {
			TLV d = TLV.fromRawData(data, offset);
			l.addTLV(d);
			offset += d.getRawData().length;
		}
		return l;
	}
	
	/**
	 * 从HEX字符串表示的TLV数据中生成一个TLVList对象，该对象可包含0~n个TLV对象的
	 * @param data	TLV数据，如"9F01021234800156"
	 * @return	TLVList对象
	 */
	public static TLVList fromBinary(String data) {
		return fromBinary(BytesUtil.hexString2Bytes(data));
	}
	
	/**
	 * 返回TLV数量
	 * @return	数量
	 */
	public int size() {
		return data.size();
	}

	/**
	 * 转为标准的二进制数据，如{0x9F,0x01,0x02,0x12,0x34,0x80,0x01,0x56}
	 * @return	二进制数据
	 */
	public byte[] toBinary() {
		byte[][] allData = new byte[data.size()][];
		for(int i=0; i<data.size(); i++) {
			allData[i] = data.get(i).getRawData();
		}
		return BytesUtil.merage(allData);
	}
	
	/**
	 * 检查是否包含某TAG所对应的TLV对象
	 * @param tag	TAG,如"9F01"
	 * @return	true表示包含，false表示不包含
	 */
	public boolean contains(String tag) {
		return null != getTLV(tag);
	}
	
	/**
	 * 获取指定TAG的TLV对象
	 * @param tag	TAG，如"9F01"
	 * @return	TLV对象
	 */
	public TLV getTLV(String tag) {
		for(TLV d : data) {
			if(d.getTag().equals(tag)) {
				return d;
			}
		}
		return null;
	}
	
	/**
	 * 获取子集
	 * @param tags	多个TAG，如“9F01”,"9F02"
	 * @return TLV对象子集
	 */
	public TLVList getTLVs(String...tags) {
		TLVList list = new TLVList();
		for(String tag : tags) {
			TLV data = getTLV(tag);
			if(data != null) {
				list.addTLV(data);
			}
		}
		if(list.size() == 0) {
			return null;
		}
		return list;
	}
	
	/**
	 * 根据指定位置获取TLV
	 * @param index	位置，从0起
	 * @return	TLV对象
	 */
	public TLV getTLV(int index) {
		return data.get(index);
	}
	
	/**
	 * 添加一个TLV对象
	 * @param tlv	TLV对象
	 */
	public void addTLV(TLV tlv) {
		if(tlv.isValid()) {
			data.add(tlv);
		}
		else {
			throw new IllegalArgumentException("tlv is not valid!");
		}
	}
	
	/**
	 * 保留特定的TAG所指向的TLV对象，剩余的全部删除
	 * @param tags	指定的TAG
	 */
	public void retainAll(String... tags) {
		List<String> tagList = Arrays.asList(tags);
		for (int index = 0; index < data.size();) {
			if (!tagList.contains(data.get(index).getTag())) {
				data.remove(index);
			} else {
				index++;
			}
		}
	}
	
	/**
	 * 删除一个TLV对象
	 * @param tag	TLV对象的TAG, 如"9F01"
	 */
	public void remove(String tag) {
		for (int i = 0; i < data.size();) {
			if (tag.equals(data.get(i).getTag())) {
				data.remove(i);
			} else {
				i++;
			}
		}
	}
	
	/**
	 * 删除一到多个TLV对象
	 * @param tags	TLV对象的TAG, 如"9F01"
	 */
	public void removeAll(String... tags) {
		List<String> tagList = Arrays.asList(tags);
		for (int i = 0; i < data.size();) {
			if (tagList.contains(data.get(i).getTag())) {
				data.remove(i);
			} else {
				i++;
			}
		}
	}
	
	/**
	 * 将TLV列表按顺序转为HEX字符串
	 */
	@Override
	public String toString() {
		if (data.isEmpty()) {
			return super.toString();
		}
		return BytesUtil.bytes2HexString(toBinary());
	}
}