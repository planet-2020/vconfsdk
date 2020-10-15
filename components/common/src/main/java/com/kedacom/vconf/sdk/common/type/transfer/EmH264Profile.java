package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

/**
  * h.264profile 
  */

@EnumCustomValueStrategy
public enum EmH264Profile {
	emBaseline(1),
	emMain(2),
	emExtended(4),
	emHigh(8),
	emHigh10(16),
	emHigh422(32),
	emHigh444(64);

	public int value;

	EmH264Profile(int v) {
		value = v;
	}

	public int getValue() {
		return value;
	}
}
