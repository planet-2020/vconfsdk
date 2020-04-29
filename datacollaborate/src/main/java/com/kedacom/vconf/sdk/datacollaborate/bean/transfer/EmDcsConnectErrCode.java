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

//    static {
//        Kson.registerAdapter(EmDcsConnectErrCode.class, new Enum2CustomValueJsonAdapter<EmDcsConnectErrCode>(){});
//    }
    private int value;

    EmDcsConnectErrCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
