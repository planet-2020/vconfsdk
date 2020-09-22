package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 会议类型
 */

@EnumOrdinalStrategy
public enum EmMeetingSafeType {
    emRestMeetingType_Public,   // 传统媒体会议
    emRestMeetingType_Port,     // 端口会议
    emRestMeetingType_Sfu,       // 纯转发会议(webrtc)
    emRestMeetingType_Mix_Api,       //混合会议(既是端口会议又是纯转发会议)
    emRestMeetingType_Auto_Api,      //平台根据会议类型能力和终端支持的协议进行自动匹配
}