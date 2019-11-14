package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 混音类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtMixType {
	emMcuNoMix_Api, // /< 不混音
	mcuWholeMix_Api, // /< 全体混音
	mcuPartMix_Api, // /< 定制混音
	mcuVacMix_Api, // /< VAC
	mcuVacWholeMix_Api, // /< 带Vac的全体混音
}
