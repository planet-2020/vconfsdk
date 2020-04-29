/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;
import com.kedacom.vconf.sdk.utils.json.Kson;

//@JsonAdapter(Enum2IntJsonAdapter.class)

@EnumCustomValueStrategy
public enum EmDcsConfMode {
    // @formatter:off
    /** 关闭数据协作 */
    emConfModeStop(0),
    /** 主席控制 */
    emConfModeManage(1),
    /** 自动协作 */
    emConfModeAuto(2);
    // @formatter:on

//    static {
//        Kson.registerAdapter(EmDcsConfMode.class, new Enum2CustomValueJsonAdapter<EmDcsConfMode>(){});
//    }

    private int value;

    EmDcsConfMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
