package com.kedacom.vconf.sdk.base.login.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/** 服务器地址类型 */
@EnumOrdinalStrategy
public enum EmServerAddrType {
    emSrvAddrTypeDefault,     ///<默认不可修改
    emSrvAddrTypeCustom,      ///<用户自定义 可编辑
}