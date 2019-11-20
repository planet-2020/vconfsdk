package com.kedacom.vconf.sdk.webrtc.bean;

/**
 * Created by Sissi on 2019/10/25
 */
public final class StreamInfo {
    public int         dwMcuId;                ///< mcu 号码
    public int         dwTerId;                ///< 终端 号码
    public String   streamId;         // 流id。注：此为平台合成的跟webrtc里面的streamId没关系。

    public StreamInfo(int dwMcuId, int dwTerId, String streamId) {
        this.dwMcuId = dwMcuId;
        this.dwTerId = dwTerId;
        this.streamId = streamId;
    }

    @Override
    public String toString() {
        return "StreamInfo{" +
                "dwMcuId=" + dwMcuId +
                ", dwTerId=" + dwTerId +
                ", streamId='" + streamId + '\'' +
                '}';
    }
}
