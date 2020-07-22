package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 视频质量模式
  */
@EnumOrdinalStrategy
public enum EmVideoQuality {
	emRestQualityPrecedence, // 质量优先
	emRestSpeedPrecedence; // 速度优先
}
