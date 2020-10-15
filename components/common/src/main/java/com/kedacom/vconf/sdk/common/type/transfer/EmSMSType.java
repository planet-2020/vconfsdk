package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 短消息业务类型
 * */
@EnumOrdinalStrategy
public enum EmSMSType {
	emSMSSingleLine, // 短消息
	emSMSPageTitle, // 翻页字幕
	emSMSRollTitle, // 滚动字幕
	emSMSStaticTitle, // 静态字幕
	emSMSUpRollTitle, // 向上滚动短消息
}