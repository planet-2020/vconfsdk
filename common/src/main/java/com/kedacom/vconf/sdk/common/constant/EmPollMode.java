package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 轮询模式
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmPollMode {
    emPollModeBegin,
    emVideoPoll,  ///<视频轮询
    emReserveTwo,  ///<如果平台发布对应2值，此项可以被替换
    emAudioPoll   ///<音视频轮询
}
