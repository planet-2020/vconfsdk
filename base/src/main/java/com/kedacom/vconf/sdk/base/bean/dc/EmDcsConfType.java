/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.base.bean.dc;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.base.tools.Enum2IntJsonAdapter;

@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmDcsConfType {
    // @formatter:off
    /** 点对点 */
    emConfTypeP2P,
    /** 多点 */
    emConfTypeMCC
    // @formatter:on
}
