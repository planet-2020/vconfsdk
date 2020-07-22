package com.kedacom.vconf.sdk.common.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

@EnumCustomValueStrategy
enum EmResourceType {
	emResourceType_LimitP_Api(0),            ///< 仅限制方数
	emResourceType_LimitPR_Api(1),			///< 同时限制方数及分辨率
    emResourceType_None_Api(2);         ///< 平台不支持此功能

	int value;

	EmResourceType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
};