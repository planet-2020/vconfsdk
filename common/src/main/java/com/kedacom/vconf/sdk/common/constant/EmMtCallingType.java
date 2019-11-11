package com.kedacom.vconf.sdk.common.constant;

/**
 * 呼叫类型
 * */
public enum EmMtCallingType {
	emJoinConf_Api, // /<ras非标加入会议，等待mcu 呼叫
	emCreateConf_Api, // /<ras非标创建会议，等待mcu 呼叫
	emOutCall_Api, // /<呼出
	emIncomingCall_Api, // /<呼入
}
