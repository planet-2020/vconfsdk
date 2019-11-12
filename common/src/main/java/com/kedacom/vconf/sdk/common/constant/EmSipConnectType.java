package com.kedacom.vconf.sdk.common.constant;

//////商密 SIP连接类型

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;

@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmSipConnectType
{
	emSipUdpConnect_Api,
	emSipTcpConnect_Api,
	emSipTlsConnect_Api,
	emSipGMTlsConnect_Api
}