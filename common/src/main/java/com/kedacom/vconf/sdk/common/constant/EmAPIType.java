package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * API类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmAPIType {
	emUnknown,
	emPlatform,
	emWeibo,
	emMeeting,
	emApp
}