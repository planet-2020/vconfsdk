package com.kedacom.vconf.sdk.common.constant;

import com.kedacom.vconf.sdk.utils.json.EnumCustomValueStrategy;

/**
 * 注册失败原因（对应消息RegResultNtf）
 */
@EnumCustomValueStrategy
public enum EmRegFailedReason {
	emGKFailedBegin(0),//<起始值
	emGKUnReachable(1),//<对端不可达
	emInvalidAliase(2),//<无效的别名
	emDupAlias(3), //<别名重复
	emInvalidCallAddress(4),//<无效的呼叫地址
	emResourceUnavailable(5),//<资源不可用
	//	emUnknown(6),  //未知原因, 不需要， 去掉
	emRegNumberFull(7),         // 注册数量满，PCMT绑定GK失败消息提示
	emGKSecurityDenial(8),      // GK注册权限失败
	emGKDismatch(9),            // GK不是运营版本,服务器不匹配
	emUnRegGKReq(10),           // GK被抢登后，要求注销GK
	emRRQCreateHRASFailed(11),  // rrq创建句柄失败
	emRRQSendFailed(12),        // rrq发送失败

	emSipFailedBegin(50),       //sip注册失败原因开始
	emSipLocalNormalUnreg(51),//<未注册
	emSipInvalidUserNameAndPassword(52),//<无效的用户名和密码
	emSipRegistrarUnReachable(53),//<注册不可达
	emSipInvalidAlias(54),//<无效的别名
	emSipUnknownReason(55),//<未知原因
	emSipRegisterFailed(56),//<注册失败
	emSipRegisterNameDup(57),//<注册名称重复

	//sec 注册失败reason
	emSecCrtNotFind(58),        // ca证书找不到
	emSecCrtVerifyFail(59),    // 证书验证失败
	emSecCrtExpired(60),        // 证书过期 有效期错误
	emSecCrtFormatError(61),    // 证书格式错误
	emSecLoadCertFailed(62),

	emRtcFailedBegin(70),                //rtc注册失败原因开始
	emRtcUnknownReason(71),            //未知原因
	emRtcRegistrarUnReachable(72),    //不可达
	emRtcNetworkBroken_Api(73),        //网络断开
	emRtcNeedAuth_Api(74),            //需要认证
	emRtcAuthTypeError_Api(75),        //认证类型错误
	emRtcAuthFailed_Api(76),            //认证失败
	emRtcDuplicated_Api(77),            //注册重复
	emRtcNotAllowed_Api(78),        //不允许(取消注册)

	emUnRegSuc(90),  // 取消注册成功。如果上层取消注册，底层取消成功，会发Ev_MtApi_Vc_RegResult_Ntf(emUnRegSuc)。再一个如果上册取消注册成功， 上层不需要再不断的注册下来。
	emRegSuccess(100),    ///注册成功， sip和323都是这个

	emAliInternalServer(150),           //ali服务器内部错误
	emAliInputInvaild(151),             //参数有误
	emAliWriteFileFail(152),            //写文件失败
	emAliTimeOut(153),                  //超时
	emAliYunAnomaly(154),               //阿里云服务器异常
	emAliUserInvalid(155),              //用户错误
	emAliPwdError(156),                 //密码错误
	emAliClientNoAuth(157),             //ws客户端没有权限验证
	emAliTokenInvaild(158),             //无效的token
	emAliUserLogged(159),               //用户已经登录
	emAliUserNoPermission(160),         //权限不足,拒绝访问
	emAliUserInConf(161),               //用户在会议中
	emAliUserLoginRemote(162),          //抢登
	emAliUserDisconnect(163),           //用户断链

	emRegReasonEnd(1000);

	public int value;
	EmRegFailedReason(int val) {
		this.value=val;
	}

	public int getValue() {
		return value;
	}
}