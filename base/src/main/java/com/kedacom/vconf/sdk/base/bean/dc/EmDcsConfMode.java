/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.kedacom.vconf.sdk.annotation.SerializeEnumAsInt;

@SerializeEnumAsInt
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
