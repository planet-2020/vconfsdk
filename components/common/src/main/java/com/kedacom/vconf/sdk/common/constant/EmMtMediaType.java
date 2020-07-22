package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 媒体类型
 */
@EnumOrdinalStrategy
public enum EmMtMediaType {
	emMediaBegin, // 非音视频
	emMediaVideo, // 视频
	emMediaAudio, // 音频
	emMediaAV, // 音频和视频
	emMediaAssVideo, // /< 辅流视频
}
