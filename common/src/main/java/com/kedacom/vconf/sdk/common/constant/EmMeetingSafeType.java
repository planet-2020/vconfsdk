package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 会议类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMeetingSafeType {
    emRestMeetingType_Public,   // 传统媒体会议
    emRestMeetingType_Port,     // 端口会议
    emRestMeetingType_Sfu       // 纯转发会议(webrtc)
}