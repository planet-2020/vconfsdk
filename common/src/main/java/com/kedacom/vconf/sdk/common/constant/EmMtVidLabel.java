package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2CustomValueJsonAdapter;

/**
 * vid能力标签
 */
@JsonAdapter(Enum2CustomValueJsonAdapter.class)
public enum EmMtVidLabel {
	emVidLabBegin(0),
	// 主流标签
	// 辅流的标签
	emPcStreamVidLab(20), // pc双流能力， pcdv, mtc
	emVideoStreamVidLab(21); // video双流能力

	public int value;
	EmMtVidLabel(int val) {
		this.value=val;
	}

	public int getValue() {
		return value;
	}
}
