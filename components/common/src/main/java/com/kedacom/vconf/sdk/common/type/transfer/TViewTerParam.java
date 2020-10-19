package com.kedacom.vconf.sdk.common.type.transfer;

public class TViewTerParam {
    public TMtId tTer; ///< 终端
    public EmMtMediaType emViewType; ///< 音频/视频/音视频
    public boolean bViewAllChan; ///< 是否选看这个终端的所有通道
    public EmCodecComponentIndex emChanIdx; ///< 如果否,具体通道id
    //rtc下使用
    public String achStreamId;  ///流标识
    public boolean bForce;  ///是否强制
}
