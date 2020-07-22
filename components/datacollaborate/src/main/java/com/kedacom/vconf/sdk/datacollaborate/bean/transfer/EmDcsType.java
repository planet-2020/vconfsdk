/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

//@JsonAdapter(Enum2IntJsonAdapter.class)
@EnumCustomValueStrategy
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

//
//    static {
//        Kson.registerAdapter(EmDcsType.class, new Enum2CustomValueJsonAdapter<EmDcsType>(){});
//    }

    private int value;

    EmDcsType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
