package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 混音类型
 */
@EnumOrdinalStrategy
public enum EmMtMixType {
	emMcuNoMix_Api, // /< 不混音
	mcuWholeMix_Api, // /< 全体混音
	mcuPartMix_Api, // /< 定制混音
	mcuVacMix_Api, // /< VAC
	mcuVacWholeMix_Api, // /< 带Vac的全体混音
}
