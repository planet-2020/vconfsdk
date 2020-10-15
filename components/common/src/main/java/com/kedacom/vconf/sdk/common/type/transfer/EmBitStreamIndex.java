package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

/**
 * 码流索引
 */

@EnumCustomValueStrategy
public enum EmBitStreamIndex {
	emBitStream1st_Api(0),
	emBitStream2nd_Api(1),
	emBitStream3rd_Api(2),
	emBitStream4th_Api(3),
	emBitStream5th_Api(4),
	emBitStream6th_Api(5),
	emBitStream7th_Api(6),
	emBitStream8th_Api(7),
	emBitStream9th_Api(8),
	emBitStreamAll_Api(200);

	public int value;

	EmBitStreamIndex(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
