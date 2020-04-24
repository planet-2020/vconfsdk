/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2CustomValueJsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Kson;

//@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsConnectErrCode {
    // @formatter:off
    emUnknown(0),
    /** 会议服务器建链成功 */
    emConfSuccess(1),
    /** 会议服务器建链失败 */
    emConfFailed(2),
    /** 会议服务器中途断链 */
    emConfDisconnect(3),
    /** 注册服务器建链成功 */
    emLoginSuccess(4),
    /** 注册服务器建链失败 */
    emLoginFailed(5),
    /** 注册服务器建链中途断链 */
    emLoginDisconnect(6);
    // @formatter:on

    static {
        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
        Kson.registerAdapter(EmDcsConnectErrCode.class, new Enum2CustomValueJsonAdapter());
    }

    private int value;

    EmDcsConnectErrCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
