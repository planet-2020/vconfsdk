package com.kedacom.vconf.sdk.base.startup.bean.transfer;

public class TMTApsLoginParam {

	public String achUsername; // 用户名
	public String achPassword; // 密码
	public String achSoftwareVer;
	public String achModelName;
	public String achOemInfo;//OEM name

	public TMTApsLoginParam(String achUsername, String achPassword, String achSoftwareVer, String achModelName, String achOemInfo) {
		this.achUsername = achUsername;
		this.achPassword = achPassword;
		this.achSoftwareVer = achSoftwareVer;
		this.achModelName = achModelName;
		this.achOemInfo = achOemInfo;
	}
}
