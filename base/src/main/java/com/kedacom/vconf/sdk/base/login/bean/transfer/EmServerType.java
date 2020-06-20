package com.kedacom.vconf.sdk.base.login.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

@EnumCustomValueStrategy
public enum EmServerType {
	emAPS(0), // 接入
	emXNU(1), // XMPP
	emSUS(2), // 升级
//	emPAS(3), // GK
	emNMS(3), // 网管
	emNTS(4), // 测速
	emSIP(5), // SIP呼叫
	emNonH323(6), ///<非标H323
	emStdH323(7), //<标准H323
	emNTP(8), // 时间同步
	emVOD(9), // VOD
	emMoMeeting(10), // 会管
	emMoPlatform(11), // 平台
	emVRS(12),	//录像
	emDCS(13),	// 数据协作
	emNS_Api(14),   // 推送
	emKIS_Api(15),
	emWebRtc_Api(16),	 //webrtc
	emCSV_Api(17),	 //用于处理阿里云rtc会议的服务器

	emServerTypeEnd(18);

	int value;

	EmServerType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

}
