package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 终端地址类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
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