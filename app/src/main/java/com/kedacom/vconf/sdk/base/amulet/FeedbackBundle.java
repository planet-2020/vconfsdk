package com.kedacom.vconf.sdk.base.amulet;

/**
 * Created by Sissi on 2018/9/13.
 */
class FeedbackBundle {
    // 消息类型
    static final int NTF = 101; // 通知
    static final int RSP = 102; // 响应
    static final int RSP_FIN = 103; // 响应结束（收到响应序列中的最后一条响应）
    static final int RSP_TIMEOUT = 104; // 响应超时。（TODO 是否要增加类似RSP_NETWORK_UNAVAILABLE的消息类型？为了使功能更加内聚，同时为了避免复杂化（如果要添加则不仅是一个要考虑可能会有许多类似消息）暂时倾向于不添加，仅关注会话流程相关的消息）

    String name;  // 响应消息名称
    Object body; // 响应消息体
    int type;   // 响应类型：NTF、RSP、FIN、TIMEOUT
    String reqName; // 对应的请求消息名称
    int reqSn;  // 对应的请求序列号

    FeedbackBundle(String name, Object body, int type, String reqName, int reqSn) {
        this.name = name;
        this.body = body;
        this.type = type;
        this.reqName = reqName;
        this.reqSn = reqSn;
    }

    FeedbackBundle(String name, Object body, int type) {
        this.name = name;
        this.body = body;
        this.type = type;
    }

}
