package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 录像模式
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmRecordMode {
    emStartRecordMode,
    emRecordMode,       // 录像
    emLiveMode,         // 直播
    emRecordLiveMode    // 录像+直播
}
