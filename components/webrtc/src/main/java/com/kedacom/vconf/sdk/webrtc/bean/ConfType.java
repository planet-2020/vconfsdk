package com.kedacom.vconf.sdk.webrtc.bean;

public enum ConfType {
    TRADITIONAL,   // 传统媒体会议
    PORT,     // 端口会议
    RTC,       // 纯转发会议(webrtc)
    MIX,       //混合会议(既是端口会议又是纯转发会议)
    AUTO,      //平台根据会议类型能力和终端支持的协议进行自动匹配
}
