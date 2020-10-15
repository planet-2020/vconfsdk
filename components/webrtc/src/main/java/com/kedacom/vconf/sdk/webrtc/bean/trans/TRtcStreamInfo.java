package com.kedacom.vconf.sdk.webrtc.bean.trans;

import com.kedacom.vconf.sdk.common.type.transfer.EmMtResolution;
import com.kedacom.vconf.sdk.common.type.transfer.TMtId;

import java.util.List;

/**
 * Created by Sissi on 2019/10/24
 */
public class TRtcStreamInfo {
    public String             achStreamId;  ///流标识
    public TMtId tMtId;                               ///属于哪个终端
    public boolean             bAudio;                              ///音频还是视频
    public boolean             bMix;                                ///是不是混音
    public boolean             bAss;                                ///是不是辅流
    public int                 byMediaIndex;                        ///如果有多路主流， 多流里的索引，暂时主流只有1路，所以为0
    public List<EmMtResolution> aemSimcastRes;       ///流支持的分辨率
    public int                 byResCount;

    public TRtcStreamInfo() {
    }

    public TRtcStreamInfo(String achStreamId, boolean bAudio, boolean bAss, List<EmMtResolution> aemSimcastRes) {
        this.achStreamId = achStreamId;
        this.bAudio = bAudio;
        this.bAss = bAss;
        this.aemSimcastRes = aemSimcastRes;
    }

    public TRtcStreamInfo(String achStreamId, int mcuId, int terId, boolean bAudio, boolean bAss, List<EmMtResolution> aemSimcastRes) {
        this.achStreamId = achStreamId;
        tMtId = new TMtId(mcuId, terId);
        this.bAudio = bAudio;
        this.bAss = bAss;
        this.aemSimcastRes = aemSimcastRes;
    }

    public TRtcStreamInfo(String achStreamId, int mcuId, int terId, boolean bMix) {
        this.achStreamId = achStreamId;
        tMtId = new TMtId(mcuId, terId);
        this.bAudio = true;
        this.bAss = false;
        this.bMix = bMix;
    }

    @Override
    public String toString() {
        return "TRtcStreamInfo{" +
                "achStreamId='" + achStreamId + '\'' +
                ", tMtId=" + tMtId +
                ", bAudio=" + bAudio +
                ", bMix=" + bMix +
                ", bAss=" + bAss +
                ", byMediaIndex=" + byMediaIndex +
                ", aemSimcastRes=" + aemSimcastRes +
                ", byResCount=" + byResCount +
                '}';
    }
}
