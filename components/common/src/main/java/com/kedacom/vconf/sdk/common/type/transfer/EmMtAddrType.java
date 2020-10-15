package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 终端地址类型
 */
@EnumOrdinalStrategy
public enum EmMtAddrType {
	/**
	 * ip
	 */
	emAddrIP,

	/**
	 * e164
	 */
	emAddrE164,

	/**
	 * emAddrAlias
	 */
	emAddrAlias,

	/**
	 * Dial Num
	 */
	emDialNum,

	/**
	 * sip
	 */
	emSipAddr,
	/**
	 * MOID
	 */
	emAddrMoid;

}