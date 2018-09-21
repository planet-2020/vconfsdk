package com.sissi.vconfsdk.base.amulet;

/**
 * Created by Sissi on 2018/9/13.
 */
class ResponseBundle {  //TODO 更名为FeedbackBundle
    // 消息类型
    static final int NTF = 101; // 通知
    static final int RSP = 102; // 响应
    static final int RSP_FIN = 103; // 响应结束（收到响应序列中的最后一条响应）
    static final int RSP_TIMEOUT = 104; // 响应超时

    String name;  // 响应消息名称
    Object body; // 响应消息体
    int type;   // 响应类型：NTF、RSP、FIN、TIMEOUT
    String reqName; // 对应的请求消息名称
    int reqSn;  // 对应的请求序列号

    ResponseBundle(String name, Object body, int type, String reqName, int reqSn) {
        this.name = name;
        this.body = body;
        this.type = type;
        this.reqName = reqName;
        this.reqSn = reqSn;
    }

    ResponseBundle(String name, Object body, int type) {
        this.name = name;
        this.body = body;
        this.type = type;
    }

}
