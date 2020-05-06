package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 双流模式
 */
@EnumOrdinalStrategy
public enum EmMtDualMode {
	emMt_Dual_Mode_Speaker, // 只有发言人能发双流
	emMt_Dual_Mode_Everyone, // 任何人都能发双流
	emMt_Dual_Mode_Invalid;// 不支持
}