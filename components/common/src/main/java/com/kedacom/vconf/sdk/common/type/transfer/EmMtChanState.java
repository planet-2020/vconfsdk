package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 通道状态
 */
@EnumOrdinalStrategy
public enum EmMtChanState {
	emChanIdle,
	emChanConnected,
	// 暂未使用
	emChanActive,

	// 暂未使用
	emChanInActive
}
