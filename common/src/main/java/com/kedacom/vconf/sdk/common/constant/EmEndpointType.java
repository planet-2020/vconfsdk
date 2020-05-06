package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

/**
  * 终端类型
  */

@EnumCustomValueStrategy
public enum EmEndpointType {
	emEndpointTypeUnknown(0),
	emEndpointTypeMT(1),
	emEndpointTypeMCU(2),
	emEndpointTypeGK(4);

	private int value;

	EmEndpointType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
