package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * API类型
 */
@EnumOrdinalStrategy
public enum EmAPIType {
	emUnknown,
	emPlatform,
	emWeibo,
	emMeeting,
	emApp
}