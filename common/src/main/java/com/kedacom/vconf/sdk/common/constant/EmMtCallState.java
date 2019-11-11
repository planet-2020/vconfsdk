package com.kedacom.vconf.sdk.common.constant;

/**
 * 呼叫状态
 */
public enum EmMtCallState {
	emCallIdle, 
	emCallRasConfJoining,      // ras非标加入会议，等待mcu 呼叫  
	emCallRasConfCreating,     // ras非标创建会议，等待mcu 呼叫
	emCallOuting,   
	emCallIncoming,
	emCallAccepted,
	emCallHanging,
	emCallConnected,          // h323 是 225connected, sip是呼叫信令交互完
	emCallP2P,  
	emCallMCC
}