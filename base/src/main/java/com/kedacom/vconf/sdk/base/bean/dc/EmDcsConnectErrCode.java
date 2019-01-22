/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.kedacom.vconf.sdk.annotation.SerializeEnumAsInt;

@SerializeEnumAsInt
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

    private int value;

    EmDcsConnectErrCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
