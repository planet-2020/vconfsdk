package com.kedacom.vconf.sdk.webrtc.bean.trans;

import com.kedacom.vconf.sdk.common.type.transfer.EmCodecComponent;
import com.kedacom.vconf.sdk.common.type.transfer.EmCodecComponentIndex;
import com.kedacom.vconf.sdk.common.type.transfer.EmMtResolution;

/**
 * Created by Sissi on 2019/10/24
 */
public final class TRtcPlayItem {
    public long                        dwPlayIdx;                           ///<播放索引值， 志林填句柄， 硬终端填数组下标
    public boolean                     bLocal;                              ///<本地还是远端
    public String                      achStreamId;  ///<如果是远端，远端流标识
    public boolean                     bAss;                                ///<是否是远端双流
    public EmMtResolution emRes;                               ///<如果是远端，播放的流的分辨率
    public EmCodecComponent emLocalChan;                         ///<本地码流
    public EmCodecComponentIndex byLocalChanIdx;                      ///<本地第几路

    public boolean bSubBigSstream;  // 是否为大画面码流。平台根据此标记针对该路码流做优化

    private String achMtName;    // windows专用


    public TRtcPlayItem(String achStreamId, boolean bAss, EmMtResolution emRes) {
        this.bLocal = false;
        this.achStreamId = achStreamId;
        this.bAss = bAss;
        this.emRes = emRes;
    }

    public TRtcPlayItem(String achStreamId, boolean bAss, EmMtResolution emRes, boolean bSubBigSstream) {
        this.bLocal = false;
        this.achStreamId = achStreamId;
        this.bAss = bAss;
        this.emRes = emRes;
        this.bSubBigSstream = bSubBigSstream;
    }

    public void setbSubBigSstream(boolean bSubBigSstream) {
        this.bSubBigSstream = bSubBigSstream;
    }
}
