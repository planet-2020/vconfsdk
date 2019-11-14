package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 呼叫模式
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmCallMode {
    emMannualCall_Api,    //手动呼叫
    emResCall_Api,        //暂不启用
    emAutoCall_Api        //自动、定时呼叫
}
