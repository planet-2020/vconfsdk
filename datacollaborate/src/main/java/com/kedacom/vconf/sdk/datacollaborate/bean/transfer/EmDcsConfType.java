/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Kson;

//@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsConfType {
    // @formatter:off
    /** 点对点 */
    emConfTypeP2P,
    /** 多点 */
    emConfTypeMCC;
    // @formatter:on
    static {
        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
        Kson.registerAdapter(EmDcsConfType.class, new Enum2IntJsonAdapter());
    }
}
