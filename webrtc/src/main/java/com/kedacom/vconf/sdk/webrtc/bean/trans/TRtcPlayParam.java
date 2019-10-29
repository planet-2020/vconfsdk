package com.kedacom.vconf.sdk.webrtc.bean.trans;

import com.kedacom.vconf.sdk.common.constant.EmMtVmpStyle;

import java.util.List;

/**
 * Created by Sissi on 2019/10/24
 */
public final class TRtcPlayParam {
    public EmMtVmpStyle emStyle;                               ///< 画面合成风格，手机， 志林不用填
    public List<TRtcPlayItem> atPlayItem; ///< 画面合成成员
    public int                byItemNum;

    public TRtcPlayParam(List<TRtcPlayItem> atPlayItem) {
        this.atPlayItem = atPlayItem;
        byItemNum = atPlayItem.size();
    }
}
