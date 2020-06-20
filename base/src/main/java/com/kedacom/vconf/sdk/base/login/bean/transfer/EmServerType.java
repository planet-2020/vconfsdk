package com.kedacom.vconf.sdk.base.login.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumOrdinalStrategy;

@EnumOrdinalStrategy
public enum EmServerType {
	// @formatter:off
	emAPS, // 接入
	emXNU, // XMPP
	emSUS, // 升级
//	emPAS, // GK
	emNMS, // 网管
	emNTS, // 测速
	emSIP, // SIP呼叫
	emNonH323, ///<非标H323
	emStdH323, //<标准H323
	emNTP, // 时间同步
	emVOD, // VOD
	emMoMeeting, // 会管
	emMoPlatform, // 平台
	emVRS,
	emDCS,
	emServerTypeEnd    
	
	// @formatter:on
}
