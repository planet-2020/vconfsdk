package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 画面合成成员类型定义
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtVMPMmbType {
	none,
	emVMPMmbMCSspec, // 会控指定
	emVMPMmbSpeaker, // 发言人跟随
	emVMPMmbChairman, // 主席跟随
	emVMPMmbPoll, // 轮询视频跟随
	emVMPMmbVAC, // 语音激励(会控不要用此类型)
}
