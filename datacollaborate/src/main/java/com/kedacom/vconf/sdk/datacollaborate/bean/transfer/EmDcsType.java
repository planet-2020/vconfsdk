/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2CustomValueJsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Kson;

//@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsType {
    // @formatter:off
    /** 未知 */
    emTypeUnknown(0),
    /** 致玲 */
    emTypeTrueLink(1),
    /** 手机-IOS */
    emTypeTrueTouchPhoneIOS(2),
    /** 平板-IOS */
    emTypeTrueTouchPadIOS(3),
    /** 手机-android */
    emTypeTrueTouchPhoneAndroid(4),
    /** 平板-android */
    emTypeTrueTouchPadAndroid(5),
    /** 硬终端 */
    emTypeTrueSens(6),
    /** imix */
    emTypeIMIX(7),
    /** 第三方终端 */
    emTypeThirdPartyTer(8),
    /** 无效的终端型号 */
    emTypeButt(255);
    // @formatter:on

    static {
        // 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
        Kson.registerAdapter(EmDcsType.class, new Enum2CustomValueJsonAdapter());
    }

    private int value;

    EmDcsType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
