package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 创会类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmVConfCreateType {
    /** 实时会议 */
    emCreateNormalConf,
    /** 虚拟会议室 */
    emCreateVirtualConf,
    /** 根据模板ID创会 */
    emCreateConfByTemplate
}
