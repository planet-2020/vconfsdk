package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
  * 会议免打扰
  */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmClosedMeeting {
	emClosedMeeting_Close, // 关闭
	emClosedMeeting_Open, // 开启
}
