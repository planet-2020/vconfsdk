package com.sissi.vconfsdk.frame;

/**
 * 协议．用于解析收到的json格式消息。
 *
 * Created by Sissi on 2018/9/13.
 */

class Contract {

    static final String KEY_MTAPI = "mtapi";
    static final String KEY_HEAD = "head";
    static final String KEY_BODY = "body";
    static final String KEY_EVENTNAME = "eventname";
    static final String KEY_BASE_TYPE = "basetype";

    /** 响应消息头定义 */
    static class Head {
        int eventid; // 消息ID（底层的消息序号，暂时没用）
        String eventname; // 消息名称
        int SessionID;   // （用于硬终端）
        public Head(int eventId, String eventName, int sessionID){
            eventid = eventId;
            eventname = eventName;
            SessionID = sessionID;
        }
    }

    /** 响应消息结构定义 */
    static class Mtapi {
        Head head; // 消息头
        Object body; // 消息体
        public Mtapi(Head head, Object body){
            this.head = head;
            this.body = body;
        }
    }

    static class RspWrapper {
        Mtapi mtapi;
        public RspWrapper(Mtapi mtapi){this.mtapi = mtapi;}
    }
}
