package com.kedacom.vconf.sdk.base.basement;

final class FeedbackBundle {
    // 消息类型
    static final int NTF = 101; // 通知
    static final int RSP = 102; // 响应
    static final int RSP_FIN = 103; // 响应结束（收到响应序列中的最后一条响应）
    static final int RSP_TIMEOUT = 104; // 请求超时。
    static final int RSP_USER_CANCELED = 105; // 用户取消请求。
    static final int RSP_USER_CANCEL_FAILED = 106; // 用户取消请求失败（如会话不存在）。

    String msgId;  // 消息Id
    Object body; // 消息体
    int type;   // 消息类型：NTF、RSP、FIN、TIMEOUT
    String reqId; // 对应的请求消息名称
    int reqSn;  // 对应的请求序列号
    Object[] reqParas;

    FeedbackBundle(String msgId, Object body, int type, String reqId, int reqSn, Object[] reqParas) {
        this.msgId = msgId;
        this.body = body;
        this.type = type;
        this.reqId = reqId;
        this.reqSn = reqSn;
        this.reqParas = reqParas;
    }

    FeedbackBundle(String msgId, Object body, int type) {
        this.msgId = msgId;
        this.body = body;
        this.type = type;
    }

}
