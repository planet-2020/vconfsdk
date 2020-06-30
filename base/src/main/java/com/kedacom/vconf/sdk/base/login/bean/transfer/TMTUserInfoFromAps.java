package com.kedacom.vconf.sdk.base.login.bean.transfer;

public class TMTUserInfoFromAps {
	public String achAccount;
	public String achCTime;  //用户创建时间。格式20170908120506(年月日时分秒)
	public String achE164;
	public String achEmail;
	public String achJid;
	public String achMoid;
	public String achVirtualRoomId; // alirtc roomId
	public String achXmppPwd;
	public boolean bIsGuest;	//FALSE表示普通用户 TRUE表示来宾用户
	public int dwRegMode;  	// 1:使用164号注册，2使用m_szMoid注册
}