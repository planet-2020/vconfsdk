package com.kedacom.vconf.sdk.common.type.transfer;


import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

/**
 * 呼叫断开原因
 * */

@EnumCustomValueStrategy
public enum EmMtCallDisReason {
	emDisconnect_Busy(0),              		// 对端忙
	emDisconnect_Normal(1),                	// 正常挂断
	emDisconnect_Rejected(2),              	// 对端拒绝
	emDisconnect_Unreachable(3),           	// 对端不可达
	emDisconnect_Local(4),                 	// 本地原因
	emDisconnect_Unknown(5),               	// 未知原因
	emDisconnect_custom(6),                	// 自定义原因
	emDisconnect_AdaptiveBusy (7),		    // 接入电话终端失败
	emDisconnect_Occupy(8), 					// 其他终端或者mcu抢断

	//下面原因是自定义的,上面是与协议栈对应起来的标准的原因
	emDisconnect_Joinconftimeout(20),			// 参加会议超时
	emDisconnect_Createconftimeout(21),		// 召集会议超时
	emDisconnect_Nomediaresource(22),			// 没有媒体资源
	emDisconnect_Exceedmaxinconfmtnum(23),	// 超过会议最大终端数（参加会议时）
	emDisconnect_Exceedmaxconfnum(24),	    // 超过会议最大数（创建会议时）
	emDisconnect_EncrypeErr(25),            	// 与会议加密模式不符
	emDisconnect_P2Ptimeout(26),            	// 点对点呼叫超时
	emDisconnect_MccDrop(27),               	// 会控挂断
	emDisconnect_ChairDrop(28),             	// 主席挂断
	emDisconnect_MMcuDrop(29),              	// 上级会议挂断
	emDisconnect_ConfRelease(30),           	// 会议结束挂断
	emDisconnect_PeerInConf(31),            	// 正在会议中
	emDisconnect_PeerNoDisturb(32),         	// 免打扰
	emDisconnect_NotInPeerContact(33),      	// 非好友
	emDisconnect_PeerNoP2PPermission(34),   	// 对端无点对点权限
	emDisconnect_PeerOnSleeping(35),			// 对端正在待机
	emDisconnect_ConfAutoRelease(36),			// 会议自动结束挂断
	emDisconnect_REASON_BUSYEXT(37),        	// 终端忙,带终端目前所在会议的级别及会议名称(邀请终端失败时原因)
	emDisconnect_REASON_REMOTERECONNECT(38),	// 本端行政级别低，由远端自动发起重连(邀请终端失败时原因)
	emDisConnect_CallNumExceed(39),          	// 呼叫数超限
	emDisConnect_NotRegedToCallFailed(40),	// 本地没有注册成功导致呼叫别名或者e164号码有问题

	emDisConnect_LocalVodPlaying(41),         // 本地正在vod点播中

	// sec 注册失败reason
	emDisConnect_SecCrtNotFind(42),   		// ca证书找不到
	emDisConnect_SecCrtVerifyFail(43),  		// 证书验证失败
	emDisConnect_SecCrtExpired(44),   		// 证书过期 有效期错误
	emDisConnect_SecCrtFormatError(45),   	// 证书格式错误
	emDisConnect_SecLoadCertFailed(46),   	// 证书加载失败
	emDisConnect_SecEncTypeError(47),     	// 加密类型不一致
	emDisConnect_AuthenticationErr(48),      	// 强认证报错 一般是终端未强认证主呼强认证会议
	emDisConnect_MtHasInConf(49),      		// 主席邀请同一个终端入会提示
	emDisConnect_MtEviction(50),      		// 强拆方式主动退会
	emDisConnect_SoftIntegrityTestErr(51),  	// 软件完整性自测试失败
	emDisConnect_GmTestErr(52),  				// 国密算法自测试失败
	emDisConnect_RandomNumTestErr(53),  		// 随机数自测试失败

	emDisConnect_StrongAuthButIpCallErr(54), 	// 强认证，不允许ip呼叫
	emDisConnect_ChannelNoAuth(55), 			// 渠道产品未经授权
	emDisConnect_QtCallButNotGetPeerE164(56),	// 量通呼叫，没有获取到对端的e164号码
	emDisconnect_CancleInputPwd(57), 			// 取消输入会议密码
	emDisconnect_ElseWhereConf(58),    		// 该账号别处入会
	emDisconnect_End(100);                 	// 前面用于扩展


	private final int value;

	EmMtCallDisReason(int i) {
		value = i;
	}

	public int getValue() {
		return value;
	}

}