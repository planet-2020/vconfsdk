package com.kedacom.vconf.sdk.common.constant;


import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

/**
 * 别名类型
 */
@EnumOrdinalStrategy
public enum EmMtAliasType {
	/**
	 * 
	 */
	emAliasBegin,

	/**
	 * //E164
	 */
	emAliasE164,

	/**
	 * 别名类型，sip的别名也用这个
	 */
	emAliasH323,

	/**
	 * 邮箱
	 */
	emAliasEmail,

	/**
	 * //EP ID (注册GK回复该类型, 上层用不到)
	 */
	emAliasEpID,
	/**
	 * //GK ID
	 */
	emAliasGKID,

}
