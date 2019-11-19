package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 通道状态
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtChanState {
	emChanIdle,
	emChanConnected,
	// 暂未使用
	emChanActive,

	// 暂未使用
	emChanInActive
}
