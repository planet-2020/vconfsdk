package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 媒体类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtMediaType {
	emMediaBegin, // 非音视频
	emMediaVideo, // 视频
	emMediaAudio, // 音频
	emMediaAV, // 音频和视频
	emMediaAssVideo, // /< 辅流视频
}
