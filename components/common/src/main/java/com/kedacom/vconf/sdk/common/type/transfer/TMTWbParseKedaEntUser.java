package com.kedacom.vconf.sdk.common.type.transfer;

/**
 * 用户详情
 * */
public class TMTWbParseKedaEntUser {

	public String achMoid; // 账号moid
	public String achJid; // xmpp账号
	public String achAccount; // 自定义账号
	public String achentMail; // 企业邮箱,微博登录账号
	public String achE164; // E164号
	public String achMobileNum; // 联系电话
	public String achUserDomainMoid; // 企业moid
	public String achUserDomainName; // 企业名
	public String achDeviceGuid; // 终端GUID
	public String achNuServerID; // 所属NU服务器ID
	public String achDeviceType; // 终端类型

	public boolean bEnable; // 禁用/启用，0/1 =>判断是否离职，一般离职之后e164即为空
	public boolean bLimited; // 是否受限
	public String achentName; // 姓名
	public String achBrief; // 一句话介绍, 支持70个汉字
	public String achJobNum; // 用户编号/工号
	public boolean bMale; // 性别(1/0,男/女)
	public String achextNum; // 分机
	public String achSeat; // 座位
	public String achOfficeLocation; // 办公地址
	public String achDateOfBirth; // 出生日期
	public String achFax; // 传真
	public String achRestrictUsedOn; // E164号限制登录的设备类型（列表）
	public String achPortrait32; // 32位头像，实际地址
	public String achPortrait40; // 40位头像，
	public String achPortrait64; // 64位头像，
	public String achPortrait128; // 128位头像，
	public String achPortrait256; // 256位头像，

	public boolean bEnableWeibo; // 微博权限(0/1,没有/有)
	public boolean bEnableMeetingsms; // 发送会管短信权限(0/1,没有/有)
	public boolean bEnableMeeting; // 会议管理权限(0/1,没有/有)
	public boolean bEnableHD; // 高清权限(0/1,没有/有)
	public boolean bEnableCall; // 电话呼叫权限(0/1,没有/有)
	public boolean bEnableRoam; // 漫游权限
	public boolean bEnableSatellite; // 卫星线路权限
	public boolean bEnableSatellitep2p; // 卫星线路点对点会议权限
	public boolean bWeiboAdmin; // 微博管理员权限
	public boolean bMeetingAdmin; // 会议管理员权限
	public boolean bEnableBMC; // BMC权限
	public boolean bEnableUMC; // UMC权限
	public boolean bEnableDCS; // DCS权限
	public boolean bEnableVRS; // VRS权限
	public boolean bEnableNM; // NM权限
	public boolean bEnableVenueMonitor; // 会场监控权限
	public boolean bDefaultUserDomainAdmin; // 默认的用户域管理员
	public boolean bEnableOut; // 出局权限
	public boolean bEnableIncoming; // 入局权限
	public boolean bDCSAdmin; // dcs管理员
	public boolean bVRSAdmin; // vrs管理员
	public boolean bNMAdmin; // nm管理员
	public boolean bEnableVideo; // VRS子权限(录像)
	public boolean bEnableLive; // VRS子权限(直播)
	public boolean bEnablePlay; // VRS子权限(放像)
	public boolean bCMSApproval; // 会管会议审批权限
	public boolean bEditName; // /< 修改姓名权限(0/1,没有/有)
	public boolean bEnableUnicat;    // VRS子权限(点播) 平台V5.2版本才支持
	public boolean bEnableDownLoad; // VRS子权限(下载)

	public TMTWbParseKedaDepts tMtWbParseKedaDepts; // 所在部门
}