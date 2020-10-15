package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

@EnumOrdinalStrategy
public enum EmDcsConfMode {
    // @formatter:off
    /** 关闭数据协作 */
    emConfModeStop,
    /** 主席控制 */
    emConfModeManage,
    /** 自动协作 */
    emConfModeAuto
    // @formatter:on
}
