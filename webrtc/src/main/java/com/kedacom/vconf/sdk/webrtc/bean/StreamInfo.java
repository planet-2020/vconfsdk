package com.kedacom.vconf.sdk.webrtc.bean;

import java.util.List;

/**
 * 流信息
 *
 * Created by Sissi on 2019/10/25
 */
public final class StreamInfo {
    public String   streamId;   // 流id
    public int      streamType; // 流类型
    public String   e164;       // 流对应的终端的e164
    public String   alias;      // 流对应的终端的别名
    public String   email;      // 流对应的终端的email
    public static final int Type_Unknown = 0;
    public static final int Type_LocalMain = 1;
    public static final int Type_LocalAss = 2;
    public static final int Type_RemoteMain = 3;
    public static final int Type_RemoteAss = 4;

    public StreamInfo(String streamId, int streamType) {
        this.streamId = streamId;
        this.streamType = streamType;
    }

    public StreamInfo(String streamId, int streamType, String e164, String alias, String email) {
        this.streamId = streamId;
        this.streamType = streamType;
        this.e164 = e164;
        this.alias = alias;
        this.email = email;
    }

    @Override
    public String toString() {
        return "StreamInfo{" +
                "streamId='" + streamId + '\'' +
                ", streamType=" + streamType +
                ", e164='" + e164 + '\'' +
                ", alias='" + alias + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

}
