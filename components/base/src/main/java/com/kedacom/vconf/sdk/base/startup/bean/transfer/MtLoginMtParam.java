package com.kedacom.vconf.sdk.base.startup.bean.transfer;

public class MtLoginMtParam {
	private EmClientAppType emAppType;
	private EmAuthType emAuthMode;
	private String achUsrName;
	private String achPwd;
	private String achMtIp;
	private long wMtListenPort;

	public MtLoginMtParam(EmClientAppType emAppType, EmAuthType emAuthMode, String achUsrName, String achPwd, String achMtIp, long wMtListenPort) {
		this.emAppType = emAppType;
		this.emAuthMode = emAuthMode;
		this.achUsrName = achUsrName;
		this.achPwd = achPwd;
		this.achMtIp = achMtIp;
		this.wMtListenPort = wMtListenPort;
	}
}