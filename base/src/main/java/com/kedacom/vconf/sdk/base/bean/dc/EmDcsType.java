/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.SerializedName;
import com.kedacom.vconf.sdk.annotation.SerializeEnumAsInt;

@SerializeEnumAsInt
public enum EmDcsType {
    // @formatter:off
    /** 未知 */
    @SerializedName("0")
    emTypeUnknown(0),
    /** 致玲 */
    @SerializedName("1")
    emTypeTrueLink(1),
    /** 手机-IOS */
    @SerializedName("2")
    emTypeTrueTouchPhoneIOS(2),
    /** 平板-IOS */
    @SerializedName("3")
    emTypeTrueTouchPadIOS(3),
    /** 手机-android */
    @SerializedName("4")
    emTypeTrueTouchPhoneAndroid(4),
    /** 平板-android */
    @SerializedName("5")
    emTypeTrueTouchPadAndroid(5),
    /** 硬终端 */
    @SerializedName("6")
    emTypeTrueSens(6),
    /** imix */
    @SerializedName("7")
    emTypeIMIX(7),
    /** 第三方终端 */
    @SerializedName("8")
    emTypeThirdPartyTer(8),
    /** 无效的终端型号 */
    @SerializedName("255")
    emTypeButt(255);
    // @formatter:on

    private int value;

    EmDcsType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
