package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 轮询模式
 */
@EnumOrdinalStrategy
public enum EmPollMode {
    emPollModeBegin,
    emVideoPoll,  ///<视频轮询
    emReserveTwo,  ///<如果平台发布对应2值，此项可以被替换
    emAudioPoll   ///<音视频轮询
}
