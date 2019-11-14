package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 视频格式
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmVidFormat {
	emVH261,
	emVH262,
	emVH263,
	emVH263plus,
	emVH264,
	emVHMPEG4,
	emVH265,
	emVEnd;
}