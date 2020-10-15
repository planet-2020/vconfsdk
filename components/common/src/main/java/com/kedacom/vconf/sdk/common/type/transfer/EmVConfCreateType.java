package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 创会类型
 */
@EnumOrdinalStrategy
public enum EmVConfCreateType {
    /** 实时会议 */
    emCreateNormalConf,
    /** 虚拟会议室 */
    emCreateVirtualConf,
    /** 根据模板ID创会 */
    emCreateConfByTemplate
}
