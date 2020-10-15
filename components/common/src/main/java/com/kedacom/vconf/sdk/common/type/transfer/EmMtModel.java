package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

/**
 * 终端型号
 */

@EnumCustomValueStrategy
public enum EmMtModel {
    emModelBegin(0),
    emSkyWindows(1), // /<桌面终端 自建
    emSkyIPad(2), // /<移动终端ipad 自建
    emSkyIPhone(3), // /<移动终端iphone 自建
    emSkyAndroidPad(7), // /<移动终端androidpad 自建
    emSkyAndroidPhone(8), // /<移动终端androidphone 自建
    emX500_1080P60(9), // /<硬终端X500 自建
    emX500_1080P30(10), // /<硬终端X500 自建
    emX500_720P60(11), // /<硬终端X500 自建
    emX500_mo_1080P(12), // /<硬终端X500 租赁
    emX500_mo_720P(13), // /<硬终端X500 租赁
    emX500_oem_1080P(14), // /<硬终端X500 自建
    emX500_oem_mo_1080P(15), // /<硬终端X500 租赁

    emTrueLink(16), // /<桌面终端 租赁
    emTrueTouchIpad(17), // /<移动终端ipad 租赁
    emTrueTouchIphone(18), // /<移动终端iphone 租赁
    emTrueTouchAndroidPhone(19), // /<移动终端androidphone 租赁
    emTrueTouchAndroidPad(20),// /<移动终端androidpad 租赁

    emH950_1080P60(21), ///<硬终端 自建
    emTrueLinkTV_Api(22),       ///<移动终端TV盒子 租赁

    emX700_4k30_Api(23),               //x700-4k30 自建
    emX700_oem_4k30_Api(24),           //x700-4k30 oem 自建
    emX500_4k30_Api(25),               //x500-4k30 自建
    emX500_oem_4k30_Api(26),           //x500-4k30 oem 自建

    emX300_1080P60_Api(27),            //x300-1080P60  自建
    emX300_1080P30_Api(28),           //x300-1080P30  自建
    emX300_720P60_Api(29),             //x300-720P60   自建
    emX300_oem_1080P60_Api(30),       //x300-1080P60 oem 自建

    emX500_1080P_60_Api(31),           //x500-1080P-1080P60  自建  SKY X500-1080P 终端 和从9-15的x500终端不一样，芯片不一样
    emX500_1080P_30_Api(32),           //x500-1080P-1080P30  自建
    emX500_oem_1080P_60_Api(33),       //x500-1080P-1080P60 oem 自建

    em300_1080P30_Api(34),             //300-1080P30  自建
    em300_720P60_Api(35),              //300-720P60  自建
    em300_oem_1080P30_Api(36),         //300-1080P30  oem 自建
    em300L_1080P30_Api(37),           //300L-1080P30  自建
    em300L_720P60_Api(38),            //300L-720P60  自建
    em300L_oem_1080P30_Api(39),        //300L-1080P30  oem 自建

    emSkyAndroidPad_s_Api(54),          //< 商密移动终端androidpad
    emSkyAndroidPhone_s_Api(55);        //< 商密移动终端androidphone

    private final int value;

    EmMtModel(int i) {
        value = i;
    }

    public int getValue() {
        return value;
    }
}