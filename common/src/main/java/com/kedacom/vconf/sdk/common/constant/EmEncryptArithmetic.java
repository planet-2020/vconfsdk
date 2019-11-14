package com.kedacom.vconf.sdk.common.constant;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

/**
 * 加密算法
 * */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmEncryptArithmetic {
	/**
	 * 不加密
	 */
	emEncryptNone,

	/**
	 * DES加密
	 */
	emDES,

	/**
	 * AES加密
	 */
	emAES,

	/**
	 * SM1加密
	 */
	emSM1,

	/**
	 * SM4加密
	 */
	emSM4,

	/**
	 * 未定义加密
	 */
	emUndefine1,
	emUndefine2,
	emUndefine3,
	emUndefine4,
	emUndefine5,

	/**
	 * 自动加密
	 */
	emAuto;
}