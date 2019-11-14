package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 双流模式
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtDualMode {
	emMt_Dual_Mode_Speaker, // 只有发言人能发双流
	emMt_Dual_Mode_Everyone, // 任何人都能发双流
	emMt_Dual_Mode_Invalid;// 不支持
}