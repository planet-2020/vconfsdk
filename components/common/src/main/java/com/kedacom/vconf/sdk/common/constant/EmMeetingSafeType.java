package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 会议类型
 */

@EnumOrdinalStrategy
public enum EmMeetingSafeType {
    emRestMeetingType_Public,   // 传统媒体会议
    emRestMeetingType_Port,     // 端口会议
    emRestMeetingType_Sfu       // 纯转发会议(webrtc)
}