package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 视频质量模式
  */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmVideoQuality {
	emRestQualityPrecedence, // 质量优先
	emRestSpeedPrecedence; // 速度优先
}
