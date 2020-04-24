package com.kedacom.vconf.sdk.datacollaborate.bean.transfer;

import com.google.gson.annotations.JsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Enum2IntJsonAdapter;
import com.kedacom.vconf.sdk.utils.json.Kson;

/**
 * 服务器连接状态
 */
//@JsonAdapter(Enum2IntJsonAdapter.class)
public enum EmServerState {
	emSrvIdle, // 空闲
	emSrvDnsQuerying, // 正在解析IP
	emSrvLogging_In, // 正在登录
	emSrvLogin_Succ, // 登录成功
	emSrvLogging_Out, // 正在登出
	emSrvDisconnected, // 断链（适用于保持心跳的服务器）
	emSrvLogin_Err;

	static {
		// 通过JsonAdapter注解的方式注册适配器更加便捷，但该注解是Gson2.3引入的，有的用户可能必须使用老版Gson，故回退使用老方式注册。
		Kson.registerAdapter(EmServerState.class, new Enum2IntJsonAdapter());
	}
}