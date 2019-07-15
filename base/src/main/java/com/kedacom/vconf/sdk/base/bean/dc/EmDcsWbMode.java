/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.tools.Enum2IntJsonAdapter;

@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsWbMode {
    emWbModeWB,     // 空白白板模式（非文档模式）
    emWBModeDOC     // 文档
}
