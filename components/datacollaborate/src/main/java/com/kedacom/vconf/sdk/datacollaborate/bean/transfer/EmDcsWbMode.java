/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

//@JsonAdapter(Enum2IntJsonAdapter.class)
@EnumOrdinalStrategy
public enum EmDcsWbMode {
    emWbModeWB,     // 空白白板模式（非文档模式）
    emWBModeDOC;     // 文档

//    static {
//        Kson.registerAdapter(EmDcsWbMode.class, new Enum2IntJsonAdapter<EmDcsWbMode>(){});
//    }

}
