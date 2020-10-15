package com.kedacom.vconf.sdk.common.type.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 呼叫模式
 */

@EnumOrdinalStrategy
public enum EmCallMode {
    emMannualCall_Api,    //手动呼叫
    emResCall_Api,        //暂不启用
    emAutoCall_Api        //自动、定时呼叫
}
