package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 录像模式
 */
@EnumOrdinalStrategy
public enum EmRecordMode {
    emStartRecordMode,
    emRecordMode,       // 录像
    emLiveMode,         // 直播
    emRecordLiveMode    // 录像+直播
}
