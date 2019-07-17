/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsConfMode {
    // @formatter:off
    /** 关闭数据协作 */
    emConfModeStop,
    /** 主席控制 */
    emConfModeManage,
    /** 自动协作 */
    emConfModeAuto
    // @formatter:on
}
