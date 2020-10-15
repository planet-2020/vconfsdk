package com.kedacom.vconf.sdk.common.type.transfer;

public class TRestErrorInfo{
	public String achErrorInfo; // 错误描述
	public EmAPIType emApiType; // 对应的api 类型
	public int dwErrorID; // 错误码
	public int dwNackEventId; // 对应的nack event id
	int      adwParams[];               ///< 保留的参数
	int      dwParamCount;
}