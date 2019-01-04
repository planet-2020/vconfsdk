/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.SerializedName;
import com.kedacom.vconf.sdk.annotation.SerializeEnumAsInt;

@SerializeEnumAsInt
public enum EmDcsWbMode {
    @SerializedName("0")
    emWbModeWB,     // 空白白板模式（非文档模式）
    @SerializedName("1")
    emWBModeDOC     // 文档
}
