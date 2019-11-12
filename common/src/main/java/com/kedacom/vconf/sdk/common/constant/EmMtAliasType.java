package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 别名类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
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
