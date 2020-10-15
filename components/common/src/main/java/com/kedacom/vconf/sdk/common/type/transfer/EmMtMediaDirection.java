package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 通道类型
 */
@EnumOrdinalStrategy
public enum EmMtMediaDirection {
	emDirectionBegin, emChannelSend, emChannelRecv,
}