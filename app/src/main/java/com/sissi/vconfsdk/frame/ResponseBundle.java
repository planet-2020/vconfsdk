package com.sissi.vconfsdk.frame;

/**
 * Created by Sissi on 2018/9/13.
 */
public class ResponseBundle {
    // 消息类型
    public static final int NTF = 101; // 通知
    public static final int RSP = 102; // 响应
    public static final int RSP_FIN = 103; // 响应结束（收到响应序列中的最后一条响应）
    public static final int RSP_TIMEOUT = 104; // 响应超时

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

    public String name() {
        return name;
    }

    public Object body() {
        return body;
    }

    public int type() {
        return type;
    }

    public String reqName() {
        return reqName;
    }

    public int reqSn() {
        return reqSn;
    }
}
