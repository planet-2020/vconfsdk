/*
 * Copyright (c) 2018 it.kedacom.com, Inc. All rights reserved.
 */

package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

//@JsonAdapter(Enum2IntJsonAdapter.class)
@EnumOrdinalStrategy
public enum EmDcsConfType {
    // @formatter:off
    /** 点对点 */
    emConfTypeP2P,
    /** 多点 */
    emConfTypeMCC;
    // @formatter:on

//    static {
//        Kson.registerAdapter(EmDcsConfType.class, new Enum2IntJsonAdapter<EmDcsConfType>(){});
//    }
}
