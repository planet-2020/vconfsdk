package com.kedacom.vconf.sdk.base.login.bean.transfer;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

@EnumCustomValueStrategy
public enum EmServerState {
	emSrvIdle(0), // 空闲
	emSrvDnsQuerying(1), // 正在解析IP
	emSrvLogging_In(2), // 正在登录
	emSrvLogin_Succ(3), // 登录成功
	emSrvLogging_Out(4), // 正在登出
	emSrvDisconnected(5), // 断链（适用于保持心跳的服务器）
	emSrvLogin_Err(6);

	int value;

	EmServerState(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}