package com.kedacom.vconf.sdk.common.constant;


import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;
/**
 * 通道类型
 */
@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmMtMediaDirection {
	emDirectionBegin, emChannelSend, emChannelRecv,
}