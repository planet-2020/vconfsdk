package com.kedacom.vconf.sdk.base.basement;

final class FeedbackBundle {
    // 消息类型
    static final int NTF = 101; // 通知
    static final int RSP = 102; // 响应
    static final int RSP_FIN = 103; // 响应结束（收到响应序列中的最后一条响应）
    static final int RSP_TIMEOUT = 104; // 响应超时。

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
