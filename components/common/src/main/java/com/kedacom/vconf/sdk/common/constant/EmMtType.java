package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

/**
 * 终端类型
 * */
@EnumCustomValueStrategy
public enum EmMtType {
	EmMt_Type_Mt(0), // /普通终端
	EmMt_Type_PHONE(1), // /电话终端
	EmMt_Type_SATD(2), // /卫星终端
	EmMt_Type_MMCU_Api  (3),     ///上级MCU
	EmMt_Type_SMCU_Api  (4),     ///下级MCU
	EmMt_Type_VRSREC_Api(5),     ///vrs新录播设备
	EmMt_Type_Other(10); // /其他终端

	public int value;

	EmMtType(int v) {
		value = v;
	}

	public int getValue() {
		return value;
	}
}
