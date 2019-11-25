package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * Created by Sissi on 2019/10/25
 */
public final class StreamInfo {
    public int      dwMcuId;                ///< mcu 号码
    public int      dwTerId;                ///< 终端 号码
    public String   streamId;         // 流id。注：此为平台合成的跟webrtc里面的streamId没关系。
    public int      streamType;         // 流id。注：此为平台合成的跟webrtc里面的streamId没关系。
    public static final int Type_LocalCamera = 1;
    public static final int Type_LocalScreenShare = 2;
    public static final int Type_RemoteCamera = 3;
    public static final int Type_RemoteScreenShare = 4;

    public StreamInfo(String streamId, int streamType) {
        this.streamId = streamId;
        this.streamType = streamType;
    }

    public StreamInfo(int dwMcuId, int dwTerId, String streamId, int streamType) {
        this.dwMcuId = dwMcuId;
        this.dwTerId = dwTerId;
        this.streamId = streamId;
        this.streamType = streamType;
    }

    @Override
    public String toString() {
        return "StreamInfo{" +
                "dwMcuId=" + dwMcuId +
                ", dwTerId=" + dwTerId +
                ", streamId='" + streamId + '\'' +
                ", streamType=" + streamType +
                '}';
    }

}
