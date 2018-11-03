package com.kedacom.vconf.sdk.base.basement;

final class FeedbackBundle {
    // 消息类型
    static final int NTF = 101; // 通知
    static final int RSP = 102; // 响应
    static final int RSP_FIN = 103; // 响应结束（收到响应序列中的最后一条响应）
    static final int RSP_TIMEOUT = 104; // 响应超时。（TODO 是否要增加类似RSP_NETWORK_UNAVAILABLE的消息类型？为了使功能更加内聚，同时为了避免复杂化（如果要添加则不仅是一个要考虑可能会有许多类似消息）暂时倾向于不添加，仅关注会话流程相关的消息）

    String msgId;  // 消息Id
    Object body; // 消息体
    int type;   // 消息类型：NTF、RSP、FIN、TIMEOUT
    String reqId; // 对应的请求消息名称
    int reqSn;  // 对应的请求序列号

    FeedbackBundle(String msgId, Object body, int type, String reqId, int reqSn) {
        this.msgId = msgId;
        this.body = body;
        this.type = type;
        this.reqId = reqId;
        this.reqSn = reqSn;
    }

    FeedbackBundle(String msgId, Object body, int type) {
        this.msgId = msgId;
        this.body = body;
        this.type = type;
    }

}
