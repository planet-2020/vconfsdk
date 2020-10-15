package com.kedacom.vconf.sdk.common.type.transfer;

//////商密 SIP连接类型

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

@EnumOrdinalStrategy
public enum EmSipConnectType
{
	emSipUdpConnect_Api,
	emSipTcpConnect_Api,
	emSipTlsConnect_Api,
	emSipGMTlsConnect_Api
}