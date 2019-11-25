package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * Created by Sissi on 2019/10/25
 */
public final class StreamInfo {
    public int      mcuId;                ///< mcu 号码
    public int      terId;                ///< 终端 号码
    public String   streamId;         // 流id。注：此为平台合成的跟webrtc里面的streamId没关系。
    public int      streamType;
    public static final int Type_Unknown = 0;
    public static final int Type_LocalCamera = 1;
    public static final int Type_LocalScreenShare = 2;
    public static final int Type_RemoteCamera = 3;
    public static final int Type_RemoteScreenShare = 4;

    public StreamInfo(String streamId, int streamType) {
        this.streamId = streamId;
        this.streamType = streamType;
    }

    public StreamInfo(int mcuId, int terId, String streamId, int streamType) {
        this.mcuId = mcuId;
        this.terId = terId;
        this.streamId = streamId;
        this.streamType = streamType;
    }

}
